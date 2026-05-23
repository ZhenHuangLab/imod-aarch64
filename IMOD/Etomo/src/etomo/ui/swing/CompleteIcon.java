package etomo.ui.swing;

import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

final class CompleteIcon {
  private final ImageIcon icon;
  private final ImageIcon selectedIcon;
  private final ImageIcon pressedIcon;

  CompleteIcon(final String imageFile, final String selectedImageFile,
    final String pressedImageFile) {
    icon = createIcon(imageFile);
    selectedIcon = createIcon(selectedImageFile);
    pressedIcon = createIcon(pressedImageFile);
  }

  public static ImageIcon createIcon(final String imageFile) {
    URL url = null;
    if (imageFile != null
      && (url = ClassLoader.getSystemResource("images/" + imageFile)) != null) {
      return new ImageIcon(url);
    }
    return null;
  }

  public String toString() {
    return icon.toString();
  }

  final void setup(final AbstractButton button) {
    if (button == null) {
      return;
    }
    if (icon != null) {
      button.setIcon(icon);
    }
    if (selectedIcon != null) {
      button.setSelectedIcon(selectedIcon);
    }
    if (pressedIcon != null) {
      button.setPressedIcon(pressedIcon);
    }
  }

  /**
  * Use to cause a button with no toggle functionality to appear to have toggle
  * functionality.
  * @param button
  * @param selected
  */
  final void setSelectedAppearance(final AbstractButton button, final boolean selected) {
    if (button == null) {
      return;
    }
    if (icon != null && selectedIcon != null) {
      button.setIcon(selected ? selectedIcon : icon);
    }
  }
}