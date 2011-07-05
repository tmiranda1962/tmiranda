
package tmiranda.navix;

import java.io.*;
import java.util.*;

/**
 * Video Object.
 *
 * @author Tom Miranda.
 */
public class VideoElement extends PlaylistEntry implements Serializable {

    public static final String DEFAULT_SAGE_ICON = "Themes" + File.separator + "Standard" + File.separator + "VideoArt.png";

    private Map<String, String> args = new HashMap<String, String>();

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    /**
     * Invokes the processor to get the video link and set the args that will be needed for RTMP.
     * @return
     */
    public String getVideoLink() {
//FIXME
        if (false && !hasProcessor()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "VideoElement.getVideoLink: No processor needed, returning " + url);
            return url;
        }


        Log.getInstance().write(Log.LOGLEVEL_TRACE, "VideoElement.getVideoLink: Processor will use " + processor + " for " + url);
        
        List<String> answer = invokeProcessor(url, processor);

        if (answer==null || answer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "VideoElement.getVideoLink: No translation from processor, returning original " + url);
            return url;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "VideoElement.getVideoLink: Answer " + answer);

        for (String element : answer) {
            String[] parts = element.split("=", 2);

            if (parts.length==2) {
                args.put(parts[0].toLowerCase(), parts[1]);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "VideoElement.getVideoLink: Found arg " + parts[0] + ":" + parts[1]);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "VideoElement.getVideoLink: Unexpected response from processor " + element);
            }
        }

        return args.get("answer");
    }

    public Map<String, String> getArgs() {
        return args;
    }
}
