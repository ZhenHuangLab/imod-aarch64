package etomo.type;

import java.util.ArrayList;

import etomo.EtomoDirector;
import etomo.comscript.SetupCombine;

/**
 * <p>Description: Combine patch sizes.  When necessary, it loads patch sizes from
 * setupcombine output.  The CUSTOM instance does not contain a patch size.</p>
 *
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.1  2006/05/12 00:08:45  sueh
 * <p> bug# 857 Placed the setupcombine options in getOption().
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.1.2.1  2003/01/24 18:37:54  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */

public final class CombinePatchSize implements EnumeratedType {
  // Fixed patch sizes
  public static final CombinePatchSize SMALL = new CombinePatchSize("Small", "S");
  public static final CombinePatchSize MEDIUM = new CombinePatchSize("Medium", "M");
  public static final CombinePatchSize LARGE = new CombinePatchSize("Large", "L");
  public static final CombinePatchSize EXTRA_LARGE = new CombinePatchSize("Extra large",
    "E");
  // Custom patch
  public static final CombinePatchSize CUSTOM = new CombinePatchSize("Custom");

  private static final int VALUE_INDEX = 0;
  private static final int XYZ_INDEX = 1;
  private static final int EMPTY_ELEMENT = -1;
  public static final int X_INDEX = 0;
  public static final int Y_INDEX = 1;
  public static final int Z_INDEX = 2;

  private static ArrayList<CombinePatchSize> PATCH_SIZE_ARRAY = null;

  private final String label;
  private final String value;
  private final int[] xyz = new int[] { EMPTY_ELEMENT, EMPTY_ELEMENT, EMPTY_ELEMENT };

  private CombinePatchSize(final String label, final String value) {
    this.label = label;
    this.value = value;
  }

  private CombinePatchSize(final String label) {
    this.label = label;
    value = label;
  }

  /**
   * Returns null if input is empty.  Returns the instance described by input, if input is
   * a character string.  Returns the instance that matches input, if input is a standard
   * xyz patch size.  Otherwise returns the CUSTOM instance.
   * @param input
   * @see setupcombine
   * @return
   */
  public static CombinePatchSize getInstance(String input) {
    if (input == null) {
      return null;
    }
    input = input.trim();
    if (input.equals("")) {
      return null;
    }
    if (input.equalsIgnoreCase(SMALL.value) || input.equalsIgnoreCase(SMALL.label)) {
      return SMALL;
    }
    if (input.equalsIgnoreCase(MEDIUM.value) || input.equalsIgnoreCase(MEDIUM.label)) {
      return MEDIUM;
    }
    if (input.equalsIgnoreCase(LARGE.value) || input.equalsIgnoreCase(LARGE.label)) {
      return LARGE;
    }
    if (input.equalsIgnoreCase(EXTRA_LARGE.value)
      || input.equalsIgnoreCase(EXTRA_LARGE.label)) {
      return EXTRA_LARGE;
    }
    if (input.equalsIgnoreCase(CUSTOM.value) || input.equalsIgnoreCase(CUSTOM.label)
      || input.indexOf(",") == -1) {
      return CUSTOM;
    }
    // Should be x,y,z. See if it matches one of the fixed instances.
    String[] xyzArray = input.split(",");
    if (xyzArray == null) {
      return CUSTOM;
    }
    loadXYZ();
    if (PATCH_SIZE_ARRAY != null) {
      int len = PATCH_SIZE_ARRAY.size();
      for (int i = 0; i < len; i++) {
        CombinePatchSize instance = PATCH_SIZE_ARRAY.get(i);
        if (instance != null && instance.equals(xyzArray)) {
          return instance;
        }
      }
    }
    return CUSTOM;
  }

  public static CombinePatchSize getInstance(final String[] xyz) {
    if (xyz == null) {
      return null;
    }
    // See if xyz match one of the fixed instances.
    loadXYZ();
    if (PATCH_SIZE_ARRAY != null) {
      int len = PATCH_SIZE_ARRAY.size();
      for (int i = 0; i < len; i++) {
        CombinePatchSize instance = PATCH_SIZE_ARRAY.get(i);
        if (instance != null && instance.equals(xyz)) {
          return instance;
        }
      }
    }
    return CUSTOM;
  }

  public static CombinePatchSize getInstance(final int[] xyz) {
    if (xyz == null) {
      return null;
    }
    // See if xyz match one of the fixed instances.
    loadXYZ();
    if (PATCH_SIZE_ARRAY != null) {
      int len = PATCH_SIZE_ARRAY.size();
      for (int i = 0; i < len; i++) {
        CombinePatchSize instance = PATCH_SIZE_ARRAY.get(i);
        if (instance != null && instance.equals(xyz)) {
          return instance;
        }
      }
    }
    return CUSTOM;
  }

