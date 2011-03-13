package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class UserAPI {
    /*
     * User login and logout.
     *
     * General behavour:
     * - Admin user accesses underlying Sage core.
     * - null user access underlying Sage core, but can't do admin functions.
     */


    /**
     * Logs on the specified user.
     * @param UserID
     */
    public static void loginUser(String UserID) {

        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "loginUser: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.logOn();
    }

    /**
     * Logs out the current user.
     */
    public static void logoutCurrentUser() {

        if (getLoggedinUser()==null)
            return;

        User user = new User(getLoggedinUser());
        user.logOff();
    }

    /**
     * Returns the currently logged on user.
     * @return The currently logged on user.
     */
    public static String getLoggedinUser() {
        return SageUtil.getUIProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    /**
     * Returns the user that should be logged on after Sage is rebooted.  It may be null
     * indicating that no user should be logged on.
     * @return The user that should be logged in after the UI is reloaded.
     */
    public static String getUserAfterReboot() {
        if (SageUtil.GetLocalBoolProperty(Plugin.PROPERTY_LOGIN_LAST_USER, "false")) {
            String userID = getLoggedinUser();

            if (userID==null || userID.isEmpty())
                return null;

            User user = new User(userID);

            if (user.exists()) {
                return userID;
            } else {
                logoutCurrentUser();
                return null;
            }
        }  else
            return null;
    }

    /**
     * Check to see if a user exists in the database.
     * @param UserID
     * @return true if the user exists in the database, false otherwise.
     */
    public static boolean userExists(String UserID) {

        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "userExists: null UserID.");
            return false;
        }

        User user = new User(UserID);
        return user.exists();
    }

    /**
     * Creates a user in the user database.  Does NOT initialize the user information in the
     * other databases, use addUserToDatabase() for that.
     * @param UserID The userID must be unique in the system.
     * @param Password The password can't be null.
     * @return true for success, false otherwise.
     */
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

    /**
     * Removes a user from the user database.  Does NOT remove the user information from
     * the other databases, use removeUserFromDatabase()for that.
     * @param UserID
     * @return true if success, false otherwise.
     */
    public static boolean removeUser(String UserID) {
        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeUser: Bad parameters " + UserID);
            return false;
        }

        User user = new User(UserID);
        return user.destroy();
    }

    /**
     * Retrieves the user password from the database.  The returned password is not encrypted
     * in any way.
     * @param UserID
     * @return The user password.
     */
    public static String getUserPassword(String UserID) {
        User user = new User(UserID);
        return user.getPassword();
    }

    /**
     * Initializes user access to the MediaFile or Airing. The user's view of the object
     * (Watched, Like/Don't Like, viewing times, etc) will initially mirror the core.
     * @param UserID The user to initialize.
     * @param MediaFileOrAiring The MediaFile or Airing to initialize.
     */
    public static void addUserToMediaFile(String UserID, Object MediaFileOrAiring) {

        if (UserID==null || MediaFileOrAiring==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addUserToMediaFile: null User or MediaFile " + UserID);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "addUserToMediaFile: Adding User to MediaFile " + UserID + ":" + sagex.api.MediaFileAPI.GetMediaTitle(MediaFileOrAiring));

        User user = new User(UserID);

        Object Airing = null;
        Object MediaFile = null;

        if (sagex.api.AiringAPI.IsAiringObject(MediaFileOrAiring)) {
            Airing = MediaFileOrAiring;
            MediaFile = sagex.api.AiringAPI.GetMediaFileForAiring(MediaFileOrAiring);
        } else {
            MediaFile = MediaFileOrAiring;
            Airing = sagex.api.MediaFileAPI.GetMediaFileAiring(MediaFileOrAiring);
        }

        user.addToMediaFile(MediaFile);
        user.addToAiring(Airing);
    }

    /**
     * Removes all user access data from the specified MediaFile (or Airing).
     * @param UserID
     * @param MediaFile
     */
    public static void removeUserFromMediaFile(String UserID, Object MediaFile) {
        User user = new User(UserID);
        user.removeFromMediaFile(MediaFile);
    }

    /**
     * Removes the information of all users from the MediaFile or Airing.
     */
    public static void removeAllUsersFromMediaFile(Object MediaFile) {
        List<String> Users = User.getAllUsers();

        for (String User : Users) {
            removeUserFromMediaFile(User, MediaFile);
        }
    }

    /**
     * Get a List of all users in the database including "Admin".
     * @return
     */
    public static List<String> getAllDefinedUsers() {
        return User.getAllUsers();
    }

    /**
     * Get a List of all users in the database, optionally returning "Admin".
     * @param includeAdmin true to include "Admin" in the returned List, false otherwise.
     * @return
     */
    public static List<String> getAllDefinedUsers(boolean includeAdmin) {
        return User.getAllUsers(includeAdmin);
    }

    /**
     * Completely removes all user information from the database.
     * @param UserID
     * @return true if success, false otherwise.
     */
    public static boolean removeUserFromDatabase(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllMediaFiles: null UserID.");
            return false;
        }

        User user = new User(UserID);
        return user.removeFromDataBase();
    }

    /**
     * Initializes the user in the database.
     * @param UserID
     */
    public static void addUserToDatabase(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.initializeInDataBase();

        return;
    }

    /**
     * Removes all user information from the user database, leaves the other databases intact.
     */
    public static void clearUserDatabase() {
        UserRecordAPI.DeleteAllUserRecords(User.STORE);
    }

    /**
     * Resets all user information (Watched, Like/Don't Like, watched times, etc.)
     * in the MediaFile database.
     */
    public static void resetMediaFileDatabase() {
        List<String> AllUsers = User.getAllUsers();

        for (String User : AllUsers) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetMediaFileDatabase: Removing " + User);
            removeUserFromDatabase(User);

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetMediaFileDatabase: Adding " + User);
            addUserToDatabase(User);
        }
    }

    public static boolean isShowImports(String U) {

        if (U==null || U.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return true;
        }

        User user = new User(U);
        return user.isShowImports();
    }

    public static void setShowImports(String U, boolean Show) {

        if (U==null || U.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        User user = new User(U);
        user.setShowImports(Show);
        return;
    }
}
