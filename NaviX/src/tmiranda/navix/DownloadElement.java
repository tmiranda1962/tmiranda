
package tmiranda.navix;

import java.io.*;

/**
 * Download Object.  Currently not supported.
 * 
 * @author Tom Miranda.
 */
public class DownloadElement extends PlaylistEntry implements Serializable {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "SortDown.png";
    @Override
    public boolean isSupportedBySage() {
        return false;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }
}
