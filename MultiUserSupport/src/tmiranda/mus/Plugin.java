
package tmiranda.mus;

import sage.*;
import sagex.api.*;
import java.util.*;

/**
 * This is the Plugin Implementation Class for a SageTV Plugin.
 * @author Tom Miranda
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    /**
     * The current Plugin version.
     */
    public static final String  VERSION = "0.09 03.16.2011";

    /*
     * Constants used throughout the Plugin.
     */

    /**
     * Admin. The user who can do anything.
     */
    public static final String  SUPER_USER          = "Admin";      // Admin gets access to everything.

    /**
     * Separates data in delimited Strings.
     */
    public static final String  LIST_SEPARATOR      = ",";          // Used to separate lists of flags.

    /**
     * The SageTV Plugin ID
     */
    public static final String  PLUGIN_ID           = "multiusersupport";

    /**
     * All of the stores (as defined in the UserRecordAPI) used in the Plugin.
     */
    public static final String[] ALL_STORES = {MultiFavorite.FAVORITE_STORE, MultiAiring.AIRING_STORE, MultiMediaFile.MEDIAFILE_STORE, User.STORE};

    /*
     * Settings used in Plugin Configuration.
     */


    /**
     * Property used to determine if the last logged in user should be logged back in after the system is rebooted.
     */
    public static final String PROPERTY_LOGIN_LAST_USER   = "mus/LoginLastUser";    // LOCAL setting.

   
    /**
     * Property used to store if passwords are in use.
     */
    public static final String PROPERTY_USE_PASSWORDS = "mus/UsePasswords";

    /**
     * Property used to store if users that have IR enabled should update the core with their actions.
     */
    public static final String PROPERTY_UPDATE_IR = "mus/UpdateIR";

    /**
     * Property used to store if multi user disk usage indicator should be displayed.
     */
    public static final String PROPERTY_SHOW_USER_DISK_USAGE = "mus/ShowUserDiskUsage";

    /**
     * Property used to store the name of the user that was last logged on.
     */
    public static final String PROPERTY_LAST_LOGGEDIN_USER = "mus/LastLoggedinUser";    // LOCAL setting.
    public static final String PROPERTY_LAST_LOGGEDIN_CONTEXT_NAME = "mus/LastLoggedinContext";

    /**
     * Property used to store a delimited string of secondary users.
     */
    public static final String PROPERTY_SECONDARY_USERS = "mus/SecondaryUsers"; // LOCAL property.
    public static final String PROPERTY_LOGIN_SECONDARY_USERS = "mus/LoginSecondaryUsers";

    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;


    /**
     * Constructor.
     * <p>
     * @param Registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
        listener = this;
        Log.start();
    }

    /**
     * Constructor.
     * @param Registry
     * @param Reset
     */
    public Plugin(sage.SageTVPluginRegistry Registry, boolean Reset) {
        registry = Registry;
        listener = this;
        Log.start();
        if (Reset)
            resetConfig();
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {
        System.out.println("MUS: Plugin starting. Version = " + VERSION);

        // Show what users are in the database
        List<String>allUsers = User.getAllUsers();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Defined users " + allUsers);

        for (String u : allUsers) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: User " + u);
            User user = new User(u);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start:   Password " + user.getPassword());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start:   IR Disabled " + user.isIntelligentRecordingDisabled());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start:   UserID " + user.getUserID());
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start:   Show Imported Videos " + user.isShowImports());
        }

        // If we're running on a client we are done.
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Running in Client mode.");
            return;
        }

        // Subscribe to what we need.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Subscribing to events.");
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingStarted");
        registry.eventSubscribe(listener, "MediaFileImported");
        registry.eventSubscribe(listener, "PlaybackStarted");
        registry.eventSubscribe(listener, "PlaybackStopped");
        registry.eventSubscribe(listener, "PlaybackFinished");
        registry.eventSubscribe(listener, "MediaFileRemoved");
    }

    // This method is called when the plugin should shutdown.
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Stop received from Plugin Manager.");

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Running in Client mode.");
            return;
        }

        registry.eventUnsubscribe(listener, "RecordingCompleted");
        registry.eventUnsubscribe(listener, "RecordingStopped");
        registry.eventUnsubscribe(listener, "MediaFileImported");
        registry.eventUnsubscribe(listener, "PlaybackStarted");
        registry.eventUnsubscribe(listener, "PlaybackStopped");
        registry.eventUnsubscribe(listener, "PlaybackFinished");
        registry.eventUnsubscribe(listener, "MediaFileRemoved");
    }

    // This method is called after plugin shutdown to free any resources used by the plugin.
    @Override
    public void destroy() {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Destroy.");
        Log.destroy();
    }

    @Override
    public String[] getConfigSettings() {
        return null;
    }

    @Override
    public String getConfigValue(String setting) {
        return null;     
    }

    // Returns the current value of the specified multichoice setting for this plugin
    @Override
    public String[] getConfigValues(String setting) {
        return null;
    }

    // Returns one of the constants above that indicates what type of value
    // is used for a specific settings
    @Override
    public int getConfigType(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: getConfigType received from Plugin Manager. Setting = " + setting);
        return 0;
    }

    // Sets a configuration value for this plugin
    @Override
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);
        return;
    }

    // Sets a configuration values for this plugin for a multiselect choice
    @Override
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: setConfigValues received from Plugin Manager. Setting = " + setting);
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    @Override
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);
        return null;
    }

    // Returns the help text for a configuration setting
    @Override
    public String getConfigHelpText(String setting) {
        return null;
    }

    // Returns the label used to present this setting to the user
    @Override
    public String getConfigLabel(String setting) {
        return null;
    }

    // Resets the configuration of this plugin
    @Override
    public void resetConfig() {
        return;
    }

