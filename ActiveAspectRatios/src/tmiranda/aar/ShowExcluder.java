
package tmiranda.aar;

import java.util.*;

/**
 *
 * @author TomMiranda.
 */
public class ShowExcluder {
    private List<String>    excludedShows;
    private String          propertyName;

    public ShowExcluder(String PropertyName) {

        excludedShows = new ArrayList<String>();

        if (PropertyName==null || PropertyName.isEmpty())
            return;

        propertyName = PropertyName;

        String[] elements = PropertyElement.GetPropertyArray(PropertyName, null);

        if (elements==null || elements.length==0)
            return;

        for (String e : elements) {
            if (e!=null && !e.isEmpty()) {
                excludedShows.add(e);
            }
        }
    }

    public boolean isShowExcluded(String Show) {
        return excludedShows.contains(Show);
    }

    public void addExcludedShow(String Show) {
        if (excludedShows.contains(Show))
            return;

        excludedShows.add(Show);
        PropertyElement.SetPropertyElement(propertyName, null, Show);
    }

    public void removeExcludedShow(String Show) {
        if (!excludedShows.contains(Show))
            return;

        excludedShows.remove(Show);
        PropertyElement.RemovePropertyElement(propertyName, null, Show);
    }
}
