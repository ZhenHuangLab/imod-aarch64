package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;

import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.EnumeratedType;
import etomo.type.EtomoAutodoc;
import etomo.type.UITestFieldType;
import etomo.ui.BooleanEfieldInterface;
import etomo.ui.BooleanFlagExtension;
import etomo.ui.BooleanFlagOrigin;
import etomo.ui.BooleanStateExtension;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagType;
import etomo.util.Utilities;

/**
 * <p>Description: Extensible radio button. </p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class RadioEbutton
  implements BooleanEfieldInterface, BooleanFlagOrigin, FlagDisplay, ActionListener {
  private final JRadioButton button;
  private final EnumeratedType enumeratedType;

  private BooleanStateExtension stateExtension = null;
  private BooleanFlagExtension flagExtension = null;
  // Currently only using a flag that alters background color so not saving foreground
  // color.
  private Color defaultBackground = null;
  private FlagType flagType = null;

  /**
   * EnumeratedType takes precence over label - only when it contains a non-null label.
   * @param enumeratedType
   * @param buttonGroup
   * @param label
   */
  private RadioEbutton(final EnumeratedType enumeratedType, String label,
    final EtomoButtonGroup buttonGroup, final ComponentStyleExtension componentStyle) {
    this.enumeratedType = enumeratedType;
    if (enumeratedType != null) {
      String tempLabel = enumeratedType.getLabel();
      if (tempLabel != null) {
        label = tempLabel;
      }
    }
    if (label != null) {
      button = new JRadioButton(label);
      setName(label);
    }
    else {
      button = new JRadioButton();
    }
    button.setModel(new RadioEButtonModel(this));
    if (buttonGroup != null) {
      buttonGroup.add(button);
    }
  }

  static RadioEbutton getEnumInstance(final EnumeratedType enumeratedType,
    final EtomoButtonGroup buttonGroup) {
    return new RadioEbutton(enumeratedType, null, buttonGroup, null);
  }

  static RadioEbutton getInstance(final String label,
    final EtomoButtonGroup buttonGroup) {
    return new RadioEbutton(null, label, buttonGroup, null);
  }

  void setLabel(final String label) {
    button.setText(label);
    setName(label);
  }

  private void setName(final String label) {
    String name = Utilities.convertLabelToName(label);
    if (name != null) {
      button.setName(
        UITestFieldType.RADIO_BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
      if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
        System.out
          .println(button.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      }
    }
  }

  void addActionListener(final ActionListener listener) {
    button.addActionListener(listener);
  }

  Component getComponent() {
    return button;
  }

  public void actionPerformed(final ActionEvent event) {
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  private EnumeratedType getEnumeratedType() {
    return enumeratedType;
  }

  public boolean isEnabled() {
    return getComponent().isEnabled();
  }

  void setEnabled(final boolean enabled) {
    button.setEnabled(enabled);
    if (flagExtension != null) {
      // A flag may have altered the field's appearance
      ComponentStyleExtension.INSTANCE.updateAppearance(button, flagType, null,
        defaultBackground);
    }
  }

  public boolean isSelected() {
    return button.isSelected();
  }

  public boolean equals(final boolean value) {
    return button.isSelected() == value;
  }

  public void setSelected(final boolean selected) {
    button.setSelected(selected);
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  String getLabel() {
    return button.getText();
  }

  final void checkpoint() {
    if (stateExtension == null) {
      stateExtension = new BooleanStateExtension(this);
    }
    stateExtension.checkpoint();
  }

  /**
   * @return the checkpoint value
   */
  final boolean isCheckpointValue() {
    if (stateExtension == null) {
      return false;
    }
    return stateExtension.isCheckpointValue();
  }

  final void enableWarning(final boolean value) {
    // Give the button a generic component style for displaying the warning.
    if (defaultBackground == null) {
      defaultBackground = button.getBackground();
      if (defaultBackground == null) {
        defaultBackground = Color.GRAY;
      }
    }
    // Use flag extension to set warning flag.
    if (flagExtension == null) {
      button.addActionListener(this);
      flagExtension = new BooleanFlagExtension(this);
      flagExtension.addFlagDisplay(this);
    }
    flagExtension.enableWarning(value);
    flagExtension.update();
  }

  final void disableWarning() {
    if (flagExtension == null) {
      return;
    }
    flagExtension.disableWarning();
    flagExtension.update();
  }

  /**
   * Update the appearance based on the flag
   */
  public final void setFlag(final FlagType flagType) {
    ComponentStyleExtension.INSTANCE.updateAppearance(button, flagType, null,
      defaultBackground);
    this.flagType = flagType;
  }

  void setFormattedTooltip(final String formattedTooltip) {
    button.setToolTipText(formattedTooltip);
  }

  final void setToolTipText(String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  /**
   * Sets a tooltip from a section using the enumeratedType, if it exists.
   *
   * @param section
   */
  void setToolTipText(final String autodocName, final ReadOnlySection section) {
    String text;
    if (enumeratedType == null) {
      text = EtomoAutodoc.getTooltip(autodocName, section, true);
    }
    else {
      text = EtomoAutodoc.getTooltip(autodocName, section, enumeratedType.toString());
    }
    setToolTipText(text);
  }

  public static final class RadioEButtonModel extends AbstractRadioButtonModel {
    private final RadioEbutton radioButton;

    private RadioEButtonModel(RadioEbutton radioButton) {
      super();
      this.radioButton = radioButton;
    }

    public EnumeratedType getEnumeratedType() {
      return radioButton.getEnumeratedType();
    }
  }
}
