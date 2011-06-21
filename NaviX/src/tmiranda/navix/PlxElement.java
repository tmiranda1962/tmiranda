
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */
public class PlxElement extends PlaylistEntry {

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    public String getNextPlaylist() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getNextPlaylist: " + url);
        return url;
    }
}
