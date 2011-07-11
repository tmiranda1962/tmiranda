
package tmiranda.navix;

import java.io.*;
import java.util.*;

/**
 * Unsupported.
 *
 * @author Tom Miranda.
 */
public class RssVideoElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    private Map<String, String> args = new HashMap<String, String>();
    
    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public String getLink() {

        if (!hasProcessor())
            return url;

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssVideoElement.getLink: Processor will use " + processor + " for " + url);

        List<String> answer = invokeProcessor(url, processor);

        if (answer==null || answer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssVideoElement.getLink: No translation from processor, returning original " + url);
            return url;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssVideoElement.getLink: Answer " + answer);

        for (String element : answer) {
            String[] parts = element.split("=", 2);

            if (parts.length==2) {
                args.put(parts[0].toLowerCase(), parts[1]);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssVideoElement.getLink: Found arg " + parts[0] + ":" + parts[1]);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "RssVideoElement.getLink: Unexpected response from processor " + element);
            }
        }

        return args.get("answer");
    }
}
