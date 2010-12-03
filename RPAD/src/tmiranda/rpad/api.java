/*
 * RPAD - Remove Plugin And Dependencies.
 */

package tmiranda.rpad;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class api {

    /*
     * Will never return null.
     */
    public static List<Object> getAllPlugins() {
        List<Object> Plugins = new ArrayList<Object>();

        Object[] PluginArray = PluginAPI.GetAllAvailablePlugins();

        if (PluginArray == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllPlugins: null PluginArray.");
            return Plugins;
        }

        if (PluginArray.length != 0) {
            Plugins = Arrays.asList(PluginArray);
        }

        return Plugins;
    }

    /*
     * Will never return null.
     */
    public static List<String> getDependencyDescriptions(Object Plugin) {
        List<String> Descriptions = new ArrayList<String>();

        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDependencyDescriptions: null Plugin.");
            return Descriptions;
        }

        String[] DescriptionArray = PluginAPI.GetPluginDependencies(Plugin);

        if (DescriptionArray == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getDependencyDescriptions: null DescriptionArray.");
            return Descriptions;
        }

        if (DescriptionArray.length != 0) {
            Descriptions = Arrays.asList(DescriptionArray);
        }

        return Descriptions;
    }

    /*
     * Will never return null.
     */
    public static List<String> getDependencyIDs(Object Plugin) {

        List<String> IDs = new ArrayList<String>();

        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDependencyIDs: null Plugin.");
            return IDs;
        }

        List<String> Descriptions = getDependencyDescriptions(Plugin);

        for (String D : Descriptions) {
            if (D != null)
                IDs.add(createIdFromDescription(D));
        }

        return IDs;
    }

    /*
     * Takes a description in the format "Type: ID xxx" and returns the ID.
     */
    public static String createIdFromDescription(String Description) {

        if (Description == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "createIdFromDescription: null Description.");
            return null;
        }

        String[] Parts = Description.split(" ");

        return (Parts.length > 1 ? Parts[1] : "UNKNOWN");
    }

    /*
     * Will never return null;
     */
    public static Map<Object, List<String>> getPluginIDMap(Object Plugin) {

        Map<Object, List<String>> IDMap = new HashMap<Object, List<String>>();

        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getPluginIDMap: null Plugin.");
            return IDMap;
        }

        IDMap.put(Plugin, getDependencyIDs(Plugin));

        return IDMap;
    }

    public static Map<Object, String> getAllPluginIDMap() {

        Map<Object, String> PluginIDMap = new HashMap<Object, String>();

        List<Object> Plugins = getAllPlugins();

        for (Object Plugin : Plugins) {
            PluginIDMap.put(Plugin, PluginAPI.GetPluginIdentifier(Plugin));
        }

        return PluginIDMap;
    }

    public static Object getPluginForID(String ID) {
        return (ID == null ? null : getAllPluginIDMap().get(ID));
    }

    public static Object getPluginForDescription(String Description) {
        return (Description == null ? null : getAllPluginIDMap().get(createIdFromDescription(Description)));
    }
}
