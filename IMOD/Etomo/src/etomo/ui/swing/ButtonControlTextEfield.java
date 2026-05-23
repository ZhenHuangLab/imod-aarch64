package etomo.ui.swing;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import etomo.ui.FieldType;

/**
 * <p>Description: An extensible text field that comes with control buttons.  Currently
 * only implemented for file fields, but may be expanded for any type of field that uses
 * control button(s).  New types of control buttons can be added to this class.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ButtonControlTextEfield extends TextEfield {
  private final Ebutton selectFileButton;
  private final Ebutton clearButton;

  /**
   * 
   * @param label - identifier and label - for labeled and unlabeled fields
   * @param fieldType - for validation
   * @param useLabelComponent - for fields with labels
   * @param useControlComponent - For stateful control
   * @param enabledField - when false field cannot be enabled
   * @param editableComponent - when false field cannot be made editable
   * @param sharedSelectFileExtension - a shared extension for building a file chooser
   * @param includeSelectFileButton - add a file chooser button
   * @param includeClearButton - add a clear button
   */
  private ButtonControlTextEfield(final String label, final FieldType fieldType,
    final boolean useLabelComponent, final boolean useControlComponent,
    final boolean enabledField, final boolean editableComponent,
    final SelectFileExtension sharedSelectFileExtension,
    final boolean includeSelectFileButton, final boolean includeClearButton,
    final boolean useGridBag) {
    super(label, fieldType, useLabelComponent, useControlComponent, enabledField,
      editableComponent, useGridBag);
    if (includeSelectFileButton) {
      selectFileButton = Ebutton.getSelectFileInstance(this, sharedSelectFileExtension);
      container.add(selectFileButton.getComponent());
    }
    else {
      selectFileButton = null;
    }
    if (includeClearButton) {
      clearButton = Ebutton.getClearInstance(this);
      container.add(clearButton.getComponent());
    }
    else {
      clearButton = null;
    }
    if (appearanceExtension != null) {
      appearanceExtension.setChildControllers(createChildControllers());
    }
  }

  /**
   * @param label
   * @param sharedFileOpenExtension (optional) for a group of fields that use the same file chooser definition
   * @return
   */
  static ButtonControlTextEfield getFileOverrideInstance(final String label,
    final SelectFileExtension sharedSelectFileExtension) {
    return new ButtonControlTextEfield(label, FieldType.FILE, false, true, true, false,
      sharedSelectFileExtension, true, true, false);
  }

  /**
   * @param label
   * @return
   */
  static ButtonControlTextEfield getFileInstance(final String label) {
    return new ButtonControlTextEfield(label, FieldType.FILE, false, false, true, false,
      null, true, true, false);
  }

  /**
   * @param label
   * @return
   */
  static ButtonControlTextEfield getLabeledFileInstance(final String label) {
    return new ButtonControlTextEfield(label, FieldType.FILE, true, false, true, false,
      null, true, false, false);
  }

  static ButtonControlTextEfield getLabeledFileInstance(final String label,
    final boolean includeClearButton, final boolean useGridBag) {
    return new ButtonControlTextEfield(label, FieldType.FILE, true, false, true, false,
      null, true, includeClearButton, useGridBag);
  }

  final void setTooltip(final String text) {
    super.setTooltip(text);
    if (selectFileButton != null) {
      selectFileButton.setTooltip(text);
    }
    if (clearButton != null) {
      clearButton.setTooltip(text);
    }
  }

  final void setTooltip(final String text, final String selectFileText) {
    super.setTooltip(text);
    if (selectFileButton != null) {
      selectFileButton.setTooltip(selectFileText);
    }
    if (clearButton != null) {
      clearButton.setTooltip(text);
    }
  }

  void setOverrideFileOpenDirectory(final File overrideFileOpenDirectory) {
    if (selectFileButton != null) {
      selectFileButton.setOverrideFileOpenDirectory(overrideFileOpenDirectory);
    }
  }

  void setFileFilter(final FileFilter fileFilter) {
    if (selectFileButton != null) {
      selectFileButton.setFileFilter(fileFilter);
    }
  }

  void setFileSelectionMode(final int fileSelectionMode) {
    if (selectFileButton != null) {
      selectFileButton.setFileSelectionMode(fileSelectionMode);
    }
  }

  void setSelectFileDir(final String dir) {
    if (selectFileButton != null) {
      selectFileButton.setSelectFileDir(dir);
    }
  }

  SelectFileExtension getSelectFileExtension() {
    if (selectFileButton != null) {
      return selectFileButton.getSelectFileExtension();
    }
    return null;
  }

  /// appearanceExtension

  @Override
  void createAppearanceExtension(final boolean enabledField,
    final boolean editableComponent) {
    boolean create = appearanceExtension == null;
    super.createAppearanceExtension(enabledField, editableComponent);
    if (create) {
      appearanceExtension.setChildControllers(createChildControllers());
    }
  }

  private Controller[] createChildControllers() {
    int size = 0;
    if (selectFileButton != null) {
      size++;
    }
    if (clearButton != null) {
      size++;
    }
    if (size == 0) {
      return null;
    }
    Controller[] controllers = new Controller[size];
    int index = 0;
    if (selectFileButton != null) {
      controllers[index++] = selectFileButton;
    }
    if (clearButton != null) {
      controllers[index] = clearButton;
    }
    return controllers;
  }

  /// flagExtension

  /**
   * Adds buttons as flag displays to the flag extension, if the flag extension was
   * created.
   * @return true if flagExtension was created
   */
  boolean createFlagExtension() {
    if (super.createFlagExtension()) {
      if (selectFileButton != null) {
        addFlagDisplay(selectFileButton);
      }
      if (clearButton != null) {
        addFlagDisplay(clearButton);
      }
      return true;
    }
    return false;
  }
}