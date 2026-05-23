package etomo.type;

import junit.framework.TestCase;

public class EtomoAutodocTest extends TestCase {
  public void testRemoveFormatting() {
    assertNull("handles null", EtomoAutodoc.removeFormatting(null));
    assertEquals("handles empty string", "", EtomoAutodoc.removeFormatting(""));
    assertEquals("formatting string removed", "XYZ",
      EtomoAutodoc.removeFormatting("^     XYZ"));
    assertEquals("formatting string removed", "ABCXYZ",
      EtomoAutodoc.removeFormatting("ABC\\fBXYZ"));
    assertEquals("formatting string removed, ignoring following space", "ABCXYZ QWERTY",
      EtomoAutodoc.removeFormatting("ABC\\fIXYZ\\fB QWERTY"));
    assertEquals("formatting string removed, ignoring following space", "ABCXYZ QWERTY",
      EtomoAutodoc.removeFormatting("ABC\\fIXYZ\\fR QWERTY"));
    assertEquals("formatting string removed, ignoring following space", "ABCXYZ QWERTY",
      EtomoAutodoc.removeFormatting("ABC\\fIXYZ\\fI QWERTY"));
    assertEquals("only specific formatting strings are removed", "ABC\\fXXYZ\\fX QWERTY",
      EtomoAutodoc.removeFormatting("ABC\\fXXYZ\\fX QWERTY"));
    assertEquals("only specific formatting strings are removed", "ABC\\\\fXXYZ QWERTY",
      EtomoAutodoc.removeFormatting("ABC\\\\fXXYZ\\fI QWERTY"));
  }
}
