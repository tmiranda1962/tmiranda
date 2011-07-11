
package tmiranda.navix;

import java.io.*;

/**
 * Unsuppoted.  This is a NaviX Search element which is not the same as what's implemented
 * by the Search class.
 *
 * @author Tom Miranda.
 */
public class SearchElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

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
