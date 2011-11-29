
package tmiranda.otf;

import java.util.*;
import sage.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    // Version 1.01: Make the recording a Manual if PROPERTY_MAKE_MANUAL is set to true. (Default.)
    final static String VERSION = "1.01";

    final static String FAVORITE_PROPERTY = "OneTimeFavorite";

    final static String SETTING_LOGLEVEL = "LogLevel";
    final static String PROPERTY_LOGLEVEL = "otf/loglevel";

    final static String SETTING_VERSION = "Version";

    // Version 1.01.
    final static String PROPERTY_MAKE_MANUAL = "otf/make_manual";

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

    public Plugin(sage.SageTVPluginRegistry Registry, boolean reset) {
        registry = Registry;
        listener = this;
        if (reset) {
            resetConfig();
        }
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {
        System.out.println("OneTimeFavorite starting. Version = " + VERSION);

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Running in client mode.");
        } else {
            registry.eventSubscribe(listener, "RecordingStopped");
        }
    }

    // This method is called when the plugin should shutdown
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Stop received from Plugin Manager.");

        if (!Global.IsClient())
            registry.eventUnsubscribe(listener, "RecordingStopped");
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    @Override
    public void destroy() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Destroy received from Plugin Manager.");
    }


    // Returns the names of the settings for this plugin
    @Override
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigSetting received from Plugin Manager.");

        List<String> CommandList = new ArrayList<String>();
        CommandList.add(SETTING_LOGLEVEL);
        CommandList.add(SETTING_VERSION);
        return (String[])CommandList.toArray(new String[CommandList.size()]);
    }

    // Returns the current value of the specified setting for this plugin
    @Override
    public String getConfigValue(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: getConfigValue received from Plugin Manager. Setting = " + setting);
        
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            switch (Log.getInstance().GetLogLevel()) {
                case Log.LOGLEVEL_MAX:      return "Maximum";
                case Log.LOGLEVEL_ERROR:    return "Error";
                case Log.LOGLEVEL_NONE:     return "None";
                case Log.LOGLEVEL_TRACE:    return "Trace";
                case Log.LOGLEVEL_VERBOSE:  return "Verbose";
                case Log.LOGLEVEL_WARN:     return "Warn";
                default:                    return "Unknown";
            }
        } if (setting.startsWith(SETTING_VERSION)) {
            return VERSION;
        } else {
            return null;
        }
    }


    // Returns the current value of the specified multichoice setting for
    // this plugin
    @Override
    public String[] getConfigValues(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigValues received from Plugin Manager. Setting = " + setting);
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
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getConfigType received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_LOGLEVEL))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_VERSION))
            return CONFIG_BUTTON;
        else
            return 0;
    }

    // Sets a configuration value for this plugin
    @Override
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);

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
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_MAX);
            else
                Log.getInstance().SetLogLevel(Log.LOGLEVEL_ERROR);
        }
    }


    // Sets a configuration values for this plugin for a multiselect choice
    @Override
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "setConfigValues received from Plugin Manager. Setting = " + setting);
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    @Override
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else {
            return null;
        }
    }

    // Returns the help text for a configuration setting
    @Override
    public String getConfigHelpText(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL))
            return "Set the Debug Logging Level.";
        if (setting.startsWith(SETTING_VERSION))
            return "The version number of the Plugin.";
        else
            return null;
    }

    // Returns the label used to present this setting to the user
    @Override
    public String getConfigLabel(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL))
            return "Debug Logging Level";
        if (setting.startsWith(SETTING_VERSION))
            return "Version";
        else
            return null;
    }

    // Resets the configuration of this plugin
    @Override
    public final void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: resetConfig received from Plugin Manager.");
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);
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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Invoked for event " + eventName);

        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null.");
            return;
        }

        if (!MediaFileAPI.IsCompleteRecording(MediaFile)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not a complete recording.");
            return;
        }

        Object Favorite = FavoriteAPI.GetFavoriteForAiring(MediaFile);

        if (Favorite == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not a favorite.");
            return;
        }

        String oneTime = FavoriteAPI.GetFavoriteProperty(Favorite, FAVORITE_PROPERTY);

        if (oneTime==null || oneTime.isEmpty() || !oneTime.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not a one time favorite.");
            return;
        }

        FavoriteAPI.RemoveFavorite(Favorite);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Removing completed one time favorite " + FavoriteAPI.GetFavoriteDescription(Favorite));

        // Version 1.01
        String makeManual = Configuration.GetProperty(PROPERTY_MAKE_MANUAL, "true").toLowerCase();
        if (makeManual.equals("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Making it a manual recording.");
            AiringAPI.Record(MediaFile);
        }
    }
}
