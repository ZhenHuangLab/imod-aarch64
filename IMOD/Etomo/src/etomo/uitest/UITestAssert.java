package etomo.uitest;

import etomo.ui.swing.Popup;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;

class UITestAssert extends Assert {
  // Warning: can create odd behavior, cause uitest to fail, and/or prevent uitest from
  // ending. Used to get a look at the dialog after a random failure. For a reasonably
  // predictable failure, it is probably better to place the "pause=" uitest command right
  // before the command that fails.
  private static boolean PopupOnError = false;

  UITestAssert() {}

  private static void popup(final String message, final AssertionFailedError error) {
    error.printStackTrace();
    Popup popup = Popup.getCustomButtonInstance(null, "Test Failed",
      message + "\n" + error.getMessage() + "\n" + "See _err.log", null,
      new String[] { "OK" });
    popup.open();
    throw error;
  }

  /**
   * 
   * @param tester
   * @param message
   */
  public static void fail(final String message) {
    try {
      Assert.fail(message);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertEquals(String message, boolean expected, boolean actual) {
    try {
      Assert.assertEquals(message, expected, actual);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertEquals(String message, Object expected, Object actual) {
    try {
      Assert.assertEquals(message, expected, actual);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertEquals(final String message, final String expected,
    String actual) {
    try {
      Assert.assertEquals(message, expected, actual);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertFalse(final String message, final boolean condition) {
    try {
      Assert.assertFalse(message, condition);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertNotNull(final String message, final Object object) {
    try {
      Assert.assertNotNull(message, object);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }

  public static void assertTrue(final String message, final boolean condition) {
    try {
      Assert.assertTrue(message, condition);
    }
    catch (AssertionFailedError error) {
      if (PopupOnError) {
        popup(message, error);
      }
      else {
        throw error;
      }
    }
  }
}
