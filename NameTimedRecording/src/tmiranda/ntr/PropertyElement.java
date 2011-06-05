/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.ntr;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class PropertyElement {

    static final String ELEMENT_DELIMITER = ";";

    static void removePropertyElement(String ChannelRecurrence, String Name) {

        String element = ChannelRecurrence + API.KEY_NAME_DELIMITER + Name;

        String rawProperty = Configuration.GetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty before " + rawProperty);

        if (rawProperty!=null && !rawProperty.isEmpty()) {
            rawProperty = rawProperty.replaceAll(";"+element+";", "");
            Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, rawProperty);
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty after " + rawProperty);
    }

    static void addPropertyElement(String ChannelRecurrence, String Name) {

        String rawProperty = Configuration.GetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty before " + rawProperty);

        if (rawProperty==null || rawProperty.isEmpty())
            rawProperty = ";" + ChannelRecurrence + API.KEY_NAME_DELIMITER + Name + ";";
        else
            rawProperty = rawProperty + ";" + ChannelRecurrence + API.KEY_NAME_DELIMITER + Name + ";";

        Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, rawProperty);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty after " + rawProperty);
    }

    static Map<String, String> getNameMap() {

        Map<String, String> nameMap = new HashMap<String, String>();

        String rawProperty = Configuration.GetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
        if (rawProperty==null || rawProperty.isEmpty())
            return nameMap;

        String stripped = rawProperty.replaceAll(";;", ";");

        String[] elements = stripped.split(";");
        if (elements==null || elements.length==0)
            return nameMap;

        for (String element : elements) {

            // The first element is usually null.
            if (element!=null && !element.isEmpty()) {

                String[] parts = element.split(API.KEY_NAME_DELIMITER);

                if (parts.length==2) {
                    nameMap.put(parts[0], parts[1]);
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameMap: Found " + parts[0] + "-" + parts[1]);
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "getNameMap: Malformed element " + element);
                }
            }
        }

        return nameMap;
    }

}