  /**
   * Does not return the CUSTOM instance.  Returns the instance described by input.
   * Otherwise returns null.
   * @param input - character string
   * @see setupcombine
   * @return
   */
  private static CombinePatchSize getFixedInstance(final String input) {
    if (input == null) {
      return null;
    }
    if (input.equalsIgnoreCase(SMALL.value) || input.equalsIgnoreCase(SMALL.label)) {
      return SMALL;
    }
    if (input.equalsIgnoreCase(MEDIUM.value) || input.equalsIgnoreCase(MEDIUM.label)) {
      return MEDIUM;
    }
    if (input.equalsIgnoreCase(LARGE.value) || input.equalsIgnoreCase(LARGE.label)) {
      return LARGE;
    }
    if (input.equalsIgnoreCase(EXTRA_LARGE.value)
      || input.equalsIgnoreCase(EXTRA_LARGE.label)) {
      return EXTRA_LARGE;
    }
    return null;
  }

  /**
   * Returns true if xyzArray equals xyz.  Empty elements are equal.
   * @param xyzArray
   * @return
   */
  public boolean equals(final String[] xyzArray) {
    loadXYZ();
    for (int i = 0; i < xyz.length; i++) {
      // Empty elements are equal
      if ((xyzArray == null || xyzArray.length <= i) || xyzArray[i] == null) {
        if (xyz[i] != EMPTY_ELEMENT) {
          return false;
        }
      }
      else {
        try {
          if ((xyz[i] != EMPTY_ELEMENT || !xyzArray[i].matches("\\s*"))
            && xyz[i] != Integer.valueOf(xyzArray[i])) {
            return false;
          }
        }
        catch (NumberFormatException e) {
          e.printStackTrace();
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if xyzArray equals xyz.  Empty elements are equal.
   * @param xyzArray
   * @return
   */
  public boolean equals(final int[] xyzArray) {
    loadXYZ();
    for (int i = 0; i < xyz.length; i++) {
      // Empty elements are equal
      if ((xyzArray == null || xyzArray.length <= i)) {
        if (xyz[i] == EMPTY_ELEMENT) {
          return false;
        }
      }
      else {
        try {
          if (xyz[i] != Integer.valueOf(xyzArray[i])) {
            return false;
          }
        }
        catch (NumberFormatException e) {
          e.printStackTrace();
          return false;
        }
      }
    }
    return true;
  }

  public int getXYZLen() {
    return xyz.length;
  }

  public int getXYZ(final int index) {
    loadXYZ();
    return xyz[index];
  }

  /**
   * Creates and loads PATCH_SIZE_ARRAY, and sets xyz for each fixed instance.  For
   * missing xyz elements, preserves -1.
   */
  private static void loadXYZ() {
    // Load PATCH_SIZE_ARRAY once.
    if (PATCH_SIZE_ARRAY != null) {
      return;
    }
    if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
      System.err.println("waiting for sync");
    }
    synchronized (SMALL) {
      if (PATCH_SIZE_ARRAY != null) {
        if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
          System.err.println("PATCH_SIZE_ARRAY was created");
        }
        return;
      }
      // Run setupcombine -info
      String[] output = SetupCombine.getInfoOnPatchSizes();
      if (output == null) {
        if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
          System.err.println("output is null");
        }
        return;
      }
      // Get the instances and the patch sizes and place them in PATCH_SIZE_ARRAY
      PATCH_SIZE_ARRAY = new ArrayList<CombinePatchSize>();
      if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
        System.err.println("output.length:" + output.length);
      }
      for (int i = 0; i < output.length; i++) {
        if (output[i] == null) {
          if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
            System.err.println("output[" + i + "] is null");
          }
          continue;
        }
        // Example of output:
        // S: 64 64 32
        String[] array = output[i].split("\\s*:\\s*");
        if (array == null || array.length <= VALUE_INDEX) {
          if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
            System.err.println("unused output[" + i + "]:" + output[i]);
          }
          continue;
        }
        CombinePatchSize combinePatchSize = getFixedInstance(array[VALUE_INDEX]);
        if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
          System.err.println("combinePatchSize:" + combinePatchSize);
        }
        if (combinePatchSize == null || array.length <= XYZ_INDEX) {
          if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
            System.err.println("unused output[" + i + "]:" + output[i]);
          }
          continue;
        }
        // set the xyz member variable
        String[] xyzArray = array[XYZ_INDEX].split("\\s+");
        if (xyzArray == null) {
          if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
            System.err.println("array[" + XYZ_INDEX + "] is null.");
          }
          continue;
        }
        PATCH_SIZE_ARRAY.add(combinePatchSize);
        int len = Math.min(xyzArray.length, SMALL.xyz.length);
        for (int j = 0; j < len; j++) {
          // Preserve -1 for missing xyz values
          if (xyzArray[j] != null) {
            try {
              combinePatchSize.xyz[j] = Integer.valueOf(xyzArray[j]);
            }
            catch (NumberFormatException e) {
              if (EtomoDirector.INSTANCE.getArguments().isDebug()) {
                System.err.println("bad xyzArray[" + j + "]:" + xyzArray[j]);
              }
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  public String getOption() {
    return value;
  }

  public boolean isDefault() {
    return this == MEDIUM;
  }

  public ConstEtomoNumber getValue() {
    return null;
  }

  public String toString() {
    return getOption();
  }

  public String getLabel() {
    return label;
  }
}
