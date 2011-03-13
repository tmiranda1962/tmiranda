package tmiranda.mus;

import sagex.api.*;

/**
 *
 * @author Default
 */
public class AiringAPI {

    public static Object record(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.Record(Airing);
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.setManualRecord();
        return sagex.api.AiringAPI.Record(Airing);
    }

    /**
     * Lets the current logged in user mark an upcoming recording as a manual for another user.
     * @param User
     * @param Airing
     */
    public static void markAsManualRecord(String User, Object Airing) {

        // Nothing to do for null user or Admin.
        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.setManualRecord();
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @param StartTime
     * @param StopTime
     * @return
     */
    public static Object setRecordingTimes(Object Airing, long StartTime, long StopTime) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.SetRecordingTimes(Airing, StartTime, StopTime);
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.setRecordingTimes(StartTime, StopTime);
        return sagex.api.AiringAPI.SetRecordingTimes(Airing, StartTime, StopTime);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static boolean isManualRecord(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.IsManualRecord(Airing);
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        return MA.isManualRecord();
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     */
    public static void cancelRecord(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.AiringAPI.CancelRecord(Airing);
            MA.cancelManualRecordForAllUsers();
        } else {
            MA.cancelManualRecord();
        }

        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static Object getMediaFileForAiring(Object Airing) {

        String User = UserAPI.getLoggedinUser();
        Object MediaFile = null;

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER) || Airing==null) {
            return sagex.api.AiringAPI.GetMediaFileForAiring(Airing);
        }

        MediaFile = sagex.api.AiringAPI.IsAiringObject(Airing) ? sagex.api.AiringAPI.GetMediaFileForAiring(Airing) : Airing;

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

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static boolean isDontLike(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER))
            return sagex.api.AiringAPI.IsDontLike(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        return MA.isDontLike();
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     */
    public static void setDontLike(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.AiringAPI.SetDontLike(Airing);
            return;
        }

        if (SageUtil.GetBoolProperty(Plugin.PROPERTY_UPDATE_IR, true) && ConfigurationAPI.isIntelligentRecordingEnabled())
            sagex.api.AiringAPI.SetDontLike(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.setDontLike();
        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     */
    public static void clearDontLike(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.AiringAPI.ClearDontLike(Airing);
            return;
        }

        if (SageUtil.GetBoolProperty(Plugin.PROPERTY_UPDATE_IR, true) && ConfigurationAPI.isIntelligentRecordingEnabled())
            sagex.api.AiringAPI.ClearDontLike(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.clearDontLike();
        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static boolean isFavorite(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.IsFavorite(Airing);
        }

        // If it's not defined in the core as a favorite always return false.
        if (!sagex.api.AiringAPI.IsFavorite(Airing)) {
            return false;
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        return MA.isFavorite();
    }

    /**
     * Invoke in place of core API.
     * @param O
     * @return
     */
    public static boolean isFavoriteObject(Object O) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.FavoriteAPI.IsFavoriteObject(O);
        }

        // If it's not defined in the core as a favorite always return false.
        if (!sagex.api.FavoriteAPI.IsFavoriteObject(O)) {
            return false;
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(O));
        return MA.isFavorite();
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static boolean isNotManualOrFavorite(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.IsNotManualOrFavorite(Airing);
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));

        return (!(MA.isManualRecord() || MA.isFavorite()));
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

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static boolean isWatched(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER))
            return sagex.api.AiringAPI.IsWatched(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        return MA.isWatched();
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     */
    public static void setWatched(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.AiringAPI.SetWatched(Airing);
            return;
        }

        // If we mark the Airing ad Watched here we run the risk of having the Airing removed
        // by the Sage core before all users have a chance to watch it.  When the Airing is
        // deleted we check to see if all Users have Watched it.

        //if (SageUtil.GetBoolProperty(Plugin.PROPERTY_UPDATE_IR, true) && isIntelligentRecordingEnabled())
            //AiringAPI.SetWatched(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.setWatched();
        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     */
    public static void clearWatched(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.AiringAPI.ClearWatched(Airing);
            return;
        }

        if (SageUtil.GetBoolProperty(Plugin.PROPERTY_UPDATE_IR, true) && ConfigurationAPI.isIntelligentRecordingEnabled())
            sagex.api.AiringAPI.ClearWatched(Airing);

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        MA.clearWatched();
        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getRealWatchedStartTime(Object Airing) {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.GetRealWatchedStartTime(Airing);
        }

        long StartTime = 0;

        if (sagex.api.MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            StartTime = MMF.getRealWatchedStartTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            StartTime = MA.getRealWatchedStartTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRealWatchedStartTime: StartTime " + sagex.api.MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(StartTime));
        return (StartTime == -1 ? sagex.api.AiringAPI.GetRealWatchedStartTime(Airing) : StartTime);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getRealWatchedEndTime(Object Airing) {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.GetRealWatchedEndTime(Airing);
        }

        long EndTime = 0;

        if (sagex.api.MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            EndTime = MMF.getRealWatchedEndTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            EndTime = MA.getRealWatchedEndTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRealWatchedEndTime: EndTime " + sagex.api.MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(EndTime));
        return (EndTime == -1 ? sagex.api.AiringAPI.GetRealWatchedEndTime(Airing) : EndTime);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getWatchedDuration(Object Airing) {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.GetWatchedDuration(Airing);
        }

        long Duration = 0;

        if (sagex.api.MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            Duration = MMF.getWatchedDuration();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            Duration = MA.getWatchedDuration();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getWatchedDuration: Duration " + sagex.api.MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(Duration));
        return (Duration == -1 ? sagex.api.AiringAPI.GetWatchedDuration(Airing) : Duration);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getWatchedStartTime(Object Airing) {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.GetWatchedStartTime(Airing);
        }

        long StartTime = 0;

        if (sagex.api.MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            StartTime = MMF.getWatchedStartTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            StartTime = MA.getWatchedStartTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getWatchedStartTime: StartTime " + sagex.api.MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(StartTime));
        return (StartTime == -1 ? sagex.api.AiringAPI.GetAiringStartTime(Airing) : StartTime);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getWatchedEndTime(Object Airing) {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.AiringAPI.GetWatchedEndTime(Airing);
        }

        // Special case.  If the Airing isWatched we must return 0 for WatchedEndTime.
        //if (isWatched(Airing))
            //return 0;

        long EndTime = 0;

        if (sagex.api.MediaFileAPI.IsMediaFileObject(Airing)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Airing);
            EndTime = MMF.getWatchedEndTime();
        } else {
            MultiAiring MA = new MultiAiring(User, Airing);
            EndTime = MA.getWatchedEndTime();
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getWatchedEndTime: EndTime " + sagex.api.MediaFileAPI.GetMediaTitle(Airing) + ":" + Plugin.PrintDateAndTime(EndTime));
        return (EndTime == -1 ? sagex.api.AiringAPI.GetAiringStartTime(Airing) : EndTime);
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static long getAiringDuration(Object Airing) {
        return sagex.api.AiringAPI.GetAiringDuration(Airing);
    }
}
