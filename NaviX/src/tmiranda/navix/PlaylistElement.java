
package tmiranda.navix;

import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class PlaylistElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = 0;

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "FileFolder.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public String getNextPlaylist() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getNextPlaylist: " + url);
        return url;
    }
}
