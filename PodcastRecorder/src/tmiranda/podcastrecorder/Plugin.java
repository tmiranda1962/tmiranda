/*
 *
Recording Flow Map


RecordManager

	Gets List of UnrecordedEpisode by invoking podcast.getEpisodesOnWebServer()

		podcast.getEpisodesOnWebServer
			gets RSSItems, creates UnrecordedEpisode(podcast, RSSItem)

				UnrecordedEpisode constructor
					super's Episode (podcast,EpisodeID) which sets podcast and EpisodeID
					Creates empty list of URLs
					Sets ChanItem

	Invokes record() for each UnrecordedEpisode, if success
		Invokes podcast.setEpisodeRecordedInDatabase() which adds Episode to podcast.episodesEverRecorded and writes to database


UnrecordedEpisode.record()
	Invokes this.startRecord()
		Creates a new RecordingEpisode with all necessary data
		DM.addRecording which adds the RequestID and RecordingEpisode to the Recordings Map
		DM.addActiveDownloads which adds RequestID to ActiveDownloads
		DM.addItem which invokes DT.addItem which adds RecordingEpisode to RecordingMaps (BlockingQueue).

	Invokes this.isRecording() which gets DM->CurrentlyRecordingID and compares to this RecordingID
	Invokes this.recordedSuccessfully() which checks if RecordingID is in CompletedDownloads

DownloadThread
	while !stop
		Wait for RecordingEpisode to appear in the RecordingMaps
		Get all of the RSSItems for the FeedContext (leftover since the RSSItems were not previously Serializable)
		Sets the correct ChanItem
		Sets the file extension based on the URL
		Sets the tempFile
		Invokes RecordingEpisode.download() which tries all URLs in List
		Invokes RecordingEpisode.MoveToFinalLocation()
		Invokes RecordingEpisode.ImportAsMediaFile()
		If anything failed Invokes RecordingEpisode.failed
			DM.setCurrentlyRecordingID to null
			DM.addFailedDownloads
			DM.removeActiveDownloads
		Otherwise Invokes RecordingEpisode.completed
			DM.setCurrentlyRecordingID to null
			DM.addCompletedDownloads
			DM.removeActiveDownloads

 */

package tmiranda.podcastrecorder;

import sage.*;
import sagex.api.*;
import java.io.*;
import java.util.*;
import ortus.mq.EventListener;

