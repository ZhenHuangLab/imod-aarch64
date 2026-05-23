package etomo.uitest.tests;

import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestActionType;
import etomo.type.UITestSubjectType;
import etomo.uitest.AutodocTester;
import etomo.uitest.UITestModifierType;

public final class ExternalUITestSuitFactory {
  public static ExternalUITestSuite getUITestSuiteInstance(final String classTag,
    final AutodocTester autodocTester) {
    if (classTag == null) {
      return null;
    }
    if (ClassTag.GPU.classTag.equals(classTag)) {
      return new GpuUITestSuite(autodocTester);
    }
    if (ClassTag.GENERIC.classTag.equals(classTag)) {
      return new GenericUITestSuite(autodocTester);
    }
    return null;
  }

  public static void runFunction(final ExternalUITestSuite externalUITestSuite,
    final String functionName) {
    if (functionName == null) {
      return;
    }
    if (FunctionName.GO.functionName.equals(functionName)) {
      externalUITestSuite.go();
    }
  }

  private static final class ClassTag {
    private static final ClassTag GPU = new ClassTag("gpu");
    private static final ClassTag GENERIC = new ClassTag("generic");

    private final String classTag;

    private ClassTag(final String classTag) {
      this.classTag = classTag;
      if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
        System.out
          .println(UITestActionType.GET.toString() + AutodocTokenizer.SEPARATOR_CHAR
            + UITestSubjectType.INSTANCE + AutodocTokenizer.SEPARATOR_CHAR + classTag
            + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      }
    }
  }

  private static final class FunctionName {
    private static final FunctionName GO = new FunctionName("go");

    private final String functionName;

    private FunctionName(final String functionName) {
      this.functionName = functionName;
      if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
        System.out
          .println(UITestActionType.RUN.toString() + AutodocTokenizer.SEPARATOR_CHAR
            + AutodocTokenizer.SEPARATOR_CHAR + UITestModifierType.EXTERNAL.toString()
            + UITestSubjectType.FUNCTION + AutodocTokenizer.SEPARATOR_CHAR + functionName
            + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      }
    }
  }
}
