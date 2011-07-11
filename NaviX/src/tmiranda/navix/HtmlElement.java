
package tmiranda.navix;

import java.io.*;

/**
 * HTML element.  Currently not supported.
 *
 * @author Tom Miranda.
 */
public class HtmlElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    @Override
    public boolean isSupportedBySage() {
        return false;
    }
}
