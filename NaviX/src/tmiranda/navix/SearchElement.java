
package tmiranda.navix;

import java.io.File;

/**
 *
 * @author Tom Miranda.
 */
public class SearchElement extends PlaylistEntry {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "SearchIcon.png";

    @Override
    public boolean isSupportedBySage() {
        return false;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }
}
