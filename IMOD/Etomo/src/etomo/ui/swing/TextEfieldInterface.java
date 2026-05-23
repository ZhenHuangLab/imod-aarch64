package etomo.ui.swing;

import etomo.storage.DirectiveDef;

/**
 * <p>Description: General interface for extensible fields that contain text.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface TextEfieldInterface {
  public DirectiveDef getDirectiveDef();

  public boolean isEnabled();

  public boolean isVisible();

  public String getText();

  public void setText(String text);

  public void setFieldHighlight(String text);

  public void setTemplateValue();

  public boolean equals(String string);
  
  public void setDebug(boolean debug);
}