package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.comscript.CombineParams;
import etomo.comscript.ConstCombineParams;
import etomo.comscript.ConstPatchcrawl3DParam;
import etomo.type.CombinePatchSize;
import etomo.type.EnumeratedType;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;

/**
 * <p>Description: Panel to hold the patch size type or x, y, and z values.</p>
 *
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class PatchSizePanel implements ActionListener {
  public static final CombinePatchSize DEFAULT_FINAL_SIZE = CombinePatchSize.EXTRA_LARGE;
  private static final int X_INDEX = 0;
  private static final int Y_INDEX = 1;
  private static final int Z_INDEX = 2;

  private final JPanel pnlRoot = new JPanel();
  private final ButtonGroup bgType = new ButtonGroup();
  private final RadioButton rbTypeMedium =
    new RadioButton("Medium patches", CombinePatchSize.MEDIUM, bgType);
  private final RadioButton rbTypeLarge =
    new RadioButton("Large patches", CombinePatchSize.LARGE, bgType);
  private final RadioButton rbTypeCustom =
    new RadioButton("Custom", CombinePatchSize.CUSTOM, bgType);
  private final String[] xyzLabels = new String[] { "X: ", "Y: ", "Z: " };
  private final LabeledTextField[] ltfXYZ = new LabeledTextField[xyzLabels.length];

  private final RadioButton rbTypeSmall;
  private final RadioButton rbTypeExtraLarge;
  private final String title;
  private final boolean finalSize;

  private PatchSizePanel(final boolean finalSize) {
    this.finalSize = finalSize;
    String baseTitle = "Patch Size";
    for (int i = 0; i < ltfXYZ.length; i++) {
      ltfXYZ[i] = new LabeledTextField(FieldType.INTEGER, xyzLabels[i]);
      ltfXYZ[i].setRequired(true);
    }
    if (!finalSize) {
      title = baseTitle;
      rbTypeSmall = new RadioButton("Small patches", CombinePatchSize.SMALL, bgType);
      rbTypeExtraLarge = null;
    }
    else {
      title = "Max " + baseTitle;
      rbTypeSmall = null;
      rbTypeExtraLarge =
        new RadioButton("Extra Large patches", CombinePatchSize.EXTRA_LARGE, bgType);
    }
  }

  static PatchSizePanel getInstance(final boolean finalSize) {
    PatchSizePanel instance = new PatchSizePanel(finalSize);
    instance.createPanel(finalSize);
    instance.addListeners();
    return instance;
  }

  private void createPanel(final boolean maxSize) {
    // init
    if (rbTypeSmall != null) {
      rbTypeSmall.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    rbTypeMedium.setAlignmentX(Component.LEFT_ALIGNMENT);
    rbTypeLarge.setAlignmentX(Component.LEFT_ALIGNMENT);
    if (rbTypeExtraLarge != null) {
      rbTypeExtraLarge.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    rbTypeCustom.setAlignmentX(Component.LEFT_ALIGNMENT);
    // panels
    JPanel pnlType = new JPanel();
    JPanel pnlXYZ = new JPanel();
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.X_AXIS));
    pnlRoot.setBorder(new EtchedBorder(title).getBorder());
    pnlRoot.setAlignmentX(Component.CENTER_ALIGNMENT);
    pnlRoot.add(pnlType);
    pnlRoot.add(Box.createHorizontalGlue());
    pnlRoot.add(pnlXYZ);
    // Type
    pnlType.setLayout(new BoxLayout(pnlType, BoxLayout.Y_AXIS));
    if (rbTypeSmall != null) {
      pnlType.add(rbTypeSmall.getComponent());
    }
    pnlType.add(rbTypeMedium.getComponent());
    pnlType.add(rbTypeLarge.getComponent());
    if (rbTypeExtraLarge != null) {
      pnlType.add(rbTypeExtraLarge.getComponent());
    }
    pnlType.add(rbTypeCustom.getComponent());
    // XYZ
    pnlXYZ.setLayout(new BoxLayout(pnlXYZ, BoxLayout.Y_AXIS));
    pnlXYZ.setBorder(new EtchedBorder("In Pixels").getBorder());
    for (int i = 0; i < ltfXYZ.length; i++) {
      pnlXYZ.add(ltfXYZ[i].getComponent());
      if (i < ltfXYZ.length - 1) {
        pnlXYZ.add(Box.createVerticalGlue());
      }
    }
    // update
    actionPerformed(null);
  }

  Component getComponent() {
    return pnlRoot;
  }

  private void addListeners() {
    if (rbTypeSmall != null) {
      rbTypeSmall.addActionListener(this);
    }
    rbTypeMedium.addActionListener(this);
    rbTypeLarge.addActionListener(this);
    if (rbTypeExtraLarge != null) {
      rbTypeExtraLarge.addActionListener(this);
    }
    rbTypeCustom.addActionListener(this);
  }

  public void actionPerformed(final ActionEvent event) {
    EnumeratedType enumeratedType =
      ((RadioButton.RadioButtonModel) bgType.getSelection()).getEnumeratedType();
    if (enumeratedType != CombinePatchSize.CUSTOM
      && enumeratedType instanceof CombinePatchSize) {
      CombinePatchSize combinedPatchSize = (CombinePatchSize) enumeratedType;
      int len = Math.min(ltfXYZ.length, combinedPatchSize.getXYZLen());
      for (int i = 0; i < len; i++) {
        ltfXYZ[i].setText(combinedPatchSize.getXYZ(i));
      }
    }
    updateDisplay();
  }

  private void updateDisplay() {
    boolean enabled = rbTypeCustom.isEnabled();
    boolean selected = rbTypeCustom.isSelected();
    for (int i = 0; i < ltfXYZ.length; i++) {
      ltfXYZ[i].setEnabled(enabled);
      ltfXYZ[i].setEditable(selected);
    }
  }

  void setEnabled(final boolean enabled) {
    if (rbTypeSmall != null) {
      rbTypeSmall.setEnabled(enabled);
    }
    rbTypeMedium.setEnabled(enabled);
    rbTypeLarge.setEnabled(enabled);
    if (rbTypeExtraLarge != null) {
      rbTypeExtraLarge.setEnabled(enabled);
    }
    rbTypeCustom.setEnabled(enabled);
    updateDisplay();
  }

  private boolean isEnabled() {
    return rbTypeMedium.isEnabled();
  }

  void setParameters(final ConstCombineParams combineParams) {
    CombinePatchSize combinePatchSize = null;
    combinePatchSize = combineParams.getPatchSize(finalSize);
    if (combinePatchSize == null && finalSize) {
      combinePatchSize = DEFAULT_FINAL_SIZE;
    }
    if (combinePatchSize == CombinePatchSize.CUSTOM) {
      rbTypeCustom.setSelected(true);
      String[] xyz = combineParams.getPatchSizeXYZArray(finalSize);
      if (xyz != null) {
        int len = Math.min(ltfXYZ.length, xyz.length);
        for (int i = 0; i < len; i++) {
          ltfXYZ[i].setText(xyz[i]);
        }
      }
    }
    else {
      setFixedType(combinePatchSize);
    }
    actionPerformed(null);
  }

  void setParameters(final ConstPatchcrawl3DParam patchrawlParam) {
    // Assume flipped
    int[] xyz = new int[] { patchrawlParam.getXPatchSize(),
      patchrawlParam.getZPatchSize(), patchrawlParam.getYPatchSize() };
    CombinePatchSize combinePatchSize = CombinePatchSize.getInstance(xyz);
    if (combinePatchSize == CombinePatchSize.CUSTOM) {
      rbTypeCustom.setSelected(true);
      int len = Math.min(ltfXYZ.length, xyz.length);
      for (int i = 0; i < len; i++) {
        ltfXYZ[i].setText(xyz[i]);
      }
    }
    else {
      setFixedType(combinePatchSize);
    }
    actionPerformed(null);
  }

  void setFixedType(final CombinePatchSize combinePatchSize) {
    if (combinePatchSize == CombinePatchSize.SMALL) {
      if (rbTypeSmall != null) {
        rbTypeSmall.setSelected(true);
      }
      else {
        selectTypeXyz(CombinePatchSize.SMALL);
      }
    }
    else if (combinePatchSize == CombinePatchSize.MEDIUM) {
      rbTypeMedium.setSelected(true);
    }
    else if (combinePatchSize == CombinePatchSize.LARGE) {
      rbTypeLarge.setSelected(true);
    }
    else if (combinePatchSize == CombinePatchSize.EXTRA_LARGE) {
      if (rbTypeExtraLarge != null) {
        rbTypeExtraLarge.setSelected(true);
      }
      else {
        selectTypeXyz(CombinePatchSize.EXTRA_LARGE);
      }
    }
  }

  boolean getParameters(final CombineParams combineParams, final boolean doValidation) {
    if (isEnabled()) {
      EnumeratedType combinedPatchSize =
        ((RadioButton.RadioButtonModel) bgType.getSelection()).getEnumeratedType();
      if (combinedPatchSize instanceof CombinePatchSize) {
        combineParams.setPatchSize(finalSize, (CombinePatchSize) combinedPatchSize);
      }
      if (combinedPatchSize == CombinePatchSize.CUSTOM) {
        String[] xyz = new String[ltfXYZ.length];
        try {
          for (int i = 0; i < ltfXYZ.length; i++) {
            xyz[i] = ltfXYZ[i].getText(doValidation);
          }
          combineParams.setPatchSizeXYZ(finalSize, xyz);
        }
        catch (FieldValidationFailedException e) {
          return false;
        }
      }
    }
    else {
      combineParams.resetPatchSize(finalSize);
    }
    return true;
  }

  private void selectTypeXyz(final CombinePatchSize combinePatchSize) {
    rbTypeCustom.setSelected(true);
    int len = Math.min(ltfXYZ.length, combinePatchSize.getXYZLen());
    for (int i = 0; i < len; i++) {
      ltfXYZ[i].setText(combinePatchSize.getXYZ(i));
    }
  }

  void setSmallTooltip(final String tooltip) {
    if (rbTypeSmall != null) {
      rbTypeSmall.setToolTipText(tooltip);
    }
  }

  void setMediumTooltip(final String tooltip) {
    rbTypeMedium.setToolTipText(tooltip);
  }

  void setLargeTooltip(final String tooltip) {
    rbTypeLarge.setToolTipText(tooltip);
  }

  void setExtraLargeTooltip(final String tooltip) {
    if (rbTypeExtraLarge != null) {
      rbTypeExtraLarge.setToolTipText(tooltip);
    }
  }

  void setCustomTooltip(final String tooltip) {
    rbTypeCustom.setToolTipText(tooltip);
  }

  void setXTooltip(final String tooltip) {
    ltfXYZ[X_INDEX].setToolTipText(tooltip);
  }

  void setYTooltip(final String tooltip) {
    ltfXYZ[Y_INDEX].setToolTipText(tooltip);
  }

  void setZTooltip(final String tooltip) {
    ltfXYZ[Z_INDEX].setToolTipText(tooltip);
  }
}
