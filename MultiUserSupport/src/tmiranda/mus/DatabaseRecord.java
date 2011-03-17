
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class DatabaseRecord {

    public static String KEY = "Key";

    private boolean isValid = true;
    private Object  record  = null;
    private String  store   = null;
    private String  key     = null;

    public DatabaseRecord(String Store, String Key) {
        init(Store, Key);
    }

    public DatabaseRecord(String Store, Integer Key) {
        init(Store, Key.toString());
    }

    private void init(String Store, String Key) {

        isValid = true;
        store = Store;
        key = Key;

        record = UserRecordAPI.GetUserRecord(Store, Key);

        if (record == null) {

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DatabaseRecord: Creating new record.");

            record = UserRecordAPI.AddUserRecord(Store, Key);

            if (record==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DatabaseRecord: Error creating record for Store:Key " + Store + ":" + Key);
                isValid = false;
                return;
            }

            // Set the key so we can generate a keyset.  There is no UserRecordAPI method to
            // get the keyset so we need to keep track of the keys here.
            setRecordData(KEY, Key);
        }
    }

    public boolean exists() {
        return isValid && record!=null;
    }

    public boolean delete() {
        return UserRecordAPI.DeleteUserRecord(record);
    }

    public static List<String> getDataFromAllStores(String Store, String Key) {

        List<String> Data = new ArrayList<String>();

        Object[] Records = UserRecordAPI.GetAllUserRecords(Store);

        if (Records==null || Records.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getDataFromAllStores: null Records.");
            return Data;
        }

        for (Object Record : Records) {
            if (Record!=null) {
                String data = UserRecordAPI.GetUserRecordData(Record, Key);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getDataFromAllStores: Found Data " + data);
                if (data != null && !data.isEmpty())
                    Data.add(data);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "getDataFromAllStores: Found null Record.");
            }
        }

        return Data;
    }

    public static void wipeAllRecords(String Store) {
        Object[] AllUserRecords = UserRecordAPI.GetAllUserRecords(Store);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: Begin wipe of Store " + AllUserRecords.length);
        for (Object Record : AllUserRecords)
            UserRecordAPI.DeleteUserRecord(Record);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: DataStore wiped.");
    }


    /**
     * Get the keyset for all of the database records.
     * @return
     */
    public List<String> keySet() {
        List<String> keySet = new ArrayList<String>();

        Object[] allUserRecords = UserRecordAPI.GetAllUserRecords(store);

        if (!isValid || allUserRecords==null || allUserRecords.length==0)
            return keySet;

        for (Object rec : allUserRecords) {
            String thisKey = getRecordData(KEY);
            if (!(thisKey==null || thisKey.isEmpty()))
                keySet.add(thisKey);
        }

        return keySet;
    }

    /**
     * Returns the raw record data.
     * @param Flag
     * @return
     */
    final String getRecordData(String Flag) {
        return UserRecordAPI.GetUserRecordData(record, Flag);
    }

    /**
     * Sets the raw record data.
     * @param Flag
     * @param Data
     */
    final void setRecordData(String Flag, String Data) {
        UserRecordAPI.SetUserRecordData(record, Flag, Data);
        return;
    }

    /**
     * Adds Data to the delimited String contained in Flag.
     * @param Flag
     * @param Data
     * @return
     */
    DelimitedString addDataToFlag(String Flag, String Data) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addFlag: Adding " + Data + " to " + Flag);
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        DS.addUniqueElement(Data);
        setRecordData(Flag, DS.toString());
        return DS;
    }

    /**
     * Removes Data from the delimited String contained in Flag.
     * @param Flag
     * @param Data
     * @return
     */
    DelimitedString removeDataFromFlag(String Flag, String Data) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removeFlag: Removing " + Data + " from " + Flag);
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        DS.removeElement(Data);
        setRecordData(Flag, DS.toString());
        return DS;
    }

    /**
     * Check if the specified Flag contains Data.
     * @param Flag
     * @param User
     * @return
     */
    final boolean containsFlag(String Flag, String User) {
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        return (DS.contains(User));
    }

    /**
     * Check if the Flag contains any data at all.
     * @param Flag
     * @return
     */
    boolean containsFlagAnyData(String Flag) {
        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);
        return (DS!=null && !DS.isEmpty());
    }

    /**
     * Check if the Flag contains all of the userIDs.  Excludes Admin.
     * @param Flag
     * @return
     */
    boolean containsFlagAllUsers(String Flag) {
        List<String> allUsers = User.getAllUsers(false);

        DelimitedString DS = new DelimitedString(getRecordData(Flag), Plugin.LIST_SEPARATOR);

        // Remove the Admin user.
        //if (allUsers.contains(Plugin.SUPER_USER)) {
            //allUsers.remove(Plugin.SUPER_USER);
        //}

        return (DS.containsAll(allUsers));
    }

    /**
     * Checks if the Flag contains any Users that have IR enabled.  Excludes Admin.
     * @param Flag
     * @return
     */
    boolean containsFlagAnyIRUsers(String Flag) {
        List<String> allUsers = User.getAllUsers(false);

        for (String U : allUsers) {
            User user = new User(U);

            if (!user.isIntelligentRecordingDisabled() && containsFlag(Flag, U)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "containsFlagAnyIRUsers: Found IR user " + U);
                return true;
            }
        }

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
        final DatabaseRecord other = (DatabaseRecord) obj;
        if ((this.store == null) ? (other.store != null) : !this.store.equals(other.store)) {
            return false;
        }
        if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.store != null ? this.store.hashCode() : 0);
        hash = 97 * hash + (this.key != null ? this.key.hashCode() : 0);
        return hash;
    }

}
