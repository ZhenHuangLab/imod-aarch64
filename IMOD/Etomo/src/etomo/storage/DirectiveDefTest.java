package etomo.storage;

import java.util.Iterator;

import junit.framework.TestCase;

public class DirectiveDefTest extends TestCase {
  public void testGetInstance() {
    // First test the standard list before wierd things are added to it.
    Iterator<DirectiveDef> iterator = DirectiveDef.MAP.values().iterator();
    while (iterator.hasNext()) {
      DirectiveDef directiveDef = iterator.next();
      assertTrue(directiveDef + " must be in the csv file",
        directiveDef.loadDirectiveDescr());
    }
    //
    assertNull("does not construct a null directiveDef", DirectiveDef.getInstance(null));
    //
    // preconstructed defs
    assertEquals("finds preconstructed copyArg",
      DirectiveDef.getInstance("setupset.copyarg.dual"), DirectiveDef.DUAL);
    assertEquals("finds preconstructed A copyArg",
      DirectiveDef.getInstance("setupset.copyarg.extract"), DirectiveDef.EXTRACT);
    assertEquals("finds preconstructed paired A copyArg",
      DirectiveDef.getInstance("setupset.copyarg.skip"), DirectiveDef.SKIP);
    assertEquals("finds preconstructed B copyArg",
      DirectiveDef.getInstance("setupset.copyarg.bskip"), DirectiveDef.BSKIP);
    assertEquals("finds preconstructed setupset",
      DirectiveDef.getInstance("setupset.scanHeader"), DirectiveDef.SCAN_HEADER);
    assertEquals("finds preconstructed any runtime",
      DirectiveDef.getInstance("runtime.Combine.any.patchSize"), DirectiveDef.PATCH_SIZE);
    assertEquals("finds preconstructed any runtime",
      DirectiveDef.getInstance("runtime.Combine.patchSize"), DirectiveDef.PATCH_SIZE);
    assertEquals("finds preconstructed any runtime",
      DirectiveDef.getInstance("runtime.Combine..patchSize"), DirectiveDef.PATCH_SIZE);
    assertEquals("finds preconstructed A runtime",
      DirectiveDef.getInstance("runtime.Combine.a.patchSize"), DirectiveDef.PATCH_SIZE);
    assertEquals("finds preconstructed B runtime",
      DirectiveDef.getInstance("runtime.Combine.b.patchSize"), DirectiveDef.PATCH_SIZE);
    assertEquals("finds preconstructed comparam",
      DirectiveDef.getInstance("comparam.align.tiltalign.LocalAlignments"),
      DirectiveDef.LOCAL_ALIGNMENTS);
    assertEquals("finds preconstructed A comparam",
      DirectiveDef.getInstance("comparam.aligna.tiltalign.LocalAlignments"),
      DirectiveDef.LOCAL_ALIGNMENTS);
    assertEquals("finds preconstructed B comparam",
      DirectiveDef.getInstance("comparam.alignb.tiltalign.LocalAlignments"),
      DirectiveDef.LOCAL_ALIGNMENTS);
    //
    // Newly constructed directives
    String directive = "setupset.copyarg.dua";
    assertEquals("finds newly constructed copyArg",
      DirectiveDef.getInstance(directive).getStandardKey(), directive);
    directive = "setupset.copyarg.extrac";
    assertEquals("finds newly constructed A copyArg",
      DirectiveDef.getInstance(directive).getStandardKey(), directive);
    directive = "setupset.copyarg.ski";
    DirectiveDef SKI = DirectiveDef.getInstance(directive);
    assertEquals("finds newly constructed paired A copyArg", SKI.getStandardKey(),
      directive);
    directive = "setupset.copyarg.bski";
    DirectiveDef BSKI = DirectiveDef.getInstance(directive);
    assertEquals("finds newly constructed B copyArg", BSKI.getStandardKey(), directive);
    directive = "setupset.scanHeade";
    assertEquals("finds newly constructed setupset",
      DirectiveDef.getInstance(directive).getStandardKey(), directive);
    directive = "runtime.Combine.any.patchSiz";
    assertEquals("finds newly constructed any runtime",
      DirectiveDef.getInstance(directive).getStandardKey(), directive);
    assertEquals("finds newly constructed any runtime",
      DirectiveDef.getInstance("runtime.Combine.patchSiz").getStandardKey(), directive);
    assertEquals("finds newly constructed any runtime",
      DirectiveDef.getInstance("runtime.Combine..patchSiz").getStandardKey(), directive);
    assertEquals("finds newly constructed A runtime",
      DirectiveDef.getInstance("runtime.Combine.a.patchSiz").getStandardKey(), directive);
    assertEquals("finds newly constructed B runtime",
      DirectiveDef.getInstance("runtime.Combine.b.patchSiz").getStandardKey(), directive);
    directive = "comparam.align.tiltalign.LocalAlignment";
    assertEquals("finds newly constructed comparam",
      DirectiveDef.getInstance(directive).getStandardKey(), directive);
    assertEquals(
      "finds newly constructed A comparam", DirectiveDef
        .getInstance("comparam.aligna.tiltalign.LocalAlignment").getStandardKey(),
      directive);
    assertEquals(
      "finds newly constructed B comparam", DirectiveDef
        .getInstance("comparam.alignb.tiltalign.LocalAlignment").getStandardKey(),
      directive);
    //
    // Are SKIP and BSKIP still correct?
    assertTrue("separateBDirective for A pair", DirectiveDef.SKIP.isAOnlyDirective());
    assertFalse("not bDirective for A pair", DirectiveDef.SKIP.isBOnlyDirective());
    assertFalse("not separateBDirective for B pair",
      DirectiveDef.BSKIP.isAOnlyDirective());
    assertTrue("bDirective for B pair", DirectiveDef.BSKIP.isBOnlyDirective());
    //
    // using prevDirectiveDefFromCsv - are they corrected?
    DirectiveDef SK = DirectiveDef.getInstance("setupset.copyarg.sk");
    DirectiveDef BSK = DirectiveDef.getInstanceFromCsv("setupset.copyarg.bsk", SK);
    assertTrue("separateBDirective for A pair", SK.isAOnlyDirective());
    assertFalse("not bDirective for A pair", SK.isBOnlyDirective());
    assertFalse("not separateBDirective for B pair", BSK.isAOnlyDirective());
    assertTrue("bDirective for B pair", BSK.isBOnlyDirective());
    //
    // B then A - are they corrected?
    DirectiveDef BS = DirectiveDef.getInstance("setupset.copyarg.bs");
    DirectiveDef S = DirectiveDef.getInstance("setupset.copyarg.s");
    assertTrue("separateBDirective for A pair", S.isAOnlyDirective());
    assertFalse("not bDirective for A pair", S.isBOnlyDirective());
    assertFalse("not separateBDirective for B pair", BS.isAOnlyDirective());
    assertTrue("bDirective for B pair", BS.isBOnlyDirective());
    //
    // A and then B - are they corrected?
    assertTrue("separateBDirective for A pair", SKI.isAOnlyDirective());
    assertFalse("not bDirective for A pair", SKI.isBOnlyDirective());
    assertFalse("not separateBDirective for B pair", BSKI.isAOnlyDirective());
    assertTrue("bDirective for B pair", BSKI.isBOnlyDirective());
    //
    // Unpaired preconstructed A - is B corrected?
    DirectiveDef BFOCUS = DirectiveDef.getInstance("setupset.copyarg.bfocus");
    assertFalse("not separateBDirective for B pair", BFOCUS.isAOnlyDirective());
    assertTrue("bDirective for B pair", BFOCUS.isBOnlyDirective());
  }

  /**
   * Testing a revised version of getName(Match,AxisDI) against the original.
   */
  /* public void testGetNameMatch() {
    DirectiveDef directiveDef = DirectiveDef.SIZE_IN_Y;
    assertEquals(directiveDef.getName())
  }*/
}
