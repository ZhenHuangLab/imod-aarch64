package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import etomo.storage.DirectiveDescrElement;
import etomo.type.BatchRunTomoStatus;
import etomo.ui.FieldDisplayer;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagType;
import etomo.ui.SectionListener;

/**
 * <p>Description: A section as specified by the directives .csv file.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DirectivesSectionRow
  implements DirectivesRow, ActionListener, SectionListener, FlagDisplay, FieldDisplayer {
  private final Ebutton hEmpty = Ebutton.getHeaderInstance();
  private final List<DirectivesDirectiveRow> directiveList =
    new ArrayList<DirectivesDirectiveRow>();

  private final Ebutton btnSection;

  private FlagType curErrorFlagType = null;

  private DirectivesSectionRow(final String[] lineArray) {
    btnSection =
      Ebutton.getOpenCloseInstance(DirectiveDescrElement.getSectionHeader(lineArray));
    btnSection.setRespondToNonErrorFlags(false);
    btnSection.setAllowFlagEditableControl(false);
  }

  private DirectivesSectionRow(final String title) {
    btnSection = Ebutton.getOpenCloseInstance(title);
    btnSection.setRespondToNonErrorFlags(false);
    btnSection.setAllowFlagEditableControl(false);
  }

  static DirectivesSectionRow getInstance(final DirectivesDialog parent,
    final String[] lineArray, final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    DirectivesSectionRow instance = new DirectivesSectionRow(lineArray);
    instance.addListeners(parent);
    return instance;
  }

  static DirectivesSectionRow getExtrasInstance(final DirectivesDialog parent) {
    DirectivesSectionRow instance = new DirectivesSectionRow("Unknown Directives");
    instance.addListeners(parent);
    return instance;
  }

  private void addListeners(final DirectivesDialog dialog) {
    btnSection.addActionListener(this);
    dialog.addSectionListener(this);
  }

  public void remove() {
    btnSection.remove();
    hEmpty.remove();
  }

  public void display(final JPanel pnlTable, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    constraints.gridwidth = 1;
    btnSection.add(pnlTable, layout, constraints);
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    hEmpty.add(pnlTable, layout, constraints);
  }

  String getTitle() {
    return btnSection.getText();
  }

  void setOpen(final boolean open) {
    if (btnSection.isSelected() == open) {
      return;
    }
    btnSection.setSelected(open);
    Iterator<DirectivesDirectiveRow> iterator = directiveList.iterator();
    while (iterator.hasNext()) {
      iterator.next().setOpen(open);
    }
  }

  void addDirective(final DirectivesDirectiveRow directive) {
    directiveList.add(directive);
  }

  boolean hasDirectives() {
    return !directiveList.isEmpty();
  }

  public void statusChanged(BatchRunTomoStatus status) {}

  public void actionPerformed(final ActionEvent event) {
    updateOpen();
  }

  public void display() {
    if (!btnSection.isSelected()) {
      btnSection.doClick();
    }
  }

  private void updateOpen() {
    boolean open = btnSection.isSelected();
    Iterator<DirectivesDirectiveRow> iterator = directiveList.iterator();
    while (iterator.hasNext()) {
      iterator.next().setOpen(open);
    }
  }

  public void sectionEvent() {
    updateEnabled();
  }

  /**
   * Set a flag if any of the rows has an error flagType.  Only do this if the flag has
   * changed.
   * @param type
   */
  public void setFlag(final FlagType type) {
    // If this change isn't significant, don't investigate it.
    if (!isProcessFlag(type)) {
      return;
    }
    Iterator<DirectivesDirectiveRow> iterator = directiveList.iterator();
    FlagType errorFlag = null;
    while (iterator.hasNext()) {
      FlagType flagType = iterator.next().getFlagType();
      if (flagType != null && flagType.isError()) {
        errorFlag = flagType;
        break;
      }
    }
    // Call setFlag if something has changed.
    // Don't care which error flag it is - they all work the same for btnSection.
    if (isProcessFlag(errorFlag)) {
      curErrorFlagType = errorFlag;
      btnSection.setFlag(curErrorFlagType);
    }
  }

  /**
   * Returns true if flagType is not equal is curErrorFlagType.  Not error flag types are
   * are treated as null flags because this class does not react to them.  Error flags are
   * all processed in the same way so them are treated as the same flag.
   * @param flagType
   * @return
   */
  private boolean isProcessFlag(FlagType flagType) {
    if (flagType != null && !flagType.isError()) {
      flagType = null;
    }
    if (flagType == null && curErrorFlagType == null) {
      return false;
    }
    return flagType == null || curErrorFlagType == null;
  }

  /**
   * Allows the section be disabled if no rows are visible.
   */
  void updateEnabled() {
    boolean canDisplay = false;
    Iterator<DirectivesDirectiveRow> iterator = directiveList.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().canDisplay()) {
        canDisplay = true;
        break;
      }
    }
    if (!canDisplay && btnSection.isSelected()) {
      btnSection.setSelected(false);
      updateOpen();
    }
    btnSection.setEnabled(canDisplay);
  }
}