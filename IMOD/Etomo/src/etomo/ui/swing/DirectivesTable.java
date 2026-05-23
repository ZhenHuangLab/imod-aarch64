package etomo.ui.swing;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import etomo.BaseManager;
import etomo.storage.DirectiveAdaptor;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveDescrElement;
import etomo.storage.DirectiveDescrFile;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.autodoc.ReadOnlyStatement;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DirectiveFileType;
import etomo.type.DirectiveInterface;
import etomo.type.DirectiveMapInterface;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldValidationFailedException;

/**
 * <p>Description: Directive editor table,</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DirectivesTable {
  private final JPanel pnlRoot = new JPanel();
  private final GridBagLayout layout = new GridBagLayout();
  private final GridBagConstraints constraints = new GridBagConstraints();
  private final RowList list = new RowList();

  private final BaseManager manager;
  private final DirectivesDialog parent;
  private final DirectiveFileType directiveFileType;
  private final Map<DirectiveDef, String> templateValues;
  private final Set<DirectiveDef> excludedDirectives;

  private boolean debug = false;

  DirectivesTable(final BaseManager manager, final DirectivesDialog parent,
    final DirectiveFileType directiveFileType,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> excludedDirectives) {
    this.manager = manager;
    this.parent = parent;
    this.directiveFileType = directiveFileType;
    this.templateValues = templateValues;
    this.excludedDirectives = excludedDirectives;
  }

  boolean validate(final FieldDisplayer fieldDisplayer) {
    return list.validate(fieldDisplayer);
  }

  DirectivesDirectiveRow getRow(final DirectiveDef directiveDef) {
    return list.getRow(directiveDef);
  }

  void init() {
    createPanel();
    list.createPanel();
  }

  private void createPanel() {
    // init
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0.0;
    constraints.weighty = 1.0;
    // root
    pnlRoot.setLayout(layout);
    pnlRoot.setBorder(LineBorder.createBlackLineBorder());
  }

  Container getContainer() {
    return pnlRoot;
  }

  private JPanel getTable() {
    return pnlRoot;
  }

  void setValues(final BaseManager sourceManager) {
    list.setValues(sourceManager);
  }

  void closeAllSections() {
    list.closeAllSections();
  }

  void setValues(final DirectiveFileInterface directiveFile,
    final boolean setFieldHighlightValue) {
    list.setValues(directiveFile, setFieldHighlightValue);
  }

  void clearTemplateValues() {
    list.clearTemplateValues();
  }

  void clear() {
    list.clear();
  }

  void checkpointAndRestoreFromBackup(final boolean retainUserValues) {
    list.checkpointAndRestoreFromBackup(retainUserValues);
  }

  boolean backupIfChanged() {
    return list.backupIfChanged();
  }

  boolean saveAutodoc(final WritableAutodoc autodoc, final boolean doValidation,
    FieldDisplayer fieldDisplayer) {
    return list.saveAutodoc(autodoc, doValidation, fieldDisplayer);
  }

  void statusChanged(final BatchRunTomoStatus status) {
    list.statusChanged(status);
  }

  final class RowList implements DirectiveMapInterface {
    private final List<DirectivesRow> list = new ArrayList<DirectivesRow>();
    private final Map<DirectiveDef, DirectivesDirectiveRow> directiveMap =
      new HashMap<DirectiveDef, DirectivesDirectiveRow>();

    private DirectivesSectionRow extrasSection = null;

    private RowList() {}

    private boolean validate(final FieldDisplayer fieldDisplayer) {
      boolean retval = true;
      // NumberOfPatchesXandY and OverlapOfPatchesXandY are mutually exclusive
      DirectivesDirectiveRow row = getRow(DirectiveDef.NUMBER_OF_PATCHES_X_AND_Y);
      if (row != null
        && !row.validateMutuallyExclusive(getRow(DirectiveDef.OVERLAP_OF_PATCHES_X_AND_Y),
          null,
          "Use only one of the following fields: \"Number of patches to track in X "
            + "and Y\" or \"Fractional overlap of patches in X and Y\".",
          fieldDisplayer)) {
        return false;
      }
      // firstinc, userawtlt, and extract are mutually exclusive
      row = getRow(DirectiveDef.FIRST_INC);
      if (row != null && !row.validateMutuallyExclusive(getRow(DirectiveDef.USE_RAW_TLT),
        getRow(DirectiveDef.EXTRACT),
        "Only one method for getting tilt angles can be used for an axis.  Please choose "
          + "only one of these fields: \"First tilt angle & increment\", \"Use existing "
          + ".rawtlt file\", or \"Extract tilt angles from data file\".",
        fieldDisplayer)) {
        return false;
      }
      // bfirstinc, buserawtlt, and bextract are mutually exclusive
      row = getRow(DirectiveDef.BFIRST_INC);
      if (row != null && !row.validateMutuallyExclusive(getRow(DirectiveDef.BUSE_RAW_TLT),
        getRow(DirectiveDef.BEXTRACT),
        "Only one method for getting tilt angles can be used for the B axis.  Please "
          + "choose only one of these fields: \"First tilt angle & increment for B "
          + "axis\",  \"Use existing .rawtlt file for B axis\", or \"Extract tilt angles "
          + "from B axis data file\".",
        fieldDisplayer)) {
        return false;
      }
      // trimvol.thickness and trimvol.findSecAddThickness
      row = getRow(DirectiveDef.THICKNESS_FOR_TRIMVOL);
      if (row != null && !row.validateMutuallyExclusive(parent.isFindSecAddThicknessSet(),
        "Use only one of the following fields: \"Fraction or # of slices to trim to in "
          + "Z\" or the \"Find plastic section limits and add\" check-box/text-field in "
          + "the Basic dialog.",
        fieldDisplayer)) {
        return false;
      }
      // trimvol.scaleToMeanSD and trimvol.scaleFromZ
      row = getRow(DirectiveDef.SCALE_TO_MEAN_SD);
      if (row != null) {
        if (!row.validateMutuallyExclusive(parent.isScaleFromZSet(),
          "Only one scaling method can be used.  Use either \"Mean and SD for byte "
            + "scaling\" or the \"Fraction of Z slices to analyze\" check-box/text-field "
            + "in the Basic dialog.",
          fieldDisplayer)) {
          return false;
        }
        DirectivesDirectiveRow compareToRow = row;
        // trimvol.scaleFromX and trimvol.scaleToMeanSD
        row = getRow(DirectiveDef.SCALE_FROM_X);
        if (row != null && !row.validateMutuallyExclusive(compareToRow, null,
          "Only one scaling method can be used.  Use either \"Mean and SD for byte "
            + "scaling\" or \"Frac or # of pixels in X for byte scaling\".",
          fieldDisplayer)) {
          return false;
        }
        // trimvol.scaleFromY and trimvol.scaleToMeanSD
        row = getRow(DirectiveDef.SCALE_FROM_Y);
        if (row != null && !row.validateMutuallyExclusive(compareToRow, null,
          "Only one scaling method can be used.  Use either \"Mean and SD for byte "
            + "scaling\" or \"Frac or # of pixels in Y for byte scaling\".",
          fieldDisplayer)) {
          return false;
        }
      }
      return true;
    }

    void createPanel() {
      DirectiveDescrFile.Iterator iterator =
        DirectiveDescrFile.INSTANCE.getIterator(null, null);
      DirectivesSectionRow currentSection = null;
      JPanel pnlTable = getTable();
      boolean sectionContainsDirectives = false;
      DirectiveDef prevDirectiveDef = null;
      DirectiveDef directiveDef = null;
      while (iterator.hasNext()) {
        prevDirectiveDef = directiveDef;
        directiveDef = null;
        String[] lineArray = iterator.next();
        if (DirectiveDescrElement.isSection(lineArray)) {
          // Finish previous section
          if (currentSection != null) {
            if (!sectionContainsDirectives) {
              list.remove(currentSection);
            }
            else {
              currentSection.updateEnabled();
            }
          }
          // Start processing new section
          currentSection = DirectivesSectionRow.getInstance(parent, lineArray, pnlTable,
            layout, constraints);
          list.add(currentSection);
          sectionContainsDirectives = false;
        }
        else {
          // If any of the datasets are dual axis, allow CopyArg B axis directives.
          directiveDef =
            DirectiveDescrElement.getDirectiveDef(lineArray, prevDirectiveDef);
          if (directiveDef != null && DirectiveDescrElement.isDirective(lineArray)
            && DirectiveDescrElement.isIncluded(lineArray, directiveFileType)
            && (excludedDirectives == null || !excludedDirectives.contains(directiveDef))
            && (parent.hasDual() || directiveDef.getCopyArgAxisID(
              DirectiveDescrElement.getName(lineArray)) != AxisID.SECOND)) {
            DirectivesDirectiveRow row = DirectivesDirectiveRow.getInstance(manager,
              parent, currentSection, lineArray, pnlTable, layout, constraints,
              directiveFileType == null, directiveDef, debug);
            list.add(row);
            sectionContainsDirectives = true;
            if (directiveDef != null) {
              directiveMap.put(directiveDef, row);
            }
          }
        }
      }
      if (currentSection != null) {
        currentSection.updateEnabled();
      }
      Iterator<DirectivesRow> rowIterator = list.iterator();
      while (rowIterator.hasNext()) {
        rowIterator.next().display(getTable(), layout, constraints);
      }
      statusChanged(BatchRunTomoStatus.DEFAULT);
    }

    void setValues(final BaseManager sourceManager) {
      sourceManager.updateDirectiveMap(this, null);
    }

    private void closeAllSections() {
      Iterator<DirectivesRow> iterator = list.iterator();
      while (iterator.hasNext()) {
        DirectivesRow row = iterator.next();
        if (row instanceof DirectivesSectionRow) {
          ((DirectivesSectionRow) row).setOpen(false);
        }
      }
    }

    private DirectivesDirectiveRow getRow(final DirectiveDef directiveDef) {
      if (directiveDef == null) {
        return null;
      }
      return directiveMap.get(directiveDef);
    }

    public DirectiveInterface getDirective(final DirectiveDef directiveDef) {
      return getRow(directiveDef);
    }

    public DirectiveInterface getDirectiveFromPair(final DirectiveDef directiveDef,
      final AxisID pairAxisID) {
      if (directiveDef == null) {
        return null;
      }
      return directiveMap.get(directiveDef.switchStandardKey(pairAxisID));
    }

    private void clearTemplateValues() {
      Iterator<DirectivesDirectiveRow> iterator = new DirectiveIterator();
      while (iterator.hasNext()) {
        iterator.next().clearTemplateValue();
      }
    }

    private void clear() {
      Iterator<DirectivesDirectiveRow> iterator = new DirectiveIterator();
      while (iterator.hasNext()) {
        iterator.next().clear();
      }
    }

    private void checkpointAndRestoreFromBackup(final boolean retainUserValues) {
      Iterator<DirectivesDirectiveRow> iterator = new DirectiveIterator();
      while (iterator.hasNext()) {
        DirectivesDirectiveRow row = iterator.next();
        // checkpoint
        row.checkpoint();
        // If the user wants to retain their values, apply backed up values and
        // then
        // delete
        // them.
        if (retainUserValues) {
          row.restoreFromBackup();
        }
      }
    }

    /**
     * Check isDifferentFromCheckpoint on all data entry fields
     *
     * @return true if any field's isDifferentFromCheckpoint function returned true
     */
    private boolean backupIfChanged() {
      boolean changed = false;
      Iterator<DirectivesDirectiveRow> iterator = new DirectiveIterator();
      while (iterator.hasNext()) {
        DirectivesDirectiveRow row = iterator.next();
        if (row.isDifferentFromCheckpoint(true)) {
          row.backup();
          changed = true;
        }
      }
      return changed;
    }

    /**
     * Set values from the directive file collection.  Only change fields that exist in
     * directive file collection.
     *
     * @param autodoc - a writable autodoc
     */
    private boolean saveAutodoc(final WritableAutodoc autodoc, final boolean doValidation,
      FieldDisplayer fieldDisplayer) {
      if (autodoc == null) {
        return true;
      }
      try {
        Iterator<DirectivesDirectiveRow> iterator = new DirectiveIterator();
        while (iterator.hasNext()) {
          iterator.next().saveAutodoc(autodoc, doValidation, fieldDisplayer,
            templateValues);
        }
        return true;
      }
      catch (FieldValidationFailedException e) {
        return false;
      }
    }

    /**
     * Set values from the directive file collection.  Only change fields that exist in
     * directive file collection.  Make sure the template values are set for each field when
     * setFieldHighlightValue is on.
     * 
     * Look for a matching directive in the table for each directive in directiveFiles and
     * set or override the row value based on the directive.  If a directive does not
     * exist in the table, then add it to an "Extras" section at the top.
     * 
     * Ignore b directives completely.  "a" directives are overridden by "any" directives.
     *
     * @param collection - templates and base directive file - sorted
     * @param setFieldHighlightValue - template only value is set in field highlight and template value
     */
    void setValues(final DirectiveFileInterface directiveFiles,
      final boolean setFieldHighlightValue) {
      Iterator<ReadOnlyStatement> statementIterator =
        directiveFiles.iterator(setFieldHighlightValue);
      if (statementIterator == null) {
        return;
      }
      DirectiveAdaptor directive = new DirectiveAdaptor();
      Set<DirectiveDef> doneSet = new HashSet<DirectiveDef>();
      boolean rowAdded = false;
      while (statementIterator.hasNext()) {
        directive.set(statementIterator.next());
        DirectiveDef directiveDef = directive.getDirectiveDef();
        // Ignore most B axis only directives, excluded directives, and directives that
        // have already been processed.
        if (directiveDef == null /*|| directive.isBAxis()*/
          || (excludedDirectives == null || excludedDirectives.contains(directiveDef))
          || doneSet.contains(directiveDef)) {
          continue;
        }
        DirectivesDirectiveRow row = getRow(directiveDef);
        if (row == null) {
          // add unknown directive to the Extras section
          if (extrasSection == null) {
            extrasSection = DirectivesSectionRow.getExtrasInstance(parent);
            list.add(extrasSection);
            rowAdded = true;
          }
          // Put the new unknown one below the section header.
          row = DirectivesDirectiveRow.getInstance(manager, parent, extrasSection,
            directive, true);
          list.add(row);
          rowAdded = true;
          directiveMap.put(directiveDef, row);
        }
        row.setValue(directiveFiles, setFieldHighlightValue, templateValues);
        // The whole collection is used to set the directive value, so only the first
        // instance of a directive has to be processed
        doneSet.add(directiveDef);
      }
      // If anything was added, remove and display everything.
      if (rowAdded) {
        extrasSection.updateEnabled();
        Iterator<DirectivesRow> rowIterator = list.iterator();
        while (rowIterator.hasNext()) {
          rowIterator.next().remove();
        }
        rowIterator = list.iterator();
        while (rowIterator.hasNext()) {
          rowIterator.next().display(getTable(), layout, constraints);
        }
      }
    }

    void statusChanged(final BatchRunTomoStatus status) {
      Iterator<DirectivesRow> iterator = list.iterator();
      while (iterator.hasNext()) {
        iterator.next().statusChanged(status);
      }
    }

    /**
     * Returns just the directive row from the list.
     */
    private final class DirectiveIterator implements Iterator<DirectivesDirectiveRow> {
      private Iterator<DirectivesRow> iterator = null;
      private DirectivesDirectiveRow next = null;

      private DirectiveIterator() {}

      public boolean hasNext() {
        if (next != null) {
          return true;
        }
        next = getNext();
        return next != null;
      }

      public DirectivesDirectiveRow next() {
        if (next != null) {
          DirectivesDirectiveRow temp = next;
          next = null;
          return temp;
        }
        return getNext();
      }

      private DirectivesDirectiveRow getNext() {
        if (iterator == null) {
          iterator = list.iterator();
        }
        while (iterator.hasNext()) {
          DirectivesRow row = iterator.next();
          if (row instanceof DirectivesDirectiveRow) {
            return (DirectivesDirectiveRow) row;
          }
        }
        return null;
      }

      public void remove() {}
    }
  }
}