package etomo.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.Border;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
final class Minibutton extends JButton {
  public static final String rcsid = "$Id:$";

  private final boolean round;

  private Color rolloverColor;
  private Color pressedColor;
  private Color outlineColor;

  private Minibutton(final String text, final Icon icon) {
    super(icon);
    round = false;
    rolloverColor = null;
    pressedColor = null;
    outlineColor = null;
    setText(text);
    setFocusable(false);
  }

  private Minibutton(final String text, final boolean round, Color color,
      final Color rolloverColor, final Color pressedColor, final Color outlineColor) {
    super();
    this.round = round;
    setText(text);
    setFocusable(false);
    if (round) {
      if (outlineColor != null) {
        this.outlineColor = outlineColor;
      }
      else {
        this.outlineColor = getForeground();
      }
      if (color != null) {
        setBackground(color);
      }
      else {
        color = getBackground();
      }
      if (rolloverColor != null) {
        this.rolloverColor = rolloverColor;
      }
      else if (color != null) {
        this.rolloverColor = color.brighter();
      }
      else {
        this.rolloverColor = null;
      }
      if (pressedColor != null) {
        this.pressedColor = pressedColor;
      }
      else if (color != null) {
        this.pressedColor = color.darker();
      }
      else {
        this.pressedColor = null;
      }
      setContentAreaFilled(false);
    }
  }

  static Minibutton getSquareInstance(final String label, final Border border) {
    Minibutton instance = new Minibutton(label, null);
    instance.setBorder(border);
    instance.setSize();
    return instance;
  }

  static Minibutton getSquareInstance(final Icon icon, final Border border) {
    Minibutton instance = new Minibutton(null, icon);
    instance.setBorder(border);
    instance.setSize();
    return instance;
  }

  static Minibutton getRoundInstance(final String label, final boolean italics,
      final boolean small, final Color color, final Color rolloverColor,
      final Color pressedColor, final Color outlineColor) {
    int labelSize = 1;
    if (label != null) {
      labelSize = label.length();
    }
    Minibutton instance = new Minibutton((italics ? "<html><i>" : "")
        + (labelSize > 1 ? " " : "") + label + (labelSize > 1 ? " " : ""), true, color,
        rolloverColor, pressedColor, outlineColor);
    if (small) {
      instance.setBorder(BorderFactory.createEmptyBorder());
    }
    else {
      instance.setBorder(BorderFactory.createEtchedBorder());
    }
    instance.setSize();
    return instance;
  }

  static Minibutton getBlueInstance(final String label, final boolean italics,
      final boolean small) {
    return getRoundInstance(label, italics, small, new Color(176, 248, 255), new Color(
        203, 232, 255), new Color(134, 189, 255), new Color(15, 4, 75));
  }

  static Minibutton getGreenInstance(final String label, final boolean italics,
      final boolean small) {
    return getRoundInstance(label, italics, small, new Color(188, 254, 186), new Color(
        231, 254, 245), new Color(86, 226, 138), new Color(0, 40, 2));
  }

  void setSize() {
    Dimension size = getPreferredSize();
    if (size.width < size.height) {
      size.width = size.height;
    }
    setSize(size);
  }

  public void setSize(final Dimension size) {
    setPreferredSize(size);
    setMaximumSize(size);
  }

  protected void paintComponent(final Graphics graphics) {
    Color color = getBackground();
    if (round && color != null) {
      ButtonModel model = getModel();
      if (model.isArmed()) {
        if (pressedColor != null) {
          graphics.setColor(pressedColor);
        }
        else {
          graphics.setColor(color.darker());
        }
      }
      else if (model.isRollover()) {
        if (rolloverColor != null) {
          graphics.setColor(rolloverColor);
        }
        else {
          graphics.setColor(color.brighter());
        }
      }
      else {
        graphics.setColor(getBackground());
      }
      graphics.fillOval(0, 0, getSize().width - 1, getSize().height - 1);
    }
    super.paintComponent(graphics);
  }

  protected void paintBorder(final Graphics graphics) {
    if (round) {
      if (graphics instanceof Graphics2D) {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
      }
      if (outlineColor != null) {
        graphics.setColor(outlineColor);
      }
      else {
        graphics.setColor(getForeground());
      }
      graphics.drawOval(0, 0, getSize().width - 1, getSize().height - 1);
    }
    else {
      super.paintBorder(graphics);
    }
  }

  Shape shape;

  public boolean contains(final int x, final int y) {
    if (round) {
      if (shape == null || !shape.getBounds().equals(getBounds())) {
        shape = new Ellipse2D.Float(0, 0, getWidth(), getHeight());
      }
      return shape.contains(x, y);
    }
    else {
      return super.contains(x, y);
    }
  }
}
