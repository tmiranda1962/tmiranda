
package tmiranda.navix;

import java.io.*;

/**
 * PlaylistElements simply point to another Plpaylist.
 *
 * @author Tom Miranda.
 */
public class PlaylistElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

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
