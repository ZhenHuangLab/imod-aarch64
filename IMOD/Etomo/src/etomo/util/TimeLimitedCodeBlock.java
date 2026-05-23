package etomo.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
* <p>Description: Attempts to open a file reader until it is told to stop or gets an
* exception other then FileNotFoundException.</p>
* 
* <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class TimeLimitedCodeBlock {
  public static void runWithTimeout(final Runnable runnable, final long timeout,
    final TimeUnit timeUnit) throws Exception {
    // Wrap the Runnable object in Callable.
    runWithTimeout(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        runnable.run();
        return null;
      }
    }, timeout, timeUnit);
  }

  public static <T> T runWithTimeout(final Callable<T> callable, final long timeout,
    final TimeUnit timeUnit) throws Exception {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Future<T> future = executor.submit(callable);
    executor.shutdown(); // This does not cancel the already-scheduled task.
    try {
      return future.get(timeout, timeUnit);
    }
    catch (TimeoutException e) {
      // remove this if you do not want to cancel the job in progress
      // or set the argument to 'false' if you do not want to interrupt the thread
      future.cancel(false);
      throw e;
    }
    catch (ExecutionException e) {
      // unwrap the root cause
      Throwable t = e.getCause();
      if (t instanceof Error) {
        throw (Error) t;
      }
      else if (t instanceof Exception) {
        throw (Exception) e;
      }
      else {
        throw new IllegalStateException(t);
      }
    }
  }
}
