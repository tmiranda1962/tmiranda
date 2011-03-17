
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 * Class that implements many of the methods needed for MultiMediaFiles, MultiAirings and MultiFavorites.
 * @author Tom Miranda
 */
public class MultiObject {

    boolean             isValid         = true;
    //Object              record          = null;
    private String      store           = null;
    boolean             isInitialized   = false;
    String              userID          = null;
    private Integer     keyInt          = 0;
    private Integer     altKey          = 0;
    private String      altStore        = null;
    DatabaseRecord      database        = null;

    /**
     * The UserRecordAPI store "name" used to keep a delimited String of users that have
     * already initialized the object.
     */
    public static final String INITIALIZED = "IsInitialized";
    
    /**
     * The UserRecordAPI store "name" used to keep the key value for the object which will
     * usually be the MediaFileID, AiringID or FavoriteID. Storing the key is necessary because
     * in the current UserRecordAPI there is no way to retrieve all of the "keys" associated 
     * with a particular "store".
     */
    //public static final String KEY         = "Key";
    
    /*
     * Used transiently to mark the record as one that needs to be kept (because there is a
     * corresponding Airing or MediaFile.
     */
    static final String KEEPER      = "Keep";

    static final String WATCHED     = "Watched";
    static final String DONTLIKE    = "DontLike";
    static final String DELETED     = "Deleted";

    static final String[]   OBJECT_FLAGS = {DELETED, DONTLIKE, WATCHED, KEEPER, DatabaseRecord.KEY, INITIALIZED};

    static final String REALWATCHEDSTARTTIME_PREFIX = "RealWatchedStartTime_";
    static final String REALWATCHEDENDTIME_PREFIX   = "RealWatchedEndTime_";
    static final String WATCHEDSTARTTIME_PREFIX     = "WatchedStartTime_";
    static final String WATCHEDENDTIME_PREFIX       = "WatchedEndTime_";
    static final String DURATION_PREFIX             = "Duration_";

    static final String[]   OBJECT_FLAG_PREFIXES = {REALWATCHEDSTARTTIME_PREFIX,
                                                    REALWATCHEDENDTIME_PREFIX,
                                                    WATCHEDSTARTTIME_PREFIX,
                                                    WATCHEDENDTIME_PREFIX,
                                                    DURATION_PREFIX};

    /**
     * Constructor.
     * @param UserID The UserID for the user we are interested in.
     * @param Store The store (from UserRecordAPI) used to keep the data.
     * @param keyInt The Object's key.  This will usually be the MediaFileID, AIringID or FavoriteID.
     * @param altKeyInt the Object's alternate key.  If this Object is a MediaFile the alternate
     * key will be the AiringID.  If the Object is an Airing, the alternate key will be the
     * MediaFileID.  Use 0 if there is no alternate.  The alternate key is used to make sure we update
     * the data for both the MediaFile AND the Airing since many of the sage APIs can use either
     * Airings or MediaFiles.
     * @param altStore The object's alternate Store.  Similar to the alyKey, if the Object is a
     * MediaFile the altStore will be the store for the corresponding Airing.  If the Object is an
     * Airing the altStore will be the MediaFile store. Use null if there is no alternate.
     */
    public MultiObject(String UserID, String Store, Integer keyInt, Integer altKeyInt, String altStore) {

        if (UserID==null || UserID.isEmpty() || Store==null || Store.isEmpty() || keyInt==null) {
            isValid = false;
            return;
        }

        this.userID = UserID;
        this.store = Store;
        this.altKey = altKeyInt;
        this.altStore = altStore;
        this.keyInt = keyInt;

        String key = keyInt.toString();
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiObject: Key is " + key);

        //record = UserRecordAPI.GetUserRecord(store, key);
        database = new DatabaseRecord(store, keyInt);

        /*
        if (record == null) {

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiObject: Creating new userRecord.");

            record = UserRecordAPI.AddUserRecord(store, key);

            if (record==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiObject: Error creating userRecord for Store:Key " + store + ":" + key);
                isValid = false;
                return;
            }

            setRecordData(KEY, key);
        }
         * 
         */

        isInitialized = database.containsFlag(INITIALIZED, userID);
        return;
    }

