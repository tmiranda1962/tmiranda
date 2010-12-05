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

        Log.getInstance().write(Log.LOGLEVEL_ALL, "getAllPlugins: Found Plugins = " + Plugins.size());
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

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getDependencyDescriptions: Descriptions " + Descriptions);
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

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getDependencyIDs: IDs = " + IDs);
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
        if (Parts.length > 1)
            Log.getInstance().write(Log.LOGLEVEL_ALL, "createIdFromDescription: Description and Parts[1] = " + Description + "&" + Parts[1]);
        return (Parts.length > 1 ? Parts[1] : "UNKNOWN");
    }

    /*
     * Map to get List of dependency IDs from Plugin.
     *
     * Will never return null.
     */
    public static Map<Object, List<String>> getPluginDependencyIDMap(Object Plugin) {

        Map<Object, List<String>> IDMap = new HashMap<Object, List<String>>();

        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getPluginIDMap: null Plugin.");
            return IDMap;
        }

        IDMap.put(Plugin, getDependencyIDs(Plugin));

        return IDMap;
    }

    /*
     * Map to get Plugin from the ID.
     * Will never return null.
     */
    public static Map<String, Object> getAllIDPluginMap() {

        Map<String, Object> PluginIDMap = new HashMap<String, Object>();

        List<Object> Plugins = getAllPlugins();

        for (Object Plugin : Plugins) {
            PluginIDMap.put(PluginAPI.GetPluginIdentifier(Plugin), Plugin);
        }

        return PluginIDMap;
    }

    /*
     * May return null if Description does not match any Plugin.
     */
    public static Object getPluginForDescription(String Description) {
        String ID = createIdFromDescription(Description);
        Log.getInstance().write(Log.LOGLEVEL_ALL, "getPluginForDescription: ID " + ID);
        return (Description == null ? null : PluginAPI.GetAvailablePluginForID(ID));
    }

    public static Map<Object, List<Object>> XXgetPluginDependencyMap(Object Plugin) {
        Map<Object, List<Object>> DependencyMap = new HashMap<Object, List<Object>>();

        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getPluginDependencyMap: null Plugin.");
            return DependencyMap;
        }

        List<String> Descriptions = getDependencyDescriptions(Plugin);

        if (Descriptions.size() == 0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPluginDependencyMap: No dependencies.");
            return DependencyMap;
        }

        List<Object> PluginsForDependencies = new ArrayList<Object>();

        for (String Description : Descriptions) {
            if (Description != null)
                PluginsForDependencies.add(getPluginForDescription(Description));
        }

        DependencyMap.put(Plugin, PluginsForDependencies);

        return DependencyMap;
    }
    
    public static List<Object> getPluginDependencies(Object Plugin) {
        
        List<Object> Dependencies = new ArrayList<Object>();
        
        if (Plugin == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getPluginDependencies: null Plugin.");
            return Dependencies;
        } 
        
        List<String> Descriptions = getDependencyDescriptions(Plugin);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getPluginDependencies: Descriptions = " + Descriptions);

        if (Descriptions.size() == 0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPluginDependencies: No dependencies.");
            return Dependencies;
        }

        List<Object> PluginsForDependencies = new ArrayList<Object>();

        for (String Description : Descriptions) {
            if (Description != null) {

                Log.getInstance().write(Log.LOGLEVEL_ALL, "getPluginDependencies: Description = " + Description);

                Object P = getPluginForDescription(Description);

                if (P != null) {
                    PluginsForDependencies.add(P);
                    Log.getInstance().write(Log.LOGLEVEL_ALL, "getPluginDependencies: Adding " + PluginAPI.GetPluginDescription(P));
                }
                
            }
        }

        return PluginsForDependencies;       
    }

    public static List<String> getPluginNamesThatUse(Object Plugin, List<Object> InstalledPlugins) {

        List<String> NewList = new ArrayList<String>();

        if (Plugin == null || InstalledPlugins == null || InstalledPlugins.isEmpty())
            return NewList;

        String ThisID = PluginAPI.GetPluginIdentifier(Plugin);

        for (Object ThisPlugin : InstalledPlugins) {

            if (!PluginAndDependencies.PluginsAreEqual(Plugin, ThisPlugin)) {
                List<String> DependencyIDs = getDependencyIDs(ThisPlugin);

                if (DependencyIDs.contains(ThisID)) {
                    NewList.add(PluginAPI.GetPluginName(ThisPlugin));
                }
            }

        }

        return NewList;

    }

}
