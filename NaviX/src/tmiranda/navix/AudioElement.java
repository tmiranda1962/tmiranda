
package tmiranda.navix;

import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class AudioElement extends PlaylistEntry implements Serializable {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "MusicArt.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public String getAudioLink() {
        return url;
    }
}
