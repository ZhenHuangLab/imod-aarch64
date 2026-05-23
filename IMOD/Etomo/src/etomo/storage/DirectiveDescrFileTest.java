package etomo.storage;

import java.io.File;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.type.AxisID;
import etomo.type.FileType;
import etomo.util.TestUtilites;
import junit.framework.TestCase;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public class DirectiveDescrFileTest extends TestCase {
  public static final String rcsid = "$Id:$";

  public void testIterator() {
    BaseManager manager = (BaseManager) EtomoDirector.INSTANCE
        .getCurrentManagerForTest();
    DirectiveDescrFile descrFile = DirectiveDescrFile.INSTANCE;
    AxisID axisID = AxisID.ONLY;
    descrFile
        .setFile(new File(TestUtilites.INSTANCE.getUnitTestData().getAbsolutePath(),
            FileType.DIRECTIVES_DESCR.getFileName(manager, axisID)));
    DirectiveDescrFile.Iterator iterator = descrFile.getIterator(manager, axisID);
    DirectiveDescrElement element = iterator.nextElement();
    element = iterator.nextElement();
    element = iterator.nextElement();
    assertTrue("blank", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("blank", element.isSection());
    assertFalse("blank", element.isDirective());
    assertNull("blank", element.getName());
    assertNull("blank", element.getDescription());
    assertEquals("blank", element.getValueType(), DirectiveValueType.UNKNOWN);
    assertFalse("blank", element.isBatch());
    assertFalse("blank", element.isTemplate());
    assertNull("blank", element.getEtomoColumn());

    assertTrue("header - Arguments to copytomocoms", iterator.hasNext());
    element = iterator.nextElement();
    assertTrue("header - Arguments to copytomocoms", element.isSection());
    assertFalse("header - Arguments to copytomocoms", element.isDirective());
    assertEquals("header - Arguments to copytomocoms", "Arguments to copytomocoms",
        element.getName());
    assertNull("header - Arguments to copytomocoms", element.getDescription());
    assertEquals("header - Arguments to copytomocoms", element.getValueType(),
        DirectiveValueType.UNKNOWN);
    assertFalse("header - Arguments to copytomocoms", element.isBatch());
    assertFalse("header - Arguments to copytomocoms", element.isTemplate());
    assertNull("header - Arguments to copytomocoms", element.getEtomoColumn());

    assertTrue("name", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("name", element.isSection());
    assertTrue("name", element.isDirective());
    assertEquals("name", "setupset.copyarg.name", element.getName());
    assertEquals("name", "Root name of data set", element.getDescription());
    assertTrue("name", element.getValueType() == DirectiveValueType.STRING);
    assertTrue("name", element.isBatch());
    assertFalse("name", element.isTemplate());
    assertNull("name", element.getEtomoColumn());

    assertTrue("dual", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("dual", element.isSection());
    assertTrue("dual", element.isDirective());
    assertEquals("dual", "setupset.copyarg.dual", element.getName());
    assertEquals("dual", "Dual-axis data set", element.getDescription());
    assertTrue("dual", element.getValueType() == DirectiveValueType.BOOLEAN);
    assertTrue("dual", element.isBatch());
    assertTrue("dual", element.isTemplate());
    assertTrue("dual", element.getEtomoColumn() == DirectiveDescrEtomoColumn.SO);

    assertTrue("montage", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("montage", element.isSection());
    assertTrue("montage", element.isDirective());
    assertEquals("montage", "setupset.copyarg.montage", element.getName());
    assertEquals("montage", "Data are montaged", element.getDescription());
    assertTrue("montage", element.getValueType() == DirectiveValueType.BOOLEAN);
    assertTrue("montage", element.isBatch());
    assertTrue("montage", element.isTemplate());
    assertTrue("montage", element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    assertTrue("pixel", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("pixel", element.isSection());
    assertTrue("pixel", element.isDirective());
    assertEquals("pixel", "setupset.copyarg.pixel", element.getName());
    assertEquals("pixel", "Pixel size of images in nanometers",
        element.getDescription());
    assertTrue("pixel", element.getValueType() == DirectiveValueType.FLOATING_POINT);
    assertTrue("pixel", element.isBatch());
    assertTrue("pixel", element.isTemplate());
    assertNull("pixel", element.getEtomoColumn());

    // gold
    iterator.nextElement();
    // rotation
    iterator.nextElement();
    // brotation
    iterator.nextElement();

    assertTrue("firstinc", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("firstinc", element.isSection());
    assertTrue("firstinc", element.isDirective());
    assertEquals("firstinc", "setupset.copyarg.firstinc", element.getName());
    assertEquals("firstinc", "First tilt angle and tilt angle increment",
        element.getDescription());
    assertTrue("firstinc",
        element.getValueType() == DirectiveValueType.FLOATING_POINT_PAIR);
    assertTrue("firstinc", element.isBatch());
    assertFalse("firstinc", element.isTemplate());
    assertNull("firstinc", element.getEtomoColumn());

    // bfirstinc
    iterator.nextElement();
    // userawtlt
    iterator.nextElement();
    // buserawtlt
    iterator.nextElement();
    // extract
    iterator.nextElement();
    // bextract
    iterator.nextElement();

    assertTrue("skip", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("skip", element.isSection());
    assertTrue("skip", element.isDirective());
    assertEquals("skip", "setupset.copyarg.skip", element.getName());
    assertEquals("skip",
        "List of views to exclude from processing for A or only axis",
        element.getDescription());
    assertTrue("skip", element.getValueType() == DirectiveValueType.LIST);
    assertTrue("skip", element.isBatch());
    assertFalse("skip", element.isTemplate());
    assertNull("skip", element.getEtomoColumn());

    // bskip
    iterator.nextElement();
    // distort
    iterator.nextElement();

    assertTrue("binning", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("binning", element.isSection());
    assertTrue("binning", element.isDirective());
    assertEquals("binning", "setupset.copyarg.binning", element.getName());
    assertEquals("binning", "Binning of raw images", element.getDescription());
    assertTrue("binning", element.getValueType() == DirectiveValueType.INTEGER);
    assertTrue("binning", element.isBatch());
    assertTrue("binning", element.isTemplate());
    assertNull("binning", element.getEtomoColumn());

    // gradient
    iterator.nextElement();
    // focus
    iterator.nextElement();
    // bfocus
    iterator.nextElement();
    // defocus
    iterator.nextElement();
    // voltage
    iterator.nextElement();

    assertTrue("Cs", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("Cs", element.isSection());
    assertTrue("Cs", element.isDirective());
    assertEquals("Cs", "setupset.copyarg.Cs", element.getName());
    assertEquals("Cs", "Spherical aberration", element.getDescription());
    assertTrue("Cs", element.getValueType() == DirectiveValueType.FLOATING_POINT);
    assertTrue("Cs", element.isBatch());
    assertTrue("Cs", element.isTemplate());
    assertTrue("Cs", element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    // ctfnoise
    iterator.nextElement();
    // blank
    iterator.nextElement();
    // header - Other setup parameters
    iterator.nextElement();

    assertTrue("scopeTemplate", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("scopeTemplate", element.isSection());
    assertTrue("scopeTemplate", element.isDirective());
    assertEquals("scopeTemplate", "setupset.scopeTemplate", element.getName());
    assertEquals("scopeTemplate", "Name of scope template file to use",
        element.getDescription());
    assertTrue("scopeTemplate", element.getValueType() == DirectiveValueType.STRING);
    assertTrue("scopeTemplate", element.isBatch());
    assertFalse("scopeTemplate", element.isTemplate());
    assertNull("scopeTemplate", element.getEtomoColumn());

    // systemTemplate
    iterator.nextElement();
    // userTemplate
    iterator.nextElement();
    // scanHeader
    iterator.nextElement();
    // datasetDirectory
    iterator.nextElement();
    // blank
    iterator.nextElement();
    // header - Preprocessing
    iterator.nextElement();

    assertTrue("removeXrays", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("removeXrays", element.isSection());
    assertTrue("removeXrays", element.isDirective());
    assertEquals("removeXrays", "runtime.Preprocessing.any.removeXrays",
        element.getName());
    assertEquals("removeXrays", "Run ccderaser to remove X rays",
        element.getDescription());
    assertTrue("removeXrays", element.getValueType() == DirectiveValueType.BOOLEAN);
    assertTrue("removeXrays", element.isBatch());
    assertFalse("removeXrays", element.isTemplate());
    assertTrue("removeXrays",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.NE);

    assertTrue("PeakCriterion", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("PeakCriterion", element.isSection());
    assertTrue("PeakCriterion", element.isDirective());
    assertEquals("PeakCriterion", "comparam.eraser.ccderaser.PeakCriterion",
        element.getName());
    assertEquals("PeakCriterion", "Peak criterion # of SDs",
        element.getDescription());
    assertTrue("PeakCriterion",
        element.getValueType() == DirectiveValueType.FLOATING_POINT);
    assertTrue("PeakCriterion", element.isBatch());
    assertTrue("PeakCriterion", element.isTemplate());
    assertTrue("PeakCriterion",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    // DiffCriterion
    iterator.nextElement();
    // MaximumRadius
    iterator.nextElement();
    // ModelFile
    iterator.nextElement();
    // LineObjects
    iterator.nextElement();
    // BoundaryObjects
    iterator.nextElement();
    // AllSectionObjects
    iterator.nextElement();
    // blank
    iterator.nextElement();
    // header - Coarse alignment
    iterator.nextElement();
    // FilterRadius2
    iterator.nextElement();
    // FilterSigma2
    iterator.nextElement();
    // fiducialless
    iterator.nextElement();

    assertTrue("newstack.BinByFactor", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("newstack.BinByFactor", element.isSection());
    assertTrue("newstack.BinByFactor", element.isDirective());
    assertEquals("newstack.BinByFactor", "comparam.prenewst.newstack.BinByFactor",
        element.getName());
    assertEquals("newstack.BinByFactor", "Coarse aligned stack binning",
        element.getDescription());
    assertTrue("newstack.BinByFactor",
        element.getValueType() == DirectiveValueType.INTEGER);
    assertTrue("newstack.BinByFactor", element.isBatch());
    assertTrue("newstack.BinByFactor", element.isTemplate());
    assertTrue("newstack.BinByFactor",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    // ModeToOutput
    iterator.nextElement();

    assertTrue("blendmont.BinByFactor", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("blendmont.BinByFactor", element.isSection());
    assertTrue("blendmont.BinByFactor", element.isDirective());
    assertEquals("blendmont.BinByFactor", "comparam.preblend.blendmont.BinByFactor",
        element.getName());
    assertEquals("blendmont.BinByFactor", "Coarse aligned stack binning",
        element.getDescription());
    assertTrue("blendmont.BinByFactor",
        element.getValueType() == DirectiveValueType.INTEGER);
    assertTrue("blendmont.BinByFactor", element.isBatch());
    assertTrue("blendmont.BinByFactor", element.isTemplate());
    assertTrue("blendmont.BinByFactor",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    // blank
    iterator.nextElement();
    // header - Tracking choices
    iterator.nextElement();
    // trackingMethod
    iterator.nextElement();
    // seedingMethod
    iterator.nextElement();
    // blank
    iterator.nextElement();
    // header - Beadtracking
    iterator.nextElement();
    // LightBeads
    iterator.nextElement();
    // LocalAreaTracking
    iterator.nextElement();

    assertTrue("LocalAreaTargetSize", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("LocalAreaTargetSize", element.isSection());
    assertTrue("LocalAreaTargetSize", element.isDirective());
    assertEquals("LocalAreaTargetSize",
        "comparam.track.beadtrack.LocalAreaTargetSize", element.getName());
    assertEquals("LocalAreaTargetSize", "Size of local areas",
        element.getDescription());
    assertTrue("LocalAreaTargetSize",
        element.getValueType() == DirectiveValueType.INTEGER_PAIR);
    assertTrue("LocalAreaTargetSize", element.isBatch());
    assertTrue("LocalAreaTargetSize", element.isTemplate());
    assertTrue("LocalAreaTargetSize",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.SD);

    // SobelFilterCentering
    iterator.nextElement();
    // KernelSigmaForSobel
    iterator.nextElement();
    // RoundsOfTracking
    iterator.nextElement();
    // NumberOfRuns
    iterator.nextElement();
    // blank
    iterator.nextElement();
    // header - Auto seed finding
    iterator.nextElement();

    assertTrue("TwoSurfaces", iterator.hasNext());
    element = iterator.nextElement();
    assertFalse("TwoSurfaces", element.isSection());
    assertTrue("TwoSurfaces", element.isDirective());
    assertEquals("TwoSurfaces", "comparam.autofidseed.autofidseed.TwoSurfaces",
        element.getName());
    assertEquals("TwoSurfaces", "Whether beads on 2 surfaces",
        element.getDescription());
    assertTrue("TwoSurfaces", element.getValueType() == DirectiveValueType.BOOLEAN);
    assertTrue("TwoSurfaces", element.isBatch());
    assertTrue("TwoSurfaces", element.isTemplate());
    assertTrue("TwoSurfaces",
        element.getEtomoColumn() == DirectiveDescrEtomoColumn.NES);

    DirectiveDescrFile.INSTANCE.releaseIterator(iterator);
  }
}
