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
public class API {

    //private static Map<String,String> LoggedOnUserMap = new HashMap<String, String>();

    /**
     * Return the ID of the last user that was logged on. Returns null if config options are set to
     * forget the last logged on user.
     *
     * @return
     */
    public static String getLastLoggedOnUser() {
        return Configuration.GetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    /**
     * Log in the specified User.
     * @param UserID
     */
    public static void loginUser(String UserID) {
        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "loginUser: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.logOn();
        //String UIContext = Global.GetUIContextName();
        //LoggedOnUserMap.put(UIContext, UserID);
        //Configuration.SetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, UserID);
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "loginUser: Set logged in user to " + UserID);
    }

    public static void logoutCurrentUser() {

        User user = new User(getLastLoggedOnUser());
        user.logOff();
        //String UIContext = Global.GetUIContextName();
        //LoggedOnUserMap.remove(UIContext);
        //Configuration.SetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "logoutUser: User has been logged out of UIContext " + UIContext);
    }

    public static String getLoggedinUser() {
        return Configuration.GetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    public static boolean isMediaFileForLoggedOnUser(Object MediaFile) {
        String UserID = getLoggedinUser();

        if (UserID == null) {
            return false;
        }

        if (UserID.equalsIgnoreCase(Plugin.SUPER_USER))
            return true;

        // Why do this?
        //String UIContext = Global.GetUIContextName();
        //UserID = LoggedOnUserMap.get(UIContext);
        //Log.getInstance().write(Log.LOGLEVEL_ALL, "isMediaFileForLoggedOnUser: Checking UserID " + UIContext + ":" + UserID);
        
        MultiMediaFile MMF = new MultiMediaFile(UserID, MediaFile);
        return !MMF.isDeleted() && MMF.isUserAllowed(UserID);
    }

    public static String getUserAfterReboot() {
        if (SageUtil.GetLocalBoolProperty(Plugin.PROPERTY_LOGIN_LAST_USER, "false"))
            return getLastLoggedOnUser();
        else
            return null;
    }




    // Use IN PLACE OF core API.
    public static boolean isDontLike(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        return MMF.isDontLike();
    }

    // Use IN PLACE OF core API.
    public static void setDontLike(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        MMF.setDontLike("true");
        return;
    }

    // Use IN PLACE OF core API.
    public static void clearDontLike(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        MMF.setDontLike("false");
        return;
    }

    // Only invoke on MediaFile that has passed isMediaFileForLoggedOnUser filter.
    public static boolean isArchived(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        return MMF.isArchived();
    }

    // Only invoke on MediaFile that has passed isMediaFileForLoggedOnUser filter.
    public static void setArchived(Object MediaFile, String value) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        MMF.setArchived(value);
        return;
    }

    // Invoke IN PLACE OF core API.
    public static boolean isFavorite(Object MediaFile) {

        Object Favorite = FavoriteAPI.GetFavoriteForAiring(MediaFile);

        if (Favorite==null) {
            return false;
        }

        MultiFavorite MF = new MultiFavorite(getLoggedinUser(), MediaFile);
        return MF.isFavorite();
    }

    // Invoke IN PLACE OF core API.
    public static boolean isNotManualOrFavorite(Object MediaFile) {
        return (!(AiringAPI.IsManualRecord(MediaFile) || isFavorite(MediaFile)));
    }

    // Invoke IN ADDITION TO core API.
    public static void addFavorite(Object Favorite) {
        MultiFavorite MF = new MultiFavorite(getLoggedinUser(), Favorite);
        MF.addFavorite();
        return;
    }

    // Invoke IN PLACE OF core API.
    public static void removeFavorite(Object Favorite) {
        MultiFavorite MF = new MultiFavorite(getLoggedinUser(), Favorite);
        MF.removeFavorite();
        return;
    }

    // Invoke this IN PLACE OF DeleteMediaFile().
    public static boolean deleteMediaFile(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        return MMF.delete(false);
    }

    // Invoke this IN PLACE OF DeleteMediaFileWithoutPrejudice().
    public static boolean deleteMediaFileWithoutPrejudice(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        return MMF.delete(true);
    }

    // Use IN PLACE OF core API.
    public static void setWatched(Object MediaFile) {
        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
        MMF.setWatched("true");
        return;
    }


    /*
     * User related methods.
     */
    public static boolean userExists(String UserID) {

        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "userExists: null UserID.");
            return false;
        }

        User user = new User(UserID);
        return user.exists();
    }

    public static boolean createNewUser(String UserID, String Password) {
        if (UserID==null || Password==null || UserID.isEmpty() || Password.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "createNewUser: Bad parameters " + UserID + ":" + Password);
            return false;
        }

        User user = new User(UserID);

        if (user.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "createNewUser: User already exists " + UserID);
            return false;
        }

        return user.create(Password);
    }

    public static boolean removeUser(String UserID) {
        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeUser: Bad parameters " + UserID);
            return false;
        }

        User user = new User(UserID);
        return user.destroy();
    }

    public static String getUserPassword(String UserID) {
        User user = new User(UserID);
        return user.getPassword();
    }

    public static void addUserToMediaFile(String UserID, Object MediaFile) {
        User user = new User(UserID);
        user.addToMediaFile(MediaFile);
    }

    public static void removeUserFromMediaFile(String UserID, Object MediaFile) {
        User user = new User(UserID);
        user.removeFromMediaFile(MediaFile);
    }

    public static void removeAllUsersFromMediaFile(Object MediaFile) {
        MediaFileControl MFC = new MediaFileControl(MediaFile);
        MFC.removeAllUsers();
    }

    public static List<String> getUsersForMediaFile(Object MediaFile) {
        MediaFileControl MFC = new MediaFileControl(MediaFile);
        return MFC.getUserList();
    }

    public static List<String> getAllDefinedUsers() {
        return User.getAllUsers();
    }

    public static void removeUserFromAllMediaFiles(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllMediaFiles: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.removeFromAllMediaFiles();
        return;
    }

    public static void addUserToAllMediaFiles(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.addToAllMediaFiles();

        return;
    }


    public static void clearUserDatabase() {
        UserRecordAPI.DeleteAllUserRecords(Plugin.STORE_RECORD_KEY);
    }

    public static void clearMediaFileDatabase() {

        Object[] MediaFiles = MediaFileAPI.GetMediaFiles();

        if (MediaFiles == null || MediaFiles.length==0)
            return;

        for (Object MediaFile : MediaFiles)
            removeAllUsersFromMediaFile(MediaFile);
    }
}
