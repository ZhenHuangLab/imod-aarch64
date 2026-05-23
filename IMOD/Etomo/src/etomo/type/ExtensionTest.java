package etomo.type;

import java.util.Iterator;

import junit.framework.TestCase;

public class ExtensionTest extends TestCase {
  public void testIsStandardizableImageFile() {
    assertFalse("ST is not a standardizable image file extension",
      Extension.ST.isStandardizableImageFile());
    assertFalse("HDF is not a standardizable image file extension",
      Extension.HDF.isStandardizableImageFile());
    assertTrue("JOIN is a standardizable image file extension",
      Extension.JOIN.isStandardizableImageFile());
    assertTrue("SINT is a standardizable image file extension",
      Extension.SINT.isStandardizableImageFile());
    assertFalse("JPG is not a standardizable image file extension",
      Extension.JPG.isStandardizableImageFile());
  }

  public void testGetInstance() {
    assertEquals("works with ext", Extension.MOD, Extension.getInstance("mod"));
    assertNull("null file name returns null", Extension.getInstance(null));
    assertNull("empty file name returns null", Extension.getInstance(""));
    assertNull("dot returns null", Extension.getInstance("."));
    assertNull("file name with no extension returns null",
      Extension.getInstance("no_extenion"));
    assertNull("file name with missing extension returns null",
      Extension.getInstance("no_extenion."));
    assertNull("file name with unrecognized extension returns null",
      Extension.getInstance("nope.foo"));
    assertEquals("gets the mrc extension", Extension.MRC,
      Extension.getInstance("yup.mrc"));
    assertEquals("the extension is at the end", Extension.ALILOG10,
      Extension.getInstance("yup.tif.alilog10"));
    assertEquals("doesn't know about standardized file names", Extension.MRC,
      Extension.getInstance("huh_bl.mrc"));
    assertEquals("works with .ext", Extension.SREC, Extension.getInstance(".srec"));
  }

  public void testGetStandardizableImageFileIterator() {
    Iterator<Extension> iterator = Extension.getStandardizableImageFileIterator();
    boolean foundHdf = false;
    boolean foundSt = false;
    boolean foundAli = false;
    boolean foundSint = false;
    boolean foundJpg = false;
    while (iterator.hasNext()) {
      Extension extension = iterator.next();
      if (extension == Extension.HDF) {
        foundHdf = true;
      }
      else if (extension == Extension.ST) {
        foundSt = true;
      }
      else if (extension == Extension.ALI) {
        foundAli = true;
      }
      else if (extension == Extension.SINT) {
        foundSint = true;
      }
      else if (extension == Extension.JPG) {
        foundJpg = true;
      }
    }
    assertFalse("HDF is not a standardizable image file", foundHdf);
    assertFalse("ST is not a standardizable image file", foundSt);
    assertTrue("ALI is a standardizable image file", foundAli);
    assertTrue("SINT is a standardizable image file", foundSint);
    assertFalse("JPG is not a standardizable image file", foundJpg);
  }

  public void testGetInputImageFileDescr() {
    String inputImageFileDescr = Extension.getInputImageFileDescr();
    assertFalse("MRC is an input image file", inputImageFileDescr.indexOf("mrc") == -1);
    assertFalse("HDF is an input image file", inputImageFileDescr.indexOf("hdf") == -1);
    assertFalse("ST is an input image file", inputImageFileDescr.indexOf("st") == -1);
    assertFalse("TIF is an input image file", inputImageFileDescr.indexOf("tif") == -1);
    assertFalse("TIFF is an input image file", inputImageFileDescr.indexOf("tiff") == -1);
    assertEquals("FLAT is not an input image file", -1,
      inputImageFileDescr.indexOf("flat"));
    assertEquals("SINT is not an input image file", -1,
      inputImageFileDescr.indexOf("sint"));
    assertEquals("JPG is not an input image file", -1,
      inputImageFileDescr.indexOf("jpg"));
  }

