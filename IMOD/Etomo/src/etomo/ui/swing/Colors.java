package etomo.ui.swing;

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;

import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2005 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.11  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.10  2010/01/13 21:54:50  sueh
 * <p> bug# 1298 Fixed highlight colors.
 * <p>
 * <p> Revision 1.9  2009/02/27 03:52:44  sueh
 * <p> bug# 1172 Added experimental automation recording background and
 * <p> border colors.  Bug# 1188 No longer checking version when setting the background colors because Java 1.4 is no longer supported.
 * <p>
 * <p> Revision 1.8  2007/04/02 21:47:43  sueh
 * <p> bug# 964 Added CELL_DISABLED_FOREGROUND.
 * <p>
 * <p> Revision 1.7  2007/03/27 19:30:49  sueh
 * <p> bug# 964 Changed InputCell.setEnabled() to setEditable.
 * <p>
 * <p> Revision 1.6  2007/03/01 01:28:42  sueh
 * <p> bug# 964 Made InputCell colors constant and moved them to Colors.
 * <p>
 * <p> Revision 1.5  2007/02/20 20:36:10  sueh
 * <p> bug# 964 Improved the colors.
 * <p>
 * <p> Revision 1.4  2007/02/19 22:00:41  sueh
 * <p> bug# 964 Added background color for PEET interface.
 * <p>
 * <p> Revision 1.3  2006/04/07 23:32:18  sueh
 * <p> bug# 846 Changing the background colors for java 1.5.
 * <p>
 * <p> Revision 1.2  2006/04/06 23:33:58  sueh
 * <p> bug# 844 Added colors for the join and the generic parallel processing
 * <p> windows.
 * <p>
 * <p> Revision 1.1  2005/04/16 01:54:54  sueh
 * <p> bug# 615 Class to hold static colors.
 * <p> </p>
 */
