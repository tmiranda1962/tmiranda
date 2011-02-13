/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

//import com.sun.org.apache.xpath.internal.operations.Equals;
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
    }

    public static String getLoggedinUser() {
        if (Global.IsClient() && Global.IsServerUI())
            return Configuration.GetServerProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
        else
            return Configuration.GetProperty(Plugin.PROPERTY_LAST_LOGGEDIN_USER, null);
    }

    public static String getUserAfterReboot() {
        if (SageUtil.GetLocalBoolProperty(Plugin.PROPERTY_LOGIN_LAST_USER, "false"))
            return getLoggedinUser();
        else
            return null;
    }


    /*
     * Admin methods.
     */

    public static void OLDgiveToUser(Object MediaFile, String User) {
        if (User==null || User.isEmpty() || MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "giveToUser: null parameter " + User);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "giveToUser: Giving access to " + User + ":" + MediaFileAPI.GetMediaTitle(MediaFile));
        User user = new User(User);
        user.addToMediaFile(MediaFile);
        return;
    }

    public static boolean isPluginEnabled() {
        Object thisPlugin = PluginAPI.GetAvailablePluginForID(Plugin.PLUGIN_ID);

        if (thisPlugin==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "isPluginEnabled: Error. null Plugin for ID " + Plugin.PLUGIN_ID);
            return false;
        }

        Object[] installedPlugins = PluginAPI.GetInstalledPlugins();

        if (installedPlugins==null || installedPlugins.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "isPluginEnabled: No Plugins are installed on the server.");
            return false;
        }

        for (Object plugin : installedPlugins)
            if (PluginAPI.GetPluginIdentifier(plugin).equalsIgnoreCase(Plugin.PLUGIN_ID))
                return true;

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "isPluginEnabled: Plugin is not installed on the server.");
        return false;
    }

    public static boolean isUpcomingRecordingForMe(Object Airing) {
        if (Airing==null || !willBeRecordedByCore(Airing))
            return false;

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isUpcomingRecordingForMe: null user or Admin " + User);
            return true;
        }

        return (isFavorite(Airing) || isManualRecord(Airing) || !isIntelligentRecordingDisabled());
    }

    public static boolean willBeRecordedByCore(Object Airing) {

        if (Airing==null)
            return false;

        int AiringID = AiringAPI.GetAiringID(Airing);
        Object[] scheduledAirings = Global.GetScheduledRecordings();

        for (Object airing : scheduledAirings)
            if (MediaFileAPI.IsFileCurrentlyRecording(airing) || (AiringID == AiringAPI.GetAiringID(airing)))
                return true;

        return false;
    }

    public static boolean isUpcomingRecordingForUser(String User, Object Airing) {
        if (!willBeRecordedByCore(Airing)) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isUpcomingRecordingForUser: Core will not record the Airing.");
            return false;
        }

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isUpcomingRecordingForUser: null user or Admin " + User);
            return true;
        }

        MultiAiring MA = new MultiAiring(User, Airing);
        User U = new User(User);

        return (MA.isFavorite() || MA.isManualRecord() || !U.isIntelligentRecordingDisabled());
    }

    public static List<String> getUsersThatWillRecord(Object Airing) {
        List<String> theList = new ArrayList<String>();

        if (Airing == null)
            return theList;

        List<String> allUsers = User.getAllUsers();

        for (String user : allUsers)
            if (isUpcomingRecordingForUser(user, Airing))
                theList.add(user);

        return theList;
    }

    /*
     * MediaPlayer API.
     *
     * WatchLive() is only used in the setup menus so there is no need to implement that.
     */

    public static void preWatch(Object Content) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "preWatch: null user or Admin " + User);
            return;
        }

        // Set flag showing that this user is watching this content. We will need this later
        // when the RecordingStopped Event is received so we can set RealWatchedEndTime and
        // WatchedEndTime for the appropriate user.
        User user = new User(User);
        user.setWatching(Content);

        long WatchedEndTime = 0;
        long RealStartTime = 0;

        if (AiringAPI.IsAiringObject(Content)) {
            MultiAiring MA = new MultiAiring(User, Content);
            MA.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MA.getWatchedEndTime();
            RealStartTime = MA.getRealWatchedStartTime();
        } else if (MediaFileAPI.IsMediaFileObject(Content)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Content);
            MMF.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MMF.getWatchedEndTime();
            RealStartTime = MMF.getRealWatchedStartTime();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "preWatch: Not an Airing or MediaFile.");
            return;
        }

        WatchedEndTime = (WatchedEndTime==-1 ? AiringAPI.GetWatchedEndTime(Content):WatchedEndTime);
        RealStartTime = (RealStartTime==-1 ? AiringAPI.GetRealWatchedStartTime(Content):RealStartTime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "watch: Setting WatchedEndTime and RealStartTime " + Plugin.PrintDateAndTime(WatchedEndTime) + ":" + Plugin.PrintDateAndTime(RealStartTime));

        // Reset the values.
        AiringAPI.ClearWatched(Content);
        AiringAPI.SetWatchedTimes(Content, WatchedEndTime, RealStartTime);

        // Let the core do its thing.
        return;
    }

    public static Object watch(Object Content) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return MediaPlayerAPI.Watch(Content);
        }

        // Set flag showing that this user is watching this content. We will need this later
        // when the RecordingStopped Event is received so we can set RealWatchedEndTime and
        // WatchedEndTime for the appropriate user.
        User user = new User(User);
        user.setWatching(Content);

        long WatchedEndTime = 0;
        long RealStartTime = 0;

        if (AiringAPI.IsAiringObject(Content)) {
            MultiAiring MA = new MultiAiring(User, Content);
            MA.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MA.getWatchedEndTime();
            RealStartTime = MA.getRealWatchedStartTime();
        } else if (MediaFileAPI.IsMediaFileObject(Content)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Content);
            MMF.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MMF.getWatchedEndTime();
            RealStartTime = MMF.getRealWatchedStartTime();
        } else {
            return MediaPlayerAPI.Watch(Content);
        }

        WatchedEndTime = (WatchedEndTime==-1 ? AiringAPI.GetWatchedEndTime(Content):WatchedEndTime);
        RealStartTime = (RealStartTime==-1 ? AiringAPI.GetRealWatchedStartTime(Content):RealStartTime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "watch: Setting WatchedEndTime and RealStartTime " + Plugin.PrintDateAndTime(WatchedEndTime) + ":" + Plugin.PrintDateAndTime(RealStartTime));
        
        // Reset the values.
        AiringAPI.ClearWatched(Content);
        AiringAPI.SetWatchedTimes(Content, WatchedEndTime, RealStartTime);

        // Let the core do its thing.
        return MediaPlayerAPI.Watch(Content);
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

    // Use IN PLACE OF core API.
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
     * - Anything to note?
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

    // Lets the current logged in user mark an upcoming recording as a manual for another user.
    public static void markAsManualRecord(String User, Object Airing) {

        // Nothing to do for null user or Admin.
        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.setManualRecord();
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

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.CancelRecord(Airing);
            MA.cancelManualRecordForAllUsers();
        } else {
            MA.cancelManualRecord();
        }
        
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


    //
    // Thoughts on managing "watched".
    //
    // IsWatchedCompletely only used in one place in the default STV.  I'm not sure of how this is
    // different from IsWatched so I did not implement it and substituted IsWatched.
    //
    // Must keep track of the settings for MediaFile and Airing because not every MediaFile will have an Airing
    // (DVD, BluRay, Imported) and not all Airings will have a MediaFile (deleted).
    //
    // GetRealWatched(Start/End)Time() returns the real time that the user started and stopped watching.
    // Nowhere in the default STV are these methods used.  Is the info used in the core?
    //
    // GetWatchedDuration() returns the time that the item was watched relative to the item.
    //
    // GetWatched(Start/End)Time() returns the time relative to the item that the user started/stopped watching.
    //
    // SetWatchedTimes(Airing, WatchedEndTime, RealStartTime)
    // - WatchedEndTime: Item relative time that the user has watched up to. The core makes the end time the
    //   maximum of this time and the existing end time so we need to reset or clear the time somehow.
    // - RealStartTime: Real time that the user started watching.
    //
    // GetLatestWatchedTime() returns the item relative time that viewing should start. Not used in the default STV.
    //
    // How is padding handled?
    //

    // Use IN PLACE OF core API.
    public static boolean isWatched(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER))
            return AiringAPI.IsWatched(Airing);

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        return MA.isWatched();
    }

    // Use IN PLACE OF core API.
    public static void setWatched(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.SetWatched(Airing);
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.setWatched();
        return;
    }

    // Use IN PLACE OF core API.
    public static void clearWatched(Object Airing) {

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            AiringAPI.ClearWatched(Airing);
            return;
        }

        MultiAiring MA = new MultiAiring(User, ensureIsAiring(Airing));
        MA.clearWatched();
        return;
    }

    // Use IN PLACE OF core API.
    public static long getRealWatchedStartTime(Object Airing) {
        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.GetRealWatchedStartTime(Airing);
        }

        long StartTime = 0;

        if (MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            StartTime = MMF.getRealWatchedStartTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            StartTime = MA.getRealWatchedStartTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRealWatchedStartTime: StartTime " + MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(StartTime));
        return (StartTime == -1 ? AiringAPI.GetRealWatchedStartTime(Airing) : StartTime);
    }

    // Use IN PLACE OF core API.
    public static long getRealWatchedEndTime(Object Airing) {
        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.GetRealWatchedEndTime(Airing);
        }

        long EndTime = 0;

        if (MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            EndTime = MMF.getRealWatchedEndTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            EndTime = MA.getRealWatchedEndTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRealWatchedEndTime: EndTime " + MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(EndTime));
        return (EndTime == -1 ? AiringAPI.GetRealWatchedEndTime(Airing) : EndTime);
    }

    // Use IN PLACE OF core API.
    public static long getWatchedDuration(Object Airing) {
        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.GetWatchedDuration(Airing);
        }

        long Duration = 0;

        if (MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            Duration = MMF.getWatchedDuration();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            Duration = MA.getWatchedDuration();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getWatchedDuration: Duration " + MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(Duration));
        return (Duration == -1 ? AiringAPI.GetWatchedDuration(Airing) : Duration);
    }

    // Use IN PLACE OF core API.
    public static long getWatchedStartTime(Object Airing) {
        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.GetWatchedStartTime(Airing);
        }

        long StartTime = 0;

        if (MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            StartTime = MMF.getWatchedStartTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            StartTime = MA.getWatchedStartTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getWatchedStartTime: StartTime " + MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(StartTime));
        return (StartTime == -1 ? AiringAPI.GetAiringStartTime(Airing) : StartTime);
    }

    // Use IN PLACE OF core API.
    public static long getWatchedEndTime(Object Airing) {
        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return AiringAPI.GetWatchedEndTime(Airing);
        }

        // Special case.  If the Airing isWatched we must return 0 for WatchedEndTime.
        //if (isWatched(Airing))
            //return 0;

        long EndTime = 0;

        if (MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            EndTime = MMF.getWatchedEndTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            EndTime = MA.getWatchedEndTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getWatchedEndTime: EndTime " + MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(EndTime));
        return (EndTime == -1 ? AiringAPI.GetAiringStartTime(Airing) : EndTime);
    }

    // Use IN PLACE OF core API.
    public static long getAiringDuration(Object Airing) {
        return AiringAPI.GetAiringDuration(Airing);
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

    public static List<String> GetUsersForFavorite(Object Favorite) {

        List<String> TheList = new ArrayList<String>();

        if (Favorite==null || !FavoriteAPI.IsFavoriteObject(Favorite)) {
            return TheList;
        }

        String User = getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return TheList;
        }

        MultiFavorite MF = new MultiFavorite(User, Favorite);
        return MF.getAllowedUsers();
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
    public static void addUserToMediaFile(String UserID, Object MediaFileOrAiring) {
        User user = new User(UserID);

        Object Airing = null;
        Object MediaFile = null;

        if (AiringAPI.IsAiringObject(MediaFileOrAiring)) {
            Airing = MediaFileOrAiring;
            MediaFile = AiringAPI.GetMediaFileForAiring(MediaFileOrAiring);
        } else {
            MediaFile = MediaFileOrAiring;
            Airing = MediaFileAPI.GetMediaFileAiring(MediaFileOrAiring);
        }

        user.addToMediaFile(MediaFile);
        user.addToAiring(Airing);
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

        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping the Favorites database.");
        MultiFavorite.WipeDatabase();

        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping the MediaFile database.");
        MultiMediaFile.WipeDatabase();

        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping the Airing database.");
        MultiAiring.WipeDatabase();

        Log.getInstance().write(Log.LOGLEVEL_WARN, "clearAll: Wiping the User database.");
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
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ensureIsAiring: Found unknown Object " + AiringAPI.PrintAiringShort(SageObject));
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
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ensureIsMediaFile: Found unknown Object " + AiringAPI.PrintAiringShort(SageObject));
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
        //TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.MEDIATIME_PREFIX));
        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.CHAPTERNUM_PREFIX));
        TheList.addAll(MultiMediaFile.GetFlagsStartingWith(MultiMediaFile.TITLENUM_PREFIX));

        Boolean IsArchived = MediaFileAPI.IsLibraryFile(MediaFile);
        Boolean DontLike = AiringAPI.IsDontLike(MediaFile);
        Boolean Manual = AiringAPI.IsManualRecord(MediaFile);
        Boolean IsFavorite = AiringAPI.IsFavorite(MediaFile);
        TheList.add("Core: Archived=" + IsArchived.toString() + " DontLike=" + DontLike.toString() + " Manual=" + Manual.toString() + " Favorite=" + IsFavorite.toString());

        return TheList;
    }

    public static List<String> getFlagsForUser(Object MediaFile) {

        List<String> TheList = new ArrayList<String>();

        if (MediaFile==null) {
            return TheList;
        }

        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {
            MultiMediaFile MMF = new MultiMediaFile(getLoggedinUser(), MediaFile);
            return MMF.getFlagsForUser();
        } else {
            MultiAiring MA = new MultiAiring(getLoggedinUser(), MediaFile);
            return MA.getFlagsForUser();
        }
    }
}
