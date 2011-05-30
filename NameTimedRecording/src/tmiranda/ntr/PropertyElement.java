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

    static void removePropertyElement(String ChannelRecurrence, String Name) {

        String element = ChannelRecurrence + API.DELIMITER + Name;

        String rawProperty = Configuration.GetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
        System.out.println("NTR: removePropertyElement: rawProperty before " + rawProperty);

        if (rawProperty!=null && !rawProperty.isEmpty()) {
            rawProperty = rawProperty.replaceAll(";"+element+";", "");
            Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, rawProperty);
        }

        System.out.println("NTR: removePropertyElement: rawProperty after " + rawProperty);
    }

    static void addPropertyElement(String ChannelRecurrence, String Name) {

        String rawProperty = Configuration.GetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
        System.out.println("NTR: addPropertyElement: rawProperty before " + rawProperty);

        if (rawProperty==null || rawProperty.isEmpty())
            rawProperty = ";" + ChannelRecurrence + API.DELIMITER + Name + ";";
        else
            rawProperty = rawProperty + ";" + ChannelRecurrence + API.DELIMITER + Name + ";";

        Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, rawProperty);
        System.out.println("NTR: addPropertyElement: rawProperty after " + rawProperty);
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

            String[] parts = element.split(API.DELIMITER);

            if (parts.length==2) {
                nameMap.put(parts[0], parts[1]);
            } else {
                System.out.println("NTR: getNameMap: Malformed element " + element);
            }
        }

        return nameMap;
    }

}