public final class Colors {
  static final ColorUIResource CELL_FOREGROUND = new ColorUIResource(0, 0, 0);
  static final ColorUIResource CELL_NOT_IN_USE_FOREGROUND =
    new ColorUIResource(102, 102, 102);
  static final ColorUIResource CELL_ERROR_BACKGROUND = new ColorUIResource(255, 204, 204);
  static final ColorUIResource CELL_ERROR_BACKGROUND_NOT_EDITABLE =
    new ColorUIResource(230, 184, 184);// 223,179,179?
  static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);
 public static final ColorUIResource WARNING_BACKGROUND = new ColorUIResource(255, 255, 204);
  static final ColorUIResource WARNING_BACKGROUND_NOT_EDITABLE =
    new ColorUIResource(230, 230, 184);
  static final ColorUIResource HIGHLIGHT_BACKGROUND = new ColorUIResource(204, 255, 255);
  static final ColorUIResource HIGHLIGHT_BACKGROUND_NOT_EDITABLE =
    new ColorUIResource(184, 230, 230);
  static final ColorUIResource RUN_HIGHLIGHT_BACKGROUND =
    new ColorUIResource(204, 255, 204);
  static final ColorUIResource RUN_HIGHLIGHT_BACKGROUND_NOT_EDITABLE =
    new ColorUIResource(184, 230, 184);

  static final ColorUIResource FOREGROUND = new ColorUIResource(0, 0, 0);

  private static final ColorUIResource BACKGROUND_GREYOUT =
    new ColorUIResource(25, 25, 25);
  static final ColorUIResource CELL_DISABLED_FOREGROUND =
    new ColorUIResource(120, 120, 120);
  static final Color AVAILABLE_BACKGROUND = new ColorUIResource(224, 240, 255);
  static final Color AVAILABLE_BORDER = new ColorUIResource(153, 204, 255);
  public static final Color FIELD_HIGHLIGHT = new Color(0, 0, 185);
  private static final int BACKGROUND_ADJUSTMENT = 20;

  private static Color backgroundA = null;
  private static Color backgroundB = null;
  private static Color backgroundJoin = null;
  private static Color backgroundParallel = null;
  private static Color backgroundBatchruntomo = null;
  private static Color backgroundSerialSections = null;
  private static Color backgroundTools = null;
  private static ColorUIResource cellNotEditableBackground = null;

  static Color getBackgroundA() {
    if (backgroundA == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundA = new Color(173, 199, 224);// saphire
      }
      else {
        backgroundA = new Color(163, 214, 247);
      }
    }
    return backgroundA;
  }

  static Color getBackgroundB() {
    if (backgroundB == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundB = new Color(173, 224, 199);// jade
      }
      else {
        backgroundB = new Color(255, 216, 141);
      }
    }
    return backgroundB;
  }

  static Color getBackgroundJoin() {
    if (backgroundJoin == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundJoin = new Color(199, 173, 224);// violet
      }
      else {
        backgroundJoin = new Color(162, 167, 255);
      }
    }
    return backgroundJoin;
  }

  static Color getBackgroundParallel() {
    if (backgroundParallel == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundParallel = new Color(186, 224, 173);// lime
      }
      else {
        backgroundParallel = new Color(255, 253, 216);
      }
    }
    return backgroundParallel;
  }

  static Color getBackgroundBatchruntomo() {
    if (backgroundBatchruntomo == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundBatchruntomo = new Color(194, 208, 251);
      }
      else {
        backgroundBatchruntomo = new Color(255, 239, 192);
      }
    }
    return backgroundBatchruntomo;
  }

  static Color getBackgroundSerialSections() {
    if (backgroundSerialSections == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundSerialSections = new Color(218, 232, 250);
      }
      else {
        backgroundSerialSections = new Color(194, 247, 159);
      }
    }
    return backgroundSerialSections;
  }

  static Color getBackgroundTools() {
    if (backgroundTools == null) {
      if (!Utilities.APRIL_FOOLS) {
        backgroundTools = new Color(173, 212, 224);// azure
      }
      else {
        backgroundTools = new Color(52, 130, 218);
      }
    }
    return backgroundTools;
  }

  static ColorUIResource getCellNotEditableBackground() {
    if (cellNotEditableBackground == null) {
      cellNotEditableBackground = subtractColor(BACKGROUND, BACKGROUND_GREYOUT);
    }
    return cellNotEditableBackground;
  }

  static ColorUIResource subtractColor(Color color, Color subtractColor) {
    return new ColorUIResource(color.getRed() - subtractColor.getRed(),
      color.getGreen() - subtractColor.getGreen(),
      color.getBlue() - subtractColor.getBlue());
  }

  private static ColorUIResource addColor(Color color, Color subtractColor) {
    return new ColorUIResource(color.getRed() + subtractColor.getRed(),
      color.getGreen() + subtractColor.getGreen(),
      color.getBlue() + subtractColor.getBlue());
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.11  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.10  2010/01/13 21:54:50  sueh
 * <p> bug# 1298 Fixed highlight colors.
 * <p>
 * <p> Revision 1.9  2009/02/27 03:52:44  sueh
 * <p> bug# 1172 Added experimental automation recording background and
 * <p> border colors.  Bug# 1188 No longer checking version when setting the background colors because Java 1.4 is no longer supported.
 * <p>
 * <p> Revision 1.8  2007/04/02 21:47:43  sueh
 * <p> bug# 964 Added CELL_DISABLED_FOREGROUND.
 * <p>
 * <p> Revision 1.7  2007/03/27 19:30:49  sueh
 * <p> bug# 964 Changed InputCell.setEnabled() to setEditable.
 * <p>
 * <p> Revision 1.6  2007/03/01 01:28:42  sueh
 * <p> bug# 964 Made InputCell colors constant and moved them to Colors.
 * <p>
 * <p> Revision 1.5  2007/02/20 20:36:10  sueh
 * <p> bug# 964 Improved the colors.
 * <p>
 * <p> Revision 1.4  2007/02/19 22:00:41  sueh
 * <p> bug# 964 Added background color for PEET interface.
 * <p>
 * <p> Revision 1.3  2006/04/07 23:32:18  sueh
 * <p> bug# 846 Changing the background colors for java 1.5.
 * <p>
 * <p> Revision 1.2  2006/04/06 23:33:58  sueh
 * <p> bug# 844 Added colors for the join and the generic parallel processing
 * <p> windows.
 * <p>
 * <p> Revision 1.1  2005/04/16 01:54:54  sueh
 * <p> bug# 615 Class to hold static colors.
 * <p> </p>
 */
