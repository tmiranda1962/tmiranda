/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.UIContext;
import sagex.api.*;

/**
 * Class that implements the user related functionality.
 * @author Default
 */
public class User {

    static final String STORE      = "MultiUser.User"; // Record Key is UserID.

    private static final String KEY_USERID          = "UserID";
    private static final String KEY_PASSWORD        = "Password";
    private static final String KEY_IR              = "IntelligentRecording";
    private static final String KEY_SHOW_IMPORTS    = "ShowImportedVideos";
    private static final String KEY_UICONTEXT       = "UIContext";
    private static final String KEY_WATCHING        = "Watching";
    private static final String KEY_PRIMARY_WATCH   = "PrimaryWatch";
    private static final String KEY_START_WATCH     = "StartWatch";
    private static final String KEY_WATCH_TIME      = "WatchTime";
    private static final String KEY_WATCH_LIMIT     = "WatchLimit";
    private static final String KEY_WATCH_PERIOD    = "WatchPeriod";

    private String  user    = null;
    private boolean isValid = true;
    private DatabaseRecord  database = null;

    public User(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "User: null UserID.");
            isValid = false;
            return;
        }

        if (UserID.equalsIgnoreCase("true") || UserID.equalsIgnoreCase("false")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "User: Invalid UserID " + UserID);
            isValid = false;
            return;
        }

        user = UserID;
        database = new DatabaseRecord(STORE, user);
        return;
    }

    void logOn() {
        SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, user);
        SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_CONTEXT_NAME, Global.GetUIContextName(UIContext.getCurrentContext()));
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOn: Logged on user " + user);
    }

    void logOff() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOff: Logged off user " + user);
        SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
        //SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_CONTEXT_NAME, null);
    }

    boolean exists() {
        return database.exists() && database.getRecordData(KEY_USERID).equals(user);
    }

    boolean create(String Password) {
        if (!isValid || Password==null || Password.isEmpty())
            return false;

        // Delete the old Record if it exists.
        database.delete();

        // Create a new record.
        database = new DatabaseRecord(STORE, user);

        // Initialize it.
        database.setRecordData(KEY_USERID, user);
        database.setRecordData(KEY_PASSWORD, Password);
        database.setRecordData(KEY_SHOW_IMPORTS, "true");
        database.setRecordData(KEY_WATCH_TIME, "0");
        database.setRecordData(KEY_WATCH_LIMIT, "0");
        database.setRecordData(KEY_WATCH_PERIOD, "DAILY");

        Boolean Intelligent = Configuration.IsIntelligentRecordingDisabled();
        database.setRecordData(KEY_IR, Intelligent.toString());

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "create: Created user " + user);
        return true;
    }

    boolean destroy() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "destroy: null Record.");
            return false;
        }

        return database.delete();
    }

    String getPassword() {
        String password = isValid ? database.getRecordData(KEY_PASSWORD) : null;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPassword: Password " + password + ":" + isValid);
        return password;
    }

    void setPassword(String Password) {

        if (!isValid || Password==null || Password.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setPassword: null Password, setting to " + user);
            database.setRecordData(KEY_PASSWORD, user);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setPassword: Password " + Password);
        database.setRecordData(KEY_PASSWORD, Password);
        return;
    }

    String getShowImports() {
        return database.getRecordData(KEY_SHOW_IMPORTS);
    }

    void setShowImports(Boolean Show) {
        setShowImports(Show.toString());
    }

    void setShowImports(String Show) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setShowImports: Invalid record " + user);
            return;
        }

        if (Show==null || Show.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setShowImports: null parameter, setting to true.");
            database.setRecordData(KEY_SHOW_IMPORTS, "true");
            return;
        }

        String setting = Show.equalsIgnoreCase("true") ? "true" : "false";

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setShowImports: Setting to " + setting);
        database.setRecordData(KEY_SHOW_IMPORTS, setting);
        return;
    }

    boolean isShowImports() {
        String show = getShowImports();
        return show==null ? true : show.equalsIgnoreCase("true");
    }

    String getUIContext() {
        return isValid ? database.getRecordData(KEY_UICONTEXT) : null;
    }

    void initializeInDataBase() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "initializeInDatabase: Can't initialize invalid User.");
            return;
        }

        setIntelligentRecordingDisabled(Configuration.IsIntelligentRecordingDisabled());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "initializeInDataBase: Add to Favorites.");
        addToAllFavorites();
        return;
    }

    /*
     *
     */
    boolean removeFromDataBase() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromDatabase: Can't remove invalid User.");
            return false;
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromDataBase: Removing from Favorites.");
        removeFromAllFavorites();

        Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromDataBase: Removing from MediaFiles.");
        removeFromAllMediaFiles();

        Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromDataBase: Removing from Airings.");
        removeFromAllAirings();

        return destroy();
    }

    String getUserID() {
        return user;
    }

    void setWatching(Object MediaFile) {
        Object MF = null;

        if (sagex.api.AiringAPI.IsAiringObject(MediaFile)) {
            MF = sagex.api.AiringAPI.GetMediaFileForAiring(MediaFile);
        } else  {
            MF = MediaFile;
        }

        if (MF==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setWatching: null MediaFile.");
            return;
        }

        Integer ID = sagex.api.MediaFileAPI.GetMediaFileID(MF);
        database.setRecordData(KEY_WATCHING, ID.toString());
        return;
    }

    void clearWatching() {
        database.setRecordData(KEY_WATCHING, null);
    }

    String getWatching() {
        return database.getRecordData(KEY_WATCHING);
    }


    void setPrimaryWatch() {
        database.setRecordData(KEY_PRIMARY_WATCH, "true");
    }

    void clearPrimaryWatch() {
        database.setRecordData(KEY_PRIMARY_WATCH, null);
    }

    boolean isPrimaryWatch() {
        String primary = database.getRecordData(KEY_PRIMARY_WATCH);
        return primary!=null && primary.equalsIgnoreCase("true");
    }


    /*
     * Watch times.
     */

    enum WatchPeriods {
        DAILY, WEEKLY, MONTHLY
    }

    public static List<String> getWatchPeriods() {
        WatchPeriods[] periods = WatchPeriods.values();
        List<String> theList = new ArrayList<String>();

        for (WatchPeriods p : periods)
            theList.add(p.toString());

        return theList;
    }

    String getWatchPeriod() {
        return database.getRecordData(KEY_WATCH_PERIOD);
    }

    void setWatchPeriod(String period) {
        if (period==null || period.isEmpty())
            return;

        database.setRecordData(KEY_WATCH_PERIOD, period);
    }

    long getWatchTime() {
        String watchTimeString = database.getRecordData(KEY_WATCH_TIME);

        if (watchTimeString==null || watchTimeString.isEmpty())
            return 0;

        try {
            long watchTime = Long.parseLong(watchTimeString);
            return watchTime;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getWatchTime: Invalid WatchTime " + watchTimeString);
            database.setRecordData(KEY_WATCH_TIME, "0");
            return 0;
        }
    }

    void setWatchTime(Long watchTime) {
        if (watchTime==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "setWatchTime: null WatchTime.");
            return;
        }

        database.setRecordData(KEY_WATCH_TIME, watchTime.toString());
    }

    void addWatchTime(Long watchTime) {
        if (watchTime==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "addWatchTime: null WatchTime.");
            return;
        }

        setWatchTime(getWatchTime() + watchTime);
    }

    long getWatchLimit() {
        String watchLimitString = database.getRecordData(KEY_WATCH_LIMIT);

        if (watchLimitString==null || watchLimitString.isEmpty())
            return 0;

        try {
            long watchLimit = Long.parseLong(watchLimitString);
            return watchLimit;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getWatchLimit: Invalid WatchLimit " + watchLimitString);
            database.setRecordData(KEY_WATCH_LIMIT, "0");
            return 0;
        }
    }

    void setWatchLimit(Long watchLimit) {
        if (watchLimit==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "setWatchLimit: null WatchTime.");
            return;
        }

        database.setRecordData(KEY_WATCH_LIMIT, watchLimit.toString());
    }

    boolean isOverWatchLimit() {
        long limit = getWatchLimit();
        if (limit==0)
            return false;
        else
            return getWatchTime() >= limit;
    }


    /*
     * MediaFiles.
     */
    void addToMediaFile(Object MediaFile) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addToMediaFile: Can't add invalid User.");
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(user, MediaFile);
        MMF.initializeCurrentUser();
        return;
    }

    void removeFromMediaFile(Object MediaFile) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromMediaFile: Can't remove invalid User.");
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(user, MediaFile);
        MMF.clearUserFromFlags();
        return;
    }

    void removeFromAllMediaFiles() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromAllMediaFiles: Can't remove invalid User.");
            return;
        }

        Object[] AllMediaFiles = sagex.api.MediaFileAPI.GetMediaFiles();

        if (AllMediaFiles==null || AllMediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: No MediaFiles.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: Removing from MediaFiles " + AllMediaFiles.length);
            for (Object MediaFile : AllMediaFiles)
                removeFromMediaFile(MediaFile);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllMediaFiles: Done.");
        return;
    }

    void addToAllMediaFiles() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addToAllMediaFiles: Can't add invalid User.");
            return;
        }

        Object[] AllMediaFiles = sagex.api.MediaFileAPI.GetMediaFiles();

        if (AllMediaFiles==null || AllMediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: No MediaFiles.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: Adding to MediaFiles " + AllMediaFiles.length);
            for (Object MediaFile : AllMediaFiles)
                addToMediaFile(MediaFile);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: Done.");
        return;
    }


    /*
     * Airings.
     */
    void addToAiring(Object Airing) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addToAiring: Can't add invalid User.");
            return;
        }

        MultiAiring MA = new MultiAiring(user, Airing);
        MA.initializeCurrentUser();
    }

    void removeFromAiring(Object Airing) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromAiring: Can't remove invalid User.");
            return;
        }

        MultiAiring MA = new MultiAiring(user, Airing);
        MA.clearUserFromFlags();
    }

    void removeFromAllAirings() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromAllAirings: Can't remove invalid User.");
            return;
        }

        Object[] AllAirings = MultiAiring.getAllSageAirings();

        if (AllAirings==null || AllAirings.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllAirings: No Airings.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllAirings: Removing from Airings " + AllAirings.length);
            for (Object Airing : AllAirings)
                removeFromAiring(Airing);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllAirings: Done.");
        return;
    }

    void addToAllAirings() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addToAllairings: Can't add invalid User.");
            return;
        }

        Object[] AllAirings = MultiAiring.getAllSageAirings();

        if (AllAirings==null || AllAirings.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllAirings: No Airings.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllAirings: Adding to Airings " + AllAirings.length);
            for (Object Airing : AllAirings)
                addToAiring(Airing);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllAirings: Done.");
        return;
    }


    /*
     * Favorites.
     */
    void addToAllFavorites() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addToFavorites: Can't add invalid User.");
            return;
        }

        Object[] Favorites = sagex.api.FavoriteAPI.GetFavorites();

        if (Favorites==null || Favorites.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addToAllFavories: No Favorites.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addToAllFavories: Found Favorites " + Favorites.length);
        for (Object Favorite : Favorites) {
            MultiFavorite MF = new MultiFavorite(user, Favorite);
            MF.addFavorite();
        }
    }

    void removeFromAllFavorites() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFromAllFavorites: Can't remove invalid User.");
            return;
        }

        Object[] Favorites = sagex.api.FavoriteAPI.GetFavorites();

        if (Favorites==null || Favorites.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeAllFavories: No Favorites.");
            return;
        }

        for (Object Favorite : Favorites) {
            MultiFavorite MF = new MultiFavorite(user, Favorite);
            MF.removeFavorite();
        }
    }


    /*
     * Configuration.
     */
    boolean isIntelligentRecordingDisabled() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "isIntelligentRecordingDisabled: Invalid User.");
            return Configuration.IsIntelligentRecordingDisabled();
        }

        return (database.getRecordData(KEY_IR).toString().equalsIgnoreCase("true") ? true : false);
    }

    void setIntelligentRecordingDisabled(boolean value) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setIntelligentRecordingDisabled: Can't set invalid User.");
            return;
        }

        Boolean Value = value;
        database.setRecordData(KEY_IR, Value.toString());
        return;
    }


    /*
     * Support methods
     */

    public static List<String> getAllUsers() {
        return DatabaseRecord.getDataFromAllStores(STORE, KEY_USERID);
        /*
        List<String> Users = new ArrayList<String>();

        Object[] Records = UserRecordAPI.GetAllUserRecords(STORE);

        if (Records==null || Records.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllUsers: null Records.");
            return Users;
        }

        for (Object Record : Records) {
            if (Record!=null) {
                String User = UserRecordAPI.GetUserRecordData(Record, KEY_USERID);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getAllUsers: Found User " + User);
                if (User != null && !User.isEmpty())
                    Users.add(User);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllUsers: Found null Record.");
            }
        }

        return Users;
         */
    }

    public static List<String> getAllUsers(boolean includeAdmin) {

        if (includeAdmin)
            return DatabaseRecord.getDataFromAllStores(STORE, KEY_USERID);

        List<String> Users = new ArrayList<String>();

        for (String User : DatabaseRecord.getDataFromAllStores(STORE, KEY_USERID)) {
            if (!User.equalsIgnoreCase(Plugin.SUPER_USER)) {
                Users.add(User);
            }
        }

        return Users;

        /*
        List<String> Users = new ArrayList<String>();

        Object[] Records = UserRecordAPI.GetAllUserRecords(STORE);

        if (Records==null || Records.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllUsers: null Records.");
            return Users;
        }

        for (Object Record : Records) {
            String User = UserRecordAPI.GetUserRecordData(Record, KEY_USERID);
            Log.getInstance().write(Log.LOGLEVEL_ALL, "getAllUsers: Found User " + User);
            if (User != null && !User.isEmpty())
                if (!(!includeAdmin && User.equalsIgnoreCase(Plugin.SUPER_USER)))
                    Users.add(User);
        }

        return Users;
         *
         */
    }

    static boolean isIntelligentRecordingEnabledForAnyUsers() {
        List<String> allUsers = getAllUsers();

        if (allUsers==null || allUsers.isEmpty())
            return false;

        for (String user : allUsers) {
            User U = new User(user);
            if (!U.isIntelligentRecordingDisabled())
                return true;
        }

        return false;
    }

    static void disableIntelligentRecordingForAllUsers() {
        List<String> allUsers = getAllUsers();

        if (allUsers==null || allUsers.isEmpty())
            return;

        for (String user : allUsers) {
            User U = new User(user);
            U.setIntelligentRecordingDisabled(true);
        }
    }

    static void wipeDatabase() {
        DatabaseRecord.wipeAllRecords(STORE);
        //Object[] AllUserRecords = UserRecordAPI.GetAllUserRecords(User.STORE);
        //Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: Begin wipe of User Store " + AllUserRecords.length);
        //for (Object Record : AllUserRecords)
            //UserRecordAPI.DeleteUserRecord(Record);
        //Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: DataStore wiped.");
    }

    static String getUserForContext(String UIContextName) {
        List<String> UserIDs = getAllUsers();

        for (String UserID : UserIDs) {
            User user = new User(UserID);
            String context = user.getUIContext();
            if (context!=null && context.equalsIgnoreCase(UIContextName)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserForContext: Found user " + UserID);
                return UserID;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserForContext: No userID found for context " + UIContextName);
        return null;
    }

    static List<String> getUsersWatchingID(Integer ID) {
        return getUsersWatchingID(ID.toString());
    }

    static List<String> getUsersWatchingID(String ID) {

        //List<String> UserIDs = getAllUsers();
        List<String> usersWatching = new ArrayList<String>();

        for (String UserID : getAllUsers()) {
            User user = new User(UserID);
            String ThisID = user.getWatching();
            if (ThisID!=null && ThisID.equalsIgnoreCase(ID)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserWatchingID: Found user " + UserID);
                usersWatching.add(UserID);
            }
        }

        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserForContext: No userID found for ID " + ID);
        return usersWatching;
    }
}
