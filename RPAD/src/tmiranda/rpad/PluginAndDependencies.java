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
    private String Status;                              // Used to keep track of the uninstall status.



    /**
     * Constructor.  Used to create a new PluginAndDependencies tree.
     *
     * @param ThePlugin The Plugin to create the Tree for.
     * @param TheParent Set to the same value as ThePlugin.
     */
    public PluginAndDependencies(Object ThePlugin, Object TheParent) {
        Plugin = ThePlugin;
        Parent = TheParent;
        Status = null;

        if (ThePlugin == null || TheParent == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PluginAndDependecies: Error. null paramter.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PluginAndDependecies: Plugin and Parent = " + PluginAPI.GetPluginIdentifier(Plugin) + ":" + PluginAPI.GetPluginIdentifier(Parent));

        if (Dependencies == null) {
            Dependencies = new ArrayList<PluginAndDependencies>();
        }

        List<Object> Plugins = api.getPluginDependencies(ThePlugin);

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

    public String getStatus() {
        return Status;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }

    /**
     * Prints the complete dependency tree to the debug log.
     *
     * @param Tree The Tree to display.
     */
    public static void showDependencyTree(PluginAndDependencies Tree) {

        System.out.println("RPAD: Showing Tree for " + PluginAPI.GetPluginIdentifier(Tree.Plugin));

        if (Tree.Plugin == Tree.Parent) {
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
     * @param Tree The PluginAndDependencies tree to process.
     * @param CurrentList The current List of Plugins.  Use null to start.
     * @return A List of Plugins that are dependencies of the parent Plugin.
     */

    public static Set<Object> getListOfDependencies(PluginAndDependencies Tree) {

        // Parameter check.
        if (Tree == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getListOfDependecies: Error. null Tree.");
            return null;
        }

        Set<Object> NewList = new LinkedHashSet<Object>();

        // Recursively add dependencies.
        if (Tree.Dependencies != null && !Tree.Dependencies.isEmpty()) {
            for (PluginAndDependencies Dependency : Tree.Dependencies) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: Processing dependency " + PluginAPI.GetPluginIdentifier(Dependency.Plugin));

                // Get the Set.
                Set<Object> TempList = getListOfDependencies(Dependency);

                // If there are items in it add them, if not already added.
                if (TempList != null && !TempList.isEmpty()) {
                    for (Object Plugin : TempList) {
                        if (!setContainsPlugin(NewList, Plugin)) {
                            NewList.add(Plugin);
                        }
                    }
                }
                //NewList.addAll(getListOfDependencies(Dependency));
            }
        }

        // Add in this Plugin if it's not the root and it's not already added.
        if (!setContainsPlugin(NewList, Tree.Plugin) && !PluginsAreEqual(Tree.Parent, Tree.Plugin)) {
            NewList.add(Tree.Plugin);
        }

        return NewList;
    }


    public static Set<Object> OLDgetListOfDependencies(PluginAndDependencies Tree, Set<Object> CurrentList) {

        // Parameter check.
        if (Tree == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getListOfDependecies: Error. null Tree.");
            return null;
        }

        Set<Object> NewList;

        if (CurrentList == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: Starting new List for " + PluginAPI.GetPluginIdentifier(Tree.Plugin));
            NewList = new LinkedHashSet<Object>();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: Adding to CurrentList for " + PluginAPI.GetPluginIdentifier(Tree.Plugin));
            NewList = CurrentList;
        }

        // If there are no dependencies, add this Plugin (if not already added) and it's not the root.
        if (Tree.Dependencies == null || Tree.Dependencies.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: No dependencies. Adding " + PluginAPI.GetPluginIdentifier(Tree.Plugin));
            if (!setContainsPlugin(NewList, Tree.Plugin) && !PluginsAreEqual(Tree.Parent, Tree.Plugin)) {
                NewList.add(Tree.Plugin);
            }
            return NewList;
        }

        // Recursively add dependencies.
        for (PluginAndDependencies Dependency : Tree.Dependencies) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getListOfDependecies: Processing dependency " + PluginAPI.GetPluginIdentifier(Dependency.Plugin));
            NewList = OLDgetListOfDependencies(Dependency, NewList);
        }

        return NewList;
    }

    public static void showDependencyList(Set<Object> Plugins) {

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
     * of dependencies.
     *
     * @param Plugin
     * @param Dependencies
     * @return
     */
    public static boolean isNeeded(Object Plugin, List<PluginAndDependencies> Dependencies) {

        // If the parameters are bad or there are no more dependencies return false.
        if (Plugin == null || Dependencies == null || Dependencies.isEmpty()) {
            return false;
        }

        for (PluginAndDependencies Dependency : Dependencies) {
            
            if (PluginsAreEqual(Plugin, Dependency.Plugin) || isNeeded(Dependency.Plugin, Dependency.Dependencies)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Helper method needed because .equals and .contains do not work for Sage Plugin Objects.
     */
    static public boolean PluginsAreEqual(Object P1, Object P2) {
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
    static private boolean setContainsPlugin(Set<Object> List, Object Plugin) {

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
