
package tmiranda.navix;

import java.util.regex.Pattern;

/**
 * Utility methods.
 *
 * @author Tom Miranda.
 */
public class NaviX {

    public static final String VERSION = "0.01";

    /**
     * Get the version number of the JAR.
     *
     * @return
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     *
     * Some text has the embedded XML [COLOR=....] [/COLOR].  This method removes it.
     *
     * @param str
     * @return
     */
    public static String stripCOLOR(String str) {
        if (str==null)
            return str;
// FIXME

        str = str.replace("[/COLOR]", "");

        if (str != null) {
            str = str.replaceAll(Pattern.quote("[COLOR=") + "[a-fA-F0-9]{8}" + Pattern.quote("]"), "");
        }

        return str;
    }
}
