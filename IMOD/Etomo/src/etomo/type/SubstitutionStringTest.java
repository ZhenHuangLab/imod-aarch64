package etomo.type;

import junit.framework.TestCase;

/**
 * <p>Description: Test for SubstitutionString.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class SubstitutionStringTest extends TestCase {
  /**
   * All instances of the single variable should be replaced with the parameter
   */
  public void testSubstitute() {
    SubstitutionString substitutionString =
      new SubstitutionString("%{var}123%{var}abc%{var}xyz", "var");
    assertEquals("variable at start", "4212342abc42xyz",
      substitutionString.substitute(42));

    substitutionString =
      new SubstitutionString("%{var}123%{abc}abc%{abc}x%{def}yz", "var");
    substitutionString.setVariable("abc", "X");
    assertEquals("set variable is substituted", "42123XabcXx%{def}yz",
      substitutionString.substitute(42));
    substitutionString.setVariable("def", "Y");
    assertEquals(
      "set variables is substituted, different values can be substituted for the "
        + "external variable",
      "2525123%{abc}abc%{abc}xYyz", substitutionString.substitute(2525));

    assertEquals("static one variable substitution", "X123Xabc%{abc}xXyz",
      SubstitutionString.substitute("%{var}123%{var}abc%{abc}x%{var}yz", "var", "X"));

    substitutionString = new SubstitutionString("123%{var}abc%{var}xyz%{var}", "var");
    assertEquals("variable at end", "12342abc42xyz42", substitutionString.substitute(42));

    String noSubstitutions = "123%{varx}abc%{varx}xyz%{varx}";
    substitutionString = new SubstitutionString(noSubstitutions, "var");
    assertEquals("no variable matches", noSubstitutions,
      substitutionString.substitute(42));

    substitutionString = new SubstitutionString("%{var}", "var");
    assertEquals("nothing but matching variable", "42",
      substitutionString.substitute(42));

    substitutionString = new SubstitutionString("%{var}123%{varx}abc%{var}xyz", "var");
    assertEquals("not all variables match", "42123%{varx}abc42xyz",
      substitutionString.substitute(42));

    String noVariables = "123abcxyz";
    substitutionString = new SubstitutionString(noSubstitutions, "var");
    assertEquals("no variables", noSubstitutions, substitutionString.substitute(42));

    substitutionString = new SubstitutionString("%{var}123%{%{var}abc%{var}xyz", "var");
    assertEquals("when %{ is not part of the variable don't skip the following variable",
      "42123%{42abc42xyz", substitutionString.substitute(42));

    String badVariable = "%{%{var}123%{%{var}abc%{%{var}xyz";
    substitutionString = new SubstitutionString(badVariable, "%{var");
    assertEquals("%{ in vaiable name makes variable unmatchable", badVariable,
      substitutionString.substitute(42));

    badVariable = "%{va}r}123%{va}r}abc%{va}r}xyz";
    substitutionString = new SubstitutionString(badVariable, "va}r");
    assertEquals("} in a variable make the variable unmatchable", badVariable,
      substitutionString.substitute(42));

    substitutionString = new SubstitutionString("%{}123%{}abc%{}xyz", "");
    assertEquals("blank variable names work", "4212342abc42xyz",
      substitutionString.substitute(42));

    String noVariable = "%{}123%{}abc%{}xyz";
    substitutionString = new SubstitutionString(noVariable, null);
    assertEquals("null variable name means there is no variable", noVariable,
      substitutionString.substitute(42));
  }
}
