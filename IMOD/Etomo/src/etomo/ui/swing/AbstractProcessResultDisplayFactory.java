package etomo.ui.swing;

import etomo.process.ProcessResultDisplayFactoryInterface;
import etomo.type.ProcessResultDisplay;

/**
 * <p>Description: A single direction link list of ProcessResultDisplays, which labels
 * each display with an ID that must be unique to the dataset.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version "$Id$
 */
public abstract class AbstractProcessResultDisplayFactory
  implements ProcessResultDisplayFactoryInterface {
  // factoryID must be unique within the dataset. If there are multiple instances of the
  // same class of factory in the dataset (as in one for each axis), they must have
  // different IDs.
  //
  // The factory ID will be saved to files as process data, so it must be the same each
  // time etomo runs. However, because process data is short-lived, this ID doesn't have
  // to stable from version to version.
  private final String factoryID;

  // Dependency list
  private ProcessResultDisplay head = null;
  private ProcessResultDisplay tail = null;

  protected AbstractProcessResultDisplayFactory(final String factoryID) {
    this.factoryID = factoryID;
  }

  /**
   * Adds display to this instance's link list.  Does not follow display's links.
   * Sets the display's displayID and factoryID.
   * @param display
   */
  protected synchronized void addDependency(final ProcessResultDisplay display,
    final int displayID) {
    if (display == null) {
      System.err.println("Error: display is null");
      Thread.dumpStack();
      return;
    }

    // Add display to the end of the linked list.
    display.setID(displayID, factoryID);
    if (tail != null) {
      tail.setNext(display);
    }
    tail = display;
    display.setNext(null);
    if (head == null) {
      // New list
      head = tail;
    }
  }

  public ProcessResultDisplay getProcessResultDisplay(final int displayID,
    final String factoryID) {
    // This link list represents the buttons that interact with each other in an interface
    // - maybe fifty elements at most. There is no reason to do a faster search for such
    // a small number of elements.
    if (head == null) {
      // DependencyID not found.
      return null;
    }
    ProcessResultDisplay current = head;
    while (current != null) {
      if (current.equalsID(displayID, factoryID)) {
        return current;
      }
      current = current.getNext();
    }
    return null;
  }

  //
  // Plugin functions

  /**
   * Just deletes the link list.
   * For plugin factories - if they have displays that need to be inserted in different
   * places in the manager's list.
   */
  protected void reset() {
    head = null;
    tail = null;
  }

  /**
   * For plugin factories - allows the insertion of a link list from another factory.
   * This allows plugin buttons to interact with etomo button.
   * Assumes that the display ID has already been set.  Replaces the factory ID.
   * @param display - The head of the link list to be inserted
   * @param startInsert - Element from the link list starting from this.head.  The display should be inserted after this element.
   */
  public void insertAfter(final AbstractProcessResultDisplayFactory factory,
    final ProcessResultDisplay startInsert) {
    if (factory == null) {
      System.err.println("Error: factory is null");
      Thread.dumpStack();
      return;
    }
    if (startInsert == null) {
      System.err.println("Error: startInsert is null");
      Thread.dumpStack();
      return;
    }
    if (factory.head == null) {
      System.err.println("Warning: Factory, " + factory + ", does not contain a list");
      Thread.dumpStack();
      return;
    }
    // Insert display
    ProcessResultDisplay endInsert = startInsert.getNext();
    // Overrides the previous factory ID.
    ProcessResultDisplay current = factory.head;
    current.setFactoryID(factoryID);
    startInsert.setNext(current);
    // Move to the end of display's link list
    ProcessResultDisplay next = null;
    while ((next = current.getNext()) != null) {
      current = next;
      current.setFactoryID(factoryID);
    }
    // Attach startInsert.next to the end of display's link list.
    current.setNext(endInsert);
  }
}