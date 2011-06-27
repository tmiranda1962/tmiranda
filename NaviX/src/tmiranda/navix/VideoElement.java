
package tmiranda.navix;

import java.io.File;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class VideoElement extends PlaylistEntry {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "VideoArt.png";

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public String getVideoLink() {
//FIXME
        if (!hasProcessor()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: No processor needed, returning " + url);
            return url;
        }


        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Processor needed, will use " + processor);
        
        List<String> answer = invokeProcessor(url, processor);

        if (answer==null || answer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getVideoLink: No response from processor.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Answer " + answer);

        if (answer.size() > 1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Concatenating.");

            String a = null;

            for (String s : answer)
                a = (a==null ? s : a + s);

            return a;
        }

        return answer.get(0).replace(PlaylistEntry.SCRIPT_ANSWER, "");
    }
}
