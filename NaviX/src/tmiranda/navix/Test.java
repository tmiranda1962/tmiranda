
package tmiranda.navix;

import java.io.*;
import java.util.*;
import org.python.util.*;

public class Test {

    public static void helloWorld() throws Exception {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "BEGIN PYTHON TEST");

        // Create the pipe that will be used to connect the python script to this method.
        PipedOutputStream output = new PipedOutputStream();
        InputStream input = new PipedInputStream(output);

        // Create the pipe reader.
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(isr);

        //Runtime.getRuntime().
        //InputStream is = new Process.getInputStream();
        //OutputStream os = new OutputStream();
        //OutputStream os = null;
        //BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        //StreamGetter getter = new StreamGetter(input);
        //getter.start();

        // Create the properties that will be needed to setup the python path.
        Properties prop = new Properties();
        prop.setProperty("python.path",".;.\\NaviX\\scripts;.\\NaviX\\python\\jython2.5.2\\lib");

        // This is the data we will pass to the python script.
        String[] argv = {"HelloWorld.py", "p1"};

        // Initialize the python runtime environment.
        PythonInterpreter.initialize(System.getProperties(), prop, argv);

        // Start the python script.
        PythonInterpreter interp = new PythonInterpreter();
        interp.setOut(output);
        interp.setErr(output);
        interp.execfile(".\\NaviX\\scripts\\HelloWorld.py");

        // Read the results.
        String line = null;
        boolean done = false;

        while (!done && (line = br.readLine()) != null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "READ LINE=" + line);
            if (line.equalsIgnoreCase("script done")) {
                done = true;
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "END OF SCRIPT REACHED" + line);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RESULT=" + line);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "END PYTHON TEST");
    }
}
