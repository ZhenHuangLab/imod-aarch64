package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.DirectiveDef;
import etomo.type.AxisID;
import etomo.ui.BrowsingDirectory;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.TextFieldInterface;
import etomo.ui.TextFieldSetting;
import etomo.ui.UIComponent;
import etomo.util.FilePath;
import etomo.util.Utilities;
import etomo.util.ValidDirectory;

/**
 * <p>Description: Like FileTextField but handles relative paths</p>
 * 
 * <p>Copyright: Copyright 2011 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class FileTextField2 implements FileTextFieldInterface, TextFieldInterface,
  ActionListener, UIComponent, SwingComponent {
  // Assuming the field type is always non-numeric
  private final FieldType STRING_FIELD_TYPE = FieldType.STRING;

  private final JPanel panel = new JPanel();

  private final SimpleButton button;
  private final TextField field;
  private final JLabel label;
  private final boolean labeled;
  private final BaseManager manager;
  private final AxisID axisID;
  final boolean alternateLayout;
  private final GridBagLayout layout;
  private final GridBagConstraints constraints;

  private List<ResultListener> resultListenerList = null;
  private int fileSelectionMode = -1;
  private FileFilter fileFilter = null;
  private boolean absolutePath = false;
  private boolean useTextAsFileChooserDir = false;
  private boolean turnOffFileHiding = false;
  /**
   * originReference overrides origin
   */
  private FileTextField2 originReference = null;
  /**
   * If origin is valid, it overrides originEtomoRunDir.
   */
  private File origin = null;
  /**
   * If true, then the origin directory of the file is the directory in which etomo was
   * run.  Useful when a dataset location has not been set.
   */
  private boolean originEtomoRunDir = false;
  private DirectiveDef directiveDef = null;
  private FontMetrics fontMetrics = null;
  private boolean textEntryPolicy = true;
  private boolean enabled = true;
  private boolean editable = true;
  private BrowsingDirectory browsingDir = null;

  public String toString() {
    return super.toString() + ":[text:" + field.getText() + ",label:" + label.getText();
  }

  private FileTextField2(final BaseManager manager, final AxisID axisID,
    final String label, final boolean labeled, final boolean peet,
    final boolean alternateLayout) {
    if (!peet) {
      button = new SimpleButton(new ImageIcon(ClassLoader.getSystemResource(
        !Utilities.APRIL_FOOLS ? "images/openFile.gif" : "images/openFileFool.png")));
    }
    else {
      button = new SimpleButton(new ImageIcon(ClassLoader.getSystemResource(
        !Utilities.APRIL_FOOLS ? "images/openFilePeet.png" : "images/openFileFool.png")));
    }
    button.setName(label);
    field = new TextField(STRING_FIELD_TYPE, label, null);
    this.label = new JLabel(label);
    this.labeled = labeled;
    this.manager = manager;
    this.axisID = axisID;
    this.alternateLayout = alternateLayout;
    if (!alternateLayout) {
      layout = new GridBagLayout();
      constraints = new GridBagConstraints();
    }
    else {
      layout = null;
      constraints = null;
    }
  }

  /**
   * Get an unlabeled instance with a PEET-style button.  The starting directory for the
   * file chooser and the origin of relative files is the manager's property user
   * directory.
   *
   * @param manager
   * @param name
   * @return
   */
  static FileTextField2 getUnlabeledPeetInstance(final BaseManager manager,
    final String name) {
    FileTextField2 instance = new FileTextField2(manager, null, name, false, true, false);
    instance.createPanel();
    instance.addListeners();
    return instance;
  }

  static FileTextField2 getUnlabeledInstance(final BaseManager manager,
    final String name) {
    FileTextField2 instance =
      new FileTextField2(manager, null, name, false, false, false);
    instance.createPanel();
    instance.addListeners();
    return instance;
  }

  /**
   * Get a labeled instance with a PEET-style button.  The starting directory for the
   * file chooser and the origin of relative files is the manager's property user
   * directory.
   *
   * @param manager
   * @param name
   * @return
   */
  static FileTextField2 getPeetInstance(final BaseManager manager, final AxisID axisID,
    final String name) {
    FileTextField2 instance =
      new FileTextField2(manager, axisID, name, true, true, false);
    instance.createPanel();
    instance.addListeners();
    return instance;
  }

  public static FileTextField2 getInstance(final BaseManager manager, final String name) {
    FileTextField2 instance = new FileTextField2(manager, null, name, true, false, false);
    instance.createPanel();
    instance.addListeners();
    return instance;
  }

  static FileTextField2 getAltLayoutInstance(final BaseManager manager,
    final String name) {
    FileTextField2 instance = new FileTextField2(manager, null, name, true, false, true);
    instance.createPanel();
    instance.addListeners();
    return instance;
  }

  public String getName() {
    return field.getName();
  }

  private void createPanel() {
    // init
    field.setTextPreferredSize(new Dimension(
      250 * (int) Math.round(UIParameters.getInstance().getFontSizeAdjustment()),
      FixedDim.folderButton.height));
    button.setName(label.getText());
    button.setPreferredSize(FixedDim.folderButton);
    button.setMaximumSize(FixedDim.folderButton);
    if (!alternateLayout) {
      // panel
      panel.setLayout(layout);
      constraints.fill = GridBagConstraints.BOTH;
      constraints.weightx = 0.0;
      constraints.weighty = 0.0;
      constraints.gridheight = 1;
      constraints.gridwidth = 1;
      if (labeled) {
        layout.setConstraints(label, constraints);
        panel.add(label);
      }
      constraints.insets = new Insets(0, 0, 0, -1);
      layout.setConstraints(field.getComponent(), constraints);
      panel.add(field.getComponent());
      constraints.insets = new Insets(0, -1, 0, 0);
      layout.setConstraints(button, constraints);
      panel.add(button);
    }
    else {
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      panel.add(label);
      panel.add(field.getComponent());
      panel.add(button);
      panel.add(Box.createHorizontalGlue());
    }
  }

  void setVisible(final boolean visible) {
    panel.setVisible(visible);
  }

  void setBackground(final Color color) {
    panel.setBackground(color);
  }

  int getPreferredWidth() {
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(label);
    }
    return UIUtilities.getPreferredWidth(label, label.getText(), fontMetrics)
      + field.getPreferredWidth() + button.getPreferredWidth();
  }

  private void addListeners() {
    button.addActionListener(this);
  }

  /**
   * Adds a result listener to a list of result listeners.  A null listener has no effect.
   *
   * @param listener
   */
  void addResultListener(final ResultListener listener) {
    if (listener == null) {
      return;
    }
    if (resultListenerList == null) {
      resultListenerList = new ArrayList<ResultListener>();
    }
    resultListenerList.add(listener);
  }

  void addFocusListener(final FocusListener listener) {
    field.addFocusListener(listener);
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return panel;
  }

  public Component getRootPanel() {
    return panel;
  }

  public boolean isText() {
    return true;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isDebug() {
    return EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  /**
   * Not true value is implemented so all non-empty values are true.
   */
  public boolean equalsSelectedStringValue(final String value) {
    return value != null && !value.isEmpty();
  }

  /**
   * @return a label suitable for a message - in single quotes and truncated at the colon.
   */
  public String getQuotedLabel() {
    return Utilities.quoteLabel(label.getText());
  }

  /**
   * Opens a file chooser and notifies the result listener list.
   */
  public void actionPerformed(ActionEvent e) {
    JFileChooser chooser =
      new FileChooser(manager, axisID, getFileChooserLocation(), browsingDir);
    chooser.setDialogTitle(Utilities.stripLabel(label.getText()));
    if (fileSelectionMode != -1) {
      chooser.setFileSelectionMode(fileSelectionMode);
    }
    if (fileFilter != null) {
      chooser.setFileFilter(fileFilter);
    }
    chooser.setFileHidingEnabled(!turnOffFileHiding);
    chooser.setPreferredSize(UIParameters.getInstance().getFileChooserDimension());
    int returnVal = chooser.showOpenDialog(panel);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      setFile(file);
      if (browsingDir != null && file != null) {
        browsingDir.setBrowsingDir(file.getParentFile());
      }
    }
    if (resultListenerList != null && resultListenerList.size() > 0) {
      Iterator<ResultListener> i = resultListenerList.iterator();
      while (i.hasNext()) {
        i.next().processResult(this, false);
      }
    }
  }

  /**
   * Sets field width with font adjustment.
   *
   * @param width
   */
  void setAdjustedFieldWidth(final double width) {
    field
      .setTextPreferredWidth(width * UIParameters.getInstance().getFontSizeAdjustment());
  }

  void setAbsolutePath(final boolean input) {
    this.absolutePath = input;
  }

  void setOriginEtomoRunDir(final boolean input) {
    this.originEtomoRunDir = input;
  }

  void setPreferredWidth(final double width) {
    field.setTextPreferredWidth(width);
  }

  /**
   * Sets the origin member variable which overrides the originEtomoRunDir member variable
   * and the propertyUserDir when it is a valid directory.
   *
   * @return
   */
  void setOrigin(final File input) {
    origin = input;
  }

  void setOrigin(final String input) {
    if (input != null) {
      origin = new File(input);
    }
  }

  /**
   * Sets the originReference member variable, which is first choice for where the file
   * chooser should open.  It is checked each time the file chooser opens.  If
   * originReference is null or empty, the fallback is the origin member variable.
   *
   * @param input
   */
  void setOriginReference(final FileTextField2 input) {
    originReference = input;
  }

  /**
   * If useTextAsFileChooserDir is true, the text in the text field with be where the file
   * chooser opens, if the text field contains a directory.
   *
   * @param input
   */
  void setUseTextAsFileChooserDir(final boolean input) {
    useTextAsFileChooserDir = input;
  }

  public boolean isEmpty() {
    String text = field.getText();
    return text == null || text.matches("\\s*");
  }

  boolean exists() {
    if (!isEmpty()) {
      return getFile().exists();
    }
    return false;
  }

  public File getFile() {
    if (!isEmpty()) {
      return FilePath.buildAbsoluteFile(getOriginDir(), field.getText());
    }
    return null;
  }

  public File getFile(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String text = field.getText(doValidation, fieldDisplayer);
    if (text != null && !text.matches("\\s*")) {
      return FilePath.buildAbsoluteFile(getOriginDir(), text);
    }
    return null;
  }

  public boolean equals(final FileTextField2 input) {
    if (input == null) {
      return false;
    }
    File file = getFile();
    File inputFile = input.getFile();
    if (file == null) {
      return inputFile == null;
    }
    return file.equals(inputFile);
  }

  /**
   * Saves the current text as the checkpoint.
   */
  public void checkpoint() {
    field.checkpoint();
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    field.setCheckpoint(input);
  }

  public FieldSettingInterface getCheckpoint() {
    return field.getCheckpoint();
  }

  public void backup() {
    field.backup();
  }

  /**
   * If the field was backed up, make the backup value the displayed value, and turn off
   * the back up.
   */
  public void restoreFromBackup() {
    field.restoreFromBackup();
  }

  public DirectiveDef getDirectiveDef() {
    return field.getDirectiveDef();
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    field.setDirectiveDef(directiveDef);
  }

  public void useDefaultValue() {
    field.useDefaultValue();
  }

  public boolean equalsDefaultValue() {
    return field.equalsDefaultValue();
  }

  public boolean equalsDefaultValue(final String value) {
    return field.equalsDefaultValue(value);
  }

  public boolean isFieldHighlightSet() {
    return field.isFieldHighlightSet();
  }

  public TextFieldSetting getFieldHighlight() {
    return field.getFieldHighlight();
  }

  public void setFieldHighlight(final String value) {
    field.setFieldHighlight(value);
  }

  public void setFieldHighlight(final boolean value) {}

  public void setFieldHighlight(final FieldSettingInterface settingInterface) {
    field.setFieldHighlight(settingInterface);
  }

  public boolean equalsFieldHighlight() {
    return field.equalsFieldHighlight();
  }

  public boolean equalsFieldHighlight(final String value) {
    return field.equalsFieldHighlight(value);
  }

  public void clearFieldHighlight() {
    field.clearFieldHighlight();
  }

  /**
   * @param alwaysCheck - check for difference even when the field is disabled or invisible
   * @return
   */
  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    return field.isDifferentFromCheckpoint(alwaysCheck);
  }

  void setBrowsingDirectory(final BrowsingDirectory browsingDir) {
    this.browsingDir = browsingDir;
  }

  /**
   * Gets the origin directory.
   *
   * @return
   */
  private String getOriginDir() {
    String dir = getOriginForFileChooser();
    if (dir != null) {
      return dir;
    }
    if (manager != null) {
      return manager.getPropertyUserDir();
    }
    return null;
  }

  private String getOriginForFileChooser() {
    if (originReference != null && !originReference.isEmpty()) {
      File dir = originReference.getFile();
      if (dir != null) {
        if (dir.isDirectory()) {
          return dir.getAbsolutePath();
        }
        else {
          return dir.getParent();
        }
      }
    }
    if (origin != null && origin.exists() && origin.isDirectory()) {
      return origin.getAbsolutePath();
    }
    if (manager == null || originEtomoRunDir) {
      return EtomoDirector.INSTANCE.getOriginalUserDir();
    }
    return null;
  }

  private String getFileChooserLocation() {
    if (useTextAsFileChooserDir) {
      File file = getFile();
      if (file != null) {
        ValidDirectory dir = new ValidDirectory(manager);
        dir.set(file);
        if (!dir.isNull()) {
          return dir.get().getAbsolutePath();
        }
      }
    }
    return getOriginForFileChooser();
  }

  /**
   * Adds the text of the file path to the field.  The file path will be either absolute
   * or relative depending on the member variable absolutePath.  The directory will be set
   * to propertyUserDir, unless the member variable originEtomoRunDir is true.  The
   * directory will be used as the origin when building a relative file, or when building
   * an absolute file out of a relative file.
   *
   * @param file
   */
  public void setFile(final File file) {
    if (absolutePath) {
      field.setText(FilePath.buildAbsoluteFile(getOriginDir(), file).getPath());
    }
    else {
      field.setText(FilePath.getRelativePath(getOriginDir(), file));
    }
  }

  /**
   * Sets the file selection mode to be used in the file chooser.
   *
   * @param input
   */
  void setFileSelectionMode(final int input) {
    if (input != JFileChooser.FILES_ONLY && input != JFileChooser.DIRECTORIES_ONLY
      && input != JFileChooser.FILES_AND_DIRECTORIES) {
      System.err.println("WARNING: Incorrect file chooser file selection mode: " + input);
      return;
    }
    fileSelectionMode = input;
  }

  void setTurnOffFileHiding(final boolean input) {
    turnOffFileHiding = input;
  }

  public void setFileFilter(final FileFilter input) {
    fileFilter = input;
  }

  public FileFilter getFileFilter() {
    return fileFilter;
  }

  public boolean isRequired() {
    return field.isRequired();
  }

  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    return field.getText(doValidation, fieldDisplayer);
  }

  public String getDescription() {
    return field.getDescription();
  }

  void setRequired(final boolean required) {
    field.setRequired(required);
  }

  public String getText() {
    return field.getText();
  }

  void setText(final String text) {
    field.setText(text);
  }

  void setText(final String text, final boolean allowEmpty) {
    if (allowEmpty || (text != null && !text.isEmpty())) {
      setText(text);
    }
  }

  public void clear() {
    field.setText("");
  }

  public void setValue(final Field input) {
    field.setValue(input);
  }

  public void setValue(final String input) {
    field.setValue(input);
  }

  public void setValue(final boolean input) {}

  public boolean isSelected() {
    return false;
  }

  void setTextEntryPolicy(final boolean input) {
    if (!input) {
      field.setEditable(false);
      textEntryPolicy = false;
    }
    else {
      textEntryPolicy = true;
    }
  }

  void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    field.setEnabled(enabled);// field handles enabled versus editable
    // Only visually enabled if both enabled and editable
    button.setEnabled(enabled && editable);
  }

  void setEditable(final boolean editable) {
    if (textEntryPolicy) {
      this.editable = editable;
      field.setEditable(editable);// field handles enabled versus editable
    }
    // Editable has no visible effect if the field is disabled.
    if (enabled) {
      button.setEnabled(editable);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setToolTipText(String text) {
    field.setToolTipText(text);
    text = TooltipFormatter.INSTANCE.format(text);
    panel.setToolTipText(text);
    button.setToolTipText(text);
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      String tooltip = field.getTooltip();
      this.field.setPreformattedTooltip(tooltip);
      panel.setToolTipText(tooltip);
      button.setToolTipText(tooltip);
    }
  }

  public String getTooltip() {
    return field.getTooltip();
  }

  void setFieldToolTipText(final String text) {
    field.setToolTipText(text);
  }

  void setButtonToolTipText(final String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }
}
