/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import sage.*;
import sagex.api.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author Default
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    private static final String     VERSION = "0.01";

    /*
     * Constants used throughout the Plugin.
     */
    public static final String  SUPER_USER          = "Admin";      // Admin gets access to everything.
    public static final String  LIST_SEPARATOR      = ",";          // Used to separate lists of flags.
    public static final String[] ALL_STORES = {MultiFavorite.FAVORITE_STORE, MultiAiring.AIRING_STORE, MultiMediaFile.MEDIAFILE_STORE};

    /*
     * Settings used in Plugin Configuration.
     */
    private static final String     SETTING_LOGLEVEL    = "LogLevel";       // Change the loglevel.
    private static final String     SETTING_NOT_ADMIN   = "NotAdmin";       // Used in case a non-admin tries to configure.

    // Keep track of if we should automatically login a user when the UI starts.
    // The property is local to each UI.
    private static final String SETTING_LOGIN_LAST_USER    = "LoginLastUser";
    public static final String PROPERTY_LOGIN_LAST_USER   = "mus/LoginLastUser";    // LOCAL setting.

    private static final String SETTING_USE_PASSWORDS = "UsePasswords";
    public static final String PROPERTY_USE_PASSWORDS = "mus/UsePasswords";

    public static final String PROPERTY_LAST_LOGGEDIN_USER = "mus/LastLoggedinUser";    // LOCAL setting.

    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;


    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
        listener = this;
    }

    public Plugin(sage.SageTVPluginRegistry Registry, boolean Reset) {
        registry = Registry;
        listener = this;
        if (Reset && !Global.IsClient())
            resetConfig();
    }

    // This method is called when the plugin should startup
    public void start() {
        System.out.println("MUS: Plugin starting. Version = " + VERSION);

        // If we're running on a client we are done.
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Running in Client mode.");
            return;
        }

        // Subscribe to what we need.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Subscribing to events.");
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingCompleted");
        registry.eventSubscribe(listener, "MediaFileImported");
        registry.eventSubscribe(listener, "PlaybackStarted");
        registry.eventSubscribe(listener, "PlaybackStopped");
        registry.eventSubscribe(listener, "PlaybackFinished");
    }

    // This method is called when the plugin should shutdown
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
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    public void destroy() {
        Log.getInstance().destroy();
    }

    // These methods are used to define any configuration settings for the
    // plugin that should be presented in the UI. If your plugin does not
    // need configuration settings; you may simply return null or zero from
    // these methods.

    // Returns the names of the settings for this plugin
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: getConfigSetting received from Plugin Manager.");

        List<String> CommandList = new ArrayList<String>();

        // See if the user is logged in as "Admin".
        String User = API.getLoggedinUser();
        boolean isAdmin = (User == null ? false : User.equalsIgnoreCase(SUPER_USER));

        if (!isAdmin) {
            CommandList.add(SETTING_NOT_ADMIN);
        } else {
            //CommandList.add(SETTING_UNASSIGNEDMF);
            CommandList.add(SETTING_LOGIN_LAST_USER);
            CommandList.add(SETTING_USE_PASSWORDS);
            CommandList.add(SETTING_LOGLEVEL);
        }

        return (String[])CommandList.toArray(new String[CommandList.size()]);
    }

    // Returns the current value of the specified setting for this plugin
    public String getConfigValue(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: setConfigValue received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            switch (Log.getInstance().GetLogLevel()) {
                case Log.LOGLEVEL_ALL:      return "Maximum";
                case Log.LOGLEVEL_ERROR:    return "Error";
                case Log.LOGLEVEL_NONE:     return "None";
                case Log.LOGLEVEL_TRACE:    return "Trace";
                case Log.LOGLEVEL_VERBOSE:  return "Verbose";
                case Log.LOGLEVEL_WARN:     return "Warn";
                default:                    return "Unknown";
            }
        }

        //if (setting.startsWith(SETTING_UNASSIGNEDMF)) {
            //return Configuration.GetServerProperty(PROPERTY_UNASSIGNEDMF, UNASSIGNEDMF_ALLOW_ALL);
        //}

        // Use local property.
        if (setting.startsWith(SETTING_LOGIN_LAST_USER)) {
            return Configuration.GetProperty(PROPERTY_LOGIN_LAST_USER, "false");
        }

        // Use local property.
        if (setting.startsWith(SETTING_USE_PASSWORDS)) {
            return Configuration.GetProperty(PROPERTY_USE_PASSWORDS, "true");
        }

        if (setting.startsWith(SETTING_NOT_ADMIN)) {
            return "OK";
        }

        return null;
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin
    public String[] getConfigValues(String setting) {
        return null;
    }

    /**
    //public static final int CONFIG_BOOL = 1;
    //public static final int CONFIG_INTEGER = 2;
    //public static final int CONFIG_TEXT = 3;
    //public static final int CONFIG_CHOICE = 4;
    //public static final int CONFIG_MULTICHOICE = 5;
    //public static final int CONFIG_FILE = 6;
    //public static final int CONFIG_DIRECTORY = 7;
    //public static final int CONFIG_BUTTON = 8;
    //public static final int CONFIG_PASSWORD = 9;
     */

    // Returns one of the constants above that indicates what type of value
    // is used for a specific settings
    public int getConfigType(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: getConfigType received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_LOGLEVEL))
            return CONFIG_CHOICE;

        //if (setting.startsWith(SETTING_UNASSIGNEDMF))
            //return CONFIG_CHOICE;

        if (setting.startsWith(SETTING_LOGIN_LAST_USER))
            return CONFIG_BOOL;

        if (setting.startsWith(SETTING_USE_PASSWORDS))
            return CONFIG_BOOL;

        if (setting.startsWith(SETTING_NOT_ADMIN))
            return CONFIG_BUTTON;

        return CONFIG_TEXT;
    }

    // Sets a configuration value for this plugin
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);

        if (setting.startsWith(SETTING_LOGLEVEL)) {
            if (value.startsWith("None"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_NONE);
            else if (value.startsWith("Error"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_ERROR);
            else if (value.startsWith("Warn"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);
            else if (value.startsWith("Trace"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_TRACE);
            else if (value.startsWith("Verbose"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_VERBOSE);
            else if (value.startsWith("Maximum"))
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_ALL);
            else Log.getInstance().SetLogLevel(Log.LOGLEVEL_ERROR);

            return;
        }

        //if (setting.startsWith(SETTING_UNASSIGNEDMF)) {
            //Configuration.SetServerProperty(PROPERTY_UNASSIGNEDMF, value);
            //return;
        //}

        // Use local property for this setting.
        if (setting.startsWith(SETTING_LOGIN_LAST_USER)) {
            Configuration.SetProperty(PROPERTY_LOGIN_LAST_USER, value);
            return;
        }

        // Use local property.
        if (setting.startsWith(SETTING_USE_PASSWORDS)) {
            Configuration.SetProperty(PROPERTY_USE_PASSWORDS, value);
            return;
        }

        // Nothing to do, just keep placeholder.
        if (setting.startsWith(SETTING_NOT_ADMIN)) {
            return;
        }
    }

    // Sets a configuration values for this plugin for a multiselect choice
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: setConfigValues received from Plugin Manager. Setting = " + setting);
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_LOGLEVEL)) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        }

        //if (setting.startsWith(SETTING_UNASSIGNEDMF)) {
            //String[] values = {UNASSIGNEDMF_ALLOW_ALL, UNASSIGNEDMF_ALLOW_NONE, UNASSIGNEDMF_ALLOW_USERS};
            //return values;
        //}

        return null;
    }

    // Returns the help text for a configuration setting
    public String getConfigHelpText(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Set the Debug Logging Level.";
        }

        //if (setting.startsWith(SETTING_UNASSIGNEDMF)) {
            //return "Set default permission for MediaFiles.";
        //}

        if (setting.startsWith(SETTING_LOGIN_LAST_USER)) {
            return "The user that was logged in will remain logged in after Sage is rebooted.";
        }

        if (setting.startsWith(SETTING_USE_PASSWORDS)) {
            return "Require passwords to log in user.";
        }

        if (setting.startsWith(SETTING_NOT_ADMIN)) {
            return "To log in as Admin go to Main Menu -> Setup -> Users -> Log In.";
        }

        return null;
    }

    // Returns the label used to present this setting to the user
    public String getConfigLabel(String setting) {

        if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Debug Logging Level";
        }

        //if (setting.startsWith(SETTING_UNASSIGNEDMF)) {
            //return "Default Unassigned MediaFile Permission";
        //}

        if (setting.startsWith(SETTING_LOGIN_LAST_USER)) {
            return "Stay Logged In After Reboot";
        }

        if (setting.startsWith(SETTING_USE_PASSWORDS)) {
            return "Require User Passwords";
        }

        if (setting.startsWith(SETTING_NOT_ADMIN)) {
            return "Not Admin, Settings Are Disabled";
        }

        return null;
    }

    // Resets the configuration of this plugin
    public void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: resetConfig received from Plugin Manager.");

        String User = API.getLoggedinUser();

        boolean isAdmin = (User == null ? false : User.equalsIgnoreCase(SUPER_USER));

        if (!isAdmin) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: resetConfig denied to non-Admin user.");
        }

        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);
        Configuration.SetProperty(PROPERTY_LOGIN_LAST_USER, "false");
        Configuration.SetProperty(PROPERTY_USE_PASSWORDS, "true");
    }