  public void testIsEtomoImageFile() {
    assertTrue("MRC is an etomo image file", Extension.MRC.isEtomoImageFile());
    assertTrue("HDF is an etomo image file", Extension.HDF.isEtomoImageFile());
    assertTrue("ST is an etomo image file", Extension.ST.isEtomoImageFile());
    assertTrue("TIF is an etomo image file", Extension.TIF.isEtomoImageFile());
    assertTrue("INPUT is an etomo image file", Extension.INPUT.isEtomoImageFile());
    assertTrue("SREC is an etomo image file", Extension.SREC.isEtomoImageFile());
    assertFalse("MOD is not an etomo image file", Extension.MOD.isEtomoImageFile());
  }

  public void testIsInputImageFile() {
    assertTrue("MRC is an input image file", Extension.MRC.isInputImageFile());
    assertTrue("HDF is an input image file", Extension.HDF.isInputImageFile());
    assertTrue("ST is an input image file", Extension.ST.isInputImageFile());
    assertTrue("TIF is an n image file", Extension.TIF.isInputImageFile());
    assertFalse("INPUT is not an input image file", Extension.INPUT.isInputImageFile());
    assertFalse("SREC is not an input image file", Extension.SREC.isInputImageFile());
    assertFalse("MOD is not an input image file", Extension.MOD.isInputImageFile());
  }

  public void testStaticIsInputImageFile() {
    assertTrue("MRC is an input image file, path works",
      Extension.isInputImageFile("/path/path/foo.mrc"));
    assertTrue("HDF is an input image file, just the extension works",
      Extension.isInputImageFile("hdf"));
    assertTrue("ST is an input image file, file name works",
      Extension.isInputImageFile("foo.st"));
    assertTrue("TIF is an input image file, .ext works",
      Extension.isInputImageFile(".tif"));
    assertFalse("INPUT is not an input image file", Extension.isInputImageFile("input"));
    assertFalse("handles null", Extension.isInputImageFile(null));
    assertFalse("handles bad data", Extension.isInputImageFile("."));
    assertFalse("handles unknown extension", Extension.isInputImageFile("foo"));
  }

  public void testIsVariable() {
    assertFalse("MRC is not a variable extension", Extension.MRC.isVariable());
    assertFalse("TIFF is not a variable extension", Extension.TIFF.isVariable());
    assertFalse("JOIN is not a variable extension", Extension.JOIN.isVariable());
    assertTrue("SINT is a variable extension", Extension.SINT.isVariable());
    assertTrue("SREC is a variable extension", Extension.SREC.isVariable());
    assertFalse("PNG is not a variable extension", Extension.PNG.isVariable());
  }

  public void testGetFileNumber() {
    assertNull("does not throw null pointer exception",
      Extension.HDF.getFileNumber(null, null));
    assertNull("not a variable extension",
      Extension.JOIN.getFileNumber("/path/path/path/file_join.mrc", null));
    assertNull("does not throw null pointer exception",
      Extension.MRC.getFileNumber(null, ImageFilenameStyle.OLD));
    assertNull("empty file name - returns null",
      Extension.ST.getFileNumber("", ImageFilenameStyle.OLD));
    assertNull("dot - returns null",
      Extension.ST.getFileNumber(".", ImageFilenameStyle.OLD));
    assertNull("not a variable extension",
      Extension.TIF.getFileNumber("tif", ImageFilenameStyle.OLD));
    assertNull("not a variable extension",
      Extension.TIFF.getFileNumber(".tiff", ImageFilenameStyle.OLD));
    assertNull("not a variable extension",
      Extension.NAD.getFileNumber("file_nad.hdf", ImageFilenameStyle.HDF));
    assertNull("not a variable extension",
      Extension.PREALI.getFileNumber("file.preali", ImageFilenameStyle.OLD));
    assertNull("numbers are required in variable extensions",
      Extension.SINT.getFileNumber("file.sint", ImageFilenameStyle.OLD));
    assertNull("at least 2 digits are required in variable extensions",
      Extension.SREC.getFileNumber("file_srec5.mrc", ImageFilenameStyle.MRC));
    assertNull("an integer is required in a variable extension",
      Extension.SINT.getFileNumber("file_sint5.3.hdf", ImageFilenameStyle.HDF));
    assertEquals("2 digits is valid in a variable extension",
      Extension.SREC.getFileNumber("file.srec53", ImageFilenameStyle.OLD).getLong(), 53);
    assertEquals("3 digits is valid in a variable extension",
      Extension.SINT.getFileNumber("file_sint531.mrc", ImageFilenameStyle.MRC).getLong(),
      531);
    assertEquals(
      "number of digits may increase and still be valid in a variable extension",
      Extension.SREC
        .getFileNumber("file_srec5313093480968394.hdf", ImageFilenameStyle.HDF).getLong(),
      5313093480968394l);
    assertEquals("_sub .sint files work", Extension.SINT
      .getFileNumber("file_sub_sint53.mrc", ImageFilenameStyle.MRC).getLong(), 53);
    assertEquals("_sub .sint files work",
      Extension.SINT.getFileNumber("file_sub.sint53", ImageFilenameStyle.OLD).getLong(),
      53);
    assertEquals("_sub .srec files work", Extension.SREC
      .getFileNumber("file_sub_srec53.mrc", ImageFilenameStyle.MRC).getLong(), 53);
    assertEquals("_sub .srec files work",
      Extension.SREC.getFileNumber("file_sub.srec53", ImageFilenameStyle.OLD).getLong(),
      53);
    assertNull("unmatched variable extension returns null",
      Extension.SINT.getFileNumber("file_sub.srec53", ImageFilenameStyle.OLD));
  }

