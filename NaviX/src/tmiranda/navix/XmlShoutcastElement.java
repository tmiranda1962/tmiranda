
package tmiranda.navix;

import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class XmlShoutcastElement extends PlaylistEntry implements Serializable {
    
    @Override
    public boolean isSupportedBySage() {
        return false;
    }
}
