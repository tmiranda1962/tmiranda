

package tmiranda.rpad;

import java.util.*;

/**
 *
 * @author Tom Miranda
 */
public class PluginList extends ArrayList {

    private List<Object> TheList = null;

    public PluginList() {
        TheList = new ArrayList<Object>();
        return;
    }

    public PluginList(Object Plugin) {
        TheList = new ArrayList<Object>();
        TheList.add(Plugin);
        return;
    }

    public PluginList(List<Object> NewList) {
        TheList = new ArrayList<Object>();
        TheList.addAll(NewList);
        return;
    }

    public boolean contains(Object Plugin) {

        if (TheList == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PluginList.contains: null List.");
            return false;
        }

        for (Object ThisPlugin : TheList) {
            if (PluginAndDependencies.PluginsAreEqual(Plugin, ThisPlugin)) {
                return true;
            }
        }

        return false;
    }

    public boolean add(Object Plugin) {

        if (TheList == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PluginList.add: null List.");
            return false;
        }

        return TheList.add(Plugin);
    }

    public boolean remove(Object Plugin) {

        if (TheList == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PluginList.remove: null List.");
            return false;
        }

        boolean Found = false;
        List<Object> NewList = new ArrayList<Object>();

        for (Object ThisPlugin : TheList) {
            if (PluginAndDependencies.PluginsAreEqual(Plugin, ThisPlugin)) {
                Found = true;
            } else {
                NewList.add(ThisPlugin);
            }
        }

        TheList = NewList;
        
        return Found;
    }
}
