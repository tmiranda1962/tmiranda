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

    private static final String KEY           = "Key";
    private static final String KEY_USERID    = "UserID";
    private static final String KEY_PASSWORD  = "Password";
    private static final String KEY_IR        = "IntelligentRecording";
    private static final String KEY_SHOW_IMPORTS = "ShowImportedVideos";
    private static final String KEY_UICONTEXT = "UIContext";
    private static final String KEY_WATCHING  = "Watching";

    private String  user    = null;
    private Object  record  = null;
    private boolean isValid = true;
    //private String  UIContext = null;

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
        record = UserRecordAPI.GetUserRecord(STORE, user);
/*
        UIContext = Global.GetUIContextName();

        if (UIContext==null || UIContext.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "User: null UIContextName.");
            isValid = false;
        }
 * 
 */

        if (record==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "User: null record.");
        }
    }

    void logOn() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOn: Logged on user " + user);
        SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, user);
        //SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_CONTEXT_NAME, Global.GetUIContextName(UIContext.getCurrentContext()));
    }

    void logOff() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOff: Logged off user " + user);
        SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
        //SageUtil.setUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_CONTEXT_NAME, null);
    }

    boolean exists() {
        return isValid && record != null;
    }

    boolean create(String Password) {
        if (!isValid || Password==null || Password.isEmpty())
            return false;

        // Delete the old Record if it exists.
        Object OldRecord = UserRecordAPI.GetUserRecord(STORE, user);
        if (OldRecord != null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "create: Removing existing User record.");
            UserRecordAPI.DeleteUserRecord(OldRecord);
        }

        record = UserRecordAPI.AddUserRecord(STORE, user);

        if (record==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "create: null Record." + user);
            return false;
        }

        UserRecordAPI.SetUserRecordData(record, KEY, user);
        UserRecordAPI.SetUserRecordData(record, KEY_USERID, user);
        UserRecordAPI.SetUserRecordData(record, KEY_PASSWORD, Password);
        UserRecordAPI.SetUserRecordData(record, KEY_SHOW_IMPORTS, "true");
        
        Boolean Intelligent = Configuration.IsIntelligentRecordingDisabled();
        UserRecordAPI.SetUserRecordData(record, KEY_IR, Intelligent.toString());

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "create: Created user " + user);
        return true;
    }

    boolean destroy() {

        if (!isValid || record==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "destroy: null Record.");
            return false;
        }

        return UserRecordAPI.DeleteUserRecord(record);
    }

    String getPassword() {
        String password = isValid ? UserRecordAPI.GetUserRecordData(record, KEY_PASSWORD) : null;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPassword: Password " + password + ":" + isValid);
        return password;
    }

    void setPassword(String Password) {

        if (!isValid || Password==null || Password.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setPassword: null Password, setting to " + user);
            UserRecordAPI.SetUserRecordData(record, KEY_PASSWORD, user);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setPassword: Password " + Password);
        UserRecordAPI.SetUserRecordData(record, KEY_PASSWORD, Password);
        return;
    }

    String getShowImports() {
        return UserRecordAPI.GetUserRecordData(record, KEY_SHOW_IMPORTS);
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
            UserRecordAPI.SetUserRecordData(record, KEY_SHOW_IMPORTS, "true");
            return;
        }

        String setting = Show.equalsIgnoreCase("true") ? "true" : "false";

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setShowImports: Setting to " + setting);
        UserRecordAPI.SetUserRecordData(record, KEY_SHOW_IMPORTS, setting);
        return;
    }

    boolean isShowImports() {
        String show = getShowImports();
        return show==null ? true : show.equalsIgnoreCase("true");
    }

    String getUIContext() {
        return isValid ? UserRecordAPI.GetUserRecordData(record, KEY_UICONTEXT) : null;
    }

    private void setUIContext() {
        String UIContextName = Global.GetUIContextName();

        if (!isValid || UIContextName==null || UIContextName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setUIContext: null UIContextName.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setUIContext: UIContext for user is " + UIContextName);
        UserRecordAPI.SetUserRecordData(record, KEY_UICONTEXT, UIContextName);
        return;
    }
    
    void initializeInDataBase() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "initializeINDatabase: Can't initialize invalid User.");
            return;
        }

        setIntelligentRecordingDisabled(Configuration.IsIntelligentRecordingDisabled());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "initializeInDataBase: Add to Favorites.");
        addToAllFavorites();
        return;
    }

    /**
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

        if (AiringAPI.IsAiringObject(MediaFile)) {
            MF = AiringAPI.GetMediaFileForAiring(MediaFile);
        } else  {
            MF = MediaFile;
        }

        if (MF==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setWatching: null MediaFile.");
            return;
        }

        Integer ID = MediaFileAPI.GetMediaFileID(MF);
        UserRecordAPI.SetUserRecordData(record, KEY_WATCHING, ID.toString());
        return;
    }

    void clearWatching() {
        UserRecordAPI.SetUserRecordData(record, KEY_WATCHING, null);
    }

    String getWatching() {
        return UserRecordAPI.GetUserRecordData(record, KEY_WATCHING);
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
        MMF.initializeUser();
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

        Object[] AllMediaFiles = MediaFileAPI.GetMediaFiles();

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

        Object[] AllMediaFiles = MediaFileAPI.GetMediaFiles();

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
        MA.initializeUser();
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

        Object[] Favorites = FavoriteAPI.GetFavorites();

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

        Object[] Favorites = FavoriteAPI.GetFavorites();

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

        return (UserRecordAPI.GetUserRecordData(record, KEY_IR).toString().equalsIgnoreCase("true") ? true : false);
    }

    void setIntelligentRecordingDisabled(boolean value) {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setIntelligentRecordingDisabled: Can't set invalid User.");
            return;
        }

        Boolean Value = value;
        UserRecordAPI.SetUserRecordData(record, KEY_IR, Value.toString());
        return;
    }


    /*
     * Support methods
     */

    public static List<String> getAllUsers() {
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
    }

    public static List<String> getAllUsers(boolean includeAdmin) {
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

        Object[] AllUserRecords = UserRecordAPI.GetAllUserRecords(User.STORE);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: Begin wipe of User Store " + AllUserRecords.length);
        for (Object Record : AllUserRecords)
            UserRecordAPI.DeleteUserRecord(Record);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "wipeDatabase: DataStore wiped.");
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

    static String getUserWatchingID(Integer ID) {
        return getUserWatchingID(ID.toString());
    }

    static String getUserWatchingID(String ID) {

        List<String> UserIDs = getAllUsers();

        for (String UserID : UserIDs) {
            User user = new User(UserID);
            String ThisID = user.getWatching();
            if (ThisID!=null && ThisID.equalsIgnoreCase(ID)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserWatchingID: Found user " + UserID);
                return UserID;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getUserForContext: No userID found for ID " + ID);
        return null;
    }
}
