/*
 * Limit Intelligent Recordings Plugin for SageTV.
 */

package tmiranda.lir;

import sage.*;
import sagex.api.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    private final String                VERSION = "0.01";
    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;

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

    @Override
    public void start() {
        System.out.println("LIR: Plugin: Starting. Version = " + VERSION);

        // Set the loglevel to what's in the .properties file.
        Integer DefaultLevel = Log.LOGLEVEL_WARN;
        String CurrentLevel = Configuration.GetServerProperty("lir/loglevel", DefaultLevel.toString());
        Integer SetLevel = Integer.decode(CurrentLevel);
        Log.getInstance().SetLogLevel(SetLevel);

        switch (Log.getInstance().GetLogLevel()) {
            case Log.LOGLEVEL_ALL:      System.out.println("LIR: Plugin: LogLevel = Maximum."); break;
            case Log.LOGLEVEL_ERROR:    System.out.println("LIR: Plugin: LogLevel = Error."); break;
            case Log.LOGLEVEL_NONE:     System.out.println("LIR: Plugin: LogLevel = None."); break;
            case Log.LOGLEVEL_TRACE:    System.out.println("LIR: Plugin: LogLevel = Trace."); break;
            case Log.LOGLEVEL_VERBOSE:  System.out.println("LIR: Plugin: LogLevel = Verbose."); break;
            case Log.LOGLEVEL_WARN:     System.out.println("LIR: Plugin: LogLevel = Warn."); break;
            default:                    System.out.println("LIR: Plugin: Error.  Unknown LogLevel."); break;
        }

        // If we're running on a client we are done.
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "start: Running in Client mode.");
            return;
        }

        // Subscribe to what we need.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Subscribing to events.");
        registry.eventSubscribe(listener, "RecordingCompleted");
    }

    // This method is called when the plugin should shutdown.
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Stop received from Plugin Manager.");

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "stop: Running in Client mode.");
            return;
        }

        registry.eventUnsubscribe(listener, "RecordingCompleted");
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin.
    @Override
    public void destroy() {
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "destroy: Running in Client mode.");
            Log.getInstance().destroy();
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "destroy: Destroy received from Plugin Manager.");
        Log.getInstance().destroy();
        registry = null;
        listener = null;
    }

    // Resets the configuration of this plugin.
    @Override
    public final void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetConfig: resetConfig received from Plugin Manager.");
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);
    }

    // Returns the names of the settings for this plugin.
    @Override
    public String[] getConfigSettings() {
        return null;
    }

    // Returns the current value of the specified setting for this plugin.
    @Override
    public String getConfigValue(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith("LogLevel")) {
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

        return null;
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin.
    @Override
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
    // is used for a specific settings.
    @Override
    public int getConfigType(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigType received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith("LogLevel"))
            return CONFIG_CHOICE;
        return 0;
    }

    // Sets a configuration value for this plugin.
    @Override
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);

        if (setting.startsWith("LogLevel")) {
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
        }
    }

    // Sets a configuration values for this plugin for a multiselect choice.
    @Override
    public void setConfigValues(String setting, String[] values) {
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices.
    @Override
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith("LogLevel")) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else {
            return null;
        }
    }

    // Returns the label used to present this setting to the user.
    @Override
    public String getConfigLabel(String setting) {

        if (setting.startsWith("LogLevel")) {
            return "Debug Logging Level";
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigLabel: Unknown setting = " + setting);
            return null;
        }
    }

    // Returns the help text for a configuration setting.
    @Override
    public String getConfigHelpText(String setting) {
        if (setting.startsWith("LogLevel")) {
            return "Set the Debug Logging Level.";
        } else {
            return null;
        }
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

    @Override
    public synchronized void sageEvent(String eventName, java.util.Map eventVars) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: event received = " + eventName);

        // Check that we have the right event.
        if (!(eventName.startsWith("RecordingCompleted"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
            return;
        }

        // Check that we have a valid MediaFile.
        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null MediaFile");
            return;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Finished recording " + AiringAPI.GetAiringTitle(MediaFile));

        // If it's a Manual, Favorite, or TimedRecord (manual) we do not need to worry about it.
        if (AiringAPI.IsFavorite(MediaFile) || AiringAPI.IsManualRecord(MediaFile)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not an Intelligent Recording.");
            return;
        }

        DataStore store = new DataStore(MediaFile);

        // If it's not monitored or it's allowed to be unlimited don't worry about it.
        if (!store.isMonitored() || store.getMax()==DataStore.UNLIMITED) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not a monitored Recording.");
            return;
        }
    }
}
