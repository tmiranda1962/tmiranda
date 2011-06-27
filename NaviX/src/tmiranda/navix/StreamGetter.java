
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */

import java.io.*;

class StreamGetter extends Thread {

    InputStream is;

    StreamGetter(InputStream is) {
        this.is = is;
    }

    @Override
    public void run() {

        System.out.println("PYTHON: Getter started.");

        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
                System.out.println("StreamGetter: read: " + line);
            }
        } catch (IOException ioe) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Error-Exception " + ioe.getMessage());
        } finally {
            try {br.close(); isr.close(); } catch (IOException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Exception closing " + e.getMessage());
            }
        }

        System.out.println("PYTHON: Getter ended.");
    }
}