    /**
     * Removes the specified User from the Flags.
     * @param User
     * @param Flags
     */
    void clearUser(String User, String[] Flags) {

        for (String Flag : Flags) {
            database.removeDataFromFlag(Flag, User);
        }

        clearUserFromFlagPrefix(OBJECT_FLAG_PREFIXES);

        database.removeDataFromFlag(INITIALIZED, userID);
        return;
    }

    /**
     * Removes the user from the Flags that begin with the specified prefixes.
     * @param Prefixes
     */
    void clearUserFromFlagPrefix(String[] Prefixes) {

        if (Prefixes==null || Prefixes.length==0)
            return;

        for (String prefix : Prefixes)
            database.setRecordData(prefix+userID, null);
    }

    /**
     * Completely removes the user record from the store.
     * @return
     */
    boolean removeRecord() {
        //return UserRecordAPI.DeleteUserRecord(record);
        return database.delete();
    }

    /*
     * Methods that must be reflected in both MediaFiles and Airings.
     */
    long getRealWatchedStartTime() {
        String D = database.getRecordData(REALWATCHEDSTARTTIME_PREFIX + userID);

        // If it's null it hasn't been watched so return 0.
        if (D==null || D.isEmpty())
            return 0;

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getRealWatchedStartTime: Bad number " + D);
            return -1;
        }
    }

    void setRealWatchedStartTime(String Time) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setRealWatchedStartTime: Setting to " + Plugin.PrintDateAndTime(Time) + " for " + userID);
        database.setRecordData(REALWATCHEDSTARTTIME_PREFIX + userID, Time);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setRealWatchedStartTime(Time);
        }
    }

    void setRealWatchedStartTime(long Time) {
        Long T = Time;
        setRealWatchedStartTime(T.toString());
    }

    void clearRealWatchedStartTime() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearRealWatchedStartTime: Setting to null.");
        database.setRecordData(REALWATCHEDSTARTTIME_PREFIX + userID, null);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearRealWatchedStartTime();
        }
    }


    long getRealWatchedEndTime() {
        String D = database.getRecordData(REALWATCHEDENDTIME_PREFIX + userID);

        // If it's null it hasn't been watched so return 0.
        if (D==null || D.isEmpty())
            return 0;

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getRealWatchedEndTime: Bad number " + D);
            return -1;
        }
    }

    void setRealWatchedEndTime(String Time) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setRealWatchedEndTime: Setting to " + Plugin.PrintDateAndTime(Time) + " for " + userID);
        database.setRecordData(REALWATCHEDENDTIME_PREFIX + userID, Time);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setRealWatchedEndTime(Time);
        }
    }

    void setRealWatchedEndTime(long Time) {
        Long T = Time;
        setRealWatchedEndTime(T.toString());
    }

    void clearRealWatchedEndTime() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearRealWatchedStartTime: Setting to null.");
        database.setRecordData(REALWATCHEDENDTIME_PREFIX + userID, null);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearRealWatchedEndTime();
        }
    }


    long getWatchedStartTime() {
        String D = database.getRecordData(WATCHEDSTARTTIME_PREFIX + userID);

        // If it's null it hasn't been watched so return 0.
        if (D==null || D.isEmpty())
            return 0;

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getWatchedStartTime: Bad number " + D);
            return -1;
        }
    }

    void setWatchedStartTime(String Time) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatchedStartTime: Setting to " + Plugin.PrintDateAndTime(Time) + " for " + userID);
        database.setRecordData(WATCHEDSTARTTIME_PREFIX + userID, Time);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setWatchedStartTime(Time);
        }
    }

    void setWatchedStartTime(long Time) {
        Long T = Time;
        setWatchedStartTime(T.toString());
    }

    void clearWatchedStartTime() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatchedStartTime: Setting to null.");
        database.setRecordData(WATCHEDSTARTTIME_PREFIX + userID, null);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearWatchedStartTime();
        }
    }


    long getWatchedEndTime() {
        String D = database.getRecordData(WATCHEDENDTIME_PREFIX + userID);

        // If it's null it hasn't been watched so return 0.
        if (D==null || D.isEmpty())
            return 0;

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getWatchedEndTime: Bad number " + D);
            return -1;
        }
    }

    void setWatchedEndTime(String Time) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatchedEndTime: Setting to " + Plugin.PrintDateAndTime(Time) + " for " + userID);
        database.setRecordData(WATCHEDENDTIME_PREFIX + userID, Time);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setWatchedEndTime(Time);
        }
    }

    void setWatchedEndTime(long Time) {
        Long T = Time;
        setWatchedEndTime(T.toString());
    }

    void clearWatchedEndTime() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatchedEndTime: Setting to null.");
        database.setRecordData(WATCHEDENDTIME_PREFIX + userID, null);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearWatchedEndTime();
        }
    }


    long getWatchedDuration() {
        String D = database.getRecordData(DURATION_PREFIX + userID);

        // If it's null it hasn't been watched so return 0.
        if (D==null || D.isEmpty())
            return 0;

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getWatchedDuration: Bad number " + D);
            return -1;
        }
    }

    void setWatchedDuration(String Duration) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatchedDuration: Setting to " + Plugin.PrintDateAndTime(Duration) + " for " + userID);
        database.setRecordData(DURATION_PREFIX + userID, Duration);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setWatchedDuration(Duration);
        }
    }

    void setWatchedDuration(long Duration) {
        Long D = Duration;
        setWatchedDuration(D.toString());
    }


    boolean isWatched() {

        if (!isValid)
            return false;
        else
            return database.containsFlag(WATCHED, userID);
    }

    void setWatched() {
        if (!isValid)
            return;

        database.addDataToFlag(WATCHED, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setWatched();
        }

        if (database.containsFlagAllUsers(WATCHED))
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatched: Setting physical file watched.");
            Object sageObject = sagex.api.MediaFileAPI.GetMediaFileForID(keyInt);

            if (sageObject!=null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatched: Found MediaFile.");
            } else {
                sageObject = sagex.api.AiringAPI.GetAiringForID(keyInt);
                if (sageObject!=null) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatched: Found Airing.");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "setWatched: MediaFile or Airing does not exist.");
                }
            }

            if (sageObject!=null)
                sagex.api.AiringAPI.SetWatched(sageObject);

        return;
    }

    void clearWatched() {
        if (!isValid)
            return;

        database.removeDataFromFlag(WATCHED, userID);
        setWatchedStartTime(0);
        setWatchedEndTime(0);
        setRealWatchedStartTime(0);
        setRealWatchedEndTime(0);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearWatched();
        }

        if (!database.containsFlagAnyData(WATCHED)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatched: Setting physical file unwatched.");
            Object sageObject = sagex.api.MediaFileAPI.GetMediaFileForID(keyInt);

            if (sageObject!=null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatched: Found MediaFile.");
            } else {
                sageObject = sagex.api.AiringAPI.GetAiringForID(keyInt);
                if (sageObject!=null) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatched: Found Airing.");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "clearWatched: MediaFile or Airing does not exist.");
                }
            }

            if (sageObject!=null)
                sagex.api.AiringAPI.ClearWatched(sageObject);
        }

    }


    boolean isDontLike() {

        if (!isValid)
            return false;
        else
            return database.containsFlag(DONTLIKE, userID);
    }

    void setDontLike() {
        if (!isValid)
            return;

        database.addDataToFlag(DONTLIKE, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.setDontLike();
        }

        return;
    }

    void clearDontLike() {
        if (!isValid)
            return;

        database.removeDataFromFlag(DONTLIKE, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.clearDontLike();
        }
    }

    boolean isDeleted() {
        return (isValid ? database.containsFlag(DELETED, userID) : false);
    }

    boolean delete(boolean WithoutPrejudice) {

        // If we have an invalid MultiObject, just return error.
        if (!isValid) {
            return false;
        }

        // Mark the MediaFile as deleted.
        database.addDataToFlag(DELETED, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.delete(WithoutPrejudice);
        }

        // If all users have it marked as deleted, delete it for real.
        if (database.containsFlagAllUsers(DELETED)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Deleting physical file for user " + userID);
            Object sageObject = sagex.api.MediaFileAPI.GetMediaFileForID(keyInt);

            if (sageObject!=null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Found MediaFile.");
            } else {
                sageObject = sagex.api.AiringAPI.GetAiringForID(keyInt);
                if (sageObject!=null) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Found Airing.");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: MediaFile or Airing already deleted.");
                }
            }

            // Check to see if we need to mark the Airing as Watched in the Sage core.
            if (database.containsFlagAllUsers(WATCHED) || database.containsFlagAnyIRUsers(WATCHED)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Setting Watched in core.");
                sagex.api.AiringAPI.SetWatched(sageObject);
            }

            removeRecord();

            if (sageObject==null)
                return true;
            else
                return(WithoutPrejudice ? sagex.api.MediaFileAPI.DeleteFileWithoutPrejudice(sageObject) : sagex.api.MediaFileAPI.DeleteFile(sageObject));
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Leaving physical file intact.");
        return true;
    }

    /**
     * Alternate way to set the DELETE Flag.
     */
    void hide() {
        if (!isValid)
            return;

        database.addDataToFlag(DELETED, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.hide();
        }
    }

    /**
     * Alternate way to clear the DELETE Flag.
     */
    void unhide() {
        if (!isValid)
            return;

        database.removeDataFromFlag(DELETED, userID);

        if (altKey != null && altKey != 0 && altKey != null) {
            MultiObject MO = new MultiObject(userID, altStore, altKey, 0, null);
            MO.unhide();
        }
    }


    static void clearKeeperFlag(Object[] allRecords) {

        if (allRecords==null || allRecords.length==0)
            return;

        for (Object record : allRecords)
            UserRecordAPI.SetUserRecordData(record, KEEPER, "false");
    }

    static void setKeeperFlag(Object record) {
        UserRecordAPI.SetUserRecordData(record, KEEPER, "true");
    }

    static int deleteNonKeepers(Object[] allRecords, boolean countOnly) {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "deleteNonKeepers: countOnly " + countOnly);

        int count = 0;

        for (Object record : allRecords) {
            String keep = UserRecordAPI.GetUserRecordData(record, KEEPER);
            if (keep.equalsIgnoreCase("false")) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "deleteNonKeepers: Found unused record.");
                count++;

                if (!countOnly) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "deleteNonKeepers: Deleting unused record.");
                    UserRecordAPI.DeleteUserRecord(record);
                }
            }
        }

        return count;
    }

    // Used for debugging.
    String getFlagString(String Flag) {
        return database.getRecordData(Flag);
    }

    // Gets the data for the current user.
    List<String> getObjectFlagsForUser() {

        List<String> theList = new ArrayList<String>();

       for (String flag : OBJECT_FLAGS)

            if (userID.equalsIgnoreCase(Plugin.SUPER_USER)) {
               theList.add(flag + database.getRecordData(flag));
            } else {
               if (database.containsFlag(flag, userID))
                   theList.add("Contains " + flag);
               else
                   theList.add("!Contains " + flag);
           }

        for (String prefix : OBJECT_FLAG_PREFIXES)
            theList.add(prefix + "=" + database.getRecordData(prefix+userID));

        return theList;
    }

}
