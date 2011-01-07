/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import java.io.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class API {

    /*
     * User login and logout.
     *
     * General behavour:
     * - Admin user accesses underlying Sage core.
     * - null user access underlying Sage core, but can't do admin functions.
     * - How to handle recording conflicts? Should probably allow user to control only the recordings they
     *   own but that will be hard.  Allow user to control all recordings will be easier.
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

        if (getLoggedinUser()==null)
            return;

        User user = new User(getLoggedinUser());
        user.logOff();
        //String UIContext = Global.GetUIContextName();
        //LoggedOnUserMap.remove(UIContext);
        //Configuration.SetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "logoutUser: User has been logged out of UIContext " + UIContext);
    }

    public static String getLoggedinUser() {
        return Configuration.GetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    public static String getUserAfterReboot() {
        if (SageUtil.GetLocalBoolProperty(Plugin.PROPERTY_LOGIN_LAST_USER, "false"))
            return getLoggedinUser();
        else
            return null;
    }


    /*
     * MediaFile API.
     */

    // Instead of creating GetMediaFiles() API, surround the core call by FilterByBoolMethod().
    public static boolean isMediaFileForLoggedOnUser(Object MediaFile) {
        String UserID = getLoggedinUser();

        if (UserID == null || UserID.equalsIgnoreCase(Plugin.SUPER_USER) || MediaFile==null)
            return true;

        MultiMediaFile MMF = new MultiMediaFile(UserID, ensureIsMediaFile(MediaFile));
        return !MMF.isDeleted();
    }

    // Use IN PLACE IF core API.
    public static Object addMediaFile(File file, String Prefix) {

        String User = getLoggedinUser();

        Object MediaFile = MediaFileAPI.AddMediaFile(file, Prefix);

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER) || file==null || Prefix==null || MediaFile==null)
            return MediaFile;

        User user = new User(User);
        user.addToMediaFile(MediaFile);
        return MediaFile;
    }

    // Use IN PLACE OF core API. (IsLibraryFile)
    public static boolean isArchived(Object MediaFile) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return MediaFileAPI.IsLibraryFile(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(User, ensureIsMediaFile(MediaFile));
        return MMF.isArchived();
    }

    // Use IN PLACE OF core API. (MoveTVFileOutOfLibrary)
    public static void clearArchived(Object MediaFile) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            MediaFileAPI.MoveTVFileOutOfLibrary(MediaFile);
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(User, ensureIsMediaFile(MediaFile));
        MMF.clearArchived();
        return;
    }

    // Use IN PLACE OF core API. (MoveFileToLibrary)
    public static void setArchived(Object MediaFile) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            MediaFileAPI.MoveFileToLibrary(MediaFile);
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(User, ensureIsMediaFile(MediaFile));
        MMF.setArchived();
        return;
    }

    // Invoke this IN PLACE OF DeleteMediaFile().
    public static boolean deleteMediaFile(Object MediaFile) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return MediaFileAPI.DeleteFile(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), ensureIsMediaFile(MediaFile));
        return MMF.delete(false);
    }

    // Invoke this IN PLACE OF DeleteMediaFileWithoutPrejudice().
    public static boolean deleteMediaFileWithoutPrejudice(Object MediaFile) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return MediaFileAPI.DeleteFileWithoutPrejudice(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(User, ensureIsMediaFile(MediaFile));
        return MMF.delete(true);
    }

    // Not used in default STV, placeholder.
    private static Object getMediaFileForID(int ID) {
        return null;
    }

    // Not implemented, placeholder.
    private static boolean isFileCurrentlyRecording(Object MediaFile) {
        return false;
    }


    /*
     * Configuration API.
     *
     * Behavour:
     * - Does not alter core.
     */

    // Invoke IN PLACE OF core API.
    public static boolean isIntelligentRecordingDisabled() {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return Configuration.IsIntelligentRecordingDisabled();
        }

        User user = new User(User);
        return user.isIntelligentRecordingDisabled();
    }

    // Invoke IN PLACE OF core API.
    public static void setIntelligentRecordingDisabled(boolean value) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Configuration.SetIntelligentRecordingDisabled(value);
            return;
        }

        User user = new User(User);
        user.setIntelligentRecordingDisabled(value);
        return;
    }


    /*
     * Airing API.
     *
     * Behavour:
     * - Watched not implemented at all.
     */

    // Invoke IN PLACE OF core API.
    public static Object record(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.Record(Airing);
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.setManualRecord();
        return AiringAPI.Record(Airing);
    }

    // Invoke IN PLACE OF core API.
    public static Object setRecordingTimes(Object Airing, long StartTime, long StopTime) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.SetRecordingTimes(Airing, StartTime, StopTime);
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.setRecordingTimes(StartTime, StopTime);
        return AiringAPI.SetRecordingTimes(Airing, StartTime, StopTime);
    }

    // Invoke IN PLACE OF core API.
    public static boolean isManualRecord(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.IsManualRecord(Airing);
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        return MA.isManualRecord();
    }

    // Invole IN PLACE OF core API.
    public static void cancelRecord(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.CancelRecord(Airing);
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.cancelManualRecord();
        return;
    }

    // Invoke IN PLACE OF core API.
    public static Object getMediaFileForAiring(Object Airing) {

        String User = getLoggedinUser();
        Object MediaFile = null;

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER) || Airing==null) {
            return AiringAPI.GetMediaFileForAiring(Airing);
        }

        MediaFile = AiringAPI.IsAiringObject(Airing) ? AiringAPI.GetMediaFileForAiring(Airing) : Airing;

        if (MediaFile==null)
            return null;

        MultiMediaFile MMF = new MultiMediaFile(User, MediaFile);
        return (!MMF.isDeleted() ? MediaFile : null);
    }

    // Not implemented, placeholder.
    private static Object addAiring() {
        return null;
    }

    // Not implemented, placeholder.
    private static Object addAiringDetailed() {
        return null;
    }

    // Use IN PLACE OF core API.
    public static boolean isDontLike(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER))
            return AiringAPI.IsDontLike(Airing);

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        return MA.isDontLike();
    }

    // Use IN PLACE OF core API.
    public static void setDontLike(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.SetDontLike(Airing);
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.setDontLike();
        return;
    }

    // Use IN PLACE OF core API.
    public static void clearDontLike(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.ClearDontLike(Airing);
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.clearDontLike();
        return;
    }

    // Invoke IN PLACE OF core API.
    public static boolean isFavorite(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.IsFavorite(Airing);
        }

        if (!AiringAPI.IsFavorite(Airing)) {
            return false;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        return MA.isFavorite();
    }

    // Invoke IN PLACE OF core API.
    public static boolean isNotManualOrFavorite(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.IsNotManualOrFavorite(Airing);
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));

        return (!MA.isManualRecord() || MA.isFavorite());
    }

    /*
     * Favorite API.
     *
     * Behavior:
     * - When a user is created the user inherits all Favorites.
     * - Favorites are added and deleted on a per-user basis.
     * - If a Favorite is added by more than one user, all users share the same Favorite.
     */

    //Invoke IN PLACE OF core API.
    public static Object[] getFavorites() {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return FavoriteAPI.GetFavorites();
        } else {
            return MultiFavorite.getFavorites();
        }
    }

    // Invoke IN PLACE OF core API.
    public static void removeFavorite(Object Favorite) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            FavoriteAPI.RemoveFavorite(Favorite);
            return;
        }

        MultiFavorite MF = new MultiFavorite(User, Favorite);
        MF.removeFavorite();
        return;
    }

    // Invoke IN ADDITION TO core API.
    public static void addFavorite(Object Favorite) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        MultiFavorite MF = new MultiFavorite(User, Favorite);
        MF.addFavorite();
        return;
    }

    // Invoke IN PLACE OF core API.
    public static Object getFavoriteForAiring(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return FavoriteAPI.GetFavoriteForAiring(Airing);
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        return MA.getFavoriteForAiring();
    }

    // Not in the default STV, but put here as a placeholder.
    private static int getFavoriteID(Object Favorite) {
        return 0;
    }


    /*
     * User related methods.
     */

    // User exists in the user access database.
    public static boolean userExists(String UserID) {

        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "userExists: null UserID.");
            return false;
        }

        User user = new User(UserID);
        return user.exists();
    }

    // Create user in the user access database.
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

    // remove user from the user access database.
    public static boolean removeUser(String UserID) {
        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeUser: Bad parameters " + UserID);
            return false;
        }

        User user = new User(UserID);
        return user.destroy();
    }

    // Gets the user password from the user access database.
    public static String getUserPassword(String UserID) {
        User user = new User(UserID);
        return user.getPassword();
    }

    // Adds the user the the MFC and MMF.
    public static void addUserToMediaFile(String UserID, Object MediaFile) {
        User user = new User(UserID);
        user.addToMediaFile(MediaFile);
    }

    // Removes the user from the MFC and MMF.
    public static void removeUserFromMediaFile(String UserID, Object MediaFile) {
        User user = new User(UserID);
        user.removeFromMediaFile(MediaFile);
    }

    // Removes all users from the MFC and MMF.
    public static void removeAllUsersFromMediaFile(Object MediaFile) {
        List<String> Users = User.getAllUsers();

        for (String User : Users) {
            removeUserFromMediaFile(User, MediaFile);
        }
    }

    // Returns all users defined in the database.
    public static List<String> getAllDefinedUsers() {
        return User.getAllUsers();
    }

    // Removes all of the MFC, MMF and MF flags.
    public static void removeUserFromDatabase(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeUserFromAllMediaFiles: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.removeFromDataBase();
        return;
    }

    // Adds all of the MFC, MMC and MF flags.
    public static void addUserToDatabase(String UserID) {

        if (UserID==null || UserID.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUserToAllMediaFiles: null UserID.");
            return;
        }

        User user = new User(UserID);
        user.initializeInDataBase();

        return;
    }

    // Wipes the User DataStore while leaving the others intact.
    public static void clearUserDatabase() {
        UserRecordAPI.DeleteAllUserRecords(User.STORE);
    }
  
    public static void resetMediaFileDatabase() {
        List<String> AllUsers = User.getAllUsers();
        
        for (String User : AllUsers) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetMediaFileDatabase: Removing " + User);
            removeUserFromDatabase(User);

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetMediaFileDatabase: Adding " + User);
            addUserToDatabase(User);
        }
    }

    /*
     * Database Maintenance.
     */

    // Wipe the DataStore cmpletely.
    public static void clearAll() {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping the entire database.");
        MultiFavorite.WipeDatabase();
        MultiMediaFile.WipeDatabase();
        MultiAiring.WipeDatabase();
        User.wipeDatabase();
        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping complete.");
    }

    public static int getUserStoreSize() {
        return getStoreSize(User.STORE);
    }

    public static int getFavoriteStoreSize() {
        return getStoreSize(MultiFavorite.FAVORITE_STORE);
    }

    public static int getMediaFileStoreSize() {
        return getStoreSize(MultiMediaFile.MEDIAFILE_STORE);
    }

    public static int getAiringStoreSize() {
        return getStoreSize(MultiAiring.AIRING_STORE);
    }
    
    public static int getStoreSize(String Store) {
        if (Store==null || Store.isEmpty())
            return 0;
        else
            return UserRecordAPI.GetAllUserRecords(Store).length;
    }

    /*
     * Support methods.
     */
    private static Object ensureIsAiring(Object SageObject) {
        if (AiringAPI.IsAiringObject(SageObject)) {
            return SageObject;
        } else if (MediaFileAPI.IsMediaFileObject(SageObject)) {
            return MediaFileAPI.GetMediaFileAiring(SageObject);
        } else {
            if (ShowAPI.IsShowObject(SageObject))
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ensureIsAiring: Found a Show.");
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ensureIsAiring: Found unknown Object.");
            return SageObject;
        }
    }

    private static Object ensureIsMediaFile(Object SageObject) {
        if (MediaFileAPI.IsMediaFileObject(SageObject)) {
            return SageObject;
        } else if (AiringAPI.IsAiringObject(SageObject)) {
            return AiringAPI.GetMediaFileForAiring(SageObject);
        } else {
            if (ShowAPI.IsShowObject(SageObject))
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ensureIsMediaFile: Found a Show.");
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ensureIsMediaFile: Found unknown Object.");
            return SageObject;
        }
    }

    /*
     * Debug stuff.
     */
    public static List<String> getFlagsForMediaFile(Object MediaFile) {
        List<String> TheList = new ArrayList<String>();

        if (MediaFile==null) {
            return TheList;
        }

        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {

            MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);

            for (String Flag : MultiMediaFile.FLAGS)
                TheList.add(Flag + "=" + MMF.getFlagString(Flag));

        } else {
            MultiAiring MA = new MultiAiring(getLoggedinUser(), MediaFile);

            for (String Flag : MultiAiring.FLAGS)
                TheList.add(Flag + "=" + MA.getFlagString(Flag));
        }

        Object Favorite = FavoriteAPI.GetFavoriteForAiring(MediaFile);

        if (Favorite!=null) {
            MultiFavorite MF = new MultiFavorite(getLoggedinUser(), Favorite);
            for (String Flag : MultiFavorite.FLAGS)
                TheList.add(Flag + "=" + MF.getFlagString(Flag));
        }

        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.DURATION_PREFIX));
        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.MEDIATIME_PREFIX));
        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.CHAPTERNUM_PREFIX));
        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.TITLENUM_PREFIX));

        Boolean IsArchived = MediaFileAPI.IsLibraryFile(MediaFile);
        Boolean DontLike = AiringAPI.IsDontLike(MediaFile);
        Boolean Manual = AiringAPI.IsManualRecord(MediaFile);
        Boolean IsFavorite = AiringAPI.IsFavorite(MediaFile);
        TheList.add("Core: Archived=" + IsArchived.toString() + " DontLike=" + DontLike.toString() + " Manual=" + Manual.toString() + " Favorite=" + IsFavorite.toString());

        return TheList;
    }
}
