package etomo.type;

final class DirectoryType extends FileType {
  static final DirectoryType NAD_SUBDIR = DirectoryType.constructInstance(
    new Object[] { "naddir", ExtensionMarker.GENERIC, Variable.DATASET });

  private DirectoryType(final Object[] pattern) {
    super(null, pattern, null, null, null, null);
  }

  private static DirectoryType constructInstance(final Object[] pattern) {
    return new DirectoryType(pattern);
  }
}
