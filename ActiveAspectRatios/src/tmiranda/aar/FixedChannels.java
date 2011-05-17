
package tmiranda.aar;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class FixedChannels {

    private String PAIR_DELIMITER = "-";

    private String              propertyName;
    private String              rawProperty;
    private Map<String, String> channelMap;

    public FixedChannels(String PropertyName) {
        
        propertyName = PropertyName;
        rawProperty = null;
        channelMap = new HashMap<String, String>();

        if (PropertyName==null || PropertyName.isEmpty())
            return;

        rawProperty = Configuration.GetProperty(PropertyName, null);

        if (rawProperty==null || rawProperty.isEmpty())
            return;

        String elements[] = rawProperty.split(";*;");

        if (elements==null || elements.length==0)
            return;

        for (String element : elements) {
            String channelMode[] = element.split(PAIR_DELIMITER);

            if (channelMode==null || channelMode.length!=2) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "FixedChannels: Invalid channel-mode entry " + element);
            } else {
                channelMap.put(channelMode[0], channelMode[1]);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "FixedChannels: Found " + channelMode[0] + ":" + channelMode[1]);
            }
        }       
    }

    public boolean isFixed(String Channel) {
        if (Channel==null || Channel.isEmpty())
            return false;

        return channelMap.get(Channel) != null ? true : false;
    }

    public String getMode(String Channel) {
        if (Channel==null || Channel.isEmpty())
            return null;
        else
            return channelMap.get(Channel);
    }

    public void add(String Channel, String Mode) {

        if (Channel==null || Channel.isEmpty() || Mode==null || Mode.isEmpty()) {
            return;
        }

        if (channelMap.get(Channel)!= null) {
            remove(Channel);
        }

        channelMap.put(Channel, Mode);
        addPropertyElement(Channel, Mode);
    }

    public void remove(String Channel) {
        if (channelMap.get(Channel)!= null) {
            String mode = channelMap.remove(Channel);
            removePropertyElement(Channel, mode);
        }
    }

    private void removePropertyElement(String Channel, String Mode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty before " + rawProperty);
        String element = Channel + PAIR_DELIMITER + Mode;
        rawProperty.replaceAll(";"+element+";", "");
        Configuration.SetProperty(propertyName, rawProperty);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty after " + rawProperty);
    }

    private void addPropertyElement(String Channel, String Mode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty before " + rawProperty);
        if (rawProperty==null || rawProperty.isEmpty())
            rawProperty = ";" + Channel + PAIR_DELIMITER + Mode + ";";
        else
            rawProperty = rawProperty + ";" + Channel + PAIR_DELIMITER + Mode + ";";
        Configuration.SetProperty(propertyName, rawProperty);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty after " + rawProperty);
    }
}
