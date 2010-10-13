/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

/**
 *
 * @author Default
 */

import java.util.*;
import java.io.*;

class StreamGetter extends Thread {

    InputStream is;
    String type;

    StreamGetter(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

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
                if (LastLine!=null && line.equalsIgnoreCase(LastLine)) {
                    DuplicateLines++;
                } else {
                    LastLine = line;
                    if (type.equalsIgnoreCase("error")) {
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "stderr: " + line);
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "stdout: " + line);
                    }
                }
            }
        } catch (IOException ioe) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Error-Exception " + type + ":" + ioe.getMessage());
            ioe.printStackTrace();
        } finally {
            try {br.close(); isr.close(); } catch (IOException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "StreamGetter: Exception closing " + e.getMessage());
            }
            if (DuplicateLines != 0)
                Log.getInstance().write(Log.LOGLEVEL_WARN, "StreamGetter: Duplicate lines = " + DuplicateLines + ":" + LastLine);
        }
    }
}