/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.ntr;

import java.util.*;
import sage.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    final static String VERSION = "1.30 06.05.2011";

    private final String SETTING_LOGLEVEL = "LogLevel";

    private final String PROPERTY_SHOW_ADVANCED = "ntr/show_advanced";

    private final String SETTING_CLEAR_PROPERTY = "ClearProperty";

    private sage.SageTVPluginRegistry registry;
    private sage.SageTVEventListener listener;

    private Timer timer;
    private TimerTask deadManSwitch;

    static long deadManResetTime = 5000;

    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
    }

    public Plugin(sage.SageTVPluginRegistry Registry, boolean reset) {
        registry = Registry;
        if (reset)
            resetConfig();
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {
        System.out.println("NameTimedRecording: Starting. Version = " + VERSION);

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Plugin running as a SageClient.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Subscribing to events.");

        listener = this;
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingCompleted");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Starting DeadManSwitch.");

        deadManSwitch = new DeadManSwitch();
        timer = new Timer();
        timer.scheduleAtFixedRate(deadManSwitch, 5000L, deadManResetTime);

        // Cleanup old property.
        Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS_OLD, null);
        return;
    }
    
    // This method is called when the plugin should shutdown
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Stopping.");
        registry.eventUnsubscribe(listener, "RecordingStopped");
        registry.eventUnsubscribe(listener, "RecordingFinished");
        timer.cancel();
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    @Override
    public void destroy() {
        timer = null;
        deadManSwitch = null;
        return;
    }


    // Returns the names of the settings for this plugin
    @Override
    public String[] getConfigSettings() {
        List<String> CommandList = new ArrayList<String>();

        CommandList.add(SETTING_LOGLEVEL);

        if (Configuration.GetProperty(PROPERTY_SHOW_ADVANCED, "false").equalsIgnoreCase("true")) {
            CommandList.add(SETTING_CLEAR_PROPERTY);
        }

        return (String[])CommandList.toArray(new String[CommandList.size()]);
    }

    // Returns the current value of the specified setting for this plugin
    @Override
    public String getConfigValue(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            switch (Log.GetLogLevel()) {
                case Log.LOGLEVEL_MAX:      return "Maximum";
                case Log.LOGLEVEL_ERROR:    return "Error";
                case Log.LOGLEVEL_NONE:     return "None";
                case Log.LOGLEVEL_TRACE:    return "Trace";
                case Log.LOGLEVEL_VERBOSE:  return "Verbose";
                case Log.LOGLEVEL_WARN:     return "Warn";
                default:                    return "Unknown";
            }
        } else if (setting.startsWith(SETTING_CLEAR_PROPERTY)) {
            return "Clear";
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "NameTimedRecording: Unknown setting from getConfigValue " + setting);
            return "Unknown";
        }
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin
    @Override
    public String[] getConfigValues(String setting) {
        return null;
    }

    //public static final int CONFIG_BOOL = 1;
    //public static final int CONFIG_INTEGER = 2;
    //public static final int CONFIG_TEXT = 3;
    //public static final int CONFIG_CHOICE = 4;
    //public static final int CONFIG_MULTICHOICE = 5;
    //public static final int CONFIG_FILE = 6;
    //public static final int CONFIG_DIRECTORY = 7;
    //public static final int CONFIG_BUTTON = 8;
    //public static final int CONFIG_PASSWORD = 9;

    // Returns one of the constants above that indicates what type of value
    // is used for a specific settings
    @Override
    public int getConfigType(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL))
            return CONFIG_CHOICE;
        if (setting.startsWith(SETTING_CLEAR_PROPERTY))
            return CONFIG_BUTTON;
        else
            return 0;
    }

    // Sets a configuration value for this plugin
    @Override
    public void setConfigValue(String setting, String value) {
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            if (value.startsWith("None"))
                Log.SetLogLevel(Log.LOGLEVEL_NONE);
            else if (value.startsWith("Error"))
                Log.SetLogLevel(Log.LOGLEVEL_ERROR);
            else if (value.startsWith("Warn"))
                Log.SetLogLevel(Log.LOGLEVEL_WARN);
            else if (value.startsWith("Trace"))
                Log.SetLogLevel(Log.LOGLEVEL_TRACE);
            else if (value.startsWith("Verbose"))
                Log.SetLogLevel(Log.LOGLEVEL_VERBOSE);
            else if (value.startsWith("Maximum"))
                Log.SetLogLevel(Log.LOGLEVEL_MAX);
            else
                Log.SetLogLevel(Log.LOGLEVEL_ERROR);
        } else if (setting.startsWith(SETTING_CLEAR_PROPERTY)) {
            Configuration.SetServerProperty(API.PROPERTY_RECURRING_RECORDINGS, null);
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setConfigValue: Cleared property.");
        }
    }

    // Sets a configuration values for this plugin for a multiselect choice
    @Override
    public void setConfigValues(String setting, String[] values) {
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    @Override
    public String[] getConfigOptions(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else
            return null;
    }

    // Returns the help text for a configuration setting
    @Override
    public String getConfigHelpText(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL))
            return "Set the Debug Logging Level.";
        if (setting.startsWith(SETTING_CLEAR_PROPERTY))
            return "Change happens immediately with no warning.";
        else
            return null;
    }

    // Returns the label used to present this setting to the user
    @Override
    public String getConfigLabel(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL))
            return "Debug Logging Level";
        if (setting.startsWith(SETTING_CLEAR_PROPERTY))
            return "Clear the Property";
        else
            return null;
    }

    // Resets the configuration of this plugin
    @Override
    public final void resetConfig() {
        Log.SetLogLevel(Log.LOGLEVEL_WARN);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "resetConfig: Reset.");
        return;
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
     */


    // This is a callback method invoked from the SageTV core for any
    // events the listener has subscribed to.
    // See the sage.SageTVPluginRegistry interface definition for details
    // regarding subscribing and unsubscribing to events.
    // The eventName will be a predefined String which indicates the event
    // type.
    // The eventVars will be a Map of variables specific to the event
    // information. This Map should NOT be modified.
    // The keys to the eventVars Map will generally be Strings; but this
    // may change in the future and plugins that submit events
    // are not required to follow that rule.

    @Override
    public void sageEvent(String eventName, java.util.Map eventVars) {

        if (!(eventName.startsWith("RecordingStopped") || eventName.startsWith("RecordingCompleted"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "NameTimedRecording: Received unsubscribed event " + eventName);
            return;
        }

        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "NameTimedRecording: null MediaFile.");
            return;
        }

        Object Airing = MediaFileAPI.GetMediaFileAiring(MediaFile);

        if (Airing==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "NameTimedRecording: null Airing for MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            return;
        }

        String title = AiringAPI.GetAiringTitle(MediaFile);

        // All timed recordings will start with the same thing.  If it's not a timed
        // recording there is nothing to do.
        if (title==null || !title.startsWith(API.TIMED_RECORDING)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Not a timed recording " + MediaFileAPI.GetMediaTitle(MediaFile));
            return;
        }

        // The name will be stored in the ManualRecordProperty of the Airing or if it's
        // a recurring recording we will have to look into the property map.
        String airingName = AiringAPI.GetManualRecordProperty(Airing, API.PROPERTY_NAME);

        if (airingName==null || airingName.isEmpty()) {

            // If there is no ManualRecordProperty it may be a recurring timed recording.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: No ManualRecordProperty for Airing " + AiringAPI.GetAiringTitle(Airing));
            
            airingName = API.getNameForRecurring(Airing);
            
            if (airingName==null || airingName.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: No NameForRecurring.");
                return;
            }
        }

        // Store the name in the MediaFile metadata.
        MediaFileAPI.SetMediaFileMetadata(MediaFile, API.PROPERTY_NAME, airingName);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: Set name for MediaFile " + airingName);
        return;
    }

}
