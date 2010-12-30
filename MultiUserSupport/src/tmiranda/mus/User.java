/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class User {

    private static String KEY_USERID    = "UserID";
    private static String KEY_PASSWORD  = "Password";

    private String user = null;
    private Object record = null;

    public User(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "User: null UserID.");
            return;
        }

        user = UserID;
        record = UserRecordAPI.GetUserRecord(Plugin.STORE_RECORD_KEY, user);

        if (record==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "User: null record.");
        }
    }

    void logOn() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOn: Logged on user " + user);
        Configuration.SetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, user);
    }

    void logOff() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logOff: Logged off user " + user);
        Configuration.SetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    boolean exists() {
        return record != null;
    }

    boolean create(String Password) {
        if (Password==null || Password.isEmpty())
            return false;

        record = UserRecordAPI.AddUserRecord(Plugin.STORE_RECORD_KEY, user);

        if (record==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "create: null Record.");
            return false;
        }

        UserRecordAPI.SetUserRecordData(record, KEY_USERID, user);
        UserRecordAPI.SetUserRecordData(record, KEY_PASSWORD, Password);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "create: Created user " + user);
        return true;
    }

    boolean destroy() {

        if (record==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "destroy: null Record.");
            return false;
        }

        return UserRecordAPI.DeleteUserRecord(record);
    }

    String getPassword() {
        return UserRecordAPI.GetUserRecordData(record, KEY_PASSWORD);
    }

    void setPassword(String Password) {

        if (Password==null || Password.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setPassword: null Password.");
            return;
        }

        UserRecordAPI.SetUserRecordData(record, KEY_PASSWORD, Password);
        return;
    }
    
    void initializeInDataBase() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "initializeDataBase: Add to Favorites.");
        addToAllFavorites();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "initializeDataBase: Add to MediaFiles.");
        addToAllMediaFiles();
    }
    
    void removeFromDataBase() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromDataBase: Removing from Favorites.");
        removeFromAllFavorites();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromDataBase: Removing from MediaFiles.");
        removeFromAllMediaFiles();
    }


    /*
     * MediaFiles, Airings and Shows.
     */
    void addToMediaFile(Object MediaFile) {
        MediaFileControl MFC = new MediaFileControl(MediaFile);
        MFC.addUser(user);
    }

    void removeFromMediaFile(Object MediaFile) {
        MediaFileControl MFC = new MediaFileControl(MediaFile);
        MFC.removeUser(user);
    }

    void removeFromAllMediaFiles() {
        Object[] AllMediaFiles = MediaFileAPI.GetMediaFiles();

        if (AllMediaFiles==null || AllMediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: No MediaFiles.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: Removing from MediaFiles " + AllMediaFiles.length);
            for (Object MediaFile : AllMediaFiles)
                removeFromMediaFile(MediaFile);
        }

        Object[] allSageAirings = MultiAiring.getAllSageAirings();

        if (allSageAirings==null || allSageAirings.length==0) {
             Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: No Airings.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFromAllMediaFiles: Removing from Airings " + allSageAirings.length);
        for (Object Airing : allSageAirings) {
            removeFromMediaFile(Airing);
            Object Show = AiringAPI.GetShow(Airing);
            if (Show != null) {
                removeFromMediaFile(Airing);    
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllMediaFiles: Done.");
        return;
    }

    void addToAllMediaFiles() {
        Object[] AllMediaFiles = MediaFileAPI.GetMediaFiles();

        if (AllMediaFiles==null || AllMediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: No MediaFiles.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: Adding to MediaFiles " + AllMediaFiles.length);
            for (Object MediaFile : AllMediaFiles)
                addToMediaFile(MediaFile);
        }

        Object[] allSageAirings = MultiAiring.getAllSageAirings();

        if (allSageAirings==null || allSageAirings.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: No Airings.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: Adding to Airings " + allSageAirings.length);
        for (Object Airing : allSageAirings) {
            addToMediaFile(Airing);
            Object Show = AiringAPI.GetShow(Airing);
            if (Show != null) {
                addToMediaFile(Airing);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: Done.");
        return;
    }

    /*
     * Favorites.
     */
    void addToAllFavorites() {
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


    static List<String> getAllUsers() {
        List<String> Users = new ArrayList<String>();

        Object[] Records = UserRecordAPI.GetAllUserRecords(Plugin.STORE_RECORD_KEY);

        if (Records==null || Records.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllUsers: null Records.");
            return Users;
        }

        for (Object Record : Records) {
            String User = UserRecordAPI.GetUserRecordData(Record, KEY_USERID);
            Log.getInstance().write(Log.LOGLEVEL_ALL, "getAllUsers: Found User " + User);
            if (User != null && !User.isEmpty())
                Users.add(User);
        }

        return Users;
    }
}