  public void testFileNameEndsWith() {
    assertFalse("does not throw null pointer exception",
      Extension.HDF.fileNameEndsWith(null, null));
    // FIXME 2206
    /* assertTrue("defaults to mrc file style",
      Extension.JOIN.fileNameEndsWith(null, "/path/path/path/file_join.mrc"));*/
    assertFalse("does not throw null pointer exception",
      Extension.MRC.fileNameEndsWith(ImageFilenameStyle.OLD, null));
    assertFalse("empty file name - returns false",
      Extension.ST.fileNameEndsWith(ImageFilenameStyle.OLD, ""));
    assertFalse("dot - returns false",
      Extension.ST.fileNameEndsWith(ImageFilenameStyle.OLD, "."));
    assertTrue("recognizes extension alone",
      Extension.TIF.fileNameEndsWith(ImageFilenameStyle.OLD, "tif"));
    assertTrue("recognizes extension with dot",
      Extension.TIFF.fileNameEndsWith(ImageFilenameStyle.OLD, ".tiff"));
    assertTrue("standardizes old style image names",
      Extension.NAD.fileNameEndsWith(ImageFilenameStyle.HDF, "file_nad.hdf"));
    assertTrue("does not standardize with old style in force",
      Extension.PREALI.fileNameEndsWith(ImageFilenameStyle.OLD, "file.preali"));
    assertFalse("does not standardize with old style in force",
      Extension.REC.fileNameEndsWith(ImageFilenameStyle.OLD, "file_rec.st"));
    assertFalse("numbers are required in variable extensions",
      Extension.SINT.fileNameEndsWith(ImageFilenameStyle.OLD, "file.sint"));
    assertFalse("at least 2 digits are required in variable extensions",
      Extension.SREC.fileNameEndsWith(ImageFilenameStyle.MRC, "file_srec5.mrc"));
    assertFalse("an integer is required in a variable extension",
      Extension.SINT.fileNameEndsWith(ImageFilenameStyle.HDF, "file_sint5.3.hdf"));
    assertTrue("2 digits is valid in a variable extension",
      Extension.SREC.fileNameEndsWith(ImageFilenameStyle.OLD, "file.srec53"));
    assertTrue("3 digits is valid in a variable extension",
      Extension.SINT.fileNameEndsWith(ImageFilenameStyle.MRC, "file_sint531.mrc"));
    assertTrue("number of digits may increase and still be valid in a variable extension",
      Extension.SREC.fileNameEndsWith(ImageFilenameStyle.HDF,
        "file_srec5313093480968394.hdf"));
    assertFalse("does not standardize other image names",
      Extension.JPG.fileNameEndsWith(ImageFilenameStyle.MRC, "file_jpg.hdf"));
    assertTrue("knowns about other image names",
      Extension.MOD.fileNameEndsWith(ImageFilenameStyle.MRC, "file.mod"));
  }

