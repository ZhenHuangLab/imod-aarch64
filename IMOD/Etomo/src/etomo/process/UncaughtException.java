package etomo.process;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006</p>
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
 final class UncaughtException {
  public static final String rcsid = "$Id$";

   static final UncaughtException INSTANCE = new UncaughtException();

  private Throwable throwable = null;

  private UncaughtException() {
  }

   void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }

   String getThrowableString() {
    String message = null;
    synchronized (this) {
      if (throwable != null) {
        message = throwable.getMessage();
        throwable = null;
      }
    }
    return message;
  }
}
