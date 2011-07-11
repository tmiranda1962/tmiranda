
package tmiranda.navix;

import java.io.*;

/**
 * A PlxElement and a PlaylistElement are the same.
 *
 * @author Tom Miranda.
 */
public class PlxElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    public String getNextPlaylist() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getNextPlaylist: " + url);
        return url;
    }
}
