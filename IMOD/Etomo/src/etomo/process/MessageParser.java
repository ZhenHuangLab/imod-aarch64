package etomo.process;

import java.util.ArrayList;
import java.util.List;

import etomo.BaseManager;
import etomo.process.ProcessMessages.Line;
import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;
import etomo.util.Queue;

/**
 * <p>Description: A parser for process output messages.  Uses tag parsers and passes
 * messages found to ProcessMessages./p>
 * 
 * <p>Copyright: Copyright 2016 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class MessageParser {
  private static final String[] STANDARD_ERROR_TAGS = { "ERROR:", "Errno", "Traceback" };

  private final List<TagInterface> tags = new ArrayList<TagInterface>();

  private final ProcessMessages processMessages;
  private final boolean debug;

  private Queue<Message> multilineMessageQueue = null;
  private TagInterface multilineTag = null;
  private PrependTag prependTag = null;
  // Number of chunk messages on the multi-line message stack
  private boolean chunkMessage = false;
  private boolean finished = false;

  private MessageParser(final ProcessMessages processMessages, final boolean debug) {
    this.processMessages = processMessages;
    this.debug = debug;
  }

  static MessageParser getInstance(final BaseManager manager,
    final ProcessMessages processMessages, final String errorTag,
    final boolean errorTagAlwaysMultiline, final boolean debug) {
    MessageParser instance = new MessageParser(processMessages, debug);
    instance.init(manager, errorTag, errorTagAlwaysMultiline);
    return instance;
  }

  /**
   * When this is on, the parser will leave multi-line messages open after it runs out of
   * strings to parse.  This is essential when parsing string by string, or parsing during
   * a run.  You must call endParse when the parse is done.  No need to call endParse if
   * multiParse was never turned on.  This is unnessary when stringfeed is in use.
   * @param multiParse
   */
  void setMultiParse(final boolean multiParse) {
    finished = !multiParse;
  }

  /**
   * Do not use this with the stringfeed as it calls the parse function.  Use at the end
   * of the parse when multi-parse is in use.  
   * Makes the parser clean up - ending and saving any potental multi-line messages.
   * Call setMultiParse(true) to undo this.
   */
  void endParse() {
    finished = true;
    parse();
  }

  private void init(final BaseManager manager, final String errorTag,
    final boolean errorTagAlwaysMultiline) {
    boolean logAllMessages = processMessages.isLogAllMessages();
    boolean multiLineAllMessages = processMessages.isMultiLineAllMessages();
    // The order in which tags are stored becomes their precedence. One message type is
    // recognized per line. This message type will be the highest precedence message.
    MessageType messageType;
    // pip warning
    // Pip warnings block other message; other messages become part of the pip warning.
    messageType = MessageType.PIP_WARNING_START;
    Tag tag = new EnclosedTag(messageType, messageType.getTag(),
      MessageType.PIP_WARNING_END.getTag(), false, messageType.getListType(), true);
    tags.add(tag);
    // logfile
    // Logfile messages cause the contents of a file to be log (placed in the project log
    // if available).
    messageType = MessageType.LOG_FILE;
    tags.add(
      new Tag(messageType, messageType.getTag(), false, true, ListType.LOGGED, false));
    // warning
    messageType = MessageType.WARNING;
    tag = new Tag(messageType, messageType.getTag(),
      multiLineAllMessages || processMessages.isMultiLineWarning(), false,
      logAllMessages ? ListType.LOGGED : messageType.getListType(), true);
    tags.add(tag);
    // chunk error
    if (processMessages.isChunks()) {
      messageType = MessageType.CHUNK_ERROR;
      tags.add(new EnclosedTag(messageType, messageType.getTag(), "END CHUNK ERROR", true,
        messageType.getListType(), true));
    }
    // info
    messageType = MessageType.INFO;
    tags.add(new Tag(messageType, messageType.getTag(),
      multiLineAllMessages || processMessages.isMultiLineInfo(), false,
      (processMessages.isLogInfoMessages() || logAllMessages) ? ListType.LOGGED
        : messageType.getListType(),
      false));
    // log
    // Log tags are always stripped.
    boolean multiLineLog = processMessages.isAllowMultiLineLog();
    messageType = MessageType.LOG;
    tags.add(new Tag(messageType, messageType.getTag(), multiLineLog, true,
      ListType.LOGGED, false));
    // End of line tags cause a truncation of everything after the tag.
    tags.add(new EolTag(messageType, messageType.getSecondaryTag(), multiLineLog,
      ListType.LOGGED));
    // error
    // Use antitags to avoid messages that are not actually error messages.
    String[] antitags = new String[] { "prnstr('ERROR:", "log.write('ERROR:" };
    // Override log tag errors cannot be logged. Treated the same as other errors. Takes
    // presedence because it most likely contains an error tag - so it must be found
    // first.
    messageType = MessageType.ERROR;
    String errorOverrideLogTag = processMessages.getErrorOverrideLogTag();
    if (errorOverrideLogTag != null) {
      tags.add(new Tag(messageType, errorOverrideLogTag, multiLineAllMessages, false,
        messageType.getListType(), true, antitags));
    }
    // Basic error message.
    tags.add(new Tag(messageType, messageType.getTag(), multiLineAllMessages, false,
      logAllMessages ? ListType.LOGGED : messageType.getListType(), true, antitags));
    // Optional error message.
    if (errorTag != null) {
      tags.add(
        new Tag(messageType, errorTag, multiLineAllMessages || errorTagAlwaysMultiline,
          false, logAllMessages ? ListType.LOGGED : messageType.getListType(), true));
    }
    // Alternative error messages.
    tags.add(new Tag(messageType, STANDARD_ERROR_TAGS[1], multiLineAllMessages, false,
      logAllMessages ? ListType.LOGGED : messageType.getListType(), true));
    tags.add(new Tag(messageType, STANDARD_ERROR_TAGS[2], true, false,
      logAllMessages ? ListType.LOGGED : messageType.getListType(), true));
    // success - flags success - uses one or two tags (in order and not overlapping)
    String successTag1 = processMessages.getSuccessTag1();
    if (successTag1 != null) {
      tags.add(new FlagTag(MessageType.SUCCESS, successTag1,
        processMessages.getSuccessTag2(), ListType.FLAG));
    }
  }

  /**
   * Creates a prepend tag and adds it to tags that require it.  Adds prepend tag to tags
   * list.  Succeeding calls just modify the prepend tag's tag string and remove the
   * current message.
   */
  synchronized void setPrepend(final String prependTagString) {
    if (prependTag == null) {
      if (prependTagString == null) {
        return;
      }
      prependTag = new PrependTag(prependTagString);
      int size = tags.size();
      for (int i = 0; i < size; i++) {
        TagInterface tag = tags.get(i);
        if (tag.takesPrepend()) {
          prependTag.addPrependTaker(tag);
        }
      }
      // Add prepend with lowest precedence.
      tags.add(prependTag);
    }
    if (prependTagString == null) {
      // The prepend tag has been removed.
      prependTag.deleteTag();
    }
    else {
      prependTag.setTag(prependTagString);
    }
  }

  void parse() {
    parse(null);
  }

  /**
   * Store all messages in all available lines.
   */
  synchronized void parse(String header) {
    String line = null;
    // Process each line.
    do {
      line = processMessages.getNextLine();
      // Stop processing and clean up if necessary on a null line.
      if (line == null) {
        break;
      }
      // Ignore the end stringfeed token
      if (line.equals(Line.END_FEED_TOKEN)) {
        continue;
      }
      // Save the interior of a multiline message without parsing, or close a multiline
      // message.
      if (multilineMessageQueue != null && !multilineMessageQueue.isEmpty()
        && multilineTag != null) {
        // Ignore the interior of a multiline message that is closed with an empty line.
        if (!multilineTag.isEnclosed()) {
          // close message
          if (line.isEmpty()) {
            // Store the multiline message
            processMessages.storeMessage(header, multilineMessageQueue, chunkMessage);
            header = null;
            multilineMessageQueue.clear();
            if (multilineTag != null && multilineTag.isChunk()) {
              chunkMessage = false;
            }
            multilineTag = null;
            continue;
          }
        }
        // Search only for the close tag of an enclosed multiline message
        else if (multilineTag.parse(line) && multilineTag.isClosed()) {
          Message message = new Message(multilineTag);
          message.append(multilineTag.getMessageString());
          multilineTag.deleteMessageString();
          multilineTag = null;
          multilineMessageQueue.add(message);
          processMessages.storeMessage(header, multilineMessageQueue, chunkMessage);
          header = null;
          multilineMessageQueue.clear();
          if (message.isChunk()) {
            chunkMessage = false;
          }
          continue;
        }
        // Save the interior line
        Message message = new Message(multilineTag);
        message.append(line);
        multilineMessageQueue.add(message);
        continue;
      }
      if (!line.isEmpty()) {
        int size = tags.size();
        for (int i = 0; i < size; i++) {
          TagInterface tag = tags.get(i);
          if (!tag.parse(line)) {
            // Parse failed - try the next tag
            continue;
          }
          // Do not store a prepend by itself
          if (tag.isPrepend()) {
            break;
          }
          // Start a multiline message
          if (tag.isMultiLine() && tag.isOpen() && !tag.isClosed()) {
            if (multilineMessageQueue == null) {
              multilineMessageQueue = new Queue<Message>();
            }
            multilineTag = tag;
            Message message = new Message(tag);
            message.append(tag.getMessageString());
            tag.deleteMessageString();
            multilineMessageQueue.add(message);
            if (message.isChunk()) {
              chunkMessage = true;
            }
            break;
          }
          // Handle single line message
          if (!tag.isMultiLine() || (tag.isOpen() && tag.isClosed())) {
            processMessages.storeMessage(header, tag, chunkMessage);
            header = null;
            tag.deleteMessageString();
            break;
          }
          // No real message was found
          tag.deleteMessageString();
        }
      }
    } while (line != null);
    // Handle clean up if necessary
    // Assume process output is complete when output ends - except when string feed is
    // in use or the parse is unfinished.
    if (!processMessages.isStringFeed() && finished && multilineMessageQueue != null
      && !multilineMessageQueue.isEmpty()) {
      boolean chunk = multilineMessageQueue.peek().isChunk();
      processMessages.storeMessage(header, multilineMessageQueue, chunkMessage);
      header = null;
      multilineMessageQueue.clear();
      if (chunk) {
        chunkMessage = false;
      }
    }
  }
}
