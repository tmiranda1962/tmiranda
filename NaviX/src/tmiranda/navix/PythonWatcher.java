
package tmiranda.navix;

import java.io.*;
import org.python.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class PythonWatcher extends Thread {

    public static String PROPERTY_MAX_RUN_TIME = "navix/PythonWatcherMaxRunTime";
    public static String MAX_RUN_TIME_DEFAULT = "25000";

    PythonInterpreter interp = null;
    PipedOutputStream output = null;

    public PythonWatcher(PythonInterpreter Interp, PipedOutputStream Output) {
        interp = Interp;
        output = Output;
    }

    @Override
    public void run() {

        String maxTime = Configuration.GetProperty(PROPERTY_MAX_RUN_TIME, MAX_RUN_TIME_DEFAULT);

        long maxRunTime = 25000;
        
        try {
            maxRunTime = Long.parseLong(maxTime);
        } catch (NumberFormatException e) {
            maxRunTime = 25000;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PythonWatcher: Max run time is " + maxRunTime);

        try {
            Thread.sleep(maxRunTime);
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PythonWatcher: Python script has hung, trying to stop it.");

            PrintStream ps = new PrintStream(output);
            ps.println("PythonWatcher forcing close.");
            ps.println("script done - forced close from PythonWatcher");
            try {output.flush();} catch (IOException e1) {}
            
            //PySystemState.exit();
        } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PythonWatcher: Thread completed.");
        }
    }
}
