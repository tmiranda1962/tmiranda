/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class MultiObject {

    boolean             isValid         = true;
    Object              record          = null;
    private String      store           = null;
    boolean             isInitialized   = false;

    static final String INITIALIZED     = "IsInitialized";

    public MultiObject(String Store, Integer keyInt) {

        if (Store==null || Store.isEmpty() || keyInt==null) {
            isValid = false;
            return;
        }

        store = Store;

        String key = keyInt.toString();
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiObject: Key is " + key);

        record = UserRecordAPI.GetUserRecord(store, key);

        if (record == null) {

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiObject: Creating new userRecord.");

            record = UserRecordAPI.AddUserRecord(store, key);

            if (record==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiObject: Error creating userRecord for Store:Key " + store + ":" + key);
                isValid = false;
                return;
            }
        }

        String init = getRecordData(INITIALIZED);
        isInitialized = (init != null && init.equalsIgnoreCase("true"));
        return;
    }

    // Returns the specified user record data.
    String getRecordData(String Flag) {
        return UserRecordAPI.GetUserRecordData(record, Flag);
    }
    
    // Sets the specified user record data.
    void setRecordData(String Flag, String Data) {       
        UserRecordAPI.SetUserRecordData(record, Flag, Data);
        return;
    }

    // Adds Data to the Flag.
    DelimitedString addFlag(String Flag, String Data) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addFlag: Adding " + Data + " to " + Flag);
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        DS.addUniqueElement(Data);
        setRecordData(Flag, DS.toString());
        return DS;
    }

    // Removes Data from the Flag.
    DelimitedString removeFlag(String Flag, String Data) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removeFlag: Removing " + Data + " from " + Flag);
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        DS.removeElement(Data);
        setRecordData(Flag, DS.toString());
        return DS;
    }

    // True if the Flag contains Data.
    boolean containsFlag(String Flag, String User) {
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        return (DS.contains(User));
    }

    boolean containsFlagAnyData(String Flag) {
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        return (DS!=null && !DS.isEmpty());
    }

    boolean containsFlagAllUsers(String Flag) {
        List<String> allUsers = User.getAllUsers();
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        allUsers.remove(Plugin.SUPER_USER);
        return (DS.containsAll(allUsers));
    }

    // Removes all Records from the DataStore.
    void wipeDatabase() {

        Object[] AllUserRecords = UserRecordAPI.GetAllUserRecords(store);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: Begin wipe of Store " + AllUserRecords.length);
        for (Object Record : AllUserRecords)
            UserRecordAPI.DeleteUserRecord(Record);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: DataStore wiped.");
    }

    // Removes the specified User from the Flags.
    void clearUser(String User, String[] Flags) {

        for (String Flag : Flags) {
            removeFlag(Flag, User);
        }

        addFlag(INITIALIZED, "false");
        return;
    }

    // Used for debugging.
    String getFlagString(String Flag) {
        return getRecordData(Flag);
    }
}
