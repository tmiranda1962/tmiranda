
package tmiranda.navix;

import java.io.*;

/**
 *
 * @author Tom Miranda
 */
public class ListNoteElement extends PlaylistEntry implements Serializable {

    @Override
    public boolean isSupportedBySage() {
        return false;
    }
}