/*
 * Interface definition for implementation classes that listen for events
 * from the SageTV core
 *
 * Variable types are in brackets[] after the var name unless they are the
 * same as the var name itself.
 * List of known core events:
 *
 * MediaFileImported - vars: MediaFile
 * ImportingStarted
 * ImportingCompleted
 * RecordingCompleted (called when a complete recording is done)
 * 	vars: MediaFile
 * RecordingStarted (called when any kind of recording is started)
 *	vars: MediaFile
 * RecordingStopped (called whenever a recording is stopped for any reason)
 *	vars: MediaFile
 * AllPluginsLoaded
 * RecordingScheduleChanged
 * ConflictStatusChanged
 * SystemMessagePosted
 *	vars: SystemMessage
 * EPGUpdateCompleted
 * MediaFileRemoved
 * 	vars: MediaFile
 * PlaybackStopped (called when the file is closed)
 * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
 * 		ChapterNum[Integer], TitleNum[Integer]
 * PlaybackFinished (called at the EOF)
 * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
 * 		ChapterNum[Integer], TitleNum[Integer]
 * PlaybackStarted
 * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
 * 		ChapterNum[Integer], TitleNum[Integer]
 * FavoriteAdded
 * 	vars: Favorite
 * FavoriteModified
 * 	vars: Favorite
 * FavoriteRemoved
 * 	vars: Favorite
 * PlaylistAdded
 * 	vars: Playlist, UIContext[String]
 * PlaylistModified
 * 	vars: Playlist, UIContext[String]
 * PlaylistRemoved
 * 	vars: Playlist, UIContext[String]
 * ClientConnected
 * 	vars: IPAddress[String], MACAddress[String] (if its a
 * 		placeshifter/extender, MACAddress is null otherwise)
 * ClientDisconnected
 * 	vars: IPAddress[String], MACAddress[String] (if its a
 * 		placeshifter/extender, MACAddress is null otherwise)
 *
 *
 * This is a callback method invoked from the SageTV core for any
 * events the listener has subscribed to.
 * See the sage.SageTVPluginRegistry interface definition for details
 * regarding subscribing and unsubscribing to events.
 * The eventName will be a predefined String which indicates the event
 * type.
 * The eventVars will be a Map of variables specific to the event
 * information. This Map should NOT be modified.
 * The keys to the eventVars Map will generally be Strings; but this
 * may change in the future and plugins that submit events
 * are not required to follow that rule.
 */

    /**
     * Handles all of the subscribed to sageEvents.
     * @param eventName
     * @param eventVars
     */
    @Override
    public synchronized void sageEvent(String eventName, java.util.Map eventVars) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: event received = " + eventName);

        // Check that we have the right event.
        if (!(  eventName.startsWith("RecordingCompleted") ||
                eventName.startsWith("RecordingStopped") ||
                eventName.startsWith("RecordingStarted") ||
                eventName.startsWith("MediaFileImported") ||
                eventName.startsWith("PlaybackStarted") ||
                eventName.startsWith("PlaybackStopped") ||
                eventName.startsWith("MediaFileRemoved") ||
                eventName.startsWith("PlaybackFinished"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
            return;
        }

        /*
         * Get data needed throughout event handler.
         */
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Received event = " + eventName);

        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null MediaFile.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaTitle " + sagex.api.MediaFileAPI.GetMediaTitle(MediaFile));

        Integer MediaFileID = sagex.api.MediaFileAPI.GetMediaFileID(MediaFile);

        //
        // MediaFileRemoved.
        //
        // If the core just removed the MediaFile make sure the record is also deleted.
        // It's possible that the API did not remove the MediaFile so we need to catch it here.
        if (eventName.startsWith("MediaFileRemoved")) {
            MultiMediaFile MMF = new MultiMediaFile(SUPER_USER, MediaFile);
            MMF.removeRecord();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Finished with MediaFileRemoved.");
            return;
        }

        List<String> Users = User.getAllUsers();

        if (Users==null || Users.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: No Users.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Defined users " + Users);
        
        Object Airing = sagex.api.MediaFileAPI.GetMediaFileAiring(MediaFile);

        if (Airing==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null Airing.");
        }

        //
        // RecordingStarted.
        //
        // If a recording has started hide it from all users except the one that created the Manual
        // or Favorite.  This will have the side effect of hiding IR's from users that have IR
        // enabled, but we will take care of that when the recording stops.
        if (eventName.startsWith("RecordingStarted")) {

            for (String ThisUser : Users) {

                MultiAiring MA = new MultiAiring(ThisUser, Airing);

                if (MA.isManualRecord() || MA.isFavorite()) {
                    User user = new User(ThisUser);
                    user.addToMediaFile(MediaFile);
                    if (Airing!=null)
                        user.addToAiring(Airing);
                    MA.clearManualRecordFlag();
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added Manual or Favorite to user " + ThisUser);
                } else {
                    MultiMediaFile MMF = new MultiMediaFile(ThisUser, MediaFile);
                    MMF.hide();
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Hiding for user " + ThisUser);
                }

            }

            return;
        }

        //
        // PlaybackStarted and PlaybackStopped.
        //
        if (eventName.startsWith("PlaybackStarted") || eventName.startsWith("PlaybackStopped") || eventName.startsWith("PlaybackFinished")) {

            List<String> UserIDs = User.getUsersWatchingID(MediaFileID);

            // Check for Admin or user that is not logged in. If either one of these users are doing the watching
            // we do not have to mess with any of the data.
            if (UserIDs==null || UserIDs.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: No users for MediaFileID " + MediaFileID);
                return;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Users watching this Airing " + UserIDs);

            // Fetch the data from the Map.
            //   MediaTime = Time playback ended relative to the actual time the Airing was recorded.
            //   Duration = Total playback duration in milliseconds.
            String UIContext    = (String)eventVars.get("UIContext");
            Long Duration       = (Long)eventVars.get("Duration");
            Long MediaTime      = (Long)eventVars.get("MediaTime");
            Integer ChapterNum  = (Integer)eventVars.get("ChapterNum");
            Integer TitleNum    = (Integer)eventVars.get("TitleNum");

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: UIContext=" + UIContext +", Duration="+Duration+", MediaTime="+MediaTime+", ChapterNum="+ChapterNum+", TitleNum="+TitleNum);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Duration=" + Utility.PrintDurationWithSeconds(Duration));
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaTime=" + PrintDateAndTime(MediaTime));

            // Nothing to do if PlaybackStarted. RealWatchedStartTime() is set in the API call.
            if (eventName.startsWith("PlaybackStarted")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: PlaybackStarted. UIContext=" + UIContext +", Duration="+Duration+", MediaTime="+MediaTime+", ChapterNum="+ChapterNum+", TitleNum="+TitleNum);
                return;
            }
                                   
            // Playback has stopped, maybe because the user manually stopped it or maybe
            // because the end of file has been reached.

            for (String UserID : UserIDs) {
                MultiMediaFile MMF = new MultiMediaFile(UserID, MediaFile);
                //MMF.setMediaTime(MediaTime);
                MMF.setWatchedDuration(MediaTime - sagex.api.AiringAPI.GetAiringStartTime(MediaFile));
                MMF.setChapterNum(ChapterNum);
                MMF.setTitleNum(TitleNum);
                MMF.setRealWatchedEndTime(Utility.Time());
                MMF.setWatchedEndTime(MediaTime);

                if (eventName.startsWith("PlaybackFinished")) {
                    MMF.setWatched();
                }

                // The user is no longer watching this MediaFile.
                User user = new User(UserID);
                user.clearWatching();
            }

            return;

        }  // End of playback event.


        // It's not a playback event, so it's either an Import event or a Recording event.

        // If we are importing a MediaFile add access for all users.
        if (eventName.startsWith("MediaFileImported")) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Importing the MediaFile.");

            // Initialize for all users, then hide for users that should not see it.
            for (String User : Users) {
                User user = new User(User);
                user.addToMediaFile(MediaFile);
                if (Airing!=null)
                    user.addToAiring(Airing);

                if (!user.isShowImports()) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Hiding imported video file from user " + User);
                    MultiMediaFile MMF = new MultiMediaFile(User, MediaFile);
                    MMF.hide();
                }

            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added users to imported MediaFile " + Users);
            return;
        }

        // We have a completed recording. It could be a Favorite, Manual or an IR.

        // At this time the MediaFile has not had its attributes (Manual, Favorite, IR) set by the Sage
        // core so we need to search manually through the user information to figure out what users should
        // be granted access to the MediaFile.
        boolean AccessGranted = false;

        if (Airing==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Can't process Recording because there is no Airing.");
            return;
        }

        for (String ThisUser : Users) {

            MultiAiring MA = new MultiAiring(ThisUser, Airing);

            if (MA.isManualRecord() || MA.isFavorite()) {
                AccessGranted = true;
                User user = new User(ThisUser);
                user.addToMediaFile(MediaFile);
                if (Airing!=null)
                    user.addToAiring(Airing);
                MA.clearManualRecordFlag();
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added Manual or Favorite to user " + ThisUser);
            } else {
                MultiMediaFile MMF = new MultiMediaFile(ThisUser, MediaFile);
                MMF.hide();
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Hiding for user " + ThisUser);
            }

        }

        if (!AccessGranted) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: This is not a Manual or a Favorite for any user.");
        }

        // If we didn't assign it to any user let's assume it's an Intelligent Recording and grant access to any
        // users that have IR enabled.
        if (!AccessGranted && !Configuration.IsIntelligentRecordingDisabled()) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Found an IntelligentRecording. Updaing all Users.");
            for (String ThisUser : Users) {

                User user = new User(ThisUser);
                MultiMediaFile MMF = new MultiMediaFile(ThisUser, MediaFile);

                if (!user.isIntelligentRecordingDisabled()) {
                    MMF.unhide();
                    user.addToMediaFile(MediaFile);
                    user.addToAiring(Airing);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added IR for user " + ThisUser);
                } else {
                    MMF.hide();
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Hiding for user " + ThisUser);
                }
            }

        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: It's not a Manual or Favorite, and IR is disabled.");
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Processing complete.");
        return;
    }

    /**
     * Prints the Date and Time in a user friendly format.
     * @param time The time.
     * @return A formatted String.
     */
    public static String PrintDateAndTime(long time) {
        if (time == 0) {
            return "0";
        } else if (time == -1) {
            return "-1";
        } else
            return Utility.PrintDateFull(time) + " - " + Utility.PrintTimeFull(time);
    }

    /**
     *
     * @param time The time.
     * @return A formatted String.
     */
    public static String PrintDateAndTime(String time) {

        if (time==null)
            return "0";

        long t;

        try {
            t = Long.parseLong(time);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PrintDateAndTime: Malformed time " + time);
            t = 0;
        }

        return PrintDateAndTime(t);
    }

}
