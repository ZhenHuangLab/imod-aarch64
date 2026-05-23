package etomo.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveDescrChoiceList;
import etomo.type.Option;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagOriginListener;
import etomo.ui.FlagType;
import etomo.ui.TextFlagExtension;
import etomo.ui.TextFlagOrigin;
import etomo.ui.TextStateExtension;
import etomo.ui.UIComponent;
import etomo.ui.ValueManipulationField;
import etomo.ui.ValueManipulationListener;

/**
 * <p>Description: An extensible combo box.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
class ComboBoxEfield implements SwingComponent, UIComponent, TextFlagOrigin,
  TextEfieldInterface, ValueManipulationField, ControlTarget {
  private final JComboBox comboBox = new JComboBox();

  private final String label;
  private final boolean includeValue;
  private final ControlComponentModule controlComponent;
  private final EfieldContainer container;

  private TextStateExtension stateExtension = null;
  private AppearanceExtension appearanceExtension = null;
  private TextFlagExtension flagExtension = null;
  private GridBagExtension gridBagExtension = null;
  private boolean choiceListSet = false;
  private boolean debug = false;
  private ValueManipulationExtension valueManipulationExtension = null;
  private int emptyIndex = -1;
  private DirectiveDef directiveDef = null;

  /**
   * 
   * @param label
   * @param includeValue - Include the value along with the description for each option in the pulldown list
   * @param includeControlComponent
   */
  ComboBoxEfield(final String label, final boolean includeValue,
    final boolean includeControlComponent) {
    this.label = label;
    this.includeValue = includeValue;
    emptyIndex = 0;
    comboBox.addItem(null);
    if (includeControlComponent) {
      controlComponent = new ControlComponentModule();
    }
    else {
      controlComponent = null;
    }
    container = new EfieldContainer(false, false, null, comboBox,
      controlComponent != null ? controlComponent.getComponent() : null);
  }

  static ComboBoxEfield getInstance(final String label, final boolean includeValue) {
    return new ComboBoxEfield(label, includeValue, false);
  }

  static ComboBoxEfield getOverrideInstance(final String label,
    final boolean includeValue) {
    return new ComboBoxEfield(label, includeValue, true);
  }

  public String getLabel() {
    return label;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
    if (flagExtension != null) {
      flagExtension.setDebug(debug);
    }
  }

  boolean isDebug() {
    return debug;
  }

  final void setChoiceList(final DirectiveDescrChoiceList choiceList) {
    choiceListSet = true;
    Iterator<Option> iterator = choiceList.iterator();
    while (iterator.hasNext()) {
      Option choice = iterator.next();
      if (choice != null) {
        Option localChoice = new Option(choice);
        localChoice.setIncludeValue(includeValue);
        comboBox.addItem(localChoice);
      }
    }
  }

  public final SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return container.getComponent();
  }

  final void setChoiceListSet(final boolean set) {
    choiceListSet = set;
  }

  final void setSelectedIndex(final int index) {
    comboBox.setSelectedIndex(index);
  }

  public final void setText(final File file) {
    if (file == null) {
      setText("");
    }
    else {
      setText(file.getAbsolutePath());
    }
  }

  public final void setText(final String text) {
    if (text == null || text.matches("\\s*")) {
      clear();
      return;
    }
    // Set an existing item
    int size = comboBox.getItemCount();
    for (int i = emptyIndex + 1; i < size; i++) {
      Object item = comboBox.getItemAt(i);
      if (item != null && item.equals(text)) {
        setSelectedIndex(i);
        if (flagExtension != null) {
          flagExtension.update();
        }
        return;
      }
    }
    // Add and select new item. This is invalid if the choice list was set.
    comboBox.addItem(text);
    setSelectedIndex(size);
    if (choiceListSet) {
      createFlagExtension();
      flagExtension.setFlagErrors();
    }
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  public final String getText() {
    if (isControlled()) {
      return "";
    }
    Object item = comboBox.getSelectedItem();
    if (item == null) {
      return null;
    }
    if (item instanceof Option) {
      return ((Option) item).getValue();
    }
    return item.toString();
  }

  boolean isControlled() {
    return false;
  }

  final int getSelectedIndex() {
    return comboBox.getSelectedIndex();
  }

  public final boolean isEmpty() {
    int index = getSelectedIndex();
    return index == -1 || emptyIndex != -1 && index == emptyIndex;
  }

  public final boolean equals(final String compareText) {
    Object item = comboBox.getSelectedItem();
    if (item == null) {
      return compareText == null;
    }
    return item.equals(compareText);
  }

  final void setToolTipText(final String text) {
    comboBox.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  final void setVisible(final boolean visible) {
    getComponent().setVisible(visible);
  }

  final void setComboBoxVisible(final boolean visible) {
    comboBox.setVisible(visible);
  }

  final void addItem(final Option option) {
    comboBox.addItem(option);
  }

  public final boolean isVisible() {
    return getComponent().isVisible();
  }

  public final DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  final void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  final boolean isTemplateValue() {
    FlagType flagType = getFlagType();
    return flagType != null && flagType.isTemplate();
  }

  /// validation

  public boolean isValid() {
    if (!choiceListSet || isControlled()) {
      return true;
    }
    Object item = comboBox.getSelectedItem();
    if (item == null) {
      return true;
    }
    if (!(item instanceof Option)) {
      // When the choice list was set, anything added using setText is invalid.
      return false;
    }
    return true;
  }

  /// valueManipulationExtension

  public final void clear() {
    if (emptyIndex != -1) {
      setSelectedIndex(emptyIndex);
    }
    else if (valueManipulationExtension != null) {
      valueManipulationExtension.substitute();
    }
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  /**
   * Not necessary for a comboBox with an ineditable text field.
   */
  public final void
    addValueManipulationListener(final ValueManipulationListener listener) {
    comboBox.addFocusListener(listener);
  }

  /// control

  boolean isOverride() {
    if (controlComponent == null) {
      return false;
    }
    return controlComponent.isOverride();
  }

  public final void setComponentControl(boolean control,
    final ControlState controlState) {
    if (controlComponent == null) {
      return;
    }
    comboBox.setVisible(!controlComponent.setComponentControl(control, controlState));
  }

  public void sendControlEvent() {}

  public final void setEnableControl(boolean control, final ControlState controlState) {}

  /// appearanceExtension

  private final void createAppearanceExtension() {
    if (appearanceExtension == null) {
      appearanceExtension = new AppearanceExtension(comboBox);
      appearanceExtension.setAllowForegroundChangeOnError(false);
      // In this class the appearanceExtension acts as the field's flag display for all
      // types of flags.
      if (flagExtension != null) {
        flagExtension.addFlagDisplay(appearanceExtension);
      }
    }
  }

  final void setEditable(final boolean editable) {
    createAppearanceExtension();
    appearanceExtension.setEditable(editable);
  }

  public final boolean isEnabled() {
    if (appearanceExtension == null) {
      return comboBox.isEnabled();
    }
    return appearanceExtension.isEnabled();
  }

  final boolean isEditable() {
    if (appearanceExtension == null) {
      return false;
    }
    return appearanceExtension.isEditable();
  }

  final void setEnabled(final boolean enabled) {
    if (appearanceExtension == null) {
      comboBox.setEnabled(enabled);
    }
    else {
      appearanceExtension.setEnabled(enabled);
    }
  }

  /// flagExtension

  void updateFlagExtension() {
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  /**
   * Creates flagExtension if it is null.
   * @return true if flagExtension was created
   */
  boolean createFlagExtension() {
    if (flagExtension == null) {
      flagExtension = new TextFlagExtension(this);
      if (appearanceExtension != null) {
        flagExtension.addFlagDisplay(appearanceExtension);
      }
      return true;
    }
    return false;
  }

  FlagType getFlagType() {
    if (appearanceExtension != null) {
      return appearanceExtension.getFlagType();
    }
    return null;
  }

  /**
   * Allow flags to listen for changes that they need to react to.
   */
  public final void addFlagOriginListener(final FlagOriginListener listener) {
    comboBox.addItemListener(listener);
  }

  /**
   * Change appearance when value matches the template value.  Also set up value
   * manipulation to prevent a blank value.
   * @param templateValue
   */
  private final void flagTemplate(final String templateValue) {
    if (templateValue != null) {
      createFlagExtension();
      flagExtension.flagTemplate(templateValue);
      flagExtension.update();
      if (valueManipulationExtension == null) {
        valueManipulationExtension = new ValueManipulationExtension(this, debug);
      }
      valueManipulationExtension.setPreventBlank(true, templateValue);
    }
  }

  final void clearTemplateValue() {
    if (flagExtension != null) {
      flagExtension.clearFlags();
    }
    if (valueManipulationExtension != null) {
      valueManipulationExtension.clearPreventBlank();
    }
  }

  public final void setTemplateValue() {
    if (flagExtension != null) {
      setText(flagExtension.getFlaggedTemplateValue());
    }
  }

  final void addFlagDisplay(final FlagDisplay flagDisplay) {
    createFlagExtension();
    flagExtension.addFlagDisplay(flagDisplay);
    flagExtension.update();
  }

  final void addFinalFlagDisplay(final FlagDisplay flagDisplay) {
    createFlagExtension();
    flagExtension.addFinalFlagDisplay(flagDisplay);
    flagExtension.update();
  }

  public final void setFieldHighlight(final String value) {
    flagTemplate(value);
  }

  /// stateExtension

  private final void createStateExtension() {
    if (stateExtension == null) {
      stateExtension = new TextStateExtension(this);
    }
  }

  final void backup() {
    createStateExtension();
    stateExtension.backup();
  }

  final void checkpoint() {
    createStateExtension();
    stateExtension.checkpoint();
  }

  final boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (stateExtension == null) {
      return false;
    }
    return stateExtension.isDifferentFromCheckpoint(alwaysCheck);
  }

  final void restoreFromBackup() {
    if (stateExtension != null) {
      stateExtension.restoreFromBackup();
    }
  }

  /// gridBagExtension

  final void remove() {
    if (gridBagExtension != null) {
      gridBagExtension.remove(getComponent());
    }
  }

  final void add(final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    if (gridBagExtension == null) {
      gridBagExtension = new GridBagExtension();
    }
    gridBagExtension.add(getComponent(), panel, layout, constraints);
  }
}