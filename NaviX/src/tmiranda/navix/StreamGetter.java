
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */


import java.io.*;

class StreamGetter extends Thread {

    InputStream is;
    String type;

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
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "StreamGetter: stderr: " + line);
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
