/*
 * Buildingblock.
 */

package tmiranda.rpad;

import sagex.api.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class PluginAndDependencies {
    private Object Plugin;                              // The Plugin we are interested in.
    private Object Parent;                              // The parent Plugin
    private List<PluginAndDependencies> Dependencies;   // null means the end of the branch.

    /**
     * Constructor.  Used to create a new PluginAndDependencies tree.
     *
     * @param ThePlugin The Plugin to create the Tree for.
     * @param TheParent Set to the same value as ThePlugin.
     */
    public PluginAndDependencies(Object ThePlugin, Object TheParent) {
        Plugin = ThePlugin;
        Parent = TheParent;

        if (ThePlugin == null || TheParent == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PluginAndDependecies: Error. null paramter.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PluginAndDependecies: Plugin and Parent = " + PluginAPI.GetPluginIdentifier(Plugin) + ":" + PluginAPI.GetPluginIdentifier(Parent));

        if (Dependencies == null) {
            Dependencies = new ArrayList<PluginAndDependencies>();
        }

        List<Object> Plugins = getPluginDependencies(ThePlugin);

        if (Plugins == null || Plugins.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PluginAndDependecies: No dependencies for " + PluginAPI.GetPluginIdentifier(Plugin));
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PluginAndDependecies: Dependencies found = " + Plugins.size());

        for (Object ThisPlugin : Plugins) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PluginAndDependecies: Recursing = " + PluginAPI.GetPluginIdentifier(Plugin) + ":" + PluginAPI.GetPluginIdentifier(ThisPlugin));
            Dependencies.add(new PluginAndDependencies(ThisPlugin, ThePlugin));
        }

        return;
    }

    /**
     * Prints the complete dependency tree to the debug log.
     *
     * @param Tree The Tree to display.
     */
    public static void showDependencyTree(PluginAndDependencies Tree) {

        System.out.println("RPAD: Showing Tree for " + PluginAPI.GetPluginIdentifier(Tree.Plugin));

        if (isRoot(Tree)) {
            System.out.println("RPAD: == It is the root.");
        } else {
            System.out.println("RPAD: == It's parent is " + PluginAPI.GetPluginIdentifier(Tree.Parent));
        }

        if (Tree.Dependencies != null && !Tree.Dependencies.isEmpty()) {

            System.out.println("RPAD: == It has " + Tree.Dependencies.size() + " dependencies.");

            for (PluginAndDependencies Dependency : Tree.Dependencies) {
                System.out.println("RPAD:   == Dependency = " + PluginAPI.GetPluginIdentifier(Dependency.Plugin));
            }

            for (PluginAndDependencies Dependency : Tree.Dependencies) {
                showDependencyTree(Dependency);
            }
        } else {
            System.out.println("RPAD: == It has no dependencies.");
        }

        System.out.println("RPAD: == Completed Tree for " + PluginAPI.GetPluginIdentifier(Tree.Plugin));
    }

    /**
     * Gets a List of all the Plugins that are dependencies in the given PluginAndDependencies.  It does NOT
     * differentiate between installed, uninstalled, needed or unneeded.
     *
     * The Plugins will be returned in reverse-dependency order, so the dependencies can be removed in
     * the order they are returned.
     *
     * No Plugins are duplicated.
     *
     * It does NOT return the root of the Tree. (The Plugin that is the parent.)
     *
     * @param Tree The PluginAndDependencies tree to process.
     * @param CurrentList The current List of Plugins.  Use null to start.
     * @return A List of Plugins that are dependencies of the parent Plugin.
     */
    public static List<Object> getListOfDependencies(PluginAndDependencies Tree) {

        // Parameter check.
        if (Tree == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getListOfDependecies: Error. null Tree.");
            return null;
        }

        List<Object> NewList = new ArrayList<Object>();

        // Recursively add dependencies.
        if (Tree.Dependencies != null && !Tree.Dependencies.isEmpty()) {
            for (PluginAndDependencies Dependency : Tree.Dependencies) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: Processing dependency " + PluginAPI.GetPluginIdentifier(Dependency.Plugin));

                // Get the Set.
                List<Object> TempList = getListOfDependencies(Dependency);

                // If there are items in it add them, if not already added.
                if (TempList != null && !TempList.isEmpty()) {
                    for (Object Plugin : TempList) {
                        if (!listContainsPlugin(NewList, Plugin)) {
                            NewList.add(Plugin);
                        }
                    }
                }
                //NewList.addAll(getListOfDependencies(Dependency));
            }
        }

        // Add in this Plugin if it's not the root and it's not already added.
        if (!listContainsPlugin(NewList, Tree.Plugin) && !isRoot(Tree)) {
            NewList.add(Tree.Plugin);
        }

        return NewList;
    }

    public static void showDependencyList(List<Object> Plugins) {

        System.out.println("RPAD: Showing dependencies.");

        if (Plugins == null || Plugins.isEmpty()) {
            System.out.println("RPAD: = None or null.");
            return;
        }

        for (Object Plugin : Plugins) {
            System.out.println("RPAD: = " + PluginAPI.GetPluginIdentifier(Plugin));
        }

    }

    /**
     * Determines if the Plugin specified is needed by any of the Plugins in the List
     * of PluginAndDependencies.
     *
     * @param Plugin
     * @param Dependencies
     * @return
     */
    public static boolean isNeeded(Object ThePlugin, List<PluginAndDependencies> Dependencies) {

        // Parameter check.
        if (ThePlugin == null || Dependencies == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "isNeeded: Found null Plugin or Dependencies.");
            return false;
        }

        for (PluginAndDependencies Dependency : Dependencies) {

            // Check if we have a bad parameter.
            if (Dependency == null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "isNeeded: Found null Dependency.");
                return false;
            }

            // Skip if we are comparing ThePlugin to itself.
            if (!(PluginsAreEqual(ThePlugin, Dependency.Plugin) && isRoot(Dependency))) {
             
                // Return true if it's needed by this Plugin or any of its dependencies.
                if (PluginsAreEqual(ThePlugin, Dependency.Plugin) || isNeeded(ThePlugin, Dependency.Dependencies)) {
                    return true;
                }
            }
        }

        // It wasn't needed.
        return false;
    }

    /**
     * Looks through the InstalledPlugins List and returns the plugins that use Plugin.
     *
     * @param Plugin The Plugin that we are interested in.
     * @param InstalledPlugins The List of Plugins to scan.
     * @return A List of Plugins that use the referenced Plugin.
     */
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

    /*
     * Returns true if this Tree is the root, false otherwise.
     */
    private static boolean isRoot(PluginAndDependencies Tree) {
        return PluginsAreEqual(Tree.Parent, Tree.Plugin);
    }

    private static List<Object> getPluginDependencies(Object Plugin) {

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

    /*
     * May return null if Description does not match any Plugin.
     */
    private static Object getPluginForDescription(String Description) {
        String ID = createIdFromDescription(Description);
        Log.getInstance().write(Log.LOGLEVEL_ALL, "getPluginForDescription: ID " + ID);
        return (Description == null ? null : PluginAPI.GetAvailablePluginForID(ID));
    }

    /*
     * Takes a description in the format "Type: ID xxx" and returns the ID.
     */
    private static String createIdFromDescription(String Description) {

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
     * Will never return null.
     */
    private static List<String> getDependencyDescriptions(Object Plugin) {
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
    private static List<String> getDependencyIDs(Object Plugin) {

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
     * Helper method needed because .equals and .contains do not work for Sage Plugin Objects.
     */
    private static boolean PluginsAreEqual(Object P1, Object P2) {
        if (P1 == null || P2 == null) {
            return false;
        }

        String Name1 = PluginAPI.GetPluginIdentifier(P1);
        String Name2 = PluginAPI.GetPluginIdentifier(P2);

        if (Name1 == null || Name2 == null) {
            return false;
        }

        return Name1.compareToIgnoreCase(Name2) == 0;
    }

    /*
     * Helper method needed because .equals and .contains do not work for Sage Plugin Objects.
     */
    private static boolean listContainsPlugin(List<Object> List, Object Plugin) {

        if (List == null || Plugin == null || List.isEmpty())
            return false;

        for (Object P : List)
            if (PluginsAreEqual(P, Plugin))
                return true;

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginAndDependencies other = (PluginAndDependencies) obj;
        if (this.Plugin != other.Plugin && (this.Plugin == null || !this.Plugin.equals(other.Plugin))) {
            return false;
        }
        if (this.Parent != other.Parent && (this.Parent == null || !this.Parent.equals(other.Parent))) {
            return false;
        }
        if (this.Dependencies != other.Dependencies && (this.Dependencies == null || !this.Dependencies.equals(other.Dependencies))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + (this.Plugin != null ? this.Plugin.hashCode() : 0);
        hash = 31 * hash + (this.Parent != null ? this.Parent.hashCode() : 0);
        hash = 31 * hash + (this.Dependencies != null ? this.Dependencies.hashCode() : 0);
        return hash;
    }

}
