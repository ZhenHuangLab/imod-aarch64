package etomo.type;

import java.io.File;
import java.util.Properties;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoDatasetMetaData {
  public static final String rcsid = "$Id:$";

  private static final String GROUP_KEY = "dataset";
  private static final String HEADER_KEY = "header";
  public static final String SCALE_FROM_Z_DEFAULT = "0.33";

  private final EtomoBoolean2 dataset = new EtomoBoolean2();
  private final StringProperty modelFile = new StringProperty("ModelFile");
  private final EtomoNumber enableStretching =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "enableStretching");
  private final EtomoNumber localAlignments =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "LocalAlignments");
  private final EtomoNumber gold = new EtomoNumber(EtomoNumber.Type.DOUBLE, "gold");
  private final EtomoNumber targetNumberOfBeads =
    new EtomoNumber(EtomoNumber.Type.INTEGER, "TargetNumberOfBeads");
  private final StringProperty localAreaTargetSize =
    new StringProperty("LocalAreaTargetSize");
  private final StringProperty sizeOfPatchesXandY =
    new StringProperty("SizeOfPatchesXandY");
  private final EtomoNumber lengthOfPieces =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "LengthOfPieces");
  private final StringProperty defocus = new StringProperty("defocus");
  private final EtomoNumber autoFitRangeAndStep =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "autoFitRangeAndStep");
  private final EtomoNumber autoFitRange =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "autoFitRange");
  private final EtomoNumber fitEveryImage =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "fitEveryImage");
  private final EtomoNumber autoFitStep =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "autoFitStep");
  private final EtomoNumber useFakeSIRTiterations =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "Use.fakeSIRTiterations");
  private final EtomoNumber fakeSIRTiterations =
    new EtomoNumber(EtomoNumber.Type.INTEGER, "fakeSIRTiterations");
  private final StringProperty leaveIterations = new StringProperty("LeaveIterations");
  private final EtomoNumber scaleToInteger =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "ScaleToInteger");
  private final EtomoNumber thickness = new EtomoNumber("THICKNESS");
  private final EtomoNumber binnedThickness = new EtomoNumber("binnedThickness");
  private final EtomoNumber extraThickness = new EtomoNumber("extraThickness");
  private final EtomoNumber fallbackThickness = new EtomoNumber("fallbackThickness");
  private final EtomoNumber prenewstBinByFactor = new EtomoNumber("Prenewst.BinByFactor");
  private final EtomoNumber preblendBinByFactor = new EtomoNumber("Preblend.BinByFactor");
  private final EtomoNumber findSecAddThickness =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "findSecAddThickness");
  private final EtomoNumber useFindSecAddThickness =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "Use.findSecAddThickness");
  private final EtomoNumber useScaleFromZ =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "Use.fcaleFromZ");
  private final EtomoNumber scaleFromZ =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "fcaleFromZ");
  private final EtomoNumber eraseGoldFid =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "eraseGold.Fid");
  private final EtomoNumber eraseGold3d =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "eraseGold.3d");
  private final EtomoNumber goldErasingThickness =
    new EtomoNumber(EtomoNumber.Type.INTEGER, "GoldErasing.thickness");
  private final EtomoNumber sampleTypePlasticSection =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "sampleType.PlasticSection");
  private final EtomoNumber sampleTypeCryo =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "sampleType.Cryo");
  private final EtomoNumber positioningThickness =
    new EtomoNumber(EtomoNumber.Type.INTEGER, "Positioning.thickness");
  private final EtomoNumber hasGoldBeads =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "hasGoldBeads");
  private final EtomoNumber positioningGold =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "Positioning.gold");
  private final PanelHeaderSettings postprocessingHeader =
    new PanelHeaderSettings("Postprocessing.header");

  private PanelHeaderSettings header = null;

  BatchRunTomoDatasetMetaData() {
    dataset.set(true);
  }

  public static String createPrepend(String prepend) {
    if (prepend == null || prepend.matches("\\s*")) {
      return GROUP_KEY;
    }
    prepend = prepend.trim();
    if (prepend.endsWith(".")) {
      return prepend + GROUP_KEY;
    }
    return prepend + "." + GROUP_KEY;
  }

  static boolean exists(final Properties props, String prepend) {
    prepend = createPrepend(prepend);
    EtomoBoolean2 bool = new EtomoBoolean2();
    bool.set(props.getProperty(prepend));
    return bool.is();
  }

  void reset() {
    dataset.reset();
    if (header != null) {
      header.reset();
    }
    postprocessingHeader.reset();
    modelFile.reset();
    enableStretching.reset();
    localAlignments.reset();
    gold.reset();
    targetNumberOfBeads.reset();
    localAreaTargetSize.reset();
    sizeOfPatchesXandY.reset();
    lengthOfPieces.reset();
    defocus.reset();
    autoFitRangeAndStep.reset();
    autoFitRange.reset();
    fitEveryImage.reset();
    autoFitStep.reset();
    useFakeSIRTiterations.reset();
    fakeSIRTiterations.reset();
    leaveIterations.reset();
    scaleToInteger.reset();
    thickness.reset();
    binnedThickness.reset();
    extraThickness.reset();
    fallbackThickness.reset();
    prenewstBinByFactor.reset();
    preblendBinByFactor.reset();
    findSecAddThickness.reset();
    useFindSecAddThickness.reset();
    useScaleFromZ.reset();
    scaleFromZ.set(SCALE_FROM_Z_DEFAULT);
    eraseGoldFid.reset();
    eraseGold3d.reset();
    goldErasingThickness.reset();
    sampleTypePlasticSection.reset();
    sampleTypeCryo.reset();
    positioningThickness.reset();
    hasGoldBeads.reset();
    positioningGold.reset();
  }

  public void load(final Properties props, String prepend) {
    // reset
    reset();
    // load
    prepend = createPrepend(prepend);
    dataset.set(props.getProperty(prepend));
    if (!dataset.is()) {
      return;
    }
    header = PanelHeaderSettings.load(header, HEADER_KEY, props, prepend);
    postprocessingHeader.load(props, prepend);
    modelFile.load(props, prepend);
    enableStretching.load(props, prepend);
    localAlignments.load(props, prepend);
    gold.load(props, prepend);
    targetNumberOfBeads.load(props, prepend);
    localAreaTargetSize.load(props, prepend);
    sizeOfPatchesXandY.load(props, prepend);
    lengthOfPieces.load(props, prepend);
    defocus.load(props, prepend);
    autoFitRangeAndStep.load(props, prepend);
    autoFitRange.load(props, prepend);
    fitEveryImage.load(props, prepend);
    autoFitStep.load(props, prepend);
    useFakeSIRTiterations.load(props, prepend);
    fakeSIRTiterations.load(props, prepend);
    leaveIterations.load(props, prepend);
    scaleToInteger.load(props, prepend);
    thickness.load(props, prepend);
    binnedThickness.load(props, prepend);
    extraThickness.load(props, prepend);
    fallbackThickness.load(props, prepend);
    prenewstBinByFactor.load(props, prepend);
    preblendBinByFactor.load(props, prepend);
    findSecAddThickness.load(props, prepend);
    useFindSecAddThickness.load(props, prepend);
    useScaleFromZ.load(props, prepend);
    scaleFromZ.load(props, prepend);
    eraseGoldFid.load(props, prepend);
    eraseGold3d.load(props, prepend);
    goldErasingThickness.load(props, prepend);
    sampleTypePlasticSection.load(props, prepend);
    sampleTypeCryo.load(props, prepend);
    positioningThickness.load(props, prepend);
    hasGoldBeads.load(props, prepend);
    positioningGold.load(props, prepend);
  }

  public void store(Properties props, String prepend) {
    if (!dataset.is()) {
      remove(props, prepend);
    }
    else {
      prepend = createPrepend(prepend);
      if (header != null) {
        header.store(props, prepend);
      }
      props.setProperty(prepend, dataset.toString());
      postprocessingHeader.store(props, prepend);
      modelFile.store(props, prepend);
      enableStretching.store(props, prepend);
      localAlignments.store(props, prepend);
      gold.store(props, prepend);
      targetNumberOfBeads.store(props, prepend);
      localAreaTargetSize.store(props, prepend);
      sizeOfPatchesXandY.store(props, prepend);
      lengthOfPieces.store(props, prepend);
      defocus.store(props, prepend);
      autoFitRangeAndStep.store(props, prepend);
      autoFitRange.store(props, prepend);
      fitEveryImage.store(props, prepend);
      autoFitStep.store(props, prepend);
      useFakeSIRTiterations.store(props, prepend);
      fakeSIRTiterations.store(props, prepend);
      leaveIterations.store(props, prepend);
      scaleToInteger.store(props, prepend);
      thickness.store(props, prepend);
      binnedThickness.store(props, prepend);
      extraThickness.store(props, prepend);
      fallbackThickness.store(props, prepend);
      prenewstBinByFactor.store(props, prepend);
      preblendBinByFactor.store(props, prepend);
      findSecAddThickness.store(props, prepend);
      useFindSecAddThickness.store(props, prepend);
      useScaleFromZ.store(props, prepend);
      scaleFromZ.store(props, prepend);
      eraseGoldFid.store(props, prepend);
      eraseGold3d.store(props, prepend);
      goldErasingThickness.store(props, prepend);
      sampleTypePlasticSection.store(props, prepend);
      sampleTypeCryo.store(props, prepend);
      positioningThickness.store(props, prepend);
      hasGoldBeads.store(props, prepend);
      positioningGold.store(props, prepend);
    }
  }

  public void remove(Properties props, String prepend) {
    prepend = createPrepend(prepend);
    if (header != null) {
      header.remove(props, prepend);
    }
    props.remove(prepend);
    postprocessingHeader.remove(props, prepend);
    modelFile.remove(props, prepend);
    enableStretching.remove(props, prepend);
    localAlignments.remove(props, prepend);
    gold.remove(props, prepend);
    targetNumberOfBeads.remove(props, prepend);
    localAreaTargetSize.remove(props, prepend);
    sizeOfPatchesXandY.remove(props, prepend);
    lengthOfPieces.remove(props, prepend);
    defocus.remove(props, prepend);
    autoFitRangeAndStep.remove(props, prepend);
    autoFitRange.remove(props, prepend);
    fitEveryImage.remove(props, prepend);
    autoFitStep.remove(props, prepend);
    useFakeSIRTiterations.remove(props, prepend);
    fakeSIRTiterations.remove(props, prepend);
    leaveIterations.remove(props, prepend);
    scaleToInteger.remove(props, prepend);
    thickness.remove(props, prepend);
    binnedThickness.remove(props, prepend);
    extraThickness.remove(props, prepend);
    fallbackThickness.remove(props, prepend);
    prenewstBinByFactor.remove(props, prepend);
    preblendBinByFactor.remove(props, prepend);
    findSecAddThickness.remove(props, prepend);
    useFindSecAddThickness.remove(props, prepend);
    useScaleFromZ.remove(props, prepend);
    scaleFromZ.remove(props, prepend);
    eraseGoldFid.remove(props, prepend);
    eraseGold3d.remove(props, prepend);
    goldErasingThickness.remove(props, prepend);
    sampleTypePlasticSection.remove(props, prepend);
    sampleTypeCryo.remove(props, prepend);
    positioningThickness.remove(props, prepend);
    hasGoldBeads.remove(props, prepend);
    positioningGold.remove(props, prepend);
  }

  public ConstPanelHeaderSettings getHeader() {
    return header;
  }

  public ConstPanelHeaderSettings getPostprocessingHeader() {
    return postprocessingHeader;
  }

  public void setHeader(final ConstPanelHeaderSettings input) {
    if (input == null) {
      return;
    }
    if (header == null) {
      header = new PanelHeaderSettings(HEADER_KEY);
    }
    header.set(input);
  }

  public void setPostprocessingHeader(final ConstPanelHeaderSettings input) {
    postprocessingHeader.set(input);
  }

  public void setDataset(final boolean input) {
    dataset.set(input);
  }

  public void setFitEveryImage(final boolean input) {
    fitEveryImage.set(input);
  }

  public ConstEtomoNumber getFitEveryImage() {
    return fitEveryImage;
  }

  public void setLocalAlignments(final boolean input) {
    localAlignments.set(input);
  }

  public ConstEtomoNumber getLocalAlignments() {
    return localAlignments;
  }

  public void setAutoFitRange(final String input) {
    autoFitRange.set(input);
  }

  public String getAutoFitRange() {
    return autoFitRange.toString();
  }

  public void setAutoFitStep(final String input) {
    autoFitStep.set(input);
  }

  public String getAutoFitStep() {
    return autoFitStep.toString();
  }

  public void setLengthOfPieces(final boolean input) {
    lengthOfPieces.set(input);
  }

  public ConstEtomoNumber getLengthOfPieces() {
    return lengthOfPieces;
  }

  public void setDefocus(final String input) {
    defocus.set(input);
  }

  public String getDefocus() {
    return defocus.toString();
  }

  public void setThickness(final String input) {
    thickness.set(input);
  }

  public String getThickness() {
    return thickness.toString();
  }

  public void setBinnedThickness(final String input) {
    binnedThickness.set(input);
  }

  public String getBinnedThickness() {
    return binnedThickness.toString();
  }

  public void setExtraThickness(final String input) {
    extraThickness.set(input);
  }

  public String getExtraThickness() {
    return extraThickness.toString();
  }

  public void setFindSecAddThickness(final String input) {
    findSecAddThickness.set(input);
  }

  public void setUseFakeSIRTiterations(final boolean input) {
    useFakeSIRTiterations.set(input);
  }

  public void setUseFindSecAddThickness(final boolean input) {
    useFindSecAddThickness.set(input);
  }

  public void setUseScaleFromZ(final boolean input) {
    useScaleFromZ.set(input);
  }

  public void setEraseGoldFid(final boolean input) {
    eraseGoldFid.set(input);
  }

  public void setEraseGold3d(final boolean input) {
    eraseGold3d.set(input);
  }

  public void setSampleTypePlasticSection(final boolean input) {
    sampleTypePlasticSection.set(input);
  }

  public void setSampleTypeCryo(final boolean input) {
    sampleTypeCryo.set(input);
  }

  public void setHasGoldBeads(final boolean input) {
    hasGoldBeads.set(input);
  }

  public void setScaleFromZ(final String input) {
    scaleFromZ.set(input);
  }

  public void setGoldErasingThickness(final String input) {
    goldErasingThickness.set(input);
  }

  public void setPositioningThickness(final String input) {
    positioningThickness.set(input);
  }

  public void setPositioningGold(final String input) {
    positioningGold.set(input);
  }

  public void setFallbackThickness(final String input) {
    fallbackThickness.set(input);
  }

  public void setPrenewstBinByFactor(final Number input) {
    prenewstBinByFactor.set(input);
  }

  public void setPreblendBinByFactor(final Number input) {
    preblendBinByFactor.set(input);
  }

  public String getFindSecAddThickness() {
    return findSecAddThickness.toString();
  }

  public ConstEtomoNumber getUseScaleFromZ() {
    return useScaleFromZ;
  }

  public ConstEtomoNumber getEraseGoldFid() {
    return eraseGoldFid;
  }

  public ConstEtomoNumber getEraseGold3d() {
    return eraseGold3d;
  }

  public ConstEtomoNumber getSampleTypePlasticSection() {
    return sampleTypePlasticSection;
  }

  public ConstEtomoNumber getSampleTypeCryo() {
    return sampleTypeCryo;
  }

  public ConstEtomoNumber getHasGoldBeads() {
    return hasGoldBeads;
  }

  public String getScaleFromZ() {
    return scaleFromZ.toString();
  }

  public String getGoldErasingThickness() {
    return goldErasingThickness.toString();
  }

  public String getPositioningThickness() {
    return positioningThickness.toString();
  }

  public String getPositioningGold() {
    return positioningGold.toString();
  }

  public boolean isUseFakeSIRTiterations() {
    return useFakeSIRTiterations.is();
  }

  public ConstEtomoNumber getUseFindSecAddThickness() {
    return useFindSecAddThickness;
  }

  public String getFallbackThickness() {
    return fallbackThickness.toString();
  }

  public String getPrenewstBinByFactor() {
    return prenewstBinByFactor.toString();
  }

  public String getPreblendBinByFactor() {
    return preblendBinByFactor.toString();
  }

  public void setGold(final String input) {
    gold.set(input);
  }

  public String getGold() {
    return gold.toString();
  }

  public void setFakeSIRTiterations(final String input) {
    fakeSIRTiterations.set(input);
  }

  public void setLeaveIterations(final String input) {
    leaveIterations.set(input);
  }

  public String getFakeSIRTiterations() {
    return fakeSIRTiterations.toString();
  }

  public String getLeaveIterations() {
    return leaveIterations.toString();
  }

  public void setLocalAreaTargetSize(final String input) {
    localAreaTargetSize.set(input);
  }

  public String getLocalAreaTargetSize() {
    return localAreaTargetSize.toString();
  }

  public void setModelFile(final File input) {
    if (input != null) {
      modelFile.set(input.getAbsolutePath());
    }
    else {
      modelFile.reset();
    }
  }

  public String getModelFile() {
    return modelFile.toString();
  }

  public void setSizeOfPatchesXandY(final String input) {
    sizeOfPatchesXandY.set(input);
  }

  public String getSizeOfPatchesXandY() {
    return sizeOfPatchesXandY.toString();
  }

  public void setTargetNumberOfBeads(final String input) {
    targetNumberOfBeads.set(input);
  }

  public String getTargetNumberOfBeads() {
    return targetNumberOfBeads.toString();
  }

  public boolean isTargetNumberOfBeads() {
    return targetNumberOfBeads.is();
  }

  public void setAutoFitRangeAndStep(final boolean input) {
    autoFitRangeAndStep.set(input);
  }

  public ConstEtomoNumber getAutoFitRangeAndStep() {
    return autoFitRangeAndStep;
  }

  public void setEnableStretching(final boolean input) {
    enableStretching.set(input);
  }

  public ConstEtomoNumber getEnableStretching() {
    return enableStretching;
  }

  public void setScaleToInteger(final boolean input) {
    scaleToInteger.set(input);
  }

  public ConstEtomoNumber getScaleToInteger() {
    return scaleToInteger;
  }
}
