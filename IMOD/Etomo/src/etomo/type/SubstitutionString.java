package etomo.type;

/**
 * <p>Description: String containing variables.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class SubstitutionString {
  private static final String START = "%{";
  private static final int START_LEN = START.length();
  private static final String END = "}";
  private static final int END_LEN = END.length();
  private final String string;

  private String externalVariableName = null;
  private String variableName = null;
  private String variableValue = null;

  /**
   * @param string
   * @param externalVariableName - variable name without %{} - may not contain "%{" or "}"
   */
  public SubstitutionString(final String string, final String externalVariableName) {
    this.string = string;
    this.externalVariableName = externalVariableName;
  }

  /**
   * @param variableName - variable name without %{} - may not contain "%{" or "}"
   * @param value - no effect if null
   */
  public void setVariable(final String variableName, final String value) {
    this.variableName = variableName;
    variableValue = value;
  }

  /**
   * Substitutes the matching variableValue for every instance of %{variableName} in
   * string.
   * @param string
   * @param variableName - variable name without %{} - may not contain "%{" or "}"
   * @param variableValue - no effect if null
   * @return
   */
  static String substitute(final String string, final String variableName,
    final String variableValue) {
    return substitute(string, variableName, variableValue, null, -1);
  }

  /**
   * Substitutes the matching value for every instance of %{name} in string.
   * @param externalVariableValue
   * @return
   */
  public String substitute(final int externalVariableValue) {
    return substitute(string, variableName, variableValue, externalVariableName,
      externalVariableValue);
  }

  /**
   * Substitutes the matching value for every instance of %{name} in string.
   * @param string
   * @param variableName
   * @param variableValue
   * @param externalVariableName
   * @param externalVariableValue
   * @return
   */
  private static String substitute(final String string, final String variableName,
    final String variableValue, final String externalVariableName,
    final int externalVariableValue) {
    if (string == null || string.equals("") || string.indexOf(START) == -1
      || string.indexOf(END) == -1) {
      return string;
    }
    StringBuilder builder = new StringBuilder();
    int index = 0;
    while (index < string.length()) {
      // Find the end of the variable
      int end = string.indexOf(END, index);
      if (end != -1) {
        // Find the start of the variable, working back from end
        int start = string.lastIndexOf(START, end);
        if (start != -1 && start >= index) {
          builder.append(string.substring(index, start));
          String var = string.substring(start + START_LEN, end);
          String value = null;
          if (var != null && (value = getVariableValue(var, variableName, variableValue,
            externalVariableName, externalVariableValue)) != null) {
            // Substitute value of variable
            builder.append(value);
          }
          else {
            // No matching variable - leave variable in place
            builder.append(string.substring(start, end + END_LEN));
          }
          index = end + END_LEN;
        }
        else {
          // No %{'s left in string
          builder.append(string.substring(index));
          break;
        }
      }
      else {
        // No }'s left in string
        builder.append(string.substring(index));
        break;
      }
    }
    return builder.toString();
  }

  /**
   * @param testVariableName
   * @param variableName
   * @param variableValue
   * @param externalVariableName
   * @param externalVariableValue
   * @return the value matching the name corresponding to testVariableName
   */
  private static String getVariableValue(final String testVariableName,
    final String variableName, final String variableValue,
    final String externalVariableName, final int externalVariableValue) {
    if (testVariableName == null) {
      return null;
    }
    if (testVariableName.equals(variableName)) {
      return variableValue;
    }
    if (testVariableName.equals(externalVariableName)) {
      return String.valueOf(externalVariableValue);
    }
    return null;
  }
}
