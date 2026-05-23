package etomo.ui.swing;

import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import etomo.type.DialogType;

/**
 * <p>Description: A button with all the functionality of MultiLineButton, but without
 * a multiple line label. </p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
class SingleLineButton extends MultiLineButton {
  SingleLineButton() {
    this(null, false, null, false);
  }

  SingleLineButton(final String label) {
    this(label, false, null, false);
  }

  SingleLineButton(String label, boolean toggleButton, DialogType dialogType) {
    this(label, toggleButton, dialogType, false);
  }

  SingleLineButton(String label, boolean toggleButton, DialogType dialogType,
    final boolean html) {
    super(label, toggleButton, dialogType, false, html, false);
  }

  static SingleLineButton getHtmlInstance(final String label) {
    return new SingleLineButton(label, false, null, true);
  }

  AbstractButton newButton() {
    String label;
    if (isHtml()) {
      label = format(getUnformattedLabel());
    }
    else {
      label = getUnformattedLabel();
    }
    if (isToggleButton()) {
      return new JToggleButton(label);
    }
    return new JButton(label);
  }

  void setupButton(final boolean setMinimumSize) {
    setName(getUnformattedLabel());
  }

  /**
   * Sets the button text without setting the name or unformattedLabel
   * @param text
   */
  final void setTextLabel(final String text) {
    if (!isHtml()) {
      getButton().setText(text);
    }
    else {
      getButton().setText(format(text));
    }
  }

  private final String format(String label) {
    if (label == null) {
      return null;
    }
    if (label.toLowerCase().startsWith("<html>")) {
      return label;
    }
    label = "<html><b><center>".concat(label).concat("</center></b>");
    return label;
  }

  final void setSize() {
    Dimension size =
      UIParameters.getInstance(getFontMetrics()).getButtonSingleLineDimension();
    AbstractButton button = getButton();
    button.setPreferredSize(size);
    button.setMaximumSize(size);
  }

  final void setSize(Dimension size) {
    AbstractButton button = getButton();
    button.setPreferredSize(size);
    button.setMaximumSize(size);
  }

  final void setToPreferredSize() {
    AbstractButton button = getButton();
    Dimension size = button.getPreferredSize();
    button.setPreferredSize(size);
    button.setMaximumSize(size);
  }

  final void setToPreferredSize(Dimension size) {
    if (size == null) {
      setToPreferredSize();
    }
    else {
      AbstractButton button = getButton();
      button.setPreferredSize(size);
      button.setMaximumSize(size);
    }
  }
}