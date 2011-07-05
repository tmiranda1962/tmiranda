
package tmiranda.navix;

import java.io.*;

/**
 * Atom feeds.  Currently not supported.
 *
 * @author Tom Miranda.
 */
public class AtomElement extends PlaylistEntry implements Serializable {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "MarkerDelete.png";

    @Override
    public boolean isSupportedBySage() {
        return false;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }
}
