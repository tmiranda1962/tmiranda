
package tmiranda.navix;

import java.io.*;

/**
 * Unsupported Element.
 *
 * @author Tom Miranda.
 */
public class HtmlYouTubeElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;
    
    @Override
    public boolean isSupportedBySage() {
        return false;
    }
}
