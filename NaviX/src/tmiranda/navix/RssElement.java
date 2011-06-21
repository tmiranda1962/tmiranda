
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */
public class RssElement extends PlaylistEntry {
    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }
}