  public void testGetSuffix() {
    // FIXME 2206
    /*assertEquals("the default is mrc file style", "_sampavg.mrc",
      Extension.SAMPAVG.getSuffix(null));*/
    assertEquals("file style has no effect on non-old-style file extensions", ".hdf",
      Extension.HDF.getSuffix(ImageFilenameStyle.OLD));
    assertEquals("file style has no effect on non-old-style file extensions", ".hdf",
      Extension.HDF.getSuffix(ImageFilenameStyle.MRC));
    assertEquals("file style has no effect on non-old-style file extensions", ".hdf",
      Extension.HDF.getSuffix(ImageFilenameStyle.HDF));
    assertEquals("file style has no effect on non-old-style file extensions", ".st",
      Extension.ST.getSuffix(ImageFilenameStyle.HDF));
    assertEquals("file style has no effect on non-old-style file extensions", ".png",
      Extension.PNG.getSuffix(ImageFilenameStyle.HDF));
    assertEquals("file style effects old style image file extensions", ".sample",
      Extension.SAMPLE.getSuffix(ImageFilenameStyle.OLD));
    assertEquals("file style effects old style image file extensions", "_sample.mrc",
      Extension.SAMPLE.getSuffix(ImageFilenameStyle.MRC));
    assertEquals("file style effects old style image file extensions", "_sample.hdf",
      Extension.SAMPLE.getSuffix(ImageFilenameStyle.HDF));
    assertEquals(
      "variable extension suffixes are incorrect - returned without variable portion",
      ".sint", Extension.SINT.getSuffix(ImageFilenameStyle.OLD));
    assertEquals(
      "variable extension suffixes are incorrect - returned without variable portion",
      "_sint.mrc", Extension.SINT.getSuffix(ImageFilenameStyle.MRC));
  }

  public void testIsStandardImageFileExtension() {
    assertTrue("MRC is a standard image file extension",
      Extension.MRC.isStandardImageFileExtension());
    assertTrue("HDF is a standard image file extension",
      Extension.HDF.isStandardImageFileExtension());
    assertFalse("ST is not a standard image file extension",
      Extension.ST.isStandardImageFileExtension());
    assertFalse("TIF is not a standard image file extension",
      Extension.TIF.isStandardImageFileExtension());
    assertFalse("SQZ is not a standard image file extension",
      Extension.SQZ.isStandardImageFileExtension());
    assertFalse("SREC is not a standard image file extension",
      Extension.SREC.isStandardImageFileExtension());
    assertFalse("PNG is not a standard image file extension",
      Extension.PNG.isStandardImageFileExtension());
  }

  public void testIsCompatible() {
    assertTrue("null extension marker is always compatible",
      Extension.JPG.isCompatible(null));
    assertTrue("generic extension marker is always compatible",
      Extension.MOD.isCompatible(ExtensionMarker.GENERIC));
    assertFalse("input image extension marker is only compatible with input images",
      Extension.PNG.isCompatible(ExtensionMarker.INPUT_IMAGE));
    assertFalse(
      "image extension marker is only compatible with standard image file extensions",
      Extension.JPG.isCompatible(ExtensionMarker.INPUT_IMAGE));
    assertTrue("ST is compatible with input image extension marker",
      Extension.ST.isCompatible(ExtensionMarker.INPUT_IMAGE));
    assertFalse("ST is not compatible with image extension marker",
      Extension.ST.isCompatible(ExtensionMarker.IMAGE));
    assertTrue("MRC is compatible with  image extension marker",
      Extension.MRC.isCompatible(ExtensionMarker.IMAGE));
    assertTrue("MRC is also compatible with input image extension marker",
      Extension.MRC.isCompatible(ExtensionMarker.INPUT_IMAGE));
  }
}
