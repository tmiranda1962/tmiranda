package tmiranda.mus;

import sagex.api.*;
import sagex.UIContext;

/**
 *
 * @author Tom Miranda.
 */
public class MediaPlayerAPI {

    /*
     * MediaPlayer API.
     *
     * WatchLive() is only used in the setup menus so there is no need to implement that.
     */

    /**
     * Invoke this method just before invoking Watch() in the STV.  We can't invoke Watch
     * from this method directly because the STV relies on having the pre-defined variable
     * "this" set appropriately.
     * @param Content
     */
    public static Object watch(String ContextName, Object Content) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "preWatch: null user or Admin " + User);
            return sagex.api.MediaPlayerAPI.Watch(new UIContext(ContextName), Content);
        }

        // Set flag showing that this user is watching this content. We will need this later
        // when the RecordingStopped Event is received so we can set RealWatchedEndTime and
        // WatchedEndTime for the appropriate user.
        User user = new User(User);
        user.setWatching(Content);

        long WatchedEndTime = 0;
        long RealStartTime = 0;

        if (sagex.api.AiringAPI.IsAiringObject(Content)) {
            MultiAiring MA = new MultiAiring(User, Content);
            MA.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MA.getWatchedEndTime();
            RealStartTime = MA.getRealWatchedStartTime();
        } else if (sagex.api.MediaFileAPI.IsMediaFileObject(Content)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Content);
            MMF.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MMF.getWatchedEndTime();
            RealStartTime = MMF.getRealWatchedStartTime();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "preWatch: Not an Airing or MediaFile.");
            return sagex.api.MediaPlayerAPI.Watch(new UIContext(ContextName), Content);
        }

        WatchedEndTime = (WatchedEndTime==-1 ? sagex.api.AiringAPI.GetWatchedEndTime(Content):WatchedEndTime);
        RealStartTime = (RealStartTime==-1 ? sagex.api.AiringAPI.GetRealWatchedStartTime(Content):RealStartTime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "watch: Setting WatchedEndTime and RealStartTime " + Plugin.PrintDateAndTime(WatchedEndTime) + ":" + Plugin.PrintDateAndTime(RealStartTime));

        // Reset the values.
        sagex.api.AiringAPI.ClearWatched(Content);
        sagex.api.AiringAPI.SetWatchedTimes(Content, WatchedEndTime, RealStartTime);

        // Let the core do its thing.
        return sagex.api.MediaPlayerAPI.Watch(new UIContext(ContextName), Content);
    }

    @Deprecated
    public static void preWatch(Object Content) {

        String User = UserAPI.getLoggedinUser();

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

        if (sagex.api.AiringAPI.IsAiringObject(Content)) {
            MultiAiring MA = new MultiAiring(User, Content);
            MA.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MA.getWatchedEndTime();
            RealStartTime = MA.getRealWatchedStartTime();
        } else if (sagex.api.MediaFileAPI.IsMediaFileObject(Content)) {
            MultiMediaFile MMF = new MultiMediaFile(User, Content);
            MMF.setRealWatchedStartTime(Utility.Time());
            WatchedEndTime = MMF.getWatchedEndTime();
            RealStartTime = MMF.getRealWatchedStartTime();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "preWatch: Not an Airing or MediaFile.");
            return;
        }

        WatchedEndTime = (WatchedEndTime==-1 ? sagex.api.AiringAPI.GetWatchedEndTime(Content):WatchedEndTime);
        RealStartTime = (RealStartTime==-1 ? sagex.api.AiringAPI.GetRealWatchedStartTime(Content):RealStartTime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "watch: Setting WatchedEndTime and RealStartTime " + Plugin.PrintDateAndTime(WatchedEndTime) + ":" + Plugin.PrintDateAndTime(RealStartTime));

        // Reset the values.
        sagex.api.AiringAPI.ClearWatched(Content);
        sagex.api.AiringAPI.SetWatchedTimes(Content, WatchedEndTime, RealStartTime);

        // Let the core do its thing.
        return;
    }
}
