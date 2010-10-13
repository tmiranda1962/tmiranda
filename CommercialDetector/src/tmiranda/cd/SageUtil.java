/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

/**
 *
 * @author Default
 */
import java.io.*;
import java.util.*;
import sagex.api.*;
/**
 *
 * @author Default
 */
public class SageUtil {

    /**
    * Returns a Sage Property in boolean form.
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value.  No error checking is done so any String will be accepted.
    * @return           A boolean indicating the status of the property.  If the Property is anything other
    *                   than "true", false is returned.
    */
    public static boolean GetBoolProperty(String Property, String Value) {
        String prop = Configuration.GetServerProperty(Property, Value);
        return prop.equalsIgnoreCase("true");
    }

    /**
    * Returns a Sage Property in boolean form.
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value. (Reference.)
    * @return           A boolean indicating the status of the property.  If the Property is anything other
    *                   than "true", false is returned.
    */
    public static boolean GetBoolProperty(String Property, Boolean Value) {
        String prop = Configuration.GetServerProperty(Property, Value.toString());
        return prop.equalsIgnoreCase("true");
    }

    /**
    * Returns a Sage Property in boolean form.
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value. (Primative.)
    * @return           A boolean indicating the status of the property.  If the Property is anything other
    *                   than "true", false is returned.
    */
    public static boolean GetBoolProperty(String Property, boolean Value) {
        Boolean DefaultValue = Value;
        String prop = Configuration.GetServerProperty(Property, DefaultValue.toString());
        return prop.equalsIgnoreCase("true");
    }

    /**
    * Returns a Sage Property as a Long..
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value.  No error checking is done so any String will be accepted.
    * @return           A Long representation of the property.
    */
    public static long GetLongProperty(String Property, String Value) {
        String prop = Configuration.GetServerProperty(Property, Value);
        return Long.parseLong(prop);
    }

    /**
    * Returns a Sage Property as a Long.
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value (primative.)
    * @return           A Long representation of the property.
    */
    public static long GetLongProperty(String Property, long Value) {
        Long DefaultValue = Value;
        String prop = Configuration.GetServerProperty(Property, DefaultValue.toString());
        Long L = 0L;
        try {L=Long.parseLong(prop);} catch (NumberFormatException e) {L=0L;}
        return L;
    }

    /**
    * Returns a Sage Property as a Long.
    * <p>
    * @param  Property  The property.
    * @param  Value     The default value (reference.)
    * @return           A Long representation of the property.
    */
    public static long GetLongProperty(String Property, Long Value) {
        String prop = Configuration.GetServerProperty(Property, Value.toString());
        Long L = 0L;
        try {L=Long.parseLong(prop);} catch (NumberFormatException e) {L=0L;}
        return L;
    }

    public static int GetIntProperty(String Property, String Value) {

        String prop = Configuration.GetServerProperty(Property, Value);

        int p = 0;

        try {
            p = Integer.parseInt(prop);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Invalid integer for GetIntProperty " + Property + " " + prop);
            p = 0;
        }

        return p;
    }

    public static int GetIntProperty(String Property, Integer Value) {
        return GetIntProperty(Property, Value.toString());
    }

    public static Float GetFloatProperty(String Property, float Value) {
        Float DefaultValue = Value;
        String prop = Configuration.GetServerProperty(Property, DefaultValue.toString());
        Float F = 0F;
        try { F=Float.parseFloat(prop); } catch (NumberFormatException e) {F=0F;}
        return F;
    }

    public static Float GetFloatProperty(String Property, String Value) {
        float v = 0F;
        try { v=Float.parseFloat(Value); } catch (NumberFormatException e) {v=0F;}
        return GetFloatProperty(Property, v);
    }

    /**
     * Renames a file but first checks to see if a file by the new name exists.  If it does it is deleted.
     * This method is intended to be used to create a backup of a file.
     * <p>
     * @param Original The name of the original file.
     * @param Backup The new name.
     * @return
     */
    public static boolean RenameFile(String Original, String Backup) {

        File orig = new File(Original);
        File back = new File(Backup);

        // Delete the backup if it already exists.
        if (back.exists()) {
            if (!back.delete()) {
                System.out.println("SageUtils: Failed to delete " + Backup);
            }
            back = new File(Backup);
        }

        // Rename the original.
        if (!orig.renameTo(back)) {
            System.out.println("SageUtils: Failed to rename " + Original + " to " + Backup);
            return false;
        }

        return true;
    }

    public static boolean StringToBool(String s) {
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    public String ObjectToString(Object o) {
        return o.toString();
    }

    /*
     * Useful becasue GetAvailablePluginForID does not work for plugins in test mode.
     */
    public static Object GetPluginForID(String ID) {
        List<Object> AllPluginsList = new ArrayList<Object>();

        Object[] AllClientPlugins = PluginAPI.GetInstalledClientPlugins();
        if (AllClientPlugins!=null && AllClientPlugins.length != 0)
            AllPluginsList.addAll(Arrays.asList(AllClientPlugins));

        Object[] AllServerPlugins = PluginAPI.GetInstalledPlugins();
        if (AllServerPlugins!=null && AllServerPlugins.length != 0)
            AllPluginsList.addAll(Arrays.asList(AllServerPlugins));

        for (Object p : AllPluginsList) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "IDENTIFIER="+PluginAPI.GetPluginIdentifier(p));
            if (PluginAPI.GetPluginIdentifier(p).equalsIgnoreCase(ID))
                return p;
        }
        return null;
    }

}
