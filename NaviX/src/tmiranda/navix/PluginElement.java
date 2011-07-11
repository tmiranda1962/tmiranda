
package tmiranda.navix;

import java.io.*;

/**
 * Unsupported.
 *
 * @author Tom Miranda.
 */
public class PluginElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    @Override
    public boolean isSupportedBySage() {
        return false;
    }
}
