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

    private final String VERSION = "0.02 2011.12.30";

    private final String SETTING_LOGLEVEL = "LogLevel";

    public static final int UNLIMITED = -1;

    private final static String  SETTING_DEFAULT_MAX     = "DefaultMax";
    private final static String  PROPERTY_DEFAULT_MAX    = "lir/DefaultMax";
    private final static Integer DEFAULT_MAX             = UNLIMITED;
    final static String  DEFAULT_MAX_STRING              = "-1";

    private final String SETTING_REDUCE_TO_MAX    = "Reduce";
    private final String PROPERTY_REDUCE_TO_MAX   = "lir/Reduce";

    private final String SETTING_KEEP_OLDEST      = "KeepOldest";
    private final String PROPERTY_KEEP_OLDEST     = "lir/KeepOldest";

    // Possibilities:
    //  Option to delete watched first IsWatched
    //
    //  Recorded Date GetAiringStartTime
    //  Original Air Date GetOriginalAiringDate
    //  Season/Episode GetShowSeasonNumber, GetShowEpisodeNumber
    //
    //  Forward and Reversed!
    //private final String SETTING_DELETE_METHOD    = "DeleteMethod";
    //private final String PROPERTY_DELETE_METHOD   = "lir/DeleteMethod";

    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;

    private static String showInFocus   = null;

    private final String SETTING_PICK_SHOW  = "PickShow";
    private final String SETTING_HAVE_SHOW  = "HaveShow";
    private final String SETTING_SHOW_MAX   = "ShowMax";
    private final String SETTING_RESET_SHOW = "ResetShow";

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
        String CurrentLevel = Configuration.GetServerProperty(Log.PROPERTY_LOGLEVEL, DefaultLevel.toString());
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
        //registry.eventSubscribe(listener, "RecordingCompleted");
        registry.eventSubscribe(listener, "RecordingStopped");
    }

    // This method is called when the plugin should shutdown.
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "stop: Stop received from Plugin Manager.");

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "stop: Running in Client mode.");
            return;
        }

        //registry.eventUnsubscribe(listener, "RecordingCompleted");
        registry.eventUnsubscribe(listener, "RecordingStopped");
        showInFocus = null;
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
        Configuration.SetServerProperty(PROPERTY_DEFAULT_MAX, DEFAULT_MAX_STRING);
        Configuration.SetServerProperty(PROPERTY_REDUCE_TO_MAX, "false");
        Configuration.SetServerProperty(PROPERTY_KEEP_OLDEST, "true");
        showInFocus = null;
    }

    // Returns the names of the settings for this plugin.
    @Override
    public String[] getConfigSettings() {
        List<String> CommandList = new ArrayList<String>();
        CommandList.add(SETTING_DEFAULT_MAX);
        CommandList.add(SETTING_KEEP_OLDEST);
        if (showInFocus==null)
            CommandList.add(SETTING_PICK_SHOW);
        else {
            CommandList.add(SETTING_HAVE_SHOW);
            CommandList.add(SETTING_SHOW_MAX);
            DataStore store = new DataStore(showInFocus);
            if (store.isMonitored())
                CommandList.add(SETTING_RESET_SHOW);
        }
        CommandList.add(SETTING_REDUCE_TO_MAX);
        CommandList.add(SETTING_LOGLEVEL);
        return (String[])CommandList.toArray(new String[CommandList.size()]);
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
        Log.getInstance().write(Log.LOGLEVEL_ALL, "PlugIn: getConfigType received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_LOGLEVEL))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_DEFAULT_MAX))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_REDUCE_TO_MAX))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_KEEP_OLDEST))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_PICK_SHOW))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_HAVE_SHOW))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_SHOW_MAX))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_RESET_SHOW))
            return CONFIG_BUTTON;
        else
            return 0;
    }

    // Returns the current value of the specified setting for this plugin.
    @Override
    public String getConfigValue(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_ALL, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting);
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
        } else if (setting.startsWith(SETTING_DEFAULT_MAX)) {
            return Configuration.GetServerProperty(PROPERTY_DEFAULT_MAX, DEFAULT_MAX_STRING);
        } else if (setting.startsWith(SETTING_REDUCE_TO_MAX)) {
            return Configuration.GetServerProperty(PROPERTY_REDUCE_TO_MAX, "false");
        } else if (setting.startsWith(SETTING_KEEP_OLDEST)) {
            return Configuration.GetServerProperty(PROPERTY_KEEP_OLDEST, "true");
        } else if (setting.startsWith(SETTING_PICK_SHOW)) {
            return "Select";
        } else if (setting.startsWith(SETTING_HAVE_SHOW)) {
            return showInFocus;
        } else if (setting.startsWith(SETTING_SHOW_MAX)) {
            DataStore store = new DataStore(showInFocus);
            return (store.isMonitored() ? store.getMaxString() : "");
        } else if (setting.startsWith(SETTING_RESET_SHOW)) {
            return "Reset Now";
        } else
            return null;
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin.
    @Override
    public String[] getConfigValues(String setting) {
        return null;
    }

    // Sets a configuration value for this plugin.
    @Override
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_ALL, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);

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
        } else if (setting.startsWith(SETTING_DEFAULT_MAX)) {
            if (value.equalsIgnoreCase("5309")) {
                System.out.println("LIR:: Deleting all user records.");
                UserRecordAPI.DeleteAllUserRecords(DataStore.STORE);
                showInFocus = null;
            } else
                Configuration.SetServerProperty(PROPERTY_DEFAULT_MAX, verifyMax(value));
        } else if (setting.startsWith(SETTING_REDUCE_TO_MAX)) {
            Configuration.SetServerProperty(PROPERTY_REDUCE_TO_MAX, value);
        } else if (setting.startsWith(SETTING_KEEP_OLDEST)) {
            Configuration.SetServerProperty(PROPERTY_KEEP_OLDEST, value);
        } else if (setting.startsWith(SETTING_PICK_SHOW)) {

            // The user just selected a show.  Put it in focus.
            showInFocus = Util.removeNumberMax(value);

        } else if (setting.startsWith(SETTING_HAVE_SHOW)) {

            // The user just selected a different show.  Put it in focus.
            showInFocus = Util.removeNumberMax(value);

        } else if (setting.startsWith(SETTING_SHOW_MAX)) {

            // The user just entered a new max for this show. If it's non null add it.
            if (value==null || value.trim().length()==0)
                return;

            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return;
            }

            DataStore store = new DataStore(showInFocus);
            store.addRecord(showInFocus);
            store.setMax(verifyMax(value));
        } else if (setting.startsWith(SETTING_RESET_SHOW)) {

            // The user wants to reset this show so just delete the User Record.
            DataStore store = new DataStore(showInFocus);
            if (store.deleteRecord())
                showInFocus = null;
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Could not delete the User Record.");
        }
    }

    private static String verifyMax(String value) {
        Integer val;

        try {
            val = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            val = UNLIMITED;
        }

        if (val < UNLIMITED)
            val = UNLIMITED;

        return (val == UNLIMITED ? "-1" : val.toString());
    }

    // Sets a configuration values for this plugin for a multiselect choice.
    @Override
    public void setConfigValues(String setting, String[] values) {
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices.
    @Override
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_ALL, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else if (setting.startsWith(SETTING_PICK_SHOW) || setting.startsWith(SETTING_HAVE_SHOW)) {

            // The user wants to select a show to put in focus.  The options are all
            // shows that are intelligent recordings.
            return Util.getAllIntelligentRecordingTitlesAndMax();
        } else {
            return null;
        }
    }

    // Returns the label used to present this setting to the user.
    @Override
    public String getConfigLabel(String setting) {

        if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Debug Logging Level";
        } else if (setting.startsWith(SETTING_DEFAULT_MAX)) {
            return "Global Maximum to Keep";
        } else if (setting.startsWith(SETTING_REDUCE_TO_MAX)) {
            return "Delete Excess Recordings";
        } else if (setting.startsWith(SETTING_KEEP_OLDEST)) {
            return "Keep the Oldest Recordings";
        } else if (setting.startsWith(SETTING_PICK_SHOW)) {
            return "Choose a Show";
        } else if (setting.startsWith(SETTING_HAVE_SHOW)) {
            return "Limit This Show";
        } else if (setting.startsWith(SETTING_SHOW_MAX)) {
            return "Max to Keep for This Show";
        } else if (setting.startsWith(SETTING_RESET_SHOW)) {
            return "Use Default for This Show";
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigLabel: Unknown setting = " + setting);
            return null;
        }
    }

    // Returns the help text for a configuration setting.
    @Override
    public String getConfigHelpText(String setting) {
        if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Set the Debug Logging Level.";
        } else if (setting.startsWith(SETTING_DEFAULT_MAX)) {
            return "-1 for unlimited. 5309 clears database.";
        } else if (setting.startsWith(SETTING_REDUCE_TO_MAX)) {
            return "Reduce total recordings to 'Maximum to Keep'.";
        } else if (setting.startsWith(SETTING_KEEP_OLDEST)) {
            return "Delete the newest recordings.";
        } else if (setting.startsWith(SETTING_PICK_SHOW)) {
            return "Select a specific show.";
        } else if (setting.startsWith(SETTING_HAVE_SHOW)) {
            return "Select to choose another show.";
        } else if (setting.startsWith(SETTING_SHOW_MAX)) {
            return "Overrides the global default.";
        } else if (setting.startsWith(SETTING_RESET_SHOW)) {
            return "No longer use a custom 'Max to Keep' value.";
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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Event received = " + eventName);

        // Check that we have the right event.
        if (!(eventName.startsWith("RecordingCompleted") || eventName.startsWith("RecordingStopped"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
            return;
        }

        // Check that we have a valid MediaFile.
        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: null MediaFile");
            return;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Finished recording " + AiringAPI.GetAiringTitle(MediaFile) + " - " + ShowAPI.GetShowEpisode(MediaFile));

        // If it's a Manual, Favorite, or TimedRecord (manual) we do not need to worry about it.
        if (AiringAPI.IsFavorite(MediaFile) || AiringAPI.IsManualRecord(MediaFile)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Is not an Intelligent Recording.");
            return;
        }

        // Create the DataStore which will allow us to access the data for this MediaFile.
        DataStore store = new DataStore(MediaFile);

        int maxToKeep;

        // If it's monitored keep the number specified. If it's not monitored use the
        // global default.
        if (store.isMonitored()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Using max for this show.");
            maxToKeep = store.getMax();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Using global max.");
            maxToKeep = Util.GetIntProperty(PROPERTY_DEFAULT_MAX, DEFAULT_MAX_STRING);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Max to keep = " + (maxToKeep==DEFAULT_MAX ? "unlimited" : maxToKeep));

        // See how many are already recorded.
        int numberRecorded = Util.getNumberRecorded(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Number already recorded = " + numberRecorded);

        // If it's unlimited or below the threshhold don't worry about it.
        if (maxToKeep==UNLIMITED || numberRecorded<=maxToKeep) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Below threshhold.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Threshhold exceeded. Deleting one or more " + AiringAPI.GetAiringTitle(MediaFile));

        // Get the direction to sort.
        boolean keepOldest = Configuration.GetServerProperty(PROPERTY_KEEP_OLDEST, "true").equalsIgnoreCase("true");
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Keep oldest = " + keepOldest);

        // Get all of the recordings in the proper order. Recordings at the beginning of the
        // List will be deleted first.
        List<Object> allRecorded = Util.getAllRecorded(MediaFile, "GetAiringStartTime", keepOldest);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Sorted list size = " + allRecorded.size());

        if (Log.getInstance().GetLogLevel() <= Log.LOGLEVEL_VERBOSE) {
            for (Object MF : allRecorded)
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "sageEvent: Date recorded = " + Utility.PrintDateLong(AiringAPI.GetAiringStartTime(MF)) + " : " + Utility.PrintTimeLong(AiringAPI.GetAiringStartTime(MF)) + " - " + AiringAPI.GetAiringTitle(MF) + " - " + ShowAPI.GetShowEpisode(MF));
        }

        boolean reduceToMax = Configuration.GetServerProperty(PROPERTY_REDUCE_TO_MAX, "false").equalsIgnoreCase("true");

        // Calculate how many to delete.
        int numberToDelete = (reduceToMax ? numberRecorded - maxToKeep : 1);
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Need to delete " + numberToDelete);

        // Sanity check.
        if (allRecorded==null || allRecorded.size()<numberToDelete || numberToDelete < 1) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Internal error. numberToDelete exceeds allRecorded. Deleting this MediaFile.");
            MediaFileAPI.DeleteFile(MediaFile);
            return;
        }

        for (int i=0; i<numberToDelete; i++) {
            Object MF = allRecorded.get(i);

            //Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: TESTMODE. Would have deleted " + AiringAPI.GetAiringTitle(MF) + " - " + ShowAPI.GetShowEpisode(MF));
            if (MediaFileAPI.DeleteFile(MF))
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Deleted " + AiringAPI.GetAiringTitle(MF) + " - " + ShowAPI.GetShowEpisode(MF));
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Failed to delete " + AiringAPI.GetAiringTitle(MF) + " - " + ShowAPI.GetShowEpisode(MF));
        }
    }
}
