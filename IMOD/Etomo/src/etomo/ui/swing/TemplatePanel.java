package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.logic.ConfigTool;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFile;
import etomo.storage.DirectiveFileCollection;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.AxisID;
import etomo.type.DirectiveFileType;
import etomo.type.UserConfiguration;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2013 - 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class TemplatePanel {
  private static final String EMPTY_OPTION = "None available";
  private static final String NO_SELECTION1 = "No selection (";
  private static final String NO_SELECTION2 = " available)";
  private static final int NUM_TEMPLATES = 3;

  private final JPanel pnlRoot = new JPanel();
  private final ComboBox cmbScopeTemplate = ComboBox.getInstance("Scope template:");
  private final ComboBox cmbSystemTemplate = ComboBox.getInstance("System template:");
  private final ComboBox cmbUserTemplate = ComboBox.getInstance("User template:");

  private final TemplateActionListener listener;
  private final BaseManager manager;
  private final AxisID axisID;
  final SettingsDialog settings;
  private final DirectiveFileCollection directiveFileCollection;
  private final boolean drawBorder;

  private File[] scopeTemplateFileList = null;
  private File[] systemTemplateFileList = null;
  private File[] userTemplateFileList = null;
  private File newUserTemplateDir = null;
  private boolean actionsActive = true;

  private TemplatePanel(final BaseManager manager, final AxisID axisID,
    final TemplateActionListener listener, final SettingsDialog settings,
    final boolean drawBorder, final DirectiveFileCollection directiveFileCollection,
    final boolean batchInterface) {
    this.listener = listener;
    this.manager = manager;
    this.axisID = axisID;
    this.settings = settings;
    this.drawBorder = drawBorder;
    if (directiveFileCollection == null) {
      if (batchInterface) {
        this.directiveFileCollection =
          DirectiveFileCollection.getBatchInstance(manager, axisID);
      }
      else {
        this.directiveFileCollection = new DirectiveFileCollection(manager, axisID);
      }
    }
    else {
      this.directiveFileCollection = directiveFileCollection;
    }
  }

  static TemplatePanel getInstance(final BaseManager manager, final AxisID axisID,
    final TemplateActionListener listener, final String title,
    final SettingsDialog settings, final boolean batchInterface) {
    TemplatePanel instance =
      new TemplatePanel(manager, axisID, listener, settings, true, null, batchInterface);
    instance.createPanel(title);
    instance.addListeners();
    return instance;
  }

  static TemplatePanel getBorderlessInstance(final BaseManager manager,
    final AxisID axisID, final TemplateActionListener listener, final String title,
    final SettingsDialog settings, final DirectiveFileCollection directiveFileCollection,
    final boolean delayListeners, final boolean batchInterface) {
    TemplatePanel instance = new TemplatePanel(manager, axisID, listener, settings, false,
      directiveFileCollection, batchInterface);
    instance.createPanel(title);
    if (!delayListeners) {
      instance.addListeners();
    }
    return instance;
  }

  private void fillComboBox(final ComboBox comboBox, final File[] fileList) {
    if (comboBox == null) {
      return;
    }
    int len = fileList != null ? fileList.length : 0;
    comboBox.setPlaceholder(EMPTY_OPTION, NO_SELECTION1 + len + NO_SELECTION2);
    if (len > 0) {
      for (int i = 0; i < len; i++) {
        comboBox.addItem(fileList[i].getName());
      }
    }
    comboBox.unselect();
  }

  private void createPanel(final String title) {
    // init
    scopeTemplateFileList = ConfigTool.getScopeTemplateFiles();
    fillComboBox(cmbScopeTemplate, scopeTemplateFileList);
    systemTemplateFileList = ConfigTool.getSystemTemplateFiles();
    fillComboBox(cmbSystemTemplate, systemTemplateFileList);
    loadUserTemplate();
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    if (drawBorder) {
      if (title != null) {
        pnlRoot.setBorder(new EtchedBorder(title).getBorder());
      }
      else {
        pnlRoot.setBorder(BorderFactory.createEtchedBorder());
      }
    }
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlRoot.add(cmbScopeTemplate.getComponent());
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlRoot.add(cmbSystemTemplate.getComponent());
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlRoot.add(cmbUserTemplate.getComponent());
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y2));
  }

  void addListeners() {
    addActionListener(listener);
    cmbUserTemplate.addFocusListener(new TemplateFocusListener(this));
  }

  Component getComponent() {
    return pnlRoot;
  }

  void addActionListener(final ActionListener listener) {
    cmbScopeTemplate.addActionListener(listener);
    cmbSystemTemplate.addActionListener(listener);
    cmbUserTemplate.addActionListener(listener);
  }

  void setEnabled(final boolean enabled) {
    cmbScopeTemplate.setEnabled(enabled);
    cmbSystemTemplate.setEnabled(enabled);
    cmbUserTemplate.setEnabled(enabled);
  }

  void setEditable(final boolean editable) {
    cmbScopeTemplate.setEditable(editable);
    cmbSystemTemplate.setEditable(editable);
    cmbUserTemplate.setEditable(editable);
  }

  void saveAutodoc(final WritableAutodoc autodoc) {
    File templateFile = getScopeTemplateFile();
    if (templateFile != null) {
      autodoc.addNameValuePairAttribute(DirectiveDef.SCOPE_TEMPLATE.getDirective(),
        templateFile.getAbsolutePath());
    }
    templateFile = getSystemTemplateFile();
    if (templateFile != null) {
      autodoc.addNameValuePairAttribute(DirectiveDef.SYSTEM_TEMPLATE.getDirective(),
        templateFile.getAbsolutePath());
    }
    templateFile = getUserTemplateFile();
    if (templateFile != null) {
      autodoc.addNameValuePairAttribute(DirectiveDef.USER_TEMPLATE.getDirective(),
        templateFile.getAbsolutePath());
    }
  }

  void setFieldHighlight() {
    cmbScopeTemplate.setFieldHighlight();
    cmbSystemTemplate.setFieldHighlight();
    cmbUserTemplate.setFieldHighlight();
  }

  private void loadUserTemplate() {
    userTemplateFileList = null;
    cmbUserTemplate.removeAllItems();
    // If the user template directory is in a different directory from the location of the
    // default user template, the user template will not be loaded.
    userTemplateFileList = ConfigTool.getUserTemplateFiles(newUserTemplateDir);
    fillComboBox(cmbUserTemplate, userTemplateFileList);
  }

  private File getTemplateFile(final ComboBox cmbTemplate,
    final File[] templateFileList) {
    if (cmbTemplate.isEnabled()) {
      int i = cmbTemplate.getSelectedIndex();
      if (i != -1 && templateFileList != null) {
        return templateFileList[i];
      }
    }
    return null;
  }

  /**
   * Choose the combobox element that matches template
   *
   * @param template         - either an absolute file path or a file name with no path
   *                         (imodhelp)
   * @param templateFileList
   * @param cmbTemplate
   */
  private void selectTemplate(final String template, final File[] templateFileList,
    final ComboBox cmbTemplate) {
    if (templateFileList == null || template == null) {
      return;
    }
    boolean absPath = false;
    if (template.indexOf(File.separator) != -1) {
      absPath = true;
    }
    // If template doesn't match something in templateFileList, nothing will be
    // selected in the combobox.
    for (int i = 0; i < templateFileList.length; i++) {
      if ((absPath && templateFileList[i].getAbsolutePath().equals(template))
        || (!absPath && templateFileList[i].getName().equals(template))) {
        cmbTemplate.setSelectedIndex(i);
        break;
      }
    }
  }

  void clear() {
    cmbScopeTemplate.unselect();
    cmbSystemTemplate.unselect();
    cmbUserTemplate.unselect();
  }

  /**
   * When input is false, indirectly inactivates actions by preventing the recognition of
   * action commands.
   * @param input
   */
  void activateActions(final boolean input) {
    actionsActive = input;
  }

  boolean equalsActionCommand(final String actionCommand) {
    if (!actionsActive) {
      return false;
    }
    return actionCommand.equals(cmbScopeTemplate.getActionCommand())
      || actionCommand.equals(cmbSystemTemplate.getActionCommand())
      || actionCommand.equals(cmbUserTemplate.getActionCommand());
  }

  void getParameters(final UserConfiguration userConfig) {
    userConfig.setScopeTemplate(getScopeTemplateFile());
    userConfig.setSystemTemplate(getSystemTemplateFile());
    userConfig.setUserTemplate(getUserTemplateFile());
  }

  /**
   * Refresh the directive file collection and return it.
   *
   * @return
   */
  DirectiveFileCollection getDirectiveFileCollection() {
    refreshDirectiveFileCollection();
    return directiveFileCollection;
  }

  /**
   * Refresh the directive file collection and return it.
   *
   * @return
   */
  void refreshDirectiveFileCollection() {
    directiveFileCollection.setDirectiveFile(getScopeTemplateFile(),
      DirectiveFileType.SCOPE);
    directiveFileCollection.setDirectiveFile(getSystemTemplateFile(),
      DirectiveFileType.SYSTEM);
    directiveFileCollection.setDirectiveFile(getUserTemplateFile(),
      DirectiveFileType.USER);
  }

  File[] getFiles() {
    File[] files = new File[NUM_TEMPLATES];
    int i = 0;
    files[i++] = getScopeTemplateFile();
    files[i++] = getSystemTemplateFile();
    files[i++] = getUserTemplateFile();
    return files;
  }

  private File getScopeTemplateFile() {
    return getTemplateFile(cmbScopeTemplate, scopeTemplateFileList);
  }

  private File getSystemTemplateFile() {
    return getTemplateFile(cmbSystemTemplate, systemTemplateFileList);
  }

  private File getUserTemplateFile() {
    return getTemplateFile(cmbUserTemplate, userTemplateFileList);
  }

  private void focusGained() {
    // Only need to listen to user template combobox
    reloadUserTemplate();
  }

  /**
   * Updates the user template combobox. It may need to change if settings are modified.
   */
  private void reloadUserTemplate() {
    if (settings != null && !settings.equalsUserTemplateDir(newUserTemplateDir)) {
      // If a new user template directory has been entered, reload the user template combo
      // box.
      newUserTemplateDir = settings.getUserTemplateDir();
      loadUserTemplate();
    }
  }

  boolean isAppearanceSettingChanged(final UserConfiguration userConfig) {
    if (!userConfig.equalsScopeTemplate(getScopeTemplateFile())
      || !userConfig.equalsSystemTemplate(getSystemTemplateFile())
      || !userConfig.equalsUserTemplate(getUserTemplateFile())) {
      return true;
    }
    return false;
  }

  void setParameters(final UserConfiguration userConfig) {
    if (userConfig.isScopeTemplateSet()) {
      selectTemplate(userConfig.getScopeTemplate(), scopeTemplateFileList,
        cmbScopeTemplate);
    }
    if (userConfig.isSystemTemplateSet()) {
      selectTemplate(userConfig.getSystemTemplate(), systemTemplateFileList,
        cmbSystemTemplate);
    }
    if (userConfig.isUserTemplateSet()) {
      selectTemplate(userConfig.getUserTemplate(), userTemplateFileList, cmbUserTemplate);
    }
  }

  /**
   * Set the template files to match the entries in the directiveFile.
   *
   * @param directiveFile
   * @return true if the directive file contained at least one template directive
   */
  void setParameters(final DirectiveFile directiveFile) {
    if (directiveFile == null) {
      return;
    }
    DirectiveDef directiveDef = DirectiveDef.SCOPE_TEMPLATE;
    if (directiveFile.contains(directiveDef)) {
      selectTemplate(directiveFile.getValue(directiveDef), scopeTemplateFileList,
        cmbScopeTemplate);
    }
    else {
      cmbScopeTemplate.unselect();
    }
    directiveDef = DirectiveDef.SYSTEM_TEMPLATE;
    if (directiveFile.contains(directiveDef)) {
      selectTemplate(directiveFile.getValue(directiveDef), systemTemplateFileList,
        cmbSystemTemplate);
    }
    else {
      cmbSystemTemplate.unselect();
    }
    directiveDef = DirectiveDef.USER_TEMPLATE;
    if (directiveFile.contains(directiveDef)) {
      reloadUserTemplate();
      selectTemplate(directiveFile.getValue(directiveDef), userTemplateFileList,
        cmbUserTemplate);
    }
    else {
      cmbUserTemplate.unselect();
    }
    refreshDirectiveFileCollection();
  }

  void setScopeTooltip(final String tooltipText) {
    cmbScopeTemplate.setToolTipText(tooltipText);
  }

  void setSystemTooltip(final String tooltipText) {
    cmbSystemTemplate.setToolTipText(tooltipText);
  }

  void setUserTooltip(final String tooltipText) {
    cmbUserTemplate.setToolTipText(tooltipText);
  }

  private static final class TemplateFocusListener implements FocusListener {
    private final TemplatePanel template;

    TemplateFocusListener(final TemplatePanel template) {
      this.template = template;
    }

    public void focusGained(final FocusEvent e) {
      template.focusGained();
    }

    public void focusLost(final FocusEvent e) {}
  }
}
