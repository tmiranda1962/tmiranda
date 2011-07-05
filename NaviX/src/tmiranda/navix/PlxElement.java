
package tmiranda.navix;

import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class PlxElement extends PlaylistEntry implements Serializable {

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    public String getNextPlaylist() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getNextPlaylist: " + url);
        return url;
    }
}
