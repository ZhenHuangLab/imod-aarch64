package etomo.ui.swing;

import etomo.storage.DirectiveAttribute;
import etomo.type.Option;
import etomo.ui.SharedStrings;

/**
 * <p>Description: Version of the extensible combo box which contains boolean choices and
 * contains the functions isSelected and setSelected.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class BooleanComboBoxEfield extends ComboBoxEfield {
  private static final int EMPTY_INDEX = 0;
  private static final int TRUE_INDEX = 1;
  private static final int FALSE_INDEX = 2;

  BooleanComboBoxEfield(final String label) {
    super(label, false, false);
    addItem(new Option(DirectiveAttribute.TRUE_VALUE, SharedStrings.TRUE_STRING));
    addItem(new Option(DirectiveAttribute.FALSE_VALUE, SharedStrings.FALSE_STRING));
    setChoiceListSet(true);
  }

  void setSelected(final boolean value) {
    if (value) {
      setSelectedIndex(TRUE_INDEX);
    }
    else {
      setSelectedIndex(FALSE_INDEX);
    }
    updateFlagExtension();
  }

  boolean isSelected() {
    return getSelectedIndex() == TRUE_INDEX;
  }
}