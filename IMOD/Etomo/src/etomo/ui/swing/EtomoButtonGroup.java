package etomo.ui.swing;

import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;

import etomo.type.EnumeratedType;

/**
 * <p>Description: Extension of ButtonGroup which allows a button to be selected based on
 * its EnumeratedType. </p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class EtomoButtonGroup extends ButtonGroup {
  private final Map<EnumeratedType, AbstractRadioButtonModel> buttonModelList =
    new HashMap<EnumeratedType, AbstractRadioButtonModel>();

  EtomoButtonGroup() {}

  public void add(final AbstractButton button) {
    super.add(button);
    ButtonModel model = button.getModel();
    // Save the button model if possible.
    if (model instanceof AbstractRadioButtonModel) {
      AbstractRadioButtonModel radioButtonModel = (AbstractRadioButtonModel) model;
      EnumeratedType enumeratedType = radioButtonModel.getEnumeratedType();
      if (enumeratedType != null) {
        buttonModelList.put(enumeratedType, radioButtonModel);
      }
    }
  }

  void setSelected(final EnumeratedType enumeratedType) {
    if (enumeratedType == null) {
      return;
    }
    super.setSelected(buttonModelList.get(enumeratedType), true);
  }
}