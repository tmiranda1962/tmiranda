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

    final static String VERSION = "0.70 04.XX.2011";

    private DataStore   dataStore;

    private TimerTask RecordManager;
    private Timer RecordManagerTimer;

    // CleanupThread should look at sage property for location of sage downloaded files?
    private TimerTask CleanupThread;
    private Timer CleanupTimer;

    //private sage.SageTVPluginRegistry registry;
    //private sage.SageTVEventListener listener;

    // Define all of the properties used in this plugin.
    public static final String PROPERTY_LOGLEVEL                    = "podcastrecorder/loglevel";
    public static final String PROPERTY_RECORD_MANAGER_CYCLE_TIME   = "podcastrecorder/record_manager_cycle_time";
    public static final String PROPERTY_DEFAULT_RECORD_MANAGER_CYCLE_TIME = "43200000";
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
    public static final String PROPERTY_MAX_RSS_ITEMS               = "podcastrecorder/max_rss_items";

    // Define the settings used in this plugin.
    public static final String SETTING_RECDIR                   = "RecDir";
    public static final String SETTING_RECSUBDIR                = "RecSubDir";
    public static final String SETTING_SHOWTITLEASSUBDIR        = "ShowTitleAsSubdir";
    public static final String SETTING_SHOWTITLEINFILENAME      = "ShowTitleInFileName";
    public static final String SETTING_MAXFILENAMELENGTH        = "MaxFileNameLength";
    public static final String SETTING_RECORDAFTERWATCHED       = "RecordAfterWatched";
    public static final String SETTING_SRMCYCLETIME             = "SRMCycleTime";
    public static final String SETTING_LOGLEVEL                 = "LogLevel";
    public static final String SETTING_ENABLE_CLEANUP_THREAD    = "EnableCleanup";
    public static final String SETTING_RERECORD_DELETED         = "ReRecord";
    public static final String SETTING_RECORD_NEW               = "RecNew";
    public static final String SETTING_AUTO_DELETE              = "AutoDelete";
    public static final String SETTING_MAX_RECORD               = "MaxRecord";

    public static final String SETTING_SHOW_ADVANCED            = "ShowAdvanced";
    public static final String PROPERTY_SHOW_ADVANCED           = "podcastrecorder/show_advanced";

    public static final String SETTING_DUMP_DB                  = "DumpDB";
    public static final String SETTING_REC_MGR_MANUAL_RUN       = "ManualRun";
    public static final String SETTING_UPDATE_DB                = "UpdateDB";

    public static boolean DBDumped = false;
    public static boolean DBUpdated = false;

    public static final String SETTING_MESSAGE_AFTER_RECORD     = "RecordMessage";
    public static final String PROPERTY_MESSAGE_AFTER_RECORD    = "podcastrecorder/record_message";

    public static final String SETTING_MESSAGE_IF_NEW_AVAIL     = "NewAvailMessage";
    public static final String PROPERTY_MESSAGE_IF_NEW_AVAIL    = "podcastrecorder/new_avail_message";

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


    private MQDataPutter    MQDataPutter;
    //private DataStore       DataStore;

    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        //registry = Registry;
        //listener = this;
        //Registry.eventSubscribe(listener, "PlaybackFinished");
    }

    public Plugin(sage.SageTVPluginRegistry Registry, boolean reset) {
        //registry = Registry;
        //listener = this;
        //Registry.eventSubscribe(listener, "PlaybackFinished");
        if (reset)
            resetConfig();
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {

        System.out.println("PodcastRecorder starting. Version = " + VERSION);

        Log.getInstance().SetLogLevel(SageUtil.GetIntProperty(PROPERTY_LOGLEVEL, Log.LOGLEVEL_ERROR));

        System.out.println("PodcastRecorder: Loglevel is " + Log.getInstance().GetLogLevel());

        // If enabled, start the CleanupThread as a TimerTask.
        if (SageUtil.GetBoolProperty(PROPERTY_ENABLE_CLEANUP_THREAD, true)) {
            CleanupThread = new CleanupThread();
            CleanupTimer = new Timer();
            Long sleeptime = SageUtil.GetLongProperty(PROPERTY_CLEANUP_THREAD_DELAY, 60L * 60L * 1000L);
            CleanupTimer.scheduleAtFixedRate(CleanupThread, 10000L, sleeptime);
        }

        // Initialize the DataStore by creating an instance.
        //DataStore = DataStore.getInstance();

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin running as a SageClient.");
            return;
        }
        
        // Load the database into memory.
        dataStore = new DataStore();

        // Start the listener for MQ messages destined to this server.
        MQDataPutter = new MQDataPutter();

        // Start the DownloadThread by gettng an instance of the DownloadManger
        // and starting the DownloadThread.
        DownloadManager.getInstance().startDownloadThread();

        // Start the RecordManager as a TimerTask.
        RecordManager = new RecordManager();
        RecordManagerTimer = new Timer();
        Long sleeptime = SageUtil.GetLongProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, PROPERTY_DEFAULT_RECORD_MANAGER_CYCLE_TIME);
        RecordManagerTimer.scheduleAtFixedRate(RecordManager, 60000L, sleeptime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: RecordManager cycle time " + sleeptime + ":" + sleeptime/1000/60);
    }

    public static void RecordManagerManualRun() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Manual run RecordManager.");
        TimerTask RecordManager = new RecordManager();
        Timer RecordManagerTimer = new Timer();
        RecordManagerTimer.schedule(RecordManager, 0L);
    }

    // This method is called when the plugin should shutdown
    @Override
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Stop received from Plugin Manager.");
        CleanupTimer.cancel();                          // Don't start the CleanupThread anymore.
        CleanupThread.cancel();                         // Kill off the currently running CleaupThread.

        if (!Global.IsClient()) {
            DownloadManager.getInstance().destroy();    // Stop any download in progress.
            RecordManagerTimer.cancel();                // Don't start the RecordManager anymore.
            RecordManager.cancel();                     // Kill off the currently running RecordManager.
        }

    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    @Override
    public void destroy() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Destroy received from Plugin Manager.");
        if (!Global.IsClient()) {
            RecordManager = null;
            RecordManagerTimer = null;
            DownloadManager.getInstance().destroy();
            MQDataPutter = null;
        }

        CleanupThread = null;
        CleanupTimer = null;
        DataStore.stop();
        Log.getInstance().destroy();
    }


    // Returns the names of the settings for this plugin
    @Override
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigSetting received from Plugin Manager.");

        List<String> CommandList = new ArrayList<String>();

        // These options are allowed under all circumstances.
        CommandList.add(SETTING_RECORD_NEW);
        CommandList.add(SETTING_RECDIR);
        CommandList.add(SETTING_RECSUBDIR);
        CommandList.add(SETTING_MAX_RECORD);
        CommandList.add(SETTING_AUTO_DELETE);
        CommandList.add(SETTING_SHOWTITLEASSUBDIR);
        CommandList.add(SETTING_SHOWTITLEINFILENAME);
        CommandList.add(SETTING_MAXFILENAMELENGTH);
        CommandList.add(SETTING_RERECORD_DELETED);

        if (!Global.IsClient()) {
            CommandList.add(SETTING_SRMCYCLETIME);
        }
        
        CommandList.add(SETTING_ENABLE_CLEANUP_THREAD);
        CommandList.add(SETTING_LOGLEVEL);
        CommandList.add(SETTING_SHOW_ADVANCED);

        if (SageUtil.GetBoolProperty(PROPERTY_SHOW_ADVANCED, Boolean.FALSE)) {
            CommandList.add(SETTING_MESSAGE_AFTER_RECORD);
            CommandList.add(SETTING_MESSAGE_IF_NEW_AVAIL);


            // Can't do these actions from a SageClient.
            if (!Global.IsClient()) {
                CommandList.add(SETTING_REC_MGR_MANUAL_RUN);
                CommandList.add(SETTING_UPDATE_DB);
                CommandList.add(SETTING_DUMP_DB);
            }
        }

        return (String[])CommandList.toArray(new String[CommandList.size()]);
    }

    // Returns the current value of the specified setting for this plugin
    @Override
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
        else if (setting.startsWith(SETTING_SHOW_ADVANCED))
            return Configuration.GetServerProperty(PROPERTY_SHOW_ADVANCED, "false");
        else if (setting.startsWith(SETTING_MESSAGE_AFTER_RECORD))
            return Configuration.GetServerProperty(PROPERTY_MESSAGE_AFTER_RECORD, "false");
        else if (setting.startsWith(SETTING_MESSAGE_IF_NEW_AVAIL))
            return Configuration.GetServerProperty(PROPERTY_MESSAGE_IF_NEW_AVAIL, "false");
        else if (setting.startsWith(SETTING_DUMP_DB))
            return (DBDumped ? "Reset" : "Do It Now");
        else if (setting.startsWith(SETTING_UPDATE_DB))
            return (DBUpdated ? "Reset" : "Do It Now");
        else if (setting.startsWith(SETTING_REC_MGR_MANUAL_RUN)) {
            if (DownloadManager.getInstance().getRecMgrStatus()) {
                return "Already Running";
            } else {
                return "Run Now";
            }
        } else if (setting.startsWith(SETTING_SRMCYCLETIME)) {
            String ms = Configuration.GetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, PROPERTY_DEFAULT_RECORD_MANAGER_CYCLE_TIME);
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
                files = Configuration.GetVideoDirectories();
                if (files.length > 0)
                    DefaultPath = files[0].toString();
                else
                    DefaultPath = "maloreOnlineBrowser";
            
            return DefaultPath;
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
        else if (setting.startsWith(SETTING_SHOW_ADVANCED))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_MESSAGE_AFTER_RECORD))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_MESSAGE_IF_NEW_AVAIL))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_DUMP_DB))
            return CONFIG_BUTTON;
        else if (setting.startsWith(SETTING_UPDATE_DB))
            return CONFIG_BUTTON;
        else if (setting.startsWith(SETTING_REC_MGR_MANUAL_RUN))
            return CONFIG_BUTTON;
        else
            return CONFIG_TEXT;
    }

    // Sets a configuration value for this plugin
    @Override
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
        } else if (setting.startsWith(SETTING_SHOW_ADVANCED)) {
            Configuration.SetServerProperty(PROPERTY_SHOW_ADVANCED, value);
        } else if (setting.startsWith(SETTING_MESSAGE_AFTER_RECORD)) {
            Configuration.SetServerProperty(PROPERTY_MESSAGE_AFTER_RECORD, value);
        } else if (setting.startsWith(SETTING_MESSAGE_IF_NEW_AVAIL)) {
            Configuration.SetServerProperty(PROPERTY_MESSAGE_IF_NEW_AVAIL, value);
        } else if (setting.startsWith(SETTING_UPDATE_DB)) {
            if (!DBUpdated) {
                DownloadManager.getInstance().updateDatabase();
                DBUpdated = true;
            } else {
                DBUpdated = false;
            }
        } else if (setting.startsWith(SETTING_DUMP_DB)) {
            if (!DBDumped) {
                String[] args = {};
                Main.main(args);
                DBDumped =  true;
            } else {
                DBDumped = false;
            }
        } else if (setting.startsWith(SETTING_REC_MGR_MANUAL_RUN)) {
            if (!DownloadManager.getInstance().getRecMgrStatus()) {
                RecordManagerManualRun();
            }
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
                Log.getInstance().write(Log.LOGLEVEL_WARN, "Plugin: Invalid cycle time, setting to 12 hours.");
                hours = 12;
            }

            if (hours <= 0) {
                ms = 12L * 60L * 60L * 1000L;
            } else {
                ms = hours * 60L * 60L * 1000L;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: Setting cycle time to " + ms.toString());
            Configuration.SetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, ms.toString());
            
            // Cancel the currently scheduled runs and reset the schedule.
            RecordManagerTimer.cancel();
            RecordManagerTimer = new Timer();
            Long sleeptime = SageUtil.GetLongProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, PROPERTY_DEFAULT_RECORD_MANAGER_CYCLE_TIME);
            RecordManagerTimer.scheduleAtFixedRate(RecordManager, 60000L, sleeptime);

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
        if (setting.startsWith(SETTING_RECDIR)) {

            File[] importPaths = Configuration.GetVideoLibraryImportPaths();
            File[] recordPaths = Configuration.GetVideoDirectories();

            String[] values = new String[importPaths.length + recordPaths.length];

            int i = 0;
            for (i=0; i<importPaths.length; i++) {
                values[i] = importPaths[i].toString();
            }

            for (int j=0; j<recordPaths.length; j++) {
                values[i+j] = recordPaths[j].toString();
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
    @Override
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
        } else if (setting.startsWith(SETTING_SHOW_ADVANCED)) {
            return "Show the advanced options.";
        } else if (setting.startsWith(SETTING_DUMP_DB)) {
            return "Will NOT have any visible effect, but if Sage debug logging is enabled the contents of the DB will be written to it.";
        } else if (setting.startsWith(SETTING_UPDATE_DB)) {
            return "Will take a long time and will display the Spinning Circle while it's running.";
        } else if (setting.startsWith(SETTING_REC_MGR_MANUAL_RUN)) {
            return "Check the web for new Episodes and record them as needed.";
        } else if (setting.startsWith(SETTING_MESSAGE_AFTER_RECORD)) {
            return "To view the System Messages go to Setup -> System Messages.";
        } else if (setting.startsWith(SETTING_MESSAGE_IF_NEW_AVAIL)) {
            return "To view the System Messages go to Setup -> System Messages.";
        } else
            return null;
    }

    // Returns the label used to present this setting to the user
    @Override
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
        } else if (setting.startsWith(SETTING_SHOW_ADVANCED)) {
            return "Show Advanced Options";
        } else if (setting.startsWith(SETTING_DUMP_DB)) {
            return "Write The DataBase to the Logfile";
        } else if (setting.startsWith(SETTING_UPDATE_DB)) {
            return "Update The Internal Database";
        } else if (setting.startsWith(SETTING_REC_MGR_MANUAL_RUN)) {
            return "Check for New Episodes Now";
        } else if (setting.startsWith(SETTING_MESSAGE_AFTER_RECORD)) {
            return "Show a Msg After Fav. Podcasts are Recorded";
        } else if (setting.startsWith(SETTING_MESSAGE_IF_NEW_AVAIL)) {
            return "Show a Message if New Episodes Are Available";
        } else
            return null;
    }

    // Resets the configuration of this plugin
    @Override
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
        Configuration.SetServerProperty(PROPERTY_RECORD_MANAGER_CYCLE_TIME, PROPERTY_DEFAULT_RECORD_MANAGER_CYCLE_TIME);
        Configuration.SetServerProperty(PROPERTY_SHOW_ADVANCED, "false");
        Configuration.SetServerProperty(PROPERTY_MESSAGE_AFTER_RECORD, "false");
        Configuration.SetServerProperty(PROPERTY_MESSAGE_IF_NEW_AVAIL, "false");
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);
        DBDumped = false;
        DBUpdated = false;
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

        //Object MediaFile = eventVars.get("MediaFile");
        //String UIContext = (String)eventVars.get("UIContext");

        Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Invoked.");
    }

}