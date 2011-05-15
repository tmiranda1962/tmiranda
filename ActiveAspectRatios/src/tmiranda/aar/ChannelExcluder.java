
package tmiranda.aar;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class ChannelExcluder {

    private List<String>    excludedChannels;
    private String          propertyName;

    public ChannelExcluder(String PropertyName) {

        excludedChannels = new ArrayList<String>();

        if (PropertyName==null || PropertyName.isEmpty())
            return;

        propertyName = PropertyName;

        String[] elements = PropertyElement.GetPropertyArray(PropertyName, null);

        if (elements==null || elements.length==0)
            return;
        
        for (String e : elements) {
            if (e!=null && !e.isEmpty()) {
                excludedChannels.add(e);
            }
        }
    }

    public boolean isChannelExcluded(String Channel) {
        return excludedChannels.contains(Channel);
    }

    public void addExcludedChannel(String Channel) {
        if (excludedChannels.contains(Channel))
            return;

        excludedChannels.add(Channel);
        PropertyElement.SetPropertyElement(propertyName, null, Channel);
    }

    public void removeExcludedChannel(String Channel) {
        if (!excludedChannels.contains(Channel))
            return;

        excludedChannels.remove(Channel);
        PropertyElement.RemovePropertyElement(propertyName, null, Channel);
    }
}
