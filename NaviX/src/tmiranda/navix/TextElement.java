
package tmiranda.navix;

import java.io.*;
import java.util.*;

/**
 * A TextElement points to a text file stored on the web.
 *
 * @author Tom Miranda.
 */
public class TextElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

// FIXME - The dialog used to display text is a mess.

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "NTE_Upper.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public List<String> getText() {

        List<String> fileContents = new ArrayList<String>();

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getText: null url.");
            return fileContents;
        }

        // Create the file reader.
        BufferedReader br = Playlist.read(url);

        if (br==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getText: Failed to open URL.");
            return fileContents;
        }

        String line = null;

        try {
            while ((line=br.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getText: line = " + line);
                if (!line.isEmpty())
                    fileContents.add(line);
            }
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getText: IO Exception " + e.getMessage());
        }

        return fileContents;
    }
}
