package etomo.util;

import java.io.File;

import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import junit.framework.TestCase;

public class UtilitiesTest extends TestCase {
  public void testRoundToMaxDecimalPlaces() {
    EtomoNumber maxDecimalPlaces = new EtomoNumber();
    maxDecimalPlaces.set(2);
    assertEquals("null number is returned as null", null,
      Utilities.roundToMaxDecimalPlaces((ConstEtomoNumber) null, maxDecimalPlaces));
    assertEquals("null number is returned as null", null,
      Utilities.roundToMaxDecimalPlaces((Number) null, maxDecimalPlaces));
    assertEquals("null number is returned as null", null,
      Utilities.roundToMaxDecimalPlaces((String) null, maxDecimalPlaces));
    String sNumber = "1.2345";
    assertEquals("null maxDecimalPlaces has no effect", sNumber,
      Utilities.roundToMaxDecimalPlaces(sNumber, null));
    maxDecimalPlaces.set(-2);
    assertEquals("negative maxDecimalPlaces has no effect", sNumber,
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(1);
    assertEquals("round to 1 decimal place", "1.2",
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(2);
    assertEquals("round to 2 decimal places", "1.23",
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(3);
    assertEquals("round to 3 decimal places (rounds up)", "1.235",
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(4);
    assertEquals("Already at 4 decimal places - no effect", sNumber,
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(5);
    assertEquals("Less then 5 decimal places - no effect", sNumber,
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
    maxDecimalPlaces.set(0);
    assertEquals("round to 0 decimal places and remove decimal", "1",
      Utilities.roundToMaxDecimalPlaces(sNumber, maxDecimalPlaces));
  }

  public void testGetExtension() {
    assertNull("returns null for null input", Utilities.getExtension(null));
    assertNull("returns null when tag not found", Utilities.getExtension("123456"));
    //
    assertEquals("when string is just the tag returns null", null,
      Utilities.getExtension("."));
    assertEquals("returns extension without tag", "456",
      Utilities.getExtension("123.456"));
    assertEquals("returns whole string minus tag when tag first", "123456",
      Utilities.getExtension(".123456"));
    assertEquals("uses last tag", "456", Utilities.getExtension(".123.456"));
    assertEquals("if tag is last returns null", null,
      Utilities.getExtension(".123.456."));
    //
    assertEquals("other tags do not work", "3_456", Utilities.getExtension("12.3_456"));
    assertEquals("multi-char tag do not work", "3MRC456",
      Utilities.getExtension("12.3MRC456"));
    assertEquals("Trims white space", " 456", Utilities.getExtension(" 1_23 . 456 "));
    assertEquals("handles file paths", "456", Utilities
      .getExtension("dir1" + File.separator + "dir2" + File.separator + "1_23.456"));
    assertEquals("handles internal whitespace", " 456", Utilities.getExtension(
      " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 . 456 "));
  }

  public void testRemoveExtension() {
    assertNull("returns null for null input", Utilities.removeExtension(null));
    assertEquals("returns string when tag not found", "123456",
      Utilities.removeExtension("123456"));
    //
    assertEquals("when string is just the tag returns empty string", "",
      Utilities.removeExtension("."));
    assertEquals("returns left side without tag", "123",
      Utilities.removeExtension("123.456"));
    assertEquals("returns empty string when tag first", "",
      Utilities.removeExtension(".123456"));
    assertEquals("uses last tag", ".123", Utilities.removeExtension(".123.456"));
    assertEquals("if tag is last just removes tag", ".123.456",
      Utilities.removeExtension(".123.456."));
    //
    assertEquals("only . tag works", "12", Utilities.removeExtension("12.3_456"));
    assertEquals("only . tag works", "12", Utilities.removeExtension("12.3MRC456"));
    assertEquals("Trims white space from input string", "1_23 ",
      Utilities.removeExtension(" 1_23 . 456 "));
    assertEquals("handles file paths",
      "dir1" + File.separator + "dir2" + File.separator + "1_23", Utilities
        .removeExtension("dir1" + File.separator + "dir2" + File.separator + "1_23.456"));
    assertEquals("handles internal whitespace",
      "dir1 " + File.separator + " dir2 " + File.separator + " 1_23 ",
      Utilities.removeExtension(
        " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 . 456 "));
  }

  public void testRemoveRightSide() {
    assertNull("returns null for null input", Utilities.removeRightSide(null, null));
    assertEquals("returns string for null tag", "123.456",
      Utilities.removeRightSide("123.456", null));
    assertNull("returns null for null input", Utilities.removeRightSide(null, "."));
    assertEquals("returns string when tag not found", "123456",
      Utilities.removeRightSide("123456", "."));
    //
    assertEquals("when string is just the tag returns empty string", "",
      Utilities.removeRightSide(".", "."));
    assertEquals("returns left side without tag", "123",
      Utilities.removeRightSide("123.456", "."));
    assertEquals("returns empty string when tag first", "",
      Utilities.removeRightSide(".123456", "."));
    assertEquals("uses last tag", ".123", Utilities.removeRightSide(".123.456", "."));
    assertEquals("if tag is last just removes tag", ".123.456",
      Utilities.removeRightSide(".123.456.", "."));
    //
    assertEquals("other tags works", "12.3", Utilities.removeRightSide("12.3_456", "_"));
    assertEquals("multi-char tag works", "12.3",
      Utilities.removeRightSide("12.3MRC456", "MRC"));
    assertEquals("Does not trim white space", " 1_23 ",
      Utilities.removeRightSide(" 1_23 . 456 ", "."));
    assertEquals("handles file paths",
      "dir1" + File.separator + "dir2" + File.separator + "1_23",
      Utilities.removeRightSide(
        "dir1" + File.separator + "dir2" + File.separator + "1_23.456", "."));
    assertEquals("handles internal whitespace",
      " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 ",
      Utilities.removeRightSide(
        " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 . 456 ", "."));
  }

  public void testRemoveLeftSide() {
    assertNull("returns null for null string", Utilities.removeLeftSide(null, null));
    assertEquals("returns string for null tag", "123.456",
      Utilities.removeLeftSide("123.456", null));
    assertNull("returns null for null string", Utilities.removeLeftSide(null, "."));
    assertEquals("returns string when tag not found", "123456",
      Utilities.removeLeftSide("123456", "."));
    //
    assertEquals("when string is just the tag returns empty string", "",
      Utilities.removeLeftSide(".", "."));
    assertEquals("returns right side without tag", "456",
      Utilities.removeLeftSide("123.456", "."));
    assertEquals("only removes tag when tag first", "123456",
      Utilities.removeLeftSide(".123456", "."));
    assertEquals("uses first tag", "123.456", Utilities.removeLeftSide(".123.456", "."));
    assertEquals("if tag is last returns empty string", "",
      Utilities.removeLeftSide("123456.", "."));
    //
    assertEquals("other tags works", "456", Utilities.removeLeftSide("12.3_456", "_"));
    assertEquals("multi-char tag works", "456",
      Utilities.removeLeftSide("12.3MRC456", "MRC"));
    assertEquals("Does not trim white space", " 456 ",
      Utilities.removeLeftSide(" 1_23 . 456 ", "."));
    assertEquals("handles file paths", "456", Utilities.removeLeftSide(
      "dir1" + File.separator + "dir2" + File.separator + "1_23.456", "."));
    assertEquals("handles internal whitespace", " 456 ", Utilities.removeLeftSide(
      " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 . 456 ", "."));
  }

  public void testGetSuffix() {
    assertNull("returns null for null input", Utilities.getSuffix(null, null));
    assertNull("returns null for null tag", Utilities.getSuffix("123.456", null));
    assertNull("returns null for null input", Utilities.getSuffix(null, "."));
    assertNull("returns null when tag not found", Utilities.getSuffix("123456", "."));
    //
    assertEquals("when string is just the tag returns the tag", ".",
      Utilities.getSuffix(".", "."));
    assertEquals("returns suffix with tag", ".456", Utilities.getSuffix("123.456", "."));
    assertEquals("returns whole string when tag first", ".123456",
      Utilities.getSuffix(".123456", "."));
    assertEquals("uses last tag", ".456", Utilities.getSuffix(".123.456", "."));
    assertEquals("if tag is last just returns tag", ".",
      Utilities.getSuffix(".123.456.", "."));
    //
    assertEquals("other tags works", "_456", Utilities.getSuffix("12.3_456", "_"));
    assertEquals("multi-char tag works", "MRC456",
      Utilities.getSuffix("12.3MRC456", "MRC"));
    assertEquals("Trims white space", ". 456", Utilities.getSuffix(" 1_23 . 456 ", "."));
    assertEquals("handles file paths", ".456", Utilities
      .getSuffix("dir1" + File.separator + "dir2" + File.separator + "1_23.456", "."));
    assertEquals("handles internal whitespace", ". 456", Utilities.getSuffix(
      " dir1 " + File.separator + " dir2 " + File.separator + " 1_23 . 456 ", "."));
  }

  public void testWrap() {
    String divider = "++";
    assertNull("Unwrappable string is returned as is",
      Utilities.wrap(null, divider, 0, 5, 7));
    assertEquals("Unwrappable string is returned as is", "",
      Utilities.wrap("", divider, 0, 5, 7));

    String input = "123++xyz++abc";
    assertEquals("Unwrappable string is returned as is", input,
      Utilities.wrap(input, null, 0, 5, 0));
    assertEquals("Unwrappable string is returned as is", input,
      Utilities.wrap(input, "", 0, 5, 0));
    assertEquals("No wrapping length causes string to be returned as is", input,
      Utilities.wrap(input, divider, 0, 0, 0));
    assertEquals("does not wrap a short line", input,
      Utilities.wrap(input, divider, 72, 80, 0));

    String wrap = "123++\nxyz++\nabc";
    assertEquals("favors ideal length wrap", wrap,
      Utilities.wrap(input, divider, 0, 5, 0));
    assertEquals("Prefers shorter less then ideal wrap", wrap,
      Utilities.wrap(input, divider, 0, 8, 0));
    assertEquals("Able to do longer less then ideal wrap", wrap,
      Utilities.wrap(input, divider, 0, 4, 0));
    assertEquals("max length wrap", wrap, Utilities.wrap(input, divider, 0, 0, 5));

    String input2 = " 123++ xyz++ abc ";
    assertEquals("external whitespace is trimmed from each line", wrap,
      Utilities.wrap(input2, divider, 0, 6, 0));

    wrap = "123++\nxyz++\nab\nc";
    assertEquals("max length is used when no more dividers", wrap,
      Utilities.wrap(input, divider, 0, 5, 2));

    wrap = "123++xyz++\nabc";
    assertEquals("longest ideal wrap has priority", wrap,
      Utilities.wrap(input, divider, 0, 10, 0));
    assertEquals("favors longest ideal length wrap", wrap,
      Utilities.wrap(input, divider, 0, 10, 0));
    assertEquals("uses the last divider within ideal length", wrap,
      Utilities.wrap(input, divider, 0, 12, 0));
    assertEquals("can find divider within ideal length", wrap,
      Utilities.wrap(input, divider, 0, 12, 0));
    assertEquals("low and high are sorted out", wrap,
      Utilities.wrap(input, divider, 0, 12, 0));

    wrap = "123\n++x\nyz+\n+ab\nc";
    assertEquals("max length can be used by itself", wrap,
      Utilities.wrap(input, null, 0, 0, 3));

    input = "123   abc";
    wrap = "123\nabc";
    assertEquals("empty lines are not added", wrap,
      Utilities.wrap(input, divider, 0, 0, 3));

    input = "123++xyz++abcdefghijklmnop";
    wrap = "123++xyz++\nabcde\nfghij\nklmno\np";
    assertEquals("max length takes over when there are no more dividers", wrap,
      Utilities.wrap(input, divider, 0, 10, 5));

    input = "123+===+xyz+===+abc";
    wrap = "123+===+\nxyz+===+\nabc";
    assertEquals("can find long divider", wrap, Utilities.wrap(input, "+===+", 0, 9, 0));
    assertEquals("too short idea lengths default to divider length", wrap,
      Utilities.wrap(input, "+===+", 0, 2, 5));

    input = "123++xyz++abc";
    wrap = "123++\nxyz++\nabc";
    assertEquals("favors ideal length wrap", wrap,
      Utilities.wrap(input, divider, 0, 5, 0));

    input = "123+xyz+abc";
    wrap = "123+\nxyz+\nabc";
    assertEquals("can wrap on single character divider", wrap,
      Utilities.wrap(input, "+", 0, 5, 0));

    input = "123 xyz abc";
    wrap = "123\nxyz\nabc";
    assertEquals("can wrap on whitespace", wrap, Utilities.wrap(input, " ", 0, 5, 0));

    input = "123+ xyz+ abc";
    wrap = "123+\nxyz+\nabc";
    assertEquals("can wrap on a mix of character and whitespace", wrap,
      Utilities.wrap(input, "+ ", 0, 7, 0));
  }

  /**
   * For performing a subset of full unit test.
   */
  public static class DiagnosticTest extends TestCase {
    UtilitiesTest testCase = new UtilitiesTest();

    public DiagnosticTest() {}

    public void testRoundToMaxDecimalPlaces() {
      testCase.testRoundToMaxDecimalPlaces();
    }
  }
}