/**
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

    public synchronized void sageEvent(String eventName, java.util.Map eventVars) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: event received = " + eventName);

        // Check that we have the right event.
        if (!(  eventName.startsWith("RecordingCompleted") ||
                eventName.startsWith("RecordingStopped") ||
                eventName.startsWith("MediaFileImported") ||
                eventName.startsWith("PlaybackStarted") ||
                eventName.startsWith("PlaybackStopped") ||
                eventName.startsWith("PlaybackFinished"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
            return;
        }

        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null MediaFile.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaTitle " + MediaFileAPI.GetMediaTitle(MediaFile));

        List<String> Users = User.getAllUsers();

        if (Users==null || Users.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: No Users.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Defined users " + Users);

        // MediaTime = Time playback ended relative to the actual time the Airing was recorded.
        // Duration = ??

        if (eventName.startsWith("PlaybackStarted")) {
            String UIContext = (String)eventVars.get("UIContext");
            Long Duration = (Long)eventVars.get("Duration");
            Long MediaTime = (Long)eventVars.get("MediaTime");
            Integer ChapterNum = (Integer)eventVars.get("ChapterNum");
            Integer TitleNum = (Integer)eventVars.get("TitleNum");

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: PlaybackStarted. UIContext=" + UIContext +", Duration="+Duration+", MediaTime="+MediaTime+", ChapterNum="+ChapterNum+", TitleNum="+TitleNum);
            return;
        }

        if (eventName.startsWith("PlaybackStopped")) {
            
            String UIContext = (String)eventVars.get("UIContext");
            Long Duration = (Long)eventVars.get("Duration");
            Long MediaTime = (Long)eventVars.get("MediaTime");
            Integer ChapterNum = (Integer)eventVars.get("ChapterNum");
            Integer TitleNum = (Integer)eventVars.get("TitleNum");

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: PlaybackStopped. UIContext=" + UIContext +", Duration="+Duration+", MediaTime="+MediaTime+", ChapterNum="+ChapterNum+", TitleNum="+TitleNum);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Duration=" + Utility.PrintTimeFull(Duration));
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MediaTime=" + Utility.PrintTimeFull(MediaTime));

            String UserID = User.getUserForContext(UIContext);

            // Check for Admin or user that is not logged in.
            if (UserID==null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: No user for context " + UIContext);
                return;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Found user for context " + UserID);

            MultiMediaFile MMF = new MultiMediaFile(UserID, MediaFile);
            MMF.setDuration(Duration);
            MMF.setMediaTime(MediaTime);
            MMF.setChapterNum(ChapterNum);
            MMF.setTitleNum(TitleNum);
     
            return;
        }

        if (eventName.startsWith("PlaybackFinished")) {

            String UIContext = (String)eventVars.get("UIContext");
            Long Duration = (Long)eventVars.get("Duration");
            Long MediaTime = (Long)eventVars.get("MediaTime");
            Integer ChapterNum = (Integer)eventVars.get("ChapterNum");
            Integer TitleNum = (Integer)eventVars.get("TitleNum");

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: PlaybackFinished. UIContext=" + UIContext +", Duration="+Duration+", MediaTime="+MediaTime+", ChapterNum="+ChapterNum+", TitleNum="+TitleNum);

            return;
        }

        // If we are importing a MediaFile add access for all users.
        if (eventName.startsWith("MediaFileImported")) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Importing the MediaFile.");

            // Add all of the users.  No explicitly needed since the default is to allow all.
            for (String User : Users) {
                User user = new User(User);
                user.addToMediaFile(MediaFile);
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added users to imported MediaFile " + Users);
            return;
        }

        // Disallow all users, then add the ones that will be allowed to see this MediaFile.
        //for (String User : Users) {
            //User user = new User(User);
            //ser.removeFromMediaFile(MediaFile);
        //}

        // We have a completed recording. It could be a Favorite, Manual or an IR.

        // At this time the MediaFile has not had its attributes (Manual, Favorite, IR) set by the Sage
        // core so we need to search manually through the user information to figure out what users should
        // be granted access to the MediaFile.
        boolean AccessGranted = false;

        for (String ThisUser : Users) {

            MultiAiring MA = new MultiAiring(ThisUser, MediaFile);

            if (MA.isManualRecord() || MA.isFavorite()) {
                AccessGranted = true;
                User user = new User(ThisUser);
                user.addToMediaFile(MediaFile);
                MA.removeFlag(MultiAiring.MANUAL_IN_PROGRESS, ThisUser);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added Manual or Favorite to user " + ThisUser);
            }

        }

        // If we didn't assign it to any user let's assume it's an Intelligent Recording and grant access to any
        // users that have IR enabled.
        if (!AccessGranted && !Configuration.IsIntelligentRecordingDisabled()) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Found an IntelligentRecording.");
            for (String ThisUser : Users) {

                User user = new User(ThisUser);

                if (!user.isIntelligentRecordingDisabled()) {
                    user.addToMediaFile(MediaFile);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Added IR for user " + ThisUser);
                }
            }

        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Processing complete.");
        return;
    }

}