/**
 *
 * @author Tom Miranda
 * <p>
 * Main class to handle the interface between the SageTV Core Plugin Manager and the Malore Online Browser.
 * The class starts the RecordManager and CleanupThread as well as handling requests to start and stop
 * the various classes related to the Malore Online Browser.
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    private final String VERSION = "0.20";

    private TimerTask RecordManager;
    private Timer RecordManagerTimer;

// CleanupThread should look at sage property for location of sage downloaded files?
    private TimerTask CleanupThread;
    private Timer CleanupTimer;

    private sage.SageTVPluginRegistry registry;
    private sage.SageTVEventListener listener;

    //private EventListener ClientListener;
    //private EventListener ServerListener;

    // Define all of the properties used in this plugin.
    public static final String PROPERTY_LOGLEVEL                    = "podcastrecorder/loglevel";
    public static final String PROPERTY_RECORD_MANAGER_CYCLE_TIME   = "podcastrecorder/record_manager_cycle_time";
    public static final String PROPERTY_ENABLE_CLEANUP_THREAD       = "podcastrecorder/enable_cleanup_thread";
    public static final String PROPERTY_CLEANUP_THREAD_DELAY        = "podcastrecorder/cleanup_thread_delay";
    public static final String PROPERTY_CLEANUP_MAX_AGE_OF_TEMPFILE = "podcastrecorder/max_age_of_tempfile";  // Not implemented in Plugin
    public static final String PROPERTY_CLEANUP_ONLINE_VIDEO        = "podcastrecorder/cleanup_onlinevideo"; // Not implemented in Plugin
    public static final String PROPERTY_CLEANUP_RSS                 = "podcastrecorder/cleanup_rss"; // Not implemented in Plugin
    public static final String PROPERTY_RECORD_DIRECTORY            = "podcastrecorder/record_directory";
    public static final String PROPERTY_RECORD_SUBDIRECTORY         = "podcastrecorder/record_subdirectory";
    public static final String PROPERTY_SHOWTITLEASSUBDIR           = "podcastrecorder/use_ShowTitle_as_SubDirectory";
    public static final String PROPERTY_SHOWTITLEINFILENAME         = "podcastrecorder/use_ShowTitle_in_FileName";
    public static final String PROPERTY_MAXFILELENGTH               = "podcastrecorder/download_file_max_length";
    public static final String PROPERTY_DOWNLOADATEOF               = "podcastrecorder/download_at_EOF";
    public static final String PROPERTY_FILE_MAX_LENGTH             = "podcastrecorder/download_file_max_length";
    public static final String PROPERTY_RERECORD_DELETED            = "podcastrecorder/rerecord_deleted";
    public static final String PROPERTY_RECORD_NEW                  = "podcastrecorder/record_new";
    public static final String PROPERTY_AUTO_DELETE                 = "podcastrecorder/auto_delete";
    public static final String PROPERTY_MAX_RECORD                  = "podcastrecorder/max_to_record";

    // Define the srttings used in this plugin.
    public static final String SETTING_RECDIR = "RecDir";
    public static final String SETTING_RECSUBDIR = "RecSubDir";
    public static final String SETTING_SHOWTITLEASSUBDIR = "ShowTitleAsSubdir";
    public static final String SETTING_SHOWTITLEINFILENAME = "ShowTitleInFileName";
    public static final String SETTING_MAXFILENAMELENGTH = "MaxFileNameLength";
    public static final String SETTING_RECORDAFTERWATCHED = "RecordAfterWatched";
    public static final String SETTING_SRMCYCLETIME = "SRMCycleTime";
    public static final String SETTING_LOGLEVEL = "LogLevel";
    public static final String SETTING_ENABLE_CLEANUP_THREAD = "EnableCleanup";
    public static final String SETTING_RERECORD_DELETED = "ReRecord";
    public static final String SETTING_RECORD_NEW = "RecNew";
    public static final String SETTING_AUTO_DELETE = "AutoDelete";
    public static final String SETTING_MAX_RECORD = "MaxRecord";

    // Settings that are not yet implemented and not noted above in the properties.
    //   Manual run of cleanup thread.
    //   Change directory cleanup thread monitors.
    //   Display last run time of cleanup thread.
    //   Change how often cleanup thread runs.
    //   Manually kill off the RecordingManager.
    //   Manually kill off DownloadThread
    //
    //   Should probably have a better way to choose the record directory.
    //     Give ability to cycle through import and recording dirs.  GetLibraryImportPaths(), GetVideoDirectories()


    private MQDataPutter MQDataPutter;

    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
        listener = this;
        Registry.eventSubscribe(listener, "PlaybackFinished");
    }

    public void Plugin(sage.SageTVPluginRegistry Registry, boolean reset) {
        registry = Registry;
        listener = this;
        Registry.eventSubscribe(listener, "PlaybackFinished");
        if (reset)
            resetConfig();
    }

    // This method is called when the plugin should startup.
    public void start() {

        System.out.println("PodcastRecorder starting. Version = " + VERSION);

        Log.getInstance().SetLogLevel(SageUtil.GetIntProperty(PROPERTY_LOGLEVEL, Log.LOGLEVEL_ERROR));

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Starting. Log level set to " + Log.getInstance().GetLogLevel());

        // If enabled, start the CleanupThread as a TimerTask.
        if (SageUtil.GetBoolProperty(PROPERTY_ENABLE_CLEANUP_THREAD, true)) {
            CleanupThread = new CleanupThread();
            CleanupTimer = new Timer();
            Long sleeptime = SageUtil.GetLongProperty(PROPERTY_CLEANUP_THREAD_DELAY, 60L * 60L * 1000L);
            CleanupTimer.scheduleAtFixedRate(CleanupThread, 10000L, sleeptime);
        }

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin running as a SageClient.");
            return;
        }

        // Start the listener for MQ messages destined to this server.
        //ServerListener = new MQListenerServer();
        MQDataPutter = new MQDataPutter();

        // Start the DownloadThread by gettng an instance of the DownloadManger.
        DownloadManager.getInstance();

        // Start the RecordManager as a TimerTask.
        RecordManager = new RecordManager();
        RecordManagerTimer = new Timer();
        Long sleeptime = SageUtil.GetLongProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, "43200000");
        RecordManagerTimer.scheduleAtFixedRate(RecordManager, 60000L, sleeptime);
    }

    public static void RecordManagerManualRun() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Manual run RecordManager.");
        TimerTask RecordManager = new RecordManager();
        Timer RecordManagerTimer = new Timer();
        RecordManagerTimer.schedule(RecordManager, 0L);
    }

    // This method is called when the plugin should shutdown
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Stop received from Plugin Manager.");
        CleanupThread.cancel();

        if (!Global.IsClient()) {
            RecordManager.cancel();
        }
        
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    public void destroy() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Destroy received from Plugin Manager.");
        if (!Global.IsClient()) {
            RecordManager = null;
            RecordManagerTimer = null;
            DownloadManager.getInstance().destroy();
        }

        CleanupThread = null;
        CleanupTimer = null;
        Log.getInstance().destroy();

        // Need to stop an recording that is in progress?

        //ClientListener = null;
        //ServerListener = null;
    }


    // Returns the names of the settings for this plugin
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigSetting received from Plugin Manager.");

         String[] S = { SETTING_RECORD_NEW,
                        SETTING_RECDIR,
                        SETTING_RECSUBDIR,
                        SETTING_MAX_RECORD,
                        SETTING_AUTO_DELETE,
                        SETTING_SHOWTITLEASSUBDIR,
                        SETTING_SHOWTITLEINFILENAME,
                        SETTING_MAXFILENAMELENGTH,
                        SETTING_RERECORD_DELETED,
                        SETTING_SRMCYCLETIME,
                        SETTING_ENABLE_CLEANUP_THREAD,
                        SETTING_LOGLEVEL};
        return S;
    }

	// Returns the current value of the specified setting for this plugin
    public String getConfigValue(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Plugin: getConfigValue received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_RECDIR))
            return Configuration.GetServerProperty(PROPERTY_RECORD_DIRECTORY, GetFirstImportDirectory());
        else if (setting.startsWith(SETTING_RECSUBDIR))
            return Configuration.GetServerProperty(PROPERTY_RECORD_SUBDIRECTORY, "Online Videos");
        else if (setting.startsWith(SETTING_SHOWTITLEASSUBDIR))
            return Configuration.GetServerProperty(PROPERTY_SHOWTITLEASSUBDIR, "false");
        else if (setting.startsWith(SETTING_SHOWTITLEINFILENAME))
            return Configuration.GetServerProperty(PROPERTY_SHOWTITLEINFILENAME, "false");
        else if (setting.startsWith(SETTING_MAXFILENAMELENGTH))
            return Configuration.GetServerProperty(PROPERTY_MAXFILELENGTH, "75");
        else if (setting.startsWith(SETTING_ENABLE_CLEANUP_THREAD))
            return Configuration.GetServerProperty(PROPERTY_ENABLE_CLEANUP_THREAD, "true");
        else if (setting.startsWith(SETTING_RERECORD_DELETED))
            return Configuration.GetServerProperty(PROPERTY_RERECORD_DELETED, "true");
        else if (setting.startsWith(SETTING_RECORD_NEW))
            return Configuration.GetServerProperty(PROPERTY_RECORD_NEW, "false");
        else if (setting.startsWith(SETTING_AUTO_DELETE))
            return Configuration.GetServerProperty(PROPERTY_AUTO_DELETE, "false");
        else if (setting.startsWith(SETTING_MAX_RECORD))
            return Configuration.GetServerProperty(PROPERTY_MAX_RECORD, "0");
        else if (setting.startsWith(SETTING_RECORDAFTERWATCHED))
            return Configuration.GetServerProperty(PROPERTY_DOWNLOADATEOF, "No");
        else if (setting.startsWith(SETTING_SRMCYCLETIME)) {
            Long sleeptime = 12L * 60L * 60L * 1000L;   // 12 hours.
            String ms = Configuration.GetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, sleeptime.toString());
            Long hours = java.lang.Long.parseLong(ms) / 60L / 60L / 1000L;
            return hours.toString();
        } else if (setting.startsWith(SETTING_LOGLEVEL)) {
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

        else return "UNKNOWN";
    }

    /**
     * Gets the first Video Import directory.
     * <p>
     * @return The first video import directory.
     */
    private static String GetFirstImportDirectory() {
            File[] files = Configuration.GetVideoLibraryImportPaths();

            String DefaultPath;
            if (files.length > 0)
                DefaultPath = files[0].toString();
            else
                DefaultPath = "MaloreOnlineVideo";
            return DefaultPath;
    }

	// Returns the current value of the specified multichoice setting for
	// this plugin
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
    public int getConfigType(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getConfigType received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_RECDIR))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_RECSUBDIR))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_SHOWTITLEASSUBDIR))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_SHOWTITLEINFILENAME))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_MAXFILENAMELENGTH))
            return CONFIG_INTEGER;
        else if (setting.startsWith(SETTING_RECORDAFTERWATCHED))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_SRMCYCLETIME))
            return CONFIG_INTEGER;
        else if (setting.startsWith(SETTING_LOGLEVEL))
            return CONFIG_CHOICE;
        else if (setting.startsWith(SETTING_ENABLE_CLEANUP_THREAD))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_RERECORD_DELETED))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_RECORD_NEW))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_MAX_RECORD))
            return CONFIG_INTEGER;
        else if (setting.startsWith(SETTING_AUTO_DELETE))
            return CONFIG_BOOL;
        else return CONFIG_TEXT;
    }

    // Sets a configuration value for this plugin
    public void setConfigValue(String setting, String value) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValue received from Plugin Manager. Setting = " + setting + ":" + value);

        if (setting.startsWith(SETTING_RECDIR)) {
            Configuration.SetServerProperty(PROPERTY_RECORD_DIRECTORY, value);
        } else if (setting.startsWith(SETTING_RECSUBDIR)) {
            Configuration.SetServerProperty(PROPERTY_RECORD_SUBDIRECTORY, value);
        } else if (setting.startsWith(SETTING_SHOWTITLEASSUBDIR)) {
            Configuration.SetServerProperty(PROPERTY_SHOWTITLEASSUBDIR, value);
        } else if (setting.startsWith(SETTING_SHOWTITLEINFILENAME)) {
            Configuration.SetServerProperty(PROPERTY_SHOWTITLEINFILENAME, value);
        } else if (setting.startsWith(SETTING_MAXFILENAMELENGTH)) {
            Configuration.SetServerProperty(PROPERTY_MAXFILELENGTH, value);
        } else if (setting.startsWith(SETTING_ENABLE_CLEANUP_THREAD)) {
            Configuration.SetServerProperty(PROPERTY_ENABLE_CLEANUP_THREAD, value);
        } else if (setting.startsWith(SETTING_RECORDAFTERWATCHED)) {
            Configuration.SetServerProperty(PROPERTY_DOWNLOADATEOF, value);
        } else if (setting.startsWith(SETTING_RERECORD_DELETED)) {
            Configuration.SetServerProperty(PROPERTY_RERECORD_DELETED, value);
        } else if (setting.startsWith(SETTING_RECORD_NEW)) {
            Configuration.SetServerProperty(PROPERTY_RECORD_NEW, value);
        } else if (setting.startsWith(SETTING_AUTO_DELETE)) {
            Configuration.SetServerProperty(PROPERTY_AUTO_DELETE, value);
        } else if (setting.startsWith(SETTING_MAX_RECORD)) {
            Integer max = 0;
            
            try {
                max = java.lang.Integer.parseInt(value);
            } catch (NumberFormatException e) {
                max = 0;
            }
            
            Configuration.SetServerProperty(PROPERTY_MAX_RECORD, max.toString());
        } else if (setting.startsWith(SETTING_SRMCYCLETIME)) {
            Long ms = 0L;
            int hours = 0;

            try {
                hours = java.lang.Integer.parseInt(value);
            } catch (NumberFormatException e) {
                hours = 12;
            }

            if (hours <= 0) {
                ms = 12L * 60L * 60L * 1000L;
            } else {
                ms = hours * 60L * 60L * 1000L;
            }
            Configuration.SetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, ms.toString());
        } else if (setting.startsWith(SETTING_LOGLEVEL)) {
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

    // Sets a configuration values for this plugin for a multiselect choice
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "setConfigValues received from Plugin Manager. Setting = " + setting);
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith(SETTING_RECDIR)) {

            File[] files = Configuration.GetVideoLibraryImportPaths();

            String[] values = new String[files.length];

            for (int i=0; i<files.length; i++) {
                values[i] = files[i].toString();
            }

            return values;
        } else if (setting.startsWith(SETTING_RECORDAFTERWATCHED)) {
            String[] values = {"No", "Ask", "Yes"};
            return values;
        } else if (setting.startsWith(SETTING_LOGLEVEL)) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else
            return null;
    }

    // Returns the help text for a configuration setting
    public String getConfigHelpText(String setting) {

        if (setting.startsWith(SETTING_RECDIR)) {
            return "Set the default Podcast recording directory.";
        } else if (setting.startsWith(SETTING_RECSUBDIR)) {
            return "Set the default Podcast recording SubDirectory, if not the Show Title.";
        } else if (setting.startsWith(SETTING_SHOWTITLEASSUBDIR)) {
            return "If set to True each Show will go into its own subdirectory. If set to False the Shows will go into the default subdirectory.";
        } else if (setting.startsWith(SETTING_SHOWTITLEINFILENAME)) {
            return "If set to True the File Name of the recorded Podcast will start with the Show Title.";
        } else if (setting.startsWith(SETTING_MAXFILENAMELENGTH)) {
            return "The maximum number of characters in the file name. (Some Podcasts use very long names. This will truncate them.)";
        } else if (setting.startsWith(SETTING_RECORDAFTERWATCHED)) {
            return "Automatically Record Podcasts after Watching Them.";
        } else if (setting.startsWith(SETTING_SRMCYCLETIME)) {
            return "The number of hours to wait before checking for new Episodes of your Favorite podcasts.";
        } else if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Set the Debug Logging Level.";
        } else if (setting.startsWith(SETTING_ENABLE_CLEANUP_THREAD)) {
            return "Cleans up old files that are sometime left behind. Reboot needed to take effect.";
        } else if (setting.startsWith(SETTING_RERECORD_DELETED)) {
            return "If True episodes that have been deleted will be re-recorded if they are still available on the web.";
        } else if (setting.startsWith(SETTING_RECORD_NEW)) {
            return "Record new episodes that become available on the web.";
        } else if (setting.startsWith(SETTING_AUTO_DELETE)) {
            return "If True the oldest watched episodes will be deleted to make room for new episodes.";
        } else if (setting.startsWith(SETTING_MAX_RECORD)) {
            return "0 = unlimited.";
        } else
            return null;
    }

    // Returns the label used to present this setting to the user
    public String getConfigLabel(String setting) {

        if (setting.startsWith(SETTING_RECDIR)) {
            return "Default Recording Directory";
        } else if (setting.startsWith(SETTING_RECSUBDIR)) {
            return "Default Recording SubDirectory.";
        } else if (setting.startsWith(SETTING_SHOWTITLEASSUBDIR)) {
            return "Use the Show Title as the SubDirectory";
        } else if (setting.startsWith(SETTING_SHOWTITLEINFILENAME)) {
            return "Use the Show Title in the File Name";
        } else if (setting.startsWith(SETTING_MAXFILENAMELENGTH)) {
            return "Maximum Length of File Name";
        } else if (setting.startsWith(SETTING_RECORDAFTERWATCHED)) {
            return "Record Podcasts After Watching Them";
        } else if (setting.startsWith(SETTING_SRMCYCLETIME)) {
            return "Favorite Recording Cycle Time (Hours)";
        } else if (setting.startsWith(SETTING_ENABLE_CLEANUP_THREAD)) {
            return "Enable Cleanup";
        } else if (setting.startsWith(SETTING_LOGLEVEL)) {
            return "Debug Logging Level";
        } else if (setting.startsWith(SETTING_RERECORD_DELETED)) {
            return "Re-Record Deleted Episodes";
        } else if (setting.startsWith(SETTING_RECORD_NEW)) {
            return "Record New Episodes";
        } else if (setting.startsWith(SETTING_AUTO_DELETE)) {
            return "Allow Auto Delete";
        } else if (setting.startsWith(SETTING_MAX_RECORD)) {
            return "Maximum Number of Episodes to Keep";
        } else return null;
    }

    // Resets the configuration of this plugin
    public void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: resetConfig received from Plugin Manager.");
        Configuration.SetServerProperty(PROPERTY_RECORD_DIRECTORY, GetFirstImportDirectory());
        Configuration.SetServerProperty(PROPERTY_RECORD_SUBDIRECTORY, "Online Videos");
        Configuration.SetServerProperty(PROPERTY_SHOWTITLEASSUBDIR, "false");
        Configuration.SetServerProperty(PROPERTY_SHOWTITLEINFILENAME, "true");
        Configuration.SetServerProperty(PROPERTY_ENABLE_CLEANUP_THREAD, "true");
        Configuration.SetServerProperty(PROPERTY_MAXFILELENGTH, "75");
        Configuration.SetServerProperty(PROPERTY_DOWNLOADATEOF, "No");
        Configuration.SetServerProperty(PROPERTY_RERECORD_DELETED, "true");
        Configuration.SetServerProperty(PROPERTY_RECORD_NEW, "false");
        Configuration.SetServerProperty(PROPERTY_AUTO_DELETE, "false");
        Configuration.SetServerProperty(PROPERTY_MAX_RECORD, "0");
        Long sleeptime = 12L * 60L * 60L * 1000L;   // 12 hours.
        Configuration.SetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, sleeptime.toString());
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

    public void sageEvent(String eventName, java.util.Map eventVars) {

        Object MediaFile = eventVars.get("MediaFile");
        String UIContext = (String)eventVars.get("UIContext");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: EOF for " + UIContext + ":" + MediaFileAPI.GetMediaTitle(MediaFile));
    }

}