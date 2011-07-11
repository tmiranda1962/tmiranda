
package tmiranda.navix;

import java.io.*;

/**
 * Unsupported.
 *
 * @author Tom Miranda.
 */
public class RssRssElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public String getLink() {
        return url;
    }
}
