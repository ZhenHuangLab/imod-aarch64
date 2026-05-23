package etomo.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import etomo.Arguments;
import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.LogFile;
import etomo.ui.swing.UIHarness;
import etomo.util.Queue;

/**
 * <p>Description: Output reading that saves tagged messages.  Designed to be used on the
 * complete log.</p>
 * 
 * <p>Copyright: Copyright 2005 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class ProcessMessages {
  private static final String ALT_ERROR_TAG1 = "Errno";
  private static final String ALT_ERROR_TAG2 = "Traceback";
  private static final String[] ERROR_TAGS =
    { MessageType.ERROR.tag, ALT_ERROR_TAG1, ALT_ERROR_TAG2 };
  private static final int MAX_MESSAGE_SIZE = 10;
  private static final String[] IGNORE_TAG = { "prnstr('ERROR:", "log.write('ERROR:" };
  private static final int STRING_FEED_QUEUE_CAPACITY = 1000;
  private static final boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();

  private final boolean chunks;
  private final BaseManager manager;
  // Send messages to project log instead of storing them.
  private final boolean logAllMessages;
  private final String errorOverrideLogTag;
  private final String[] errorTags;
  private final MessageParser parser;
  private final boolean[] errorTagsAlwaysMultiline;
  private final Line currentLine = new Line();
  private final boolean logInfoMessages;
  private final MessageBuilder messageBuilder = new MessageBuilder();

  private OutputBufferManager outputBufferManager = null;
  private BufferedReader bufferedReader = null;
  private String processOutputString = null;
  private String[] processOutputStringArray = null;
  private LogFile logFile = null;
  private LogFile.ReaderId logFileReaderId = null;
  private int index = -1;
  private RatedList infoList = null;
  private RatedList warningList = null;
  private RatedList errorList = null;
  private RatedList chunkErrorList = null;
  private RatedList chunkWarningList = null;
  private String successTag1 = null;
  private String successTag2 = null;
  private boolean success = false;
  // multi line error, warning, and info strings; terminated by an empty line
  // may be turned off temporarily. Does not affect LOG strings.
  private boolean multiLineAllMessages = false;
  private boolean multiLineWarning = false;// overridden by multiLineAllMessages
  private boolean multiLineInfo = false;// overridden by multiLineAllMessages
  private String messagePrependTag = null;
  private String messagePrepend = null;
  // When set, a worker thread is spawned and nextLine() to wait for strings to be
  // added to the queue. The instance of the queue should be received from
  private ArrayBlockingQueue<String> stringFeed = null;
  private Thread stringFeedThread = null;
  private Object stringFeedLock = new Object();
  private boolean hibernate = false;
  private boolean allowMultiLineLog = false;// Not overridden by multiLineAllMessages
  private boolean debug = false;

  static ProcessMessages getInstance(final BaseManager manager) {
    return new ProcessMessages(manager, false, false, null, null, false, false, false,
      null, null, false, false, true, false);
  }

  static ProcessMessages getInstance(final BaseManager manager,
    final boolean allowMultiLineLog) {
    return new ProcessMessages(manager, false, false, null, null, false, false, false,
      null, null, false, allowMultiLineLog, true, false);
  }

  static ProcessMessages getInstance(final BaseManager manager, final String successTag) {
    return new ProcessMessages(manager, false, true, successTag, null, false, false,
      false, null, null, false, false, true, true);
  }

  static ProcessMessages getInstance(final BaseManager manager, final String successTag1,
    String successTag2) {
    return new ProcessMessages(manager, false, true, successTag1, successTag2, false,
      false, false, null, null, false, false, true, true);
  }

  static ProcessMessages getMultiLineInstance(final BaseManager manager) {
    return new ProcessMessages(manager, true, false, null, null, false, false, false,
      null, null, false, false, true, false);
  }

  static ProcessMessages getMultiLineInstance(final BaseManager manager,
    final boolean allowMultiLineLog) {
    return new ProcessMessages(manager, true, false, null, null, false, false, false,
      null, null, false, allowMultiLineLog, true, false);
  }

  static ProcessMessages getInstanceForParallelProcessing(final BaseManager manager,
    final boolean multiLineMessages) {
    return new ProcessMessages(manager, multiLineMessages, true, null, null, false, false,
      false, null, null, false, false, true, true);
  }

  public static ProcessMessages getLoggedInstance(final BaseManager manager,
    final boolean multiLineMessages, final boolean logAllMessages,
    final String errorOverrideLogTag, final String errorTag,
    final boolean alwaysMultiline, final boolean allowMultiLineLog) {
    return new ProcessMessages(manager, multiLineMessages, false, null, null, false,
      false, logAllMessages, errorOverrideLogTag, errorTag, alwaysMultiline,
      allowMultiLineLog, true, false);
  }

  public static ProcessMessages getBatchruntomoTestInstance(final BaseManager manager,
    final boolean multiLineMessages, final boolean logAllMessages,
    final String errorOverrideLogTag, final String errorTag,
    final boolean alwaysMultiline, final boolean allowMultiLineLog) {
    return new ProcessMessages(manager, multiLineMessages, false, null, null, false,
      false, logAllMessages, errorOverrideLogTag, errorTag, alwaysMultiline,
      allowMultiLineLog, false, false);
  }

  static ProcessMessages getMultiLineInstance(final BaseManager manager,
    final boolean multiLineWarning, final boolean multiLineInfo,
    final boolean logInfoMessages) {
    return new ProcessMessages(manager, false, false, null, null, multiLineWarning,
      multiLineInfo, false, null, null, false, false, logInfoMessages, false);
  }

  private ProcessMessages(final BaseManager manager, final boolean multiLineMessages,
    final boolean chunks, final String successTag1, final String successTag2,
    final boolean multiLineWarning, final boolean multiLineInfo,
    final boolean logAllMessages, final String errorOverrideLogTag, final String errorTag,
    final boolean errorTagAlwaysMultiline, final boolean allowMultiLineLog,
    final boolean logInfoMessages, final boolean debug) {
    this.multiLineAllMessages = multiLineMessages;
    this.multiLineWarning = multiLineWarning;
    this.multiLineInfo = multiLineInfo;
    this.chunks = chunks;
    this.successTag1 = successTag1;
    this.successTag2 = successTag2;
    this.manager = manager;
    this.logAllMessages = logAllMessages;
    this.errorOverrideLogTag = errorOverrideLogTag;
    this.allowMultiLineLog = allowMultiLineLog;
    this.logInfoMessages = logInfoMessages;
    this.debug = debug;
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      parser = null;
    }
    else {
      parser = MessageParser.getInstance(manager, this, errorTag, errorTagAlwaysMultiline,
        debug);
    }
    if (errorTag == null) {
      errorTags = ERROR_TAGS;
      errorTagsAlwaysMultiline = new boolean[] { false, false, true };
    }
    else {
      errorTags =
        new String[] { MessageType.ERROR.tag, errorTag, ALT_ERROR_TAG1, ALT_ERROR_TAG2 };
      errorTagsAlwaysMultiline =
        new boolean[] { false, errorTagAlwaysMultiline, false, true };
    }
  }

  public static void grabIt() {
    Arguments arguments = EtomoDirector.INSTANCE.getArguments();
    List<String> fileNameList = arguments.getParamFileNameList();
    if (fileNameList == null || fileNameList.size() == 0) {
      System.out.println("Missing input file name");
    }
    // Construct a ProcessMessage instance.
    Arguments.GrabItParameter grabItParameter = arguments.getGrabItParameter();
    ProcessMessages processMessages;
    if (grabItParameter == Arguments.GrabItParameter.COPY_TOMO_COMS) {
      // multi-line warning and info
      processMessages = ProcessMessages.getMultiLineInstance(null, true, true, false);
    }
    else if (grabItParameter == Arguments.GrabItParameter.PARALLEL_PROCESSING) {
      // chunks, multi-line messages off
      processMessages = getInstanceForParallelProcessing(null, false);
    }
    else if (grabItParameter == Arguments.GrabItParameter.BATCHRUNTOMO) {
      processMessages = ProcessMessages.getBatchruntomoTestInstance(null, true, false,
        ProcessOutputStrings.BRT_BATCH_RUN_TOMO_ERROR_TAG,
        ProcessOutputStrings.BRT_ABORT_TAG, false, false);
    }
    else {
      processMessages = new ProcessMessages(null, false, false, null, null, false, false,
        false, null, null, false, false, false, false);
    }
    // Process each file.
    Iterator<String> fileNameIterator = fileNameList.iterator();
    while (fileNameIterator.hasNext()) {
      LogFile logFile = new LogFile(new File(fileNameIterator.next()));
      try {
        processMessages.clear();
        processMessages.addProcessOutput(logFile);
        ProcessMessages.ListTypeIterator listTypeIterator =
          processMessages.listTypeIterator();
        System.out.println("\n\n" + logFile.getAbsolutePath() + ":");
        if (!listTypeIterator.hasNext()) {
          System.out.println("No messages.");
        }
        else {
          while (listTypeIterator.hasNext()) {
            ListType listType = listTypeIterator.next();
            System.out.println("\n" + listType + " list:");
            Iterator<String> iterator = processMessages.iterator(listType);
            while (iterator.hasNext()) {
              String message = iterator.next();
              if (message != null && message.matches(".*[\\n\\r\\f]")) {
                System.out.print(message);
              }
              else {
                System.out.println(message);
              }
            }
          }
        }
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      catch (LogFile.LockException e) {
        e.printStackTrace();
      }
    }
  }

  void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public boolean isLogAllMessages() {
    return logAllMessages;
  }

  public boolean isLogInfoMessages() {
    return logInfoMessages;
  }

  public boolean isMultiLineAllMessages() {
    return multiLineAllMessages;
  }

  public boolean isMultiLineWarning() {
    return multiLineWarning;
  }

  public boolean isMultiLineInfo() {
    return multiLineInfo;
  }

  public boolean isAllowMultiLineLog() {
    return allowMultiLineLog;
  }

  public String getErrorOverrideLogTag() {
    return errorOverrideLogTag;
  }

  public String getSuccessTag1() {
    return successTag1;
  }

  public String getSuccessTag2() {
    return successTag2;
  }

  public boolean isChunks() {
    return chunks;
  }

  /**
   * Instance should ignore all input.  Lines sent to instance will be lost.
   */
  void hibernate() {
    hibernate = true;
  }

  /**
   * Instance should stop hibernating and behave normally.
   */
  void wake() {
    hibernate = false;
  }

  public ListTypeIterator listTypeIterator() {
    return new ListTypeIterator();
  }

  public void startStringFeed() {
    synchronized (stringFeedLock) {
      if (stringFeed != null) {
        return;
      }
      stringFeed = new ArrayBlockingQueue<String>(STRING_FEED_QUEUE_CAPACITY);
      // Start string feed thread
      // Run parse() on a separate thread. NextLine will wait for stringFeed when no
      // other input is available.
      stringFeedThread = new Thread(new ParseThread());
      stringFeedThread.start();
    }
  }

  /**
   * Sends a stop token to the string feed and then tells this thread to wait for the
   * string feed thread to complete.  All previously added strings in the string feed will
   * be processed before the string feed thread exits.
   */
  public void stopStringFeed() {
    feedString(Line.END_FEED_TOKEN);
    synchronized (stringFeedLock) {
      if (stringFeedThread == null) {
        return;
      }
    }
    try {
      // Calling thread waits until run thread stops
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        System.err.println("Waiting for stringFeed");
      }
      if (stringFeedThread != null) {
        stringFeedThread.join(1000);
      }
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        System.err.println("StringFeed stopped");
      }
    }
    catch (InterruptedException e) {}
  }

  /**
   * When multilinemessage is set, must send an extra message to get all the messages to
   * be processed, because the parse may be waiting to see if there is more of the last
   * message.
   */
  void feedEndMessage() {
    if (hibernate) {
      return;
    }
    feedString("");
  }

  /**
   * Clear all lists
   */
  public void clear() {
    if (hibernate) {
      return;
    }
    if (infoList != null) {
      infoList.clear();
    }
    if (warningList != null) {
      warningList.clear();
    }
    if (errorList != null) {
      errorList.clear();
    }
    if (chunkErrorList != null) {
      chunkErrorList.clear();
    }
    if (chunkWarningList != null) {
      chunkWarningList.clear();
    }
  }

  public void dumpState() {
    if (EtomoDirector.INSTANCE.isUnitTest() && !DEBUG) {
      return;
    }
    System.err.print("[chunks:" + chunks + ",processOutputString:" + processOutputString
      + ",processOutputStringArray:");
    if (processOutputStringArray != null) {
      System.err.print("{");
      for (int i = 0; i < processOutputStringArray.length; i++) {
        System.err.print(processOutputStringArray[i]);
        if (i < processOutputStringArray.length - 1) {
          System.err.print(",");
        }
      }
      System.err.print("}");
    }
    System.err.print(",index:" + index + ",line:" + currentLine.line + ",infoList:");
    if (infoList != null) {
      System.err.println(infoList.toString());
    }
    System.err.print(",warningList:");
    if (warningList != null) {
      System.err.println(warningList.toString());
    }
    System.err.print(",errorList:");
    if (errorList != null) {
      System.err.println(errorList.toString());
    }
    System.err.print(",chunkErrorList:");
    if (chunkErrorList != null) {
      System.err.println(chunkErrorList.toString());
    }
    System.err.print(",chunkWarningList:");
    if (chunkWarningList != null) {
      System.err.println(chunkWarningList.toString());
    }
    System.err.print(",successTag1:" + successTag1 + ",successTag2:" + successTag2
      + ",\nsuccess:" + success + ",multiLineMessages:" + multiLineAllMessages + "]");
  }

  /**
   * Sends the string to the stringFeed blocking queue, so it can be processed by the
   * worker thread running ParseThread.run.
   * @param string
   */
  void feedString(final String string) {
    if (hibernate) {
      return;
    }
    boolean stringFeedAvailable = false;
    synchronized (stringFeedLock) {
      stringFeedAvailable = stringFeed != null;
    }
    if (stringFeedAvailable) {
      try {
        stringFeed.put(string);
      }
      catch (InterruptedException e) {
        if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
          e.printStackTrace();
        }
      }
      return;
    }
    else {
      // If the string feed isn't operating, treat as a normal process output.
      addProcessOutput(string);
    }
  }

  public boolean isStringFeed() {
    synchronized (stringFeedLock) {
      return stringFeed != null;
    }
  }

  /**
   * Sends an empty message of the type listed in parameter to the stringFeed.
   * For a LOG message, acts as a newline because the LOG tags is removed.
   * @param type
   */
  void feedNewline(final MessageType type) {
    if (hibernate) {
      return;
    }
    if (type == null) {
      feedString("");
    }
    else {
      feedString(type.tag);
    }
  }

  /**
   * Sends a single-line message of the type listed in parameter to the stringFeed.
   * Prepends the type tag to the message if the string does not start with the message
   * type tag.  Adds a blank line after to the message so that it will be treated as
   * single line message.  When the type is LOG, sends the message to the project log with
   * the tag removed.
   * @param type
   */
  void feedMessage(final MessageType type, final String message) {
    if (hibernate) {
      return;
    }
    if (type == null) {
      feedString(message);
    }
    else if (message == null) {
      feedString(type.tag);
    }
    else if (message.startsWith(type.tag)) {
      feedString(message);
    }
    else {
      feedString(type.tag + " " + message);
    }
    feedString("");
  }

  /**
   * While messagePrependTag is set, the most recent line containing the tag will be
   * saved to messagePrepend and added to the beginning of the next error/warning message.
   * MessagePrepend will be deleted after it is used.  If no error or warning message
   * appears, messagePrepend will not be used and the tag will have no effect.  All other
   * types of message tags take precedence over this tag.  Passing null to this function
   * will cause both messagePrependTag and messagePrepend to be set to null.
   * @param tag
   */
  void setMessagePrependTag(final String tag) {
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      messagePrependTag = tag;
      if (messagePrependTag == null) {
        messagePrepend = null;
      }
    }
    else {
      parser.setPrepend(messagePrependTag);
    }
  }

  /**
   * Set processOutput to outputBufferManager and parse it.
   * Function must be synchronized because it relies on member variables to
   * parse processOutput.
   * @param processOutput: OutputBufferManager
   */
  synchronized void addProcessOutput(final OutputBufferManager processOutput) {
    if (hibernate) {
      return;
    }
    outputBufferManager = processOutput;
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      nextLine();
      while (outputBufferManager != null) {
        parse();
      }
    }
    else {
      parser.parse();
    }
  }

  /**
   * Set this when sending single strings or parsing during a run.  When multi-parse is in
   * use, you must call endParse when all the output has been parsed.  Unnecessary when
   * the string feed is in use.  This was added because the string feed is unreliable when
   * used with the reconstruction processes and monitors.  Only available when
   * EtomoDirector.NEW_MESSAGE_PARSER on.
   * @param multiParse
   */
  void setMultiParse(final boolean multiParse) {
    // Not adding new functionality to the old version of the parser
    if (EtomoDirector.NEW_MESSAGE_PARSER) {
      parser.setMultiParse(multiParse);
    }
  }

  /**
   * Must be called after strings are parsed when multi-parse is on.  Do not use when the
   * string feed is in use.  Only available when EtomoDirector.NEW_MESSAGE_PARSER on.
   */
  void endParse() {
    // Not adding new functionality to the old version of the parser
    if (EtomoDirector.NEW_MESSAGE_PARSER) {
      parser.endParse();
    }
  }

  /**
   * Set processOutput to processOutputStringArray and parse it.
   * Function must be synchronized because it relies on member variables to
   * parse processOutput.
   * @param processOutput: String[]
   */
  synchronized void addProcessOutput(final String header, final String[] processOutput) {
    if (hibernate) {
      return;
    }
    processOutputStringArray = processOutput;
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      nextLine();
      while (processOutputStringArray != null) {
        // Old version - not adding new functionality - header not added
        parse();
      }
    }
    else {
      parser.parse(header);
    }
  }

  /**
   * Set processOutput to bufferedReader and parse it.
   * Function must be synchronized because it relies on member variables to
   * parse processOutput.
   * @param processOutput: File
   * @throws FileNotFoundException
   */
  synchronized void addProcessOutput(final File processOutput)
    throws FileNotFoundException {
    if (hibernate) {
      return;
    }
    // Open the file as a stream
    InputStream fileStream = new FileInputStream(processOutput);
    bufferedReader = new BufferedReader(new InputStreamReader(fileStream));
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      nextLine();
      while (bufferedReader != null) {
        parse();
      }
    }
    else {
      parser.parse();
    }
  }

  synchronized void addProcessOutput(final LogFile processOutput)
    throws LogFile.LockException, FileNotFoundException {
    if (hibernate) {
      return;
    }
    // Open the log file
    logFile = processOutput;
    logFileReaderId = logFile.openReader();
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      nextLine();
      while (logFile != null) {
        parse();
      }
    }
    else {
      parser.parse();
    }
  }

  /**
   * Set processOutput to processOutputString and parse it.
   * Function must be synchronized because it relies on member variables to
   * parse processOutput.  For the old parser it temporarily turns off multi-line messages.
   * 
   * When using without string feed, and with the new parser, call setMultiParse(true)
   * before any calls to this function, and endParse after all parsing is done.
   * @see EtomoDirector.NEW_MESSAGE_PARSER
   * @param processOutput: String
   */
  synchronized void addProcessOutput(final String processOutput) {
    if (hibernate) {
      return;
    }
    // Open the file as a stream
    processOutputString = processOutput;
    if (!EtomoDirector.NEW_MESSAGE_PARSER) {
      boolean oldMultiLineMessages = multiLineAllMessages;
      multiLineAllMessages = false;
      boolean oldMultiLineWarning = multiLineWarning;
      multiLineWarning = false;
      boolean oldMultiLineInfo = multiLineInfo;
      multiLineInfo = false;
      // processOutput may be broken up into multiple lines
      if (nextLine()) {
        parse();
      }
      multiLineAllMessages = oldMultiLineMessages;
      multiLineWarning = oldMultiLineWarning;
      multiLineInfo = oldMultiLineInfo;
    }
    else {
      parser.parse();
    }
  }

  synchronized void add(final ProcessMessages processMessages) {
    if (hibernate) {
      return;
    }
    if (processMessages == null) {
      return;
    }
    add(MessageType.ERROR, processMessages.errorList);
    add(MessageType.WARNING, processMessages.warningList);
    add(MessageType.INFO, processMessages.infoList);
    add(MessageType.CHUNK_ERROR, processMessages.chunkErrorList);
    add(MessageType.CHUNK_WARNING, processMessages.chunkWarningList);
  }

  synchronized void add(final MessageType type, final String header,
    final ProcessMessages processMessages) {
    messageBuilder.add(type, header, processMessages);
  }

  synchronized void add(final MessageType type, final MessageType fromType,
    final String header, final ProcessMessages processMessages) {
    messageBuilder.add(type, fromType, header, processMessages);
  }

  synchronized void add(final MessageType type, final RatedList input) {
    messageBuilder.add(type, null, input != null ? input.getList() : null);
  }

  public synchronized void storeMessage(final String header,
    final Queue<Message> messageQueue, final boolean chunkMessage) {
    messageBuilder.add(header, messageQueue, chunkMessage, false);
  }

  public synchronized void storeMessage(final String header, final TagInterface tag,
    final boolean chunkMessage) {
    messageBuilder.add(header, tag, chunkMessage, false);
  }

  public synchronized void storeMessageOLD(final Message message,
    final boolean chunkMessage) {
    if (message == null) {
      return;
    }
    storeMessageOLD(message.getMessageType(), message.getListType(),
      message.getMessageString(), chunkMessage);
  }

  public synchronized void storeMessageOLD(final TagInterface tag,
    final boolean chunkMessage) {
    if (tag == null) {
      return;
    }
    storeMessageOLD(tag.getMessageType(), tag.getListType(), tag.getMessageString(),
      chunkMessage);
  }

  private synchronized void storeMessageOLD(final MessageType messageType,
    final ListType listType, final String message, final boolean chunkMessage) {
    if (hibernate) {
      return;
    }
    if (messageType == MessageType.SUCCESS) {
      success = true;
      return;
    }
    if (listType == null || message == null || message.isEmpty()) {
      return;
    }
    if (!chunkMessage && listType == ListType.ERROR) {
      if (errorList == null) {
        errorList = new RatedList();
      }
      errorList.add(message);
    }
    else if ((chunkMessage && listType == ListType.ERROR)
      || listType == ListType.CHUNK_ERROR) {
      if (chunkErrorList == null) {
        chunkErrorList = new RatedList();
      }
      else if (chunkErrorList.contains(message)) {
        // Don't save duplicate chunk errors
        return;
      }
      chunkErrorList.add(message);
    }
    else if (!chunkMessage && listType == ListType.WARNING) {
      if (warningList == null) {
        warningList = new RatedList();
      }
      warningList.add(message);
    }
    else if ((chunkMessage && listType == ListType.WARNING)
      || listType == ListType.CHUNK_WARNING) {
      if (chunkWarningList == null) {
        chunkWarningList = new RatedList();
      }
      else if (chunkWarningList.contains(message)) {
        // Don't save duplicate chunk warnings
        return;
      }
      chunkWarningList.add(message);
    }
    else if (listType == ListType.INFO) {
      if (infoList == null) {
        infoList = new RatedList();
      }
      infoList.add(message);
    }
    else if (listType == ListType.LOGGED) {
      if (messageType == MessageType.LOG_FILE) {
        File file = new File(manager.getPropertyUserDir(), message);
        if (file.exists() && file.isFile() && file.canRead()) {
          if (manager != null) {
            manager.logSimpleMessage(file);
          }
          else if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
            System.err.println("Messages logged in " + file.getAbsolutePath());
          }
        }
        else if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
          System.err
            .println("Warning: unable to log from file:" + file.getAbsolutePath());
        }
      }
      else if (manager != null) {
        manager.logSimpleMessage(message);
      }
      else if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        System.err.println(message);
      }
    }
  }

  synchronized void add(final MessageType type, final String input) {
    messageBuilder.add(type, input);
  }

  /**
   * Add empty message
   * @param type
   */
  synchronized void add(final MessageType type) {
    messageBuilder.add(type);
  }

  synchronized void add(final MessageType type, final String header,
    final String[] input) {
    messageBuilder.add(type, header, input);
  }

  public int size(final MessageType type) {
    RatedList list = getList(type, false);
    if (list == null) {
      return 0;
    }
    return list.size();
  }

  public String get(final MessageType type, final int index) {
    RatedList list;
    if (index >= 0 && (list = getList(type, false)) != null && index < list.size()) {
      return list.get(index);
    }
    return null;
  }

  /**
   * Returns messages which contain matchString.
   * @param matchString
   * @return
   */
  public ArrayList<String> match(final MessageType type,
    final String[] matchStringArray) {
    if (isEmpty(type)) {
      return null;
    }
    ArrayList<String> matches = new ArrayList<String>();
    RatedList list = getList(type, false);
    if (list != null) {
      Iterator<String> i = list.iterator();
      while (i.hasNext()) {
        String message = i.next();
        for (int j = 0; j < matchStringArray.length; j++) {
          if (message.indexOf(matchStringArray[j]) != -1) {
            matches.add(message);
            break;
          }
        }
      }
      if (matches.size() != 0) {
        return matches;
      }
    }
    return null;
  }

  /**
   * @deprecated - 11/28/16
   * @param type
   * @return
   */
  public String getLast(final MessageType type) {
    if (type == null) {
      return null;
    }
    RatedList list = getList(type, false);
    if (list == null || list.size() == 0) {
      return null;
    }
    return list.get(list.size() - 1);
  }

  final void print() {
    print(MessageType.ERROR);
    print(MessageType.WARNING);
    print(MessageType.INFO);
  }

  public boolean isEmpty(final MessageType type) {
    if (type == null) {
      return false;
    }
    RatedList list = getList(type, false);
    return list == null || list.isEmpty();
  }

  boolean isSuccess() {
    return success;
  }

  public void print(final MessageType type) {
    if (type == null) {
      return;
    }
    RatedList list = getList(type, false);
    if (list == null) {
      return;
    }
    if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
      for (int i = 0; i < list.size(); i++) {
        System.err.println(list.get(i));
      }
    }
  }

  public void print(final ListType type) {
    if (type == null) {
      return;
    }
    RatedList list = getList(type, false);
    if (list == null) {
      return;
    }
    if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
      for (int i = 0; i < list.size(); i++) {
        System.err.println(list.get(i));
      }
    }
  }

  /**
   * Look for a message or a success tag.  Return when something is found or
   * when there is nothing left to look for.  Causes nextLine to be called after the
   * message has been processed.
   */
  private void parse() {
    if (parsePipWarning()) {
      return;
    }
    if (multiLineAllMessages) {
      if (parseMultiLineMessage()) {
        return;
      }
    }
    else if (parseSingleLineMessage()) {
      return;
    }
    // No message has been found on this line, so check it for success tags
    if (parseSuccessLine()) {
      return;
    }
    parseMessagePrepend();
    // parse functions go to the next line only when they find something
    // if all parse functions failed, call nextLine()
    nextLine();
  }

  /**
   * Sets messagePrepend to the whole line.  Never called nextLine.
   */
  private void parseMessagePrepend() {
    if (currentLine.messageType == MessageType.PREPEND) {
      messagePrepend = currentLine.line;
    }
  }

  /**
   * Sets success if success tags found.
   * @return true if success found
   */
  private boolean parseSuccessLine() {
    if (currentLine.messageType == MessageType.SUCCESS) {
      nextLine();
      success = true;
      return true;
    }
    return false;
  }

  /**
   * Looks for pip warnings and adds them to infoList.
   * Pip warnings are multi-line and have a start and end tag.  They may start
   * and end in the middle of a line.
   * Should be run before parseMultiLineMessage or parseSingleListMessage.
   * If a pip warning is found, line will be set to a new line.
   * @return true if pip warning is found
   */
  private boolean parsePipWarning() {
    if (currentLine.messageType != MessageType.PIP_WARNING_START) {
      return false;
    }
    // create message starting at pip warning tag.
    StringBuffer pipWarning = new StringBuffer();
    if (messagePrepend != null) {
      pipWarning.append(messagePrepend + "\n");
      messagePrepend = null;
    }
    pipWarning.append(currentLine.getMessage());
    // check for a multiline pip warning.
    if (currentLine.endMessageType != MessageType.PIP_WARNING_END) {
      boolean moreLines = nextLine();
      while (moreLines) {
        // Everything within start and end is part of message
        pipWarning.append(" " + currentLine.getMessage());
        if (currentLine.messageType == MessageType.PIP_WARNING_END) {
          // Found end tag
          moreLines = false;
        }
        else {
          // Have not found end tag - list line is part of PIP warning
          moreLines = nextLine();
        }
      }
    }
    // add pip warning to infoList
    add(MessageType.PIP_WARNING_START, pipWarning.toString());
    return true;
  }

  /**
   * Looks for single line errors, warnings, and info messages.
   * Messages have start tags and may start in the middle of the line.
   * If a message is found, line will be set to a new line.
   * Looks for a log file entry.  A log file entry is a single line.  The tag may have
   * text preceeding it.  The text following the tag is the path of a file, the contents
   * of which should be added to _project.log.
   * @return true if a message is found
   */
  private boolean parseSingleLineMessage() {
    if (currentLine.messageType == null || currentLine.messageType.exclusive) {
      return false;
    }
    if (currentLine.messageType == MessageType.LOG_FILE) {
      File file = new File(manager.getPropertyUserDir(), currentLine.getMessage());
      if (file.exists() && file.isFile() && file.canRead()) {
        if (manager != null) {
          manager.logSimpleMessage(file);
        }
        else {
          if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
            System.err
              .println(MessageType.LOG_FILE.getTag() + " " + file.getAbsolutePath());
          }
          try {
            LogFile logFile = LogFile.getInstance(file);
            LogFile.ReaderId id = logFile.openReader();
            String logFileLine = null;
            if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
              while ((logFileLine = logFile.readLine(id)) != null) {
                System.err.println(logFileLine);
              }
            }
          }
          catch (LogFile.LockException e) {
            if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
              e.printStackTrace();
              System.err.println(
                "Unable to process " + currentLine.line + ".  " + e.getMessage());
            }
          }
          catch (FileNotFoundException e) {
            if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
              e.printStackTrace();
              System.err.println(
                "Unable to process " + currentLine.line + ".  " + e.getMessage());
            }
          }
          catch (IOException e) {
            if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
              e.printStackTrace();
              System.err.println(
                "Unable to process " + currentLine.line + ".  " + e.getMessage());
            }
          }
        }
      }
      else if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        System.err.println("Warning: unable to log from file:" + file.getAbsolutePath());
      }
      return true;
    }
    if (currentLine.alwaysMultiLine
      || (currentLine.messageType == MessageType.WARNING && multiLineWarning)
      || (currentLine.messageType == MessageType.INFO && multiLineInfo)
      || (currentLine.messageType == MessageType.LOG && allowMultiLineLog)) {
      return parseMultiLineMessage();
    }
    if (currentLine.messageType == MessageType.LOG) {
      // Log messages are not saved
      manager.logSimpleMessage(currentLine.getMessage());
      nextLine();
      return true;
    }
    // message found - add to list
    StringBuilder builder = new StringBuilder();
    if (messagePrepend != null) {
      builder.append(messagePrepend + "\n");
      messagePrepend = null;
    }
    builder.append(currentLine.getMessage());
    add(currentLine.messageType, builder.toString());
    nextLine();
    return true;
  }

  public static int getErrorIndex(String line) {
    int index = -1;
    for (int j = 0; j < ProcessMessages.ERROR_TAGS.length; j++) {
      index = line.indexOf(ProcessMessages.ERROR_TAGS[j]);
      if (index != -1) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Looks for multi-line errors, warnings, and info messages.
   * Messages have start tags and may start in the middle of the line.
   * Messages end with an empty line.
   * If a message is found, currentLine will be set to the next line.
   * @return true if a message is found
   */
  private boolean parseMultiLineMessage() {
    if (currentLine.messageType == MessageType.LOG_FILE) {
      return parseSingleLineMessage();
    }
    if (currentLine.messageType == null || currentLine.messageType.exclusive) {
      return false;
    }
    // create the message starting from the message tag
    StringBuffer messageBuffer = new StringBuffer();
    if (messagePrepend != null) {
      messageBuffer.append(messagePrepend + "\n");
      messagePrepend = null;
    }
    messageBuffer.append(currentLine.getMessage());
    MessageType messageType = currentLine.messageType;
    // Handling old functionality:
    // Originally, multi-line error messages where handled by putting the same tag on each
    // line. Handle this by continuing the message if the tag remains the same, and
    // starting a new message if the tag changes.
    int messageTagIndex = currentLine.tagIndex;
    // Look for the rest of the multi-line message
    if (messageType != MessageType.LOG || allowMultiLineLog) {
      // Get next currentLine
      boolean moreLines = nextLine();
      int count = 0;
      while (moreLines) {
        // Multiline messages can contain multiple tagless lines. They end with a blank
        // line, or a different message tag. They also have a size limit.
        if (currentLine.isEmpty()
          || (currentLine.messageType != null
            && !currentLine.equals(messageType, messageTagIndex))
          || count > MAX_MESSAGE_SIZE) {
          // End current message.
          // Add completed message in a list
          messageBuffer.append("\n");
          String message = messageBuffer.toString();
          add(messageType, message);
          return true;
        }
        else {
          // Continue current message.
          messageBuffer.append("\n" + currentLine.line);
          moreLines = nextLine();
          count++;
        }
      }
    }
    else {
      // Handle a single line message
      nextLine();
    }
    // No more lines - process the message
    if (messageType == MessageType.LOG) {
      // Log messages are not saved
      manager.logSimpleMessage(messageBuffer.toString());
    }
    else {
      // add message to list
      messageBuffer.append("\n");
      add(messageType, messageBuffer.toString());
    }
    return true;
  }

  /**
   * Uses nextLine to return the next output line.
   * @return
   */
  public String getNextLine() {
    if (nextLine()) {
      return currentLine.line;
    }
    return null;
  }

  /**
   * Figure out which type of process output is being read and call the
   * corresponding nextLine function.
   * When stringFeed is in use, waits for stringFeed to contain something and returns
   * true unless one of the ending tokens are found.
   * @return
   */
  private boolean nextLine() {
    // A message with an end tag ends before the end of the line. In this case process
    // the rest of the line.
    if (!currentLine.isDone()) {
      if (currentLine.nextMessage()) {
        return true;
      }
    }
    // String feed thread - do NOT wait for a string, except on the string feed thread.
    if (stringFeed != null && Thread.currentThread().equals(stringFeedThread)) {
      // This is the string feed thread - wait for a string
      boolean takeSucceeded = false;
      while (!takeSucceeded) {
        try {
          currentLine.load(stringFeed.take());
          if (currentLine.isEndFeed()) {
            return false;
          }
          takeSucceeded = true;
          return true;
        }
        catch (InterruptedException e) {
          if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
            e.printStackTrace();
          }
        }
      }
    }
    boolean retval = false;
    if (outputBufferManager != null) {
      retval = nextOutputBufferManagerLine();
    }
    else if (bufferedReader != null) {
      retval = nextBufferedReaderLine();
    }
    else if (logFile != null) {
      retval = nextLogFileLine();
    }
    else if (processOutputString != null) {
      currentLine.load(processOutputString);
      processOutputString = null;
      retval = true;
    }
    else if (processOutputStringArray != null) {
      retval = nextStringArrayLine();
    }
    return retval;
  }

  /**
   * Increment index and place the entry at index in outputBufferManager into line.
   * Trim line.
   * Return true if line can be set to a new line
   * Return false, sets outputBufferManager and line to null, and sets index to
   * -1 when there is nothing left in outputBufferManager.
   */
  private boolean nextOutputBufferManagerLine() {
    index++;
    if (index >= outputBufferManager.size()) {
      index = -1;
      outputBufferManager = null;
      currentLine.reset();
      return false;
    }
    currentLine.load(outputBufferManager.get(index).trim());
    return true;
  }

  /**
   * Increment index and place the entry at index in processOutputStringArray into line.
   * Trim line.
   * Return true if line can be set to a new line
   * Return false, sets processOutputStringArray and line to null, and sets index to
   * -1 when there is nothing left in processOutputStringArray.
   */
  private boolean nextStringArrayLine() {
    index++;
    if (index >= processOutputStringArray.length) {
      index = -1;
      processOutputStringArray = null;
      currentLine.reset();
      return false;
    }
    currentLine.load(processOutputStringArray[index].trim());
    return true;
  }

  /**
   * Increment index and place the entry at index in bufferedReader into line.
   * Trim line.
   * Return true if line can be set to a new line
   * Return false, and sets bufferedReader and line to null when there is
   * nothing left in bufferedReader.
   * Does not change index.
   */
  private boolean nextBufferedReaderLine() {
    try {
      String line;
      if ((line = bufferedReader.readLine()) == null) {
        currentLine.reset();
        bufferedReader = null;
        return false;
      }
      else {
        currentLine.load(line);
      }
    }
    catch (IOException e) {
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        e.printStackTrace();
      }
      bufferedReader = null;
      currentLine.reset();
      return false;
    }
    return true;
  }

  private boolean nextLogFileLine() {
    try {
      String line;
      if ((line = logFile.readLine(logFileReaderId)) == null) {
        currentLine.reset();
        logFile.closeRead(logFileReaderId);
        logFile = null;
        logFileReaderId = null;
        return false;
      }
      else {
        currentLine.load(line);
      }
    }
    catch (LogFile.LockException e) {
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        e.printStackTrace();
      }
      logFile.closeRead(logFileReaderId);
      logFile = null;
      logFileReaderId = null;
      currentLine.reset();
      return false;
    }
    catch (IOException e) {
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        e.printStackTrace();
      }
      logFile.closeRead(logFileReaderId);
      logFile = null;
      logFileReaderId = null;
      currentLine.reset();
      return false;
    }
    return true;
  }

  private RatedList getList(final MessageType type, final boolean create) {
    if (type != null) {
      return getList(type.listType, create);
    }
    return null;
  }

  public Iterator<String> iterator(final ListType listType) {
    RatedList list = getList(listType, false);
    if (list != null) {
      return list.iterator();
    }
    return null;
  }

  private RatedList getList(final ListType type, final boolean create) {
    if (type == ListType.CHUNK_ERROR) {
      if (create && chunkErrorList == null) {
        chunkErrorList = new RatedList();
      }
      return chunkErrorList;
    }
    if (type == ListType.CHUNK_WARNING) {
      if (create && chunkWarningList == null) {
        chunkWarningList = new RatedList();
      }
      return chunkWarningList;
    }
    if (type == ListType.ERROR) {
      if (create && errorList == null) {
        errorList = new RatedList();
      }
      return errorList;
    }
    if (type == ListType.INFO) {
      if (create && infoList == null) {
        infoList = new RatedList();
      }
      return infoList;
    }
    if (type == ListType.WARNING) {
      if (create && warningList == null) {
        warningList = new RatedList();
      }
      return warningList;
    }
    return null;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    if (infoList != null) {
      for (int i = 0; i < infoList.size(); i++) {
        buffer.append(infoList.get(i) + "\n");
      }
    }
    if (warningList != null) {
      for (int i = 0; i < warningList.size(); i++) {
        buffer.append(warningList.get(i) + "\n");
      }
    }
    if (errorList != null) {
      for (int i = 0; i < errorList.size(); i++) {
        buffer.append(errorList.get(i) + "\n");
      }
    }
    if (chunkErrorList != null) {
      for (int i = 0; i < chunkErrorList.size(); i++) {
        buffer.append(chunkErrorList.get(i) + "\n");
      }
    }
    if (chunkWarningList != null) {
      for (int i = 0; i < chunkWarningList.size(); i++) {
        buffer.append(chunkWarningList.get(i) + "\n");
      }
    }
    return buffer.toString();
  }

  private final class ParseThread implements Runnable {
    /**
     * Gets lines from stringFeed (must be set up with startStringFeed).  Stops when the
     * END_STRING_FEED_TOKEN is received. Clears the string feed queue when it ends.
     */
    public void run() {
      if (!EtomoDirector.NEW_MESSAGE_PARSER) {
        nextLine();
      }
      while (!currentLine.isEndFeed()) {
        if (!EtomoDirector.NEW_MESSAGE_PARSER) {
          parse();
        }
        else {
          parser.parse();
        }
      }
      synchronized (stringFeedLock) {
        stringFeed = null;
        stringFeedThread = null;
      }
    }
  }

  static final class ListType {
    static final ListType CHUNK_ERROR = new ListType("CHUNK_ERROR", true);
    static final ListType ERROR = new ListType("ERROR");
    static final ListType INFO = new ListType("INFO");
    static final ListType WARNING = new ListType("WARNING");
    static final ListType LOGGED = new ListType("LOGGED");
    static final ListType FLAG = new ListType("FLAG");
    static final ListType CHUNK_WARNING = new ListType("CHUNK_WARNING", true);

    private final String name;
    private final boolean chunk;

    private ListType(final String name) {
      this.name = name;
      chunk = false;
    }

    public String toString() {
      return name;
    }

    private ListType(final String name, final boolean chunk) {
      this.name = name;
      this.chunk = chunk;
    }

    boolean isChunk() {
      return chunk;
    }
  }

  public static final class MessageType {
    static final MessageType CHUNK_ERROR =
      new MessageType("CHUNK ERROR:", ListType.CHUNK_ERROR, false);
    public static final MessageType ERROR =
      new MessageType("ERROR:", ListType.ERROR, false);
    public static final MessageType INFO = new MessageType("INFO:", ListType.INFO, false);
    public static final MessageType WARNING =
      new MessageType("WARNING:", ListType.WARNING, false);
    // Always goes to the project log
    public static final MessageType LOG = new MessageType(UIHarness.LOG_TAG + ":",
      "[:" + UIHarness.LOG_TAG + "]", null, false);
    // Followed by the name of a file
    static final MessageType LOG_FILE = new MessageType("LOGFILE:", null, false);
    static final MessageType PIP_WARNING_START =
      new MessageType("PIP WARNING:", ListType.INFO, true);
    static final MessageType PIP_WARNING_END =
      new MessageType("Using fallback options in main program", ListType.INFO, true);
    static final MessageType SUCCESS = new MessageType(null, null, true);
    static final MessageType PREPEND = new MessageType(null, null, true);
    static final MessageType CHUNK_WARNING =
      new MessageType(null, ListType.CHUNK_WARNING, false);

    private final String tag;
    private final String secondaryTag;
    private final ListType listType;
    private final boolean exclusive;// parsed separately

    private MessageType(final String tag, final ListType listType,
      final boolean exclusive) {
      this.tag = tag;
      this.listType = listType;
      this.exclusive = exclusive;
      secondaryTag = null;
    }

    private MessageType(final String tag, final String secondaryTag,
      final ListType listType, final boolean exclusive) {
      this.tag = tag;
      this.secondaryTag = secondaryTag;
      this.listType = listType;
      this.exclusive = exclusive;
    }

    public String getTag() {
      return tag;
    }

    String getSecondaryTag() {
      return secondaryTag;
    }

    public String toString() {
      return tag;
    }

    ListType getListType() {
      return listType;
    }

    /**
     * Returns true if tag or secondary tag is in string
     * @param string
     * @return
     */
    public boolean isType(final String string) {
      return string != null
        && ((secondaryTag != null && string.indexOf(secondaryTag) != -1)
          || (tag != null && string.indexOf(tag) != -1));
    }
  }

  static final class ProcessMessagesTestHarness {
    ProcessMessagesTestHarness() {}

    /**
     * @return null if succeeded, or error message
     */
    String testSubLine() {
      String tag = "Tag:";
      String origTag = "First " + tag;
      String notTheTag = "hello:";
      SubLine subLine = SubLine.getInstance(
        origTag + " blah1 " + tag + notTheTag + " " + tag + " blah2" + tag + "blah3 ", 0,
        origTag, tag);
      String expected = tag + notTheTag + "\n" + tag + " blah2\n" + tag + "blah3";
      String actual = subLine.getMessage();
      if (!expected.equals(actual)) {
        return "ProcessMessagesTestHarness.assertSubLine failed:each subLine returns its "
          + "portion of the line (trimmed),and inserts a newline "
          + "before the next portion.";
      }
      return null;
    }

    /**
     * @return null if succeeded, or error message
     */
    String testSecondaryLogTag() {
      ProcessMessages processMessages = ProcessMessages.getInstance(null);
      String msg = "This message should be logged.";
      processMessages.currentLine.load(msg + "[:LOG]");
      if (processMessages.currentLine.messageType != MessageType.LOG) {
        return "ending LOG tag was not recognized, processMessages.currentLine.messageType:"
          + processMessages.currentLine.messageType;
      }
      if (processMessages.currentLine.equals(msg)) {
        return "ending LOG tag should be stripped";
      }
      return null;
    }
  }

  /**
   * Class to handle a line with further tags.  Only one tag string can be checked.
   * Does not strip tags.  Doesn't handle start and end tags.
   */
  private static final class SubLine {
    private final String line;
    private final int startIndex;
    private final int endIndex;
    private final SubLine next;

    private SubLine(final String line, final int startIndex, final String tag) {
      this.line = line;
      this.startIndex = startIndex;
      next = getInstance(line, startIndex, tag, tag);
      if (next != null) {
        endIndex = next.startIndex;
      }
      else {
        endIndex = -1;
      }
    }

    /**
     * Returns an instance of SubLine if tag is found after startIndex+lastTag-length.
     * @param line
     * @param startIndex
     * @param messageType
     * @param subTag
     * @return
     */
    private static SubLine getInstance(final String line, final int lastIndex,
      final String lastTag, final String tag) {
      if (line == null || lastTag == null || tag == null) {
        return null;
      }
      int index = line.indexOf(tag, lastIndex + lastTag.length());
      if (index != -1) {
        return new SubLine(line, index, tag);
      }
      return null;
    }

    private String getMessage() {
      return substring() + (next == null ? "" : "\n" + next.getMessage());
    }

    private String substring() {
      int length = line.length();
      if (startIndex >= length
        || (startIndex >= 0 && endIndex >= 0 && startIndex >= endIndex)) {
        // Empty string
        return "";
      }
      boolean useStartIndex = startIndex > 0;
      boolean useEndIndex = endIndex >= 0 && endIndex < length;
      if (line == null || line.isEmpty()) {
        return line;
      }
      String substring;
      if (useStartIndex || useEndIndex) {
        if (useStartIndex && useEndIndex) {
          substring = line.substring(startIndex, endIndex);
        }
        else if (useStartIndex) {
          substring = line.substring(startIndex);
        }
        else {
          substring = line.substring(0, endIndex);
        }
      }
      else {
        substring = line;
      }
      return substring.trim();
    }
  }

  final class Line {
    static final String END_FEED_TOKEN =
      "This the END of the String Feed!!!  239asdlkjgsafT$LSFJsGHW($(gjhaehgpasjdhf0w235";

    private String line = null;
    private MessageType messageType = null;
    private MessageType endMessageType = null;
    private int startIndex = -1;
    private int endIndex = -1;
    // for list types with arrays of tags. Refers to the tag array that existed when
    // load() was run.
    private int tagIndex = -1;
    private boolean alwaysMultiLine = false;
    private SubLine subLine = null;
    private boolean truncateLine = false;

    private Line() {}

    public String toString() {
      return "[messageType:" + messageType + ",startIndex:" + startIndex + ",tagIndex:"
        + tagIndex + "\n" + line;
    }

    private void reset() {
      line = null;
      messageType = null;
      endMessageType = null;
      startIndex = -1;
      endIndex = -1;
      tagIndex = -1;
      subLine = null;
      truncateLine = false;
    }

    /**
     * Preferred way to retrieve the line.  Line is modified based on the message type.
     * @return
     */
    private String getMessage() {
      return substring() + (subLine == null ? "" : "\n" + subLine.getMessage());
    }

    private String substring() {
      int length = line.length();
      if (startIndex >= length
        || (startIndex >= 0 && endIndex >= 0 && startIndex >= endIndex)) {
        // Empty string
        return "";
      }
      boolean useStartIndex = startIndex > 0;
      boolean useEndIndex = endIndex >= 0 && endIndex < length;
      if (line == null || line.isEmpty()) {
        return line;
      }
      String substring;
      if (useStartIndex || useEndIndex) {
        if (useStartIndex && useEndIndex) {
          substring = line.substring(startIndex, endIndex);
        }
        else if (useStartIndex) {
          substring = line.substring(startIndex);
        }
        else {
          substring = line.substring(0, endIndex);
        }
      }
      else {
        substring = line;
      }
      return substring.trim();
    }

    /**
     * Done if there is no end index, or it goes to the end of the line.  Assumes that no
     * end index tag gets stripped.
     * @return
     */
    private boolean isDone() {
      return line == null || truncateLine || endIndex < 0 || endIndex >= line.length()
        || subLine != null;
    }

    private boolean isEndFeed() {
      return line != null && line.equals(END_FEED_TOKEN);
    }

    /**
     * Returns true if the line was not done, and load() was called on the part of the
     * line after the end tag.
     * @return
     */
    private boolean nextMessage() {
      if (isDone()) {
        return false;
      }
      load(line.substring(endIndex));
      return true;
    }

    private boolean isEmpty() {
      return line == null || line.isEmpty();
    }

    /**
     * Returns true if the line contains the same tag described by the parameters.
     * @param inputListType
     * @param inputTagIndex - only used for ERROR list types
     * @return
     */
    public boolean equals(final MessageType inputMessageType, final int inputTagIndex) {
      if (inputMessageType == null && messageType == null) {
        return true;
      }
      if (inputMessageType == messageType) {
        if (messageType != MessageType.ERROR) {
          return true;
        }
        return tagIndex == inputTagIndex;
      }
      return false;
    }

    /**
     * Find the message type of a line.  Save information about the tag which matched:
     * where is was in the line, and which tag it was.
     * @param line
     */
    private void load(final String line) {
      reset();
      this.line = line;
      if (line == null) {
        return;
      }
      if ((startIndex = line.indexOf(MessageType.PIP_WARNING_START.tag)) != -1) {
        messageType = MessageType.PIP_WARNING_START;
      }
      else if ((endIndex = line.indexOf(MessageType.PIP_WARNING_END.tag)) != -1) {
        // end tag - do not strip tag.
        endIndex += MessageType.PIP_WARNING_END.tag.length();
        if (messageType == null) {
          startIndex = 0;
          messageType = MessageType.PIP_WARNING_END;
        }
        else {
          endMessageType = MessageType.PIP_WARNING_END;
        }
      }
      else if ((startIndex = line.indexOf(MessageType.LOG_FILE.tag)) != -1) {
        messageType = MessageType.LOG_FILE;
        // The tag should be striped from log file messages.
        startIndex += MessageType.LOG_FILE.tag.length();
      }
      else if ((startIndex = line.indexOf(MessageType.WARNING.tag)) != -1) {
        messageType = MessageType.WARNING;
      }
      else if (chunks && (startIndex = line.indexOf(MessageType.CHUNK_ERROR.tag)) != -1) {
        // CHUNK_ERROR takes precedence over ERROR
        messageType = MessageType.CHUNK_ERROR;
        // Errors may be added to the chunk error line. They are errors associated with
        // the chunk and should not be treated as process errors, so put them on separate
        // lines but keep them with the chunk error. This is only needed for single-line
        // chunk errors
        if (!multiLineAllMessages) {
          subLine = SubLine.getInstance(line, startIndex, MessageType.CHUNK_ERROR.tag,
            MessageType.ERROR.tag);
          if (subLine != null) {
            endIndex = subLine.startIndex;
          }
        }
      }
      else if ((startIndex = line.indexOf(MessageType.INFO.tag)) != -1) {
        messageType = MessageType.INFO;
      }
      else if ((startIndex = line.indexOf(MessageType.LOG.tag)) != -1) {
        messageType = MessageType.LOG;
        // The tag should be striped from log messages.
        startIndex += MessageType.LOG.tag.length();
      }
      else if ((endIndex = line.indexOf(MessageType.LOG.secondaryTag)) != -1) {
        messageType = MessageType.LOG;
        // The tag, and everything after it, should be stripped from the log message.
        truncateLine = true;
      }
      else {
        // look for an error message
        for (int i = 0; i < errorTags.length; i++) {
          startIndex = line.indexOf(errorTags[i]);
          if (startIndex != -1) {
            // Check list of text that looks like an error message but isn't.
            for (int j = 0; j < IGNORE_TAG.length; j++) {
              if (line.indexOf(IGNORE_TAG[j]) != -1) {
                startIndex = -1;
                break;
              }
            }
            if (startIndex != -1) {
              messageType = MessageType.ERROR;
              tagIndex = i;
              alwaysMultiLine = errorTagsAlwaysMultiline[i];
            }
            break;
          }
        }
      }
      if (messageType == null && (successTag1 != null || successTag2 != null)) {
        // Look for success tags
        // Looks for success tags in the line. Sets success = true if all set tags
        // are found. Tags are checked in order. Tag 1 must come first and the tags
        // must not overlap.
        if (successTag1 != null && successTag2 != null) {
          if ((startIndex = line.indexOf(successTag1)) != -1) {
            String substring = line.substring(startIndex + successTag1.length());
            if (substring != null && substring.indexOf(successTag2) != -1) {
              messageType = MessageType.SUCCESS;
            }
          }
        }
        else if (successTag1 != null) {
          if ((startIndex = line.indexOf(successTag1)) != -1) {
            messageType = MessageType.SUCCESS;
          }
        }
        else if ((startIndex = line.indexOf(successTag2)) != -1) {
          messageType = MessageType.SUCCESS;
        }
      }
      if (messageType == null && messagePrependTag != null) {
        if ((startIndex = line.indexOf(messagePrependTag)) != -1) {
          messageType = MessageType.PREPEND;
        }
      }
    }
  }

  /**
   * Added messages to a list specified by MessageType.  Filters out uninformative
   * messages.  Handles messages from any source.  Handles redirecting messages to the
   * log - and the override which redirects messages back the a list.  Handles duplicate
   * chunk messages.
   * 
   * This class is for handling all message filtering complexity.
   * 
   * Does not create objects for each add() call because some messages are sometimes added
   * a string at a time.
   */
  private final class MessageBuilder {
    private final MultiSourceStringIterator fromIterator =
      new MultiSourceStringIterator();

    private MessageBuilder() {}

    /**
     * Adds an empty message to a list denoted by messageType.
     * @param messageType
     * @param fromMessage
     */
    private void add(final MessageType messageType) {
      add(messageType, null, null, null, "", null, null, false, true);
    }

    /**
     * Adds message to a list denoted by messageType.
     * @param messageType
     * @param fromMessage
     */
    private void add(final MessageType messageType, final String fromMessage) {
      add(messageType, null, null, null, fromMessage, null, null, false, true);
    }

    /**
     * Adds messages to a list denoted by messageType.
     * @param messageType
     * @param header added to toList right before the first message is added
     * @param fromProcessMessages
     */
    private void add(final MessageType messageType, final String header,
      final ProcessMessages fromProcessMessages) {
      if (messageType != null && fromProcessMessages != null) {
        RatedList list = fromProcessMessages.getList(messageType, false);
        add(messageType, null, null, header, null, list != null ? list.getList() : null,
          null, false, true);
      }
    }

    /**
     * Adds messages to a list denoted by messageType.
     * @param messageType
     * @param header added to toList right before the first message is added
     * @param fromProcessMessages
     */
    private void add(final MessageType messageType, final MessageType fromMessageType,
      final String header, final ProcessMessages fromProcessMessages) {
      if (messageType != null && fromProcessMessages != null) {
        RatedList list = fromProcessMessages.getList(fromMessageType, false);
        add(messageType, fromMessageType, null, header, null,
          list != null ? list.getList() : null, null, false, true);
      }
    }

    /**
     * Adds messages to a list denoted by messageType.
     * @param messageType
     * @param header added to toList right before the first message is added
     * @param fromList
     */
    private void add(final MessageType messageType, final String header,
      final List<String> fromList) {
      add(messageType, null, null, header, null, fromList, null, false, true);
    }

    /**
     * Adds messages to a list denoted by messageType.
     * @param messageType
     * @param header added to toList right before the first message is added
     * @param fromArray
     */
    private void add(final MessageType messageType, final String header,
      final String[] fromArray) {
      add(messageType, null, null, header, null, null, fromArray, false, true);
    }

    /**
     * Add messages from messageQueue - destructively - using remove.
     * @param header
     * @param messageQueue
     * @param chunkMessage
     * @param allowLogOverride
     */
    private void add(final String header, final Queue<Message> messageQueue,
      final boolean chunkMessage, final boolean allowLogOverride) {
      if (messageQueue == null) {
        return;
      }
      while (!messageQueue.isEmpty()) {
        Message message = messageQueue.remove();
        if (message != null) {
          add(message.getMessageType(), null, message.getListType(), header,
            message.getMessageString(), null, null, chunkMessage, allowLogOverride);
        }
      }
    }

    private void add(final String header, final TagInterface tag,
      final boolean chunkMessage, final boolean allowLogOverride) {
      if (tag == null) {
        return;
      }
      add(tag.getMessageType(), null, tag.getListType(), header, tag.getMessageString(),
        null, null, chunkMessage, allowLogOverride);
    }

    /**
     * Adds messages to a list denoted by messageType.  This function takes all sources
     * of messages.  Don't call it directly.
     * @param messageType
     * @param header added to toList right before the first message is added
     * @param fromMessage
     * @param fromList
     * @param fromArray
     */

    // TODO
    private synchronized void add(final MessageType messageType,
      MessageType fromMessageType, ListType listType, String header,
      final String fromMessage, final List<String> fromList, final String[] fromArray,
      final boolean chunkMessage, final boolean allowLogOverride) {
      if (hibernate) {
        return;
      }
      if (messageType == MessageType.SUCCESS) {
        success = true;
        return;
      }
      if (fromMessageType == null) {
        fromMessageType = messageType;
      }
      // Set list type.
      if (listType == null && messageType != null) {
        listType = messageType.getListType();
      }
      if (chunkMessage) {
        if (listType == ListType.ERROR) {
          listType = ListType.CHUNK_ERROR;
        }
        if (listType == ListType.WARNING) {
          listType = ListType.CHUNK_WARNING;
        }
      }
      // Non-logged messages will be stored in toList.
      RatedList toList = getList(listType, true);
      if (toList == null) {
        // May be putting homeless messages into the etomo error log.
        System.err.println();
      }
      // settings
      boolean preventDuplicates = false;
      String logOverrideTag = null;
      if (!logAllMessages) {
        if (fromMessageType == MessageType.CHUNK_ERROR
          || fromMessageType == MessageType.CHUNK_WARNING) {
          preventDuplicates = true;
        }
      }
      else if (allowLogOverride && messageType == MessageType.ERROR
        && errorOverrideLogTag != null) {
        logOverrideTag = errorOverrideLogTag;
      }
      boolean logMessages = logAllMessages || listType == ListType.LOGGED;
      String logHeader = header;
      // Reuse the iterator.
      fromIterator.reset(fromMessage, fromList, fromArray);
      // go through from messages and put the keepers into toList
      while (fromIterator.hasNext()) {
        String message = fromIterator.next();
        // Rate the message and store it or drop it according to the rating.
        MessageRating rating = MessageRating.rate(message);
        if (rating.isDrop()) {
          continue;
        }
        // ToList is a RatedList. It will only save the highest rated messages it
        // receives so the alternative messages will only be used if there aren't any
        // keepers.
        if (!logMessages
          || (logOverrideTag != null && message.indexOf(logOverrideTag) != -1)) {
          if (addMessage(header, message, preventDuplicates, toList, rating)) {
            header = null;
          }
        }
        else if (logMessage(messageType, logHeader, message)) {
          logHeader = null;
        }
      }
    }

    /**
     * Keep private.  Do not call this directly.
     * Add an unused header and a message to toList.
     * @param useHeader - header can be added when this is true - kept up to date
     * @param header - added first if useHeader is on and the message is added, useHeader is turned off after it is used
     * @param message
     * @return true if message added
     */
    private boolean addMessage(final String header, final String message,
      final boolean preventDuplicates, final RatedList toList,
      final MessageRating rating) {
      if (toList == null) {
        if (header != null) {
          System.err.println("\n" + header);
        }
        System.err.println(message);
        return true;
      }
      if (preventDuplicates && toList.contains(message)) {
        return false;
      }
      return toList.add(header, message, rating);
    }

    /**
     * Keep private.  Do not call this directly.
     * Log a message
     * @param header - log if not null
     * @param message
     * @return true if message logged or placed in the error log because there is no manager
     */
    private boolean logMessage(final MessageType messageType, final String header,
      final String message) {
      if (messageType == MessageType.LOG_FILE) {
        // Log from a file
        File file = new File(manager.getPropertyUserDir(), message);
        if (file.exists() && file.isFile() && file.canRead()) {
          if (manager != null) {
            if (header != null) {
              manager.logSimpleMessage(header);
            }
            manager.logSimpleMessage(file);
            return true;
          }
          if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
            if (header != null) {
              System.err.println(header);
            }
            System.err.println("Messages logged in " + file.getAbsolutePath());
            return true;
          }
        }
        else if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
          if (header != null) {
            System.err.println(header);
          }
          System.err
            .println("Warning: unable to log from file:" + file.getAbsolutePath());
          return false;
        }
      }
      // Log message
      else if (manager != null) {
        if (header != null) {
          manager.logSimpleMessage(header);
        }
        manager.logSimpleMessage(message);
        return true;
      }
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        if (header != null) {
          manager.logSimpleMessage(header);
        }
        System.err.println(message);
        return true;
      }
      return false;
    }
  }

  private static final class MessageRating {
    private static final MessageRating DROP = new MessageRating(0, 0);
    private static final MessageRating ALTERNATIVE_B = new MessageRating(1, 1);
    private static final MessageRating ALTERNATIVE_A = new MessageRating(2, 1);
    private static final MessageRating KEEPER = new MessageRating(3, -1);

    private static final MessageRating MAX = KEEPER;

    private final int rating;
    private final int maxToStore;

    private MessageRating(final int rating, final int maxToStore) {
      this.rating = rating;
      this.maxToStore = maxToStore;
    }

    /**
     * Does not return null
     * @param message
     * @return
     */
    private static MessageRating rate(final String message) {
      // return null for all uninformative messages
      if (message == null || message.matches("\\s*")
        || (message.indexOf("PID:") != -1 && message.length() <= 20)) {
        return DROP;
      }
      if (message.indexOf("exited with status 1") != -1) {
        if (message.indexOf("python -u") != -1) {
          return ALTERNATIVE_B;
        }
        return ALTERNATIVE_A;
      }
      return KEEPER;
    }

    private boolean isDrop() {
      return maxToStore == 0;
    }

    private boolean isStore(final int alreadyStored) {
      if (maxToStore == 0) {
        return false;
      }
      if (maxToStore < 0) {
        return true;
      }
      return alreadyStored < maxToStore;
    }

    /**
     * A null message is treated as unitialized and having the lowest rating.
     * RatedList works.
     * @param messageRating
     * @return
     */
    private boolean gt(MessageRating messageRating) {
      if (messageRating == null) {
        return true;
      }
      return rating > messageRating.rating;
    }

    /**
     * A null message is treated as unitialized and having the lowest rating.
     * RatedList works.
     * @param messageRating
     * @return
     */
    private boolean lt(MessageRating messageRating) {
      if (messageRating == null) {
        return false;
      }
      return rating < messageRating.rating;
    }

    public String toString() {
      if (this == DROP) {
        return "DROP";
      }
      if (this == ALTERNATIVE_B) {
        return "ALTERNATIVE_B";
      }
      if (this == ALTERNATIVE_A) {
        return "ALTERNATIVE_A";
      }
      if (this == KEEPER) {
        return "KEEPER";
      }
      return "unknown";
    }
  }

  /**
   * Saves the most highly rated messages it gets.  Messages with a lower rating are not
   * kept.  Unrated messages are treated as the most highly rated.
   */
  private static final class RatedList {
    private final List<String> list = new Vector<String>();

    private MessageRating messageRating = null;
    // Hold onto the recent header in case messages need to be stored under it. Previous
    // header is ignored as every message that comes after a header as assumed to belong
    // under that header.
    private String mostRecentHeader = null;
    private int headerLines = 0;

    private RatedList() {}

    private boolean add(final String header, final String message,
      MessageRating newMessageRating) {
      boolean addHeader = false;
      if (header != null && !header.equals(mostRecentHeader)) {
        mostRecentHeader = header;
        // This causes the header to be added to the list if the message is added.
        addHeader = true;
      }
      // Assume an unrated message is a keeper. Messages added without a rating should be
      // automatically kept, so rate it at the highest level and bump up the current
      // rating.
      if (newMessageRating == null) {
        newMessageRating = MessageRating.MAX;
      }
      if (newMessageRating.isDrop()) {
        return false;
      }
      // Don't save anything with a rating lower then the current rating.
      if (newMessageRating.lt(messageRating)) {
        return false;
      }
      if (newMessageRating.gt(messageRating)) {
        // If the new message rating is higher then the current rating, then dump all
        // the preceding (lower rated) messages.
        messageRating = newMessageRating;
        list.clear();
        // Make sure the most recent header is add if a message is saved
        addHeader = true;
        headerLines = 0;
      }
      // Add the header and message.
      if (newMessageRating.isStore(list.size() - headerLines)) {
        // When there is a new header or the list was just cleared add the header before
        // the message.
        if (addHeader && mostRecentHeader != null) {
          list.add(mostRecentHeader);
          list.add("");
          headerLines += 2;
        }
        list.add(message);
        return true;
      }
      return false;
    }

    /**
     * Add a message without a rating.  This bumps the rating up to the highest level
     * since messages without ratings should always be added (so they are keepers which is
     * the maximum rating).
     * @param message
     */
    private void add(final String message) {
      if (messageRating != MessageRating.MAX) {
        messageRating = MessageRating.MAX;
        list.clear();
      }
      list.add(message);
    }

    private boolean contains(final Object object) {
      return list.contains(object);
    }

    private void clear() {
      list.clear();
    }

    private boolean isEmpty() {
      return list.isEmpty();
    }

    private int size() {
      return list.size();
    }

    private String get(final int index) {
      return list.get(index);
    }

    private List<String> getList() {
      return list;
    }

    private Iterator<String> iterator() {
      return list.iterator();
    }

    public String toString() {
      return "[messageRating=" + messageRating + ",list=" + list.toString() + "]";
    }
  }

  /**
   * Iterator for one of the three sources it can store: String, List<String>, or
   * String[].  Chooses one source to iterate. Checks in the order: String,
   * Iterator<String>, String[].
   * 
   * Call reset to setup or reuse with a different source.
   */
  private static final class MultiSourceStringIterator {
    private String fromMessage = null;
    private boolean singleMessage = false;
    private Iterator<String> fromIterator = null;
    private String[] fromArray = null;
    private int fromArrayIndex = -1;

    private MultiSourceStringIterator() {}

    private void reset(final String fromMessage, final List<String> fromList,
      final String[] fromArray) {
      this.fromMessage = fromMessage;
      if (fromMessage != null) {
        singleMessage = true;
      }
      else {
        singleMessage = false;
      }
      if (fromList != null) {
        fromIterator = fromList.iterator();
      }
      else {
        fromIterator = null;
      }
      this.fromArray = fromArray;
      fromArrayIndex = -1;
    }

    /**
     * @return true if selected source has any thing left to return.
     */
    private boolean hasNext() {
      if (singleMessage) {
        return fromMessage != null;
      }
      if (fromIterator != null) {
        return fromIterator.hasNext();
      }
      if (fromArray != null) {
        return fromArrayIndex >= 0 && fromArrayIndex < fromArray.length;
      }
      return false;
    }

    /**
     * Returns selected source's next string.  For a single message, this deletes the
     * message.
     * @return
     */
    private String next() {
      if (singleMessage) {
        if (fromMessage != null) {
          String temp = fromMessage;
          fromMessage = null;
          return temp;
        }
        return null;
      }
      if (fromIterator != null) {
        return fromIterator.next();
      }
      if (fromArray != null && hasNext()) {
        return fromArray[fromArrayIndex++];
      }
      return null;
    }
  }

  public final class ListTypeIterator {
    ListType curListType = null;

    public boolean hasNext() {
      return getNext(curListType) != ListType.FLAG;
    }

    public ListType next() {
      curListType = getNext(curListType);
      return curListType;
    }

    private ListType getNext(ListType listType) {
      while ((listType = increment(listType)) != ListType.FLAG) {
        RatedList list = getList(listType, false);
        if (list != null && !list.isEmpty()) {
          return listType;
        }
      }
      return ListType.FLAG;
    }

    private ListType increment(ListType listType) {
      if (listType == null) {
        return ListType.ERROR;
      }
      if (listType == ListType.ERROR) {
        return ListType.CHUNK_ERROR;
      }
      if (listType == ListType.CHUNK_ERROR) {
        return ListType.WARNING;
      }
      if (listType == ListType.WARNING) {
        return ListType.CHUNK_WARNING;
      }
      if (listType == ListType.CHUNK_WARNING) {
        return ListType.INFO;
      }
      return ListType.FLAG;
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.15  2011/02/22 04:09:08  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.14  2010/11/13 16:03:45  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.13  2010/06/18 16:23:31  sueh
 * <p> bug# 1385 Added addWarning and getLastWarning.
 * <p>
 * <p> Revision 1.12  2010/02/17 04:49:20  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 1.11  2009/05/02 01:08:46  sueh
 * <p> bug# 1216 In addElement fixed a problem where empty messages where
 * <p> saved.
 * <p>
 * <p> Revision 1.10  2009/02/04 23:26:53  sueh
 * <p> bug# 1158 Changed id and exceptions classes in LogFile.
 * <p>
 * <p> Revision 1.9  2008/01/14 21:58:21  sueh
 * <p> bug# 1050 Added getInstance(String successTag) for finding message with one
 * <p> successTag.
 * <p>
 * <p> Revision 1.8  2006/10/10 05:13:05  sueh
 * <p> bug# 931 Added addProcessOutput(LogFile).
 * <p>
 * <p> Revision 1.7  2006/08/03 21:30:59  sueh
 * <p> bug# 769 Fixed parse():  once a message is found, return.  This means that all
 * <p> line will be checked for all messages.  Added parseSuccessLine().
 * <p>
 * <p> Revision 1.6  2006/06/14 00:09:03  sueh
 * <p> bug# 785 Not saving to error list when saving to chunk error list
 * <p>
 * <p> Revision 1.5  2006/05/22 22:50:34  sueh
 * <p> bug# 577 Added toString().
 * <p>
 * <p> Revision 1.4  2006/03/16 01:53:06  sueh
 * <p> Made constructor private
 * <p>
 * <p> Revision 1.3  2005/11/30 21:15:11  sueh
 * <p> bug# 744 Adding addProcessOutput(String[]) to get standard out error
 * <p> messages.
 * <p>
 * <p> Revision 1.2  2005/11/19 02:38:58  sueh
 * <p> bug# 744 Added parsing and separate storage for chunk errors.  Added
 * <p> addProcessOutput(String) for output that must be handled one line at a
 * <p> time.
 * <p>
 * <p> Revision 1.1  2005/11/02 21:59:50  sueh
 * <p> bug# 754 Class to parse and hold error, warning, and information
 * <p> messages.  Message can also be set directly in this class without
 * <p> parsing.  Can parse or set messages from multiple sources.
 * <p> </p>
 */
