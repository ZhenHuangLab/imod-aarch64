package etomo.ui.swing;

import junit.framework.TestCase;

public class TooltipFormatterTest extends TestCase {
  @SuppressWarnings("deprecation")
  public void testFormat() {
    // Testing the new format function against the old format function. The new function
    // occasionally puts a break tag in a different location, so the class can't do a
    // regression self test again all input during uitest.
    String explanation =
      "New format function should return the same output as the old format function in most cases.";
    String rawString = null;
    assertEquals(explanation,
      TooltipFormatter.INSTANCE.formatForRegressionTest(rawString),
      TooltipFormatter.INSTANCE.format(rawString));
    rawString = "";
    assertEquals(explanation,
      TooltipFormatter.INSTANCE.formatForRegressionTest(rawString),
      TooltipFormatter.INSTANCE.format(rawString));
    rawString = "123456781012345678201234567830123456784012345678501234567860";
    assertEquals(explanation,
      TooltipFormatter.INSTANCE.formatForRegressionTest(rawString),
      TooltipFormatter.INSTANCE.format(rawString));
    rawString =
      "Open CTF corrected stack (series0001_21oct02a_callus1_00010gr_00001sq_v02_00002hln_v02_00002_ctfcorr_ali.mrc).";
    assertEquals(explanation,
      TooltipFormatter.INSTANCE.formatForRegressionTest(rawString),
      TooltipFormatter.INSTANCE.format(rawString));
    // this input caused the old format function to go into an infinite loop.
    explanation =
      "Formatter should add html and br tags with the 60 column limit where possible.";
    rawString =
      "Replace full aligned stack (series0001_21oct02a_callus1_00010gr_00001sq_v02_00002hln_v02_00002_ali.mrc) with CTF corrected stack (series0001_21oct02a_callus1_00010gr_00001sq_v02_00002hln_v02_00002_ctfcorr_ali.mrc).";
    String formatted =
      "<html>Replace full aligned stack<br>(series0001_21oct02a_callus1_00010gr_00001sq_v02_00002hln_v02_00002_ali.mrc)<br>with CTF corrected stack<br>(series0001_21oct02a_callus1_00010gr_00001sq_v02_00002hln_v02_00002_ctfcorr_ali.mrc).";
    assertEquals(explanation, formatted, TooltipFormatter.INSTANCE.format(rawString));
  }
}
