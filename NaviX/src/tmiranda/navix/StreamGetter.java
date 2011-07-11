
package tmiranda.navix;

/**
 * Class used to consume the stdout and stderr streams from spawned processes.
 *
 * @author Tom Miranda.
 */
import java.io.*;

class StreamGetter extends Thread {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    private InputStream is;
    private String type;

    /**
     * Constructor.
     *
     * @param is The InputStream to consume. If the loglevel is set to Trace (or higher) all
     *  output collected from the stream will be written to the Sage logfile. Output collected
     * from stderr will be written to the Sage logfile if the loglevel is set to Warn (or higher).
     * @param type "Error" for stderr, anything else for stdout.
     */
    StreamGetter(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    @Override
    public void run() {

        InputStreamReader isr = null;
        BufferedReader br = null;

        String LastLine = null;
        int DuplicateLines = 0;

        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
                if (false && LastLine!=null && line.equalsIgnoreCase(LastLine)) {
                    DuplicateLines++;
                } else {
                    LastLine = line;
                    if (type.equalsIgnoreCase("error")) {
                        Log.getInstance().write(Log.LOGLEVEL_WARN, "StreamGetter: stderr: " + line);
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "StreamGetter: stdout: " + line);
                    }
                }
            }
        } catch (IOException ioe) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Error-Exception " + type + ":" + ioe.getMessage());
        } finally {
            try {br.close(); isr.close(); } catch (IOException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Exception closing " + e.getMessage());
            }
            if (DuplicateLines != 0)
                Log.getInstance().write(Log.LOGLEVEL_WARN, "StreamGetter: Duplicate lines = " + DuplicateLines + ":" + LastLine);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "StreamGetter: Ending.");
    }
}
