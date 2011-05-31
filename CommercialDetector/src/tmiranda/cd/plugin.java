/*
 * CommercialDetector Plugin for SageTV.
 */

package tmiranda.cd;

import sage.*;
import sagex.api.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author Tom Miranda
 * <p>
 * Possible enhancements:
 * - 
 */
public class plugin implements sage.SageTVPlugin, SageTVEventListener {

    private final String                VERSION = "0.9X";
    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;
    private static Integer              NumberScanned;
    private static Map<String, String>  ChannelNames;
    private static Set<String>          Names;

    private static Integer              NumberOfOrphans = -1;

    public static Map<String, Float>    ChannelTimeRatios;

    public static final String          SETTING_USE_INTELLIGENT_SCHEDULING          = "UseIntelligent";
    public static final String          PROPERTY_USE_INTELLIGENT_SCHEDULING         = "cd/use_intelligent";
    public static final String          PROPERTY_DEFAULT_USE_INTELLIGENT_SCHEDULING = "false";

    public static final String          SETTING_TIME_RATIOS             = "TimeRatios";
    public static final String          PROPERTY_TIME_RATIOS            = "cd/time_ratios";
    public static final String          PROPERTY_DEFAULT_TIME_RATIOS    = null;
    public static final Float           RATIO_DEFAULT                   = 0.5F;

    public static final String          SETTING_VIDEO_FILE_EXTENSIONS           = "VideoExt";
    public static final String          PROPERTY_VIDEO_FILE_EXTENSIONS          = "cd/video_ext";
    public static final String          PROPERTY_DEFAULT_VIDEO_FILE_EXTENSIONS  = "mpg,mp4,avi,ts,mkv,m4v";
    public static final String[]        ARRAY_VIDEO_EXTENSIONS                  = new String[] {"mpg", "mp4", "avi", "ts", "mkv", "m4v"};

    public static final String          SETTING_CLEANUP_EXTENSIONS          = "CleanupExt";
    public static final String          PROPERTY_CLEANUP_EXTENSIONS         = "cd/cleanup_ext";
    public static final String          PROPERTY_DEFAULT_CLEANUP_EXTENSIONS = "edl,log,txt,incommercial";
    public static final String[]        ARRAY_CLEANUP_EXTENSIONS            = new String[] {"edl", "log", "txt", "incommercial"};

    public static final String          SETTING_DELETE_ORPHANS = "DeleteOrphans";

    public static final String          SETTING_SHOW_INTELLIGENT_TUNING             = "ShowTuning";
    public static final String          PROPERTY_SHOW_INTELLIGENT_TUNING            = "cd/show_tuning";
    public static final String          PROPERTY_DEFAULT_SHOW_INTELLIGENT_TUNING    = "false";

    public static final String          SETTING_RECORDING_IMPACT_ON_CHANNEL = "ChannelImpact";
    public static final String          SETTING_RECORDING_IMPACT            = "RecordImpact";
    public static final String          SETTING_RUNNING_IMPACT              = "RunningImpact";

    public static final String          SETTING_SHOW_QUEUE      = "ShowQueue";
    public static final String          SETTING_SHOW_RUNNING    = "ShowRunning";

    public Map<String, Integer>         IDMap = null;

    public static final String          PROPERTY_DRIVE_MAP          = "cd/UNC_map";
    public static final String          PROPERTY_DEFAULT_DRIVE_MAP  = null;

    public static final String          SETTING_RESTRICTED_TIMES            = "RestrictTimes";
    public static final String          PROPERTY_RESTRICTED_TIMES           = "cd/restricted_times";
    public static final String          PROPERTY_DEFAULT_RESTRICTED_TIMES   = null;

    public static final String          SETTING_SKIP_CHANNELS   = "SkipChannels";
    public static final String          PROPERTY_SKIP_CHANNELS  = "cd/skip_channels";

    public static final String          SETTING_SKIP_CATEGORIES     = "SkipCategories";
    public static final String          PROPERTY_SKIP_CATEGORIES    = "cd/skip_categories";

    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
        listener = this;
        NumberScanned = 0;
        ChannelNames = new HashMap<String, String>();
        ChannelTimeRatios = new HashMap<String, Float>();
    }

    public plugin(sage.SageTVPluginRegistry Registry, boolean Reset) {
        registry = Registry;
        listener = this;
        NumberScanned = 0;
        ChannelNames = new HashMap<String, String>();
        ChannelTimeRatios = new HashMap<String, Float>();
        if (Reset && !Global.IsClient())
            resetConfig();
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {
        System.out.println("CD: PlugIn: Starting. Version = " + VERSION);

        // Set the loglevel to what's in the .properties file.
        Integer DefaultLevel = Log.LOGLEVEL_WARN;
        String CurrentLevel = Configuration.GetServerProperty("cd/loglevel", DefaultLevel.toString());
        Integer SetLevel = Integer.decode(CurrentLevel);
        Log.getInstance().SetLogLevel(SetLevel);

        switch (Log.getInstance().GetLogLevel()) {
            case Log.LOGLEVEL_ALL:      System.out.println("CD: PlugIn: LogLevel = Maximum."); break;
            case Log.LOGLEVEL_ERROR:    System.out.println("CD: PlugIn: LogLevel = Error."); break;
            case Log.LOGLEVEL_NONE:     System.out.println("CD: PlugIn: LogLevel = None."); break;
            case Log.LOGLEVEL_TRACE:    System.out.println("CD: PlugIn: LogLevel = Trace."); break;
            case Log.LOGLEVEL_VERBOSE:  System.out.println("CD: PlugIn: LogLevel = Verbose."); break;
            case Log.LOGLEVEL_WARN:     System.out.println("CD: PlugIn: LogLevel = Warn."); break;
            default:                    System.out.println("CD: PlugIn: Error.  Unknown LogLevel."); break;
        }

        // If we're running on a client we are done.
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "start: Running in Client mode.");
            return;
        }

        // Set the type of the server so we can tell from a SageClient.
        if (Global.IsWindowsOS()) {
            Configuration.SetServerProperty("cd/server_is", "windows");
        } else if (Global.IsLinuxOS()) {
            Configuration.SetServerProperty("cd/server_is", "linux");
        } else {
            Configuration.SetServerProperty("cd/server_is", "mac");
        }

        // Create the data structures that will be used to determine which ini and profile files
        // will be used.
        Object[] Channels = ChannelAPI.GetAllChannels();
        if (Channels==null || Channels.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "start: No Channels defined.");
        } else {
            for (Object Channel : Channels) {
                String Name = ChannelAPI.GetChannelName(Channel);
                String Number = ChannelAPI.GetChannelNumber(Channel);
                if (Name==null || Number==null || Name.isEmpty() || Number.isEmpty()) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "start: null Name or Number ");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "start: Found channel " + Name + ":" + Number);
                    ChannelNames.put(Name, Number);
                }
            }
            Names = ChannelNames.keySet();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Channelnames created. Size = " + ChannelNames.size());
        }

        // Clear the properties that we use to communicate with SageClients.
        // - processing is a list of the MediaFileIDs of jobs that are being processed.
        // - queue is a list of MediaFileIDs that SageClients want to have processed.
        CSC.getInstance().setStatus(CSC.STATUS_PROCESSING, null);
        CSC.getInstance().setStatus(CSC.STATUS_QUEUE, null);

        // Make sure the DB file and all necessary directories exist.
        ComskipManager.getInstance().makeResources();

        // Restart any jobs that were queued but not completed.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Restarting any pending jobs.");
        ComskipManager.getInstance().startMaxJobs();

        // Print debug info if needed.
        if (Log.getInstance().GetLogLevel()==Log.LOGLEVEL_ALL) {
            List<Object> MediaFilesToQueue = ComskipManager.getInstance().getMediaFilesWithout("T");
            for (Object MediaFile : MediaFilesToQueue) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "start: No edl for " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        }

        // Load the time ratios that tell us how long comskip takes to finish jobs for each channel.
        loadTimeRatios();

        // Subscribe to what we need.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Subscribing to events.");
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingStarted");
        registry.eventSubscribe(listener, "MediaFileRemoved");
        registry.eventSubscribe(listener, "RecordingScheduleChanged");

        // Start the task that will monitor the queue that SageClients use to request jobs.
        if (SageUtil.GetBoolProperty("cd/monitor_clients", "true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Starting MonitorClient.");
            long MonitorClientPeriod = SageUtil.GetLongProperty("cd/monitor_client_period", 60*1000);
            TimerTask MonitorClient = new MonitorClient();
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(MonitorClient, MonitorClientPeriod, MonitorClientPeriod);
        }

        // Start the task that wakes up every hour to see if jobs waiting because they were queued in
        // restricted times should be started.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Starting RestartRestricted.");

        long Frequency = 60 * 60 * 1000;        // Once every hour.

        Calendar Now = Calendar.getInstance();
        int Minute = Now.get(Calendar.MINUTE);
        int MinutesToNextHour = 60 - Minute;
        long MillisToNextHour = MinutesToNextHour * 60L * 1000L;

        //long NowInMillis = Now.getTimeInMillis();

        //Date FirstRun = new Date(NowInMillis + MillisToNextHour + 1000L);

        TimerTask RestartRestricted = new RestartRestricted();
        Timer RRTimer = new Timer();
        RRTimer.scheduleAtFixedRate(RestartRestricted, MillisToNextHour, Frequency);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "start: Minutes until first RestartRestricted " + MinutesToNextHour + ":" + MillisToNextHour);

        // Print a bunch of debug information.
        SystemStatus.getInstance().printSystemStatus();
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
        registry.eventUnsubscribe(listener, "RecordingStopped");
        registry.eventUnsubscribe(listener, "RecordingScheduleChanged");
        ComskipManager.getInstance().stopAll();
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin.
    @Override
    public void destroy() {
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "destroy: Running in Client mode.");
            CSC.getInstance().destroy();
            Log.getInstance().destroy();
            return;
        }

        ComskipManager.getInstance().destroy();
        CSC.getInstance().destroy();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Destroy received from Plugin Manager.");
        Log.getInstance().destroy();
        registry = null;                    // 0.80
        listener = null;                    // 0.80
        ChannelNames = null;
        Names = null;
        ChannelTimeRatios = null;           // 0.80
        IDMap = null;
    }

    // These methods are used to define any configuration settings for the
    // plugin that should be presented in the UI. If your plugin does not
    // need configuration settings; you may simply return null or zero from
    // these methods.

    // Returns the names of the settings for this plugin.
    @Override
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigSetting received from Plugin Manager.");
        boolean TestMode = SageUtil.GetBoolProperty("cd/test_mode", false);

        List<String> CommandList = new ArrayList<String>();

        // These options are allowed under all circumstances.
        CommandList.add("MaxJobs");
        CommandList.add("ComskipLocation");
        CommandList.add("IniLocation");
        CommandList.add("RunSlow");
        CommandList.add(SETTING_RESTRICTED_TIMES);
        CommandList.add(SETTING_CLEANUP_EXTENSIONS);
        CommandList.add(SETTING_VIDEO_FILE_EXTENSIONS);
        CommandList.add(SETTING_SKIP_CHANNELS);
        CommandList.add(SETTING_SKIP_CATEGORIES);
        CommandList.add("StartImmediately");

        String ServerOS = Configuration.GetServerProperty("cd/server_is", "unknown");

        // These options are only allowed if the server in linux.  It does not matter what the client is.
        if (ServerOS.equalsIgnoreCase("linux")) {
            CommandList.add("RunningAsRoot");
            CommandList.add("WineUser");
            CommandList.add("WineHome");
        }

        // Allowed for all.
        CommandList.add("LogLevel");
        CommandList.add("ShowAdvanced");

        // Only show these option if show_advanced. Valid for both Windows and Linux.
        if (SageUtil.GetBoolProperty("cd/show_advanced", false)) {
            CommandList.add(SETTING_USE_INTELLIGENT_SCHEDULING);
            CommandList.add("ComskipParms");


            // These functions do not work on SageClients.  Need to use OrtusMQ at some point
            // in the future.
            if (!Global.IsClient()) {
                CommandList.add("ScanAll");
                CommandList.add(SETTING_DELETE_ORPHANS);
                CommandList.add(SETTING_SHOW_QUEUE);
                CommandList.add("ClearQueue");
                CommandList.add(SETTING_SHOW_RUNNING);
                CommandList.add("StopAll");
            }

            // Only show these options if the server is not linux.  It does not matter what the client is.
            if (!ServerOS.equalsIgnoreCase("linux")) {
                CommandList.add("UseShowAnalyzer");
                CommandList.add("ShowAnalyzerLocation");
                CommandList.add("ProfileLocation");
                CommandList.add("ShowChannels");
            }
        }

        // Dependent on show_advanced which is controlled above.
        if (SageUtil.GetBoolProperty("cd/show_channels", false) && SageUtil.GetBoolProperty("cd/show_advanced", false)) {
            for (String Name : Names) {
                CommandList.add(Name);
            }
        }

        // Enter 5309 in "Max Concurrent Jobs" field.
        if (TestMode) {
            CommandList.add(SETTING_TIME_RATIOS);
            CommandList.add(ComskipJob.SETTING_RUNNING_IMPACT);
            CommandList.add(ComskipJob.SETTING_RECORD_IMPACT);
            CommandList.add(ComskipJob.SETTING_RECORDING_IMPACT_ON_CHANNEL);
            CommandList.add("ManualRun");
            CommandList.add("Restart");
            CommandList.add("SetEnv");
            CommandList.add("UNCMap");
            CommandList.add("SetCommand");
            CommandList.add("RunCommand");
        }

        return (String[])CommandList.toArray(new String[CommandList.size()]);
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
        } else if (setting.startsWith("MaxJobs")) {
            Integer processors = 0;
            switch (getAvailableProcessors()) {
                case 0:
                case 1:
                case 2:
                case 3:
                    processors = 1;
                    break;
                case 4:
                case 5:
                    processors = 2;
                    break;
                default:
                    processors = 1;
                    break;
            }
            return Configuration.GetServerProperty("cd/max_jobs", processors.toString());
        } else if (setting.startsWith("StopAll")) {
            Integer n = ComskipManager.getInstance().getNumberRunning();
            return n.toString();
        } else if (setting.startsWith("ClearQueue") || setting.startsWith(SETTING_SHOW_QUEUE) || setting.startsWith("Restart")) {
            Integer n = ComskipManager.getInstance().getQueueSize(false);
            return n.toString();
        } else if (setting.startsWith("ComskipLocation")) {
            return Configuration.GetServerProperty("cd/comskip_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        } else if (setting.startsWith("IniLocation")) {
            return Configuration.GetServerProperty("cd/ini_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        } else if (setting.startsWith("UNCMap")) {
            return Configuration.GetProperty(PROPERTY_DRIVE_MAP, PROPERTY_DEFAULT_DRIVE_MAP);
        } else if (setting.startsWith("ComskipParms")) {
            return Configuration.GetServerProperty("cd/comskip_parms", "");
        } else if (setting.startsWith(SETTING_VIDEO_FILE_EXTENSIONS)) {
            return Configuration.GetServerProperty(PROPERTY_VIDEO_FILE_EXTENSIONS, PROPERTY_DEFAULT_VIDEO_FILE_EXTENSIONS);
        } else if (setting.startsWith(SETTING_CLEANUP_EXTENSIONS)) {
            return Configuration.GetServerProperty(PROPERTY_CLEANUP_EXTENSIONS, PROPERTY_DEFAULT_CLEANUP_EXTENSIONS);
        } else if (setting.startsWith("WineHome")) {
            return Configuration.GetServerProperty("cd/wine_home", getDefaultWineHome());
        } else if (setting.startsWith("WineUser")) {
            return Configuration.GetServerProperty("cd/wine_user", getDefaultWineUser());
        } else if (setting.startsWith("RunSlow")) {
            return Configuration.GetServerProperty("cd/run_slow", "false");
        } else if (setting.startsWith(SETTING_SKIP_CHANNELS)) {
            return Configuration.GetServerProperty(PROPERTY_SKIP_CHANNELS, "");
        } else if (setting.startsWith(SETTING_SKIP_CATEGORIES)) {
            return Configuration.GetServerProperty(PROPERTY_SKIP_CATEGORIES, "");
        } else if (setting.startsWith("SetEnv")) {
            return Configuration.GetServerProperty("cd/set_env", "WINEPREFIX=/root/.wine,WINEPATH=/root/.wine");
        } else if (setting.startsWith("SetCommand")) {
            return Configuration.GetServerProperty("cd/set_command", "wine,comskip.exe");
        } else if (setting.startsWith("StartImm")) {
            return Configuration.GetServerProperty("cd/start_imm", "false");
        } else if (setting.startsWith("RunCommand")) {
            return "Select Me";
        } else if (setting.startsWith("ManualRun")) {
            return "Select File";
        } else if (setting.startsWith("RunningAsRoot")) {
            return Configuration.GetServerProperty("cd/running_as_root", "true");
        } else if (setting.startsWith("ShowAdvanced")) {
            return Configuration.GetServerProperty("cd/show_advanced", "false");
        } else if (setting.startsWith("UseShowAnalyzer")) {
            return Configuration.GetServerProperty("cd/use_showanalyzer", "false");
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            return Configuration.GetServerProperty("cd/showanalyzer_location", "Select");
        } else if (setting.startsWith("ProfileLocation")) {
            return Configuration.GetServerProperty("cd/profile_location", "Select");
        } else if (setting.startsWith("ShowChannels")) {
            return Configuration.GetServerProperty("cd/show_channels", "false");
        } else if (setting.startsWith(SETTING_DELETE_ORPHANS)) {
            if (NumberOfOrphans == -1) {
                return "Scan";
            } else {
                return NumberOfOrphans.toString();
            }
        } else if (setting.startsWith(SETTING_TIME_RATIOS)) {
            return Configuration.GetServerProperty(PROPERTY_TIME_RATIOS, PROPERTY_DEFAULT_TIME_RATIOS);
        } else if (setting.startsWith(SETTING_USE_INTELLIGENT_SCHEDULING)) {
            return Configuration.GetServerProperty(PROPERTY_USE_INTELLIGENT_SCHEDULING, PROPERTY_DEFAULT_USE_INTELLIGENT_SCHEDULING);
        } else if (setting.startsWith(SETTING_SHOW_INTELLIGENT_TUNING)) {
            return Configuration.GetServerProperty(PROPERTY_SHOW_INTELLIGENT_TUNING, PROPERTY_DEFAULT_SHOW_INTELLIGENT_TUNING);
        } else if (setting.startsWith("ScanAll")) {
            if (NumberScanned==null || NumberScanned==0) {
                return "Queue Files";
            } else {
                return NumberScanned.toString() + " Queued";
            }
        } else if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {
            return "Select";
        } else if (Names.contains(setting)) {
            return Configuration.GetServerProperty("cd/map_"+setting, "Default");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigValue: Unknown setting from getConfigValue = " + setting);
            return "UNKNOWN";
        }
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin.
    @Override
    public String[] getConfigValues(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigValues received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {
            String S = Configuration.GetServerProperty(PROPERTY_RESTRICTED_TIMES, PROPERTY_DEFAULT_RESTRICTED_TIMES);

            if (S==null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getConfigValues: No Restricted times.");
                String[] v = {};
                return v;
            }

            String[] Hours = S.split(",");

            if (Hours==null || Hours.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getConfigValues: Malformed restricted_times " + S);
                String[] v = {};
                return v;
            }

            return Hours;
        } else if (setting.startsWith(SETTING_SHOW_QUEUE)) {

            // Return the queue size.
            Integer size = ComskipManager.getInstance().getQueueSize(false);
            String[] S = {size.toString()};
            return S;
        } else if (setting.startsWith(SETTING_SHOW_RUNNING)) {

            // Return the number running.
            Integer number = ComskipManager.getInstance().getNumberRunning();
            String[] S = {number.toString()};
            return S;
        }

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
        else if (setting.startsWith("MaxJobs"))
            return CONFIG_INTEGER;
        else if (setting.startsWith("StopAll"))
            return CONFIG_BUTTON;
        else if (setting.startsWith("Restart"))
            return CONFIG_BUTTON;
        else if (setting.startsWith(SETTING_SHOW_QUEUE))
            return CONFIG_MULTICHOICE;
        else if (setting.startsWith(SETTING_SHOW_RUNNING))
            return CONFIG_MULTICHOICE;
        else if (setting.startsWith("ClearQueue"))
            return CONFIG_BUTTON;
        else if (setting.startsWith("ComskipLocation"))
            return CONFIG_FILE;
        else if (setting.startsWith("IniLocation"))
            return CONFIG_FILE;
        else if (setting.startsWith("UNCMap"))
            return CONFIG_TEXT;
        else if (setting.startsWith("ComskipParms"))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_CLEANUP_EXTENSIONS))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_VIDEO_FILE_EXTENSIONS))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_SKIP_CHANNELS))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_SKIP_CATEGORIES))
            return CONFIG_TEXT;
        else if (setting.startsWith("WineHome"))
            return CONFIG_DIRECTORY;
        else if (setting.startsWith("RunSlow"))
            return CONFIG_BOOL;
        else if (setting.startsWith("StartImm"))
            return CONFIG_BOOL;
        else if (setting.startsWith("SetEnv"))
            return CONFIG_TEXT;
        else if (setting.startsWith("SetCommand"))
            return CONFIG_TEXT;
        else if (setting.startsWith("RunCommand"))
            return CONFIG_BUTTON;
        else if (setting.startsWith("WineUser"))
            return CONFIG_TEXT;
        else if (setting.startsWith("RunningAsRoot"))
            return CONFIG_BOOL;
        else if (setting.startsWith("ManualRun"))
            return CONFIG_FILE;
        else if (setting.startsWith("ScanAll"))
            return CONFIG_BUTTON;
        else if (setting.startsWith("ShowAdvanced"))
            return CONFIG_BOOL;
        else if (setting.startsWith("UseShowAnalyzer"))
            return CONFIG_BOOL;
        else if (setting.startsWith("ShowAnalyzerLocation"))
            return CONFIG_FILE;
        else if (setting.startsWith("ProfileLocation"))
            return CONFIG_DIRECTORY;
        else if (setting.startsWith(SETTING_RESTRICTED_TIMES))
            return CONFIG_MULTICHOICE;
        else if (setting.startsWith("ShowChannels"))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_TIME_RATIOS))
            return CONFIG_TEXT;
        else if (setting.startsWith(SETTING_USE_INTELLIGENT_SCHEDULING))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_SHOW_INTELLIGENT_TUNING))
            return CONFIG_BOOL;
        else if (setting.startsWith(SETTING_DELETE_ORPHANS))
            return CONFIG_BUTTON;
        else if (Names.contains(setting)) {
            return CONFIG_CHOICE;
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigType: Unknown setting = " + setting);
            return CONFIG_TEXT;
        }
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
        } else if (setting.startsWith("MaxJobs")) {
            int v = 0;
            try {
                v = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                v = 1;
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValue: Invalid MaxJobs entry " + value);
            }
            if (v<=0) value = "1";
            if (v==5309) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "setConfigValue: Toggling TestMode.");
                boolean TestMode = SageUtil.GetBoolProperty("cd/test_mode", false);
                Configuration.SetServerProperty("cd/test_mode", (TestMode ? "false":"true"));
            } else {
                Configuration.SetServerProperty("cd/max_jobs", value);

                // If the user just increased MaxJobs we may be able to start more.
                ComskipManager.getInstance().startMaxJobs();
            }
        } else if (setting.startsWith("ComskipLocation")) {
            Configuration.SetServerProperty("cd/comskip_location", value);
        } else if (setting.startsWith("IniLocation")) {
            Configuration.SetServerProperty("cd/ini_location", value);
        } else if (setting.startsWith("UNCMap")) {
            Configuration.SetProperty(PROPERTY_DRIVE_MAP, value);
        } else if (setting.startsWith("ComskipParms")) {
            Configuration.SetServerProperty("cd/comskip_parms", value);
        } else if (setting.startsWith(SETTING_CLEANUP_EXTENSIONS)) {
            Configuration.SetServerProperty(PROPERTY_CLEANUP_EXTENSIONS, value);
        } else if (setting.startsWith(SETTING_VIDEO_FILE_EXTENSIONS)) {
            Configuration.SetServerProperty(PROPERTY_VIDEO_FILE_EXTENSIONS, value);
        } else if (setting.startsWith("RunSlow")) {
            Configuration.SetServerProperty("cd/run_slow", value);
        } else if (setting.startsWith(SETTING_SKIP_CHANNELS)) {
            Configuration.SetServerProperty(PROPERTY_SKIP_CHANNELS, value);
        } else if (setting.startsWith(SETTING_SKIP_CATEGORIES)) {
            Configuration.SetServerProperty(PROPERTY_SKIP_CATEGORIES, value);
        } else if (setting.startsWith("WineHome")) {
            Configuration.SetServerProperty("cd/wine_home", value);
        } else if (setting.startsWith("StartImm")) {
            Configuration.SetServerProperty("cd/start_imm", value);
        } else if (setting.startsWith("Restart")) {
            ComskipManager.getInstance().startMaxJobs();
        } else if (setting.startsWith("StopAll")) {
            ComskipManager.getInstance().stopAll();
        } else if (setting.startsWith("ClearQueue")) {
            ComskipManager.getInstance().clearQueue();
        } else if (setting.startsWith("SetEnv")) {
            Configuration.SetServerProperty("cd/set_env", value);
        } else if (setting.startsWith("SetCommand")) {
            Configuration.SetServerProperty("cd/set_command", value);
        } else if (setting.startsWith("RunCommand")) {
            ComskipManager.getInstance().runTestCommand(Configuration.GetServerProperty("cd/set_env", "WINEPREFIX=/home/tom/.wine,WINEPATH=/home/tom/.wine"),Configuration.GetServerProperty("cd/set_command", "wine,comskip.exe"));
        } else if (setting.startsWith("RunningAsRoot")) {
            Configuration.SetServerProperty("cd/running_as_root", value);
        } else if (setting.startsWith("ShowAdvanced")) {
            Configuration.SetServerProperty("cd/show_advanced", value);
        } else if (setting.startsWith("UseShowAnalyzer")) {
            Configuration.SetServerProperty("cd/use_showanalyzer", value);
        } else if (setting.startsWith("ShowChannels")) {
            Configuration.SetServerProperty("cd/show_channels", value);
        } else if (setting.startsWith(SETTING_DELETE_ORPHANS)) {
            if (NumberOfOrphans <= 0) {
                NumberOfOrphans = ComskipManager.getInstance().countAllOrphans();
            } else if (NumberOfOrphans > 0) {
                ComskipManager.getInstance().deleteAllOrphans();
                NumberOfOrphans = ComskipManager.getInstance().countAllOrphans();
            }
        } else if (setting.startsWith(SETTING_TIME_RATIOS)) {
            Configuration.SetServerProperty(PROPERTY_TIME_RATIOS, value);
        } else if (setting.startsWith(SETTING_USE_INTELLIGENT_SCHEDULING)) {
            Configuration.SetServerProperty(PROPERTY_USE_INTELLIGENT_SCHEDULING, value);
            if (value.equalsIgnoreCase("false")) {
                ComskipManager.getInstance().startMaxJobs();
            }
        } else if (setting.startsWith(SETTING_SHOW_INTELLIGENT_TUNING)) {
            Configuration.SetServerProperty(PROPERTY_SHOW_INTELLIGENT_TUNING, value);
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            if (value.contains("ShowAnalyzer.exe")) {
                String newValue = value.replace("ShowAnalyzer.exe", "ShowAnalyzerEngine.exe");
                Configuration.SetServerProperty("cd/showanalyzer_location", newValue);
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValue: User choose ShowAnalyzer.exe, fixing " + newValue);
            } else {
                Configuration.SetServerProperty("cd/showanalyzer_location", value);
            }
        } else if (setting.startsWith("ProfileLocation")) {
            Configuration.SetServerProperty("cd/profile_location", value);
        } else if (setting.startsWith("ManualRun")) {
            manualRun(value);
        } else if (setting.startsWith("ScanAll")) {
            List<Object> MediaFilesToQueue = ComskipManager.getInstance().getMediaFilesWithout("T");
            NumberScanned = MediaFilesToQueue.size();

            for (Object MediaFile : MediaFilesToQueue) {
                CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile);
                if (!CDMediaFile.queue()) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValue: Error queuing file " + MediaFileAPI.GetMediaTitle(MediaFile));
                }
            }
        } else if (setting.startsWith("WineUser")) {
            if (!value.equalsIgnoreCase("root")) {
                Configuration.SetServerProperty("cd/wine_user", value);
                Configuration.SetServerProperty("cd/wine_home", getDefaultWineHome());
            }
        } else if (Names.contains(setting)) {
            Configuration.SetServerProperty("cd/map_"+setting, value);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setConfigValue: Unknown setting = " + setting);
        }
        
    }

    // Sets a configuration values for this plugin for a multiselect choice.
    @Override
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValues received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {

            // Put the values into the property string.
            if (values==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValues: null values.");
                return;
            }

            if (values.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "setConfigValues: Resetting restricted_times.");
                Configuration.SetServerProperty(PROPERTY_RESTRICTED_TIMES, PROPERTY_DEFAULT_RESTRICTED_TIMES);
                return;
            }

            String NewString = null;

            for (String S : values) {
                if (NewString==null) {
                    NewString = S;
                } else {
                    NewString = NewString + "," + S;
                }
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setConfigValues: Setting restricted_times to " + NewString);
            Configuration.SetServerProperty(PROPERTY_RESTRICTED_TIMES, NewString);
            return;
        } else if (setting.startsWith(SETTING_SHOW_QUEUE)) {

            // Do nothing for now.
            if (true) {
                return;
            }

            if (values==null || values.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValues: null values.");
                return;
            }

            if (IDMap==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValues: null IDMaps.");
                return;
            }
            
            // Move the selected files to the head of the queue.

            if (Log.getInstance().GetLogLevel()==Log.LOGLEVEL_TRACE)
                SystemStatus.getInstance().printJobQueue();

            for (String Descr : values) {

                // Check for special case.
                if (Descr.equalsIgnoreCase("None")) {
                    continue;
                }

                // Retrieve MediaFileID from Map.
                Integer ID = IDMap.get(Descr);

                if (ID==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValues: null ID for Descr " + Descr);
                } else {

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "setConfigValues: ID for selected show " + ID);

                    if (!ComskipManager.getInstance().moveToFrontOfQueue(ID)) {
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "setConfigValues: Error moving to front of queue.");
                    }
                }
            }
            
            if (Log.getInstance().GetLogLevel()==Log.LOGLEVEL_TRACE)
                SystemStatus.getInstance().printJobQueue();

        } else if (setting.startsWith(SETTING_SHOW_RUNNING)) {
            // Do nothing for now.
        }

        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices.
    @Override
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith("LogLevel")) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {
            String[] values = { "00:00 - 00:59", "01:00 - 01:59", "02:00 - 02:59", "03:00 - 03:59", "04:00 - 04:59",
                                "05:00 - 05:59", "06:00 - 06:59", "07:00 - 07:59", "08:00 - 08:59", "09:00 - 09:59",
                                "10:00 - 10:59", "11:00 - 11:59", "12:00 - 12:59", "13:00 - 13:59", "14:00 - 14:59",
                                "15:00 - 15:59", "16:00 - 16:59", "17:00 - 17:59", "18:00 - 18:59", "19:00 - 19:59",
                                "20:00 - 20:59", "21:00 - 21:59", "22:00 - 22:59", "23:00 - 23:59"};
            return values;
        } else if (setting.startsWith(SETTING_SHOW_QUEUE)) {

            List<QueuedJob> Jobs = ComskipManager.getInstance().readQueuedJobs();

            if (Jobs==null || Jobs.isEmpty()) {
                String[] v = {"None"};
                return v;
            }

            // Map used to save the MediaFileID so we can retrieve it after the user makes a selection.
            IDMap = new HashMap<String, Integer>();

            List<String> Descr = new ArrayList<String>();
            for (QueuedJob Job : Jobs) {
                Integer ID = Job.getMediaFileID();
                String S = Job.getShowTitleEpisode();
                Descr.add(S);
                IDMap.put(S, ID);
            }

            String[] S = new String[Descr.size()];
            for (int i=0; i<Descr.size(); i++) {
                S[i] = Descr.get(i);
            }

            return S;

        } else if (setting.startsWith(SETTING_SHOW_RUNNING)) {

            List<ComskipJob> Jobs = ComskipManager.getInstance().getRunningJobs();

            if (Jobs==null || Jobs.isEmpty()) {
                String[] v = {"None"};
                return v;
            }

            String[] S = new String[Jobs.size()];

            for (int i=0; i<Jobs.size(); i++) {
                ComskipJob Job = Jobs.get(i);
                S[i] = Job.getShowTitleEpisode();
            }

            return S;

        } else if (Names.contains(setting)) {
            String[] v = {"Comskip","ShowAnalyzer","None","Default"};
            return v;
        } else {
            return null;
        }
    }

    // Returns the help text for a configuration setting.
    @Override
    public String getConfigHelpText(String setting) {
        if (setting.startsWith("LogLevel")) {
            return "Set the Debug Logging Level.";
        } else if (setting.startsWith("MaxJobs")) {
            return "The maximum number of detection jobs to run at the same time.";
        } else if (setting.startsWith("StopAll")) {
            return "Stop the currently running jobs immediately. There is NO confirmation dialog.";
        } else if (setting.startsWith("ClearQueue")) {
            return "Remove all jobs from the queue.";
        } else if (setting.startsWith(SETTING_SHOW_QUEUE)) {
            return "Select to show recordings that are waiting to be processed.";
        } else if (setting.startsWith(SETTING_SHOW_RUNNING)) {
            return "Select to show recordings that are being processed."; 
        } else if (setting.startsWith("Restart")) {
            return "Restart the jobs in the queue.";
        } else if (setting.startsWith("RunSlow")) {
            return "Runs comskip with the --playnice parameter.";
        } else if (setting.startsWith("ComskipParms")) {
            return "Passed to comskip exactly as typed. Delimit with a space.";
        } else if (setting.startsWith("UNCMap")) {
            return "Map drive letters to UNC paths. Format is DriveLetter-UNCPath. (ex E-\\\\Server\\Directory";
        } else if (setting.startsWith("ComskipLocation")) {
            return "Default is " + getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe";
        } else if (setting.startsWith("IniLocation")) {
            return "Default is " + getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini";
        } else if (setting.startsWith(SETTING_CLEANUP_EXTENSIONS)) {
            return "When Recordings are deleted, the corresponding files with these extensions will be deleted.";
        } else if (setting.startsWith("WineHome")) {
            return "Override for non standard installs.";
        } else if (setting.startsWith("SetEnv")) {
            return "Delimit using a comma.";
        } else if (setting.startsWith(SETTING_SKIP_CHANNELS)) {
            return "Enter channel names, numbers or range of numbers delimited by a comma.";
        } else if (setting.startsWith(SETTING_SKIP_CATEGORIES)) {
            return "Enter Categories and Subcategories delimited by a comma,";
        } else if (setting.startsWith("SetCommand")) {
            return "Delimit using a comma.";
        } else if (setting.startsWith("RunningAsRoot")) {
            return "In most cases this will be true.";
        } else if (setting.startsWith("StartImm")) {
            return "Do not wait until recording completes. Will also run comskip on LiveTV.";
        } else if (setting.startsWith("RunCommand")) {
            return "Select to exec() the command and environment.";
        } else if (setting.startsWith("ManualRun")) {
            return "comskip will run in the background.";
        } else if (setting.startsWith("ScanAll")) {
            return "Runs comskip on all files that have not already been processed.";
        } else if (setting.startsWith("ShowAdvanced")) {
            return "Configure advanced options.";
        } else if (setting.startsWith("UseShowAnalyzer")) {
            return "Use ShowAnalyzer Instead of comskip.";
        } else if (setting.startsWith(SETTING_VIDEO_FILE_EXTENSIONS)) {
            return "No files will be cleaned up if a corresponding video file exists.";
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            return "Do NOT choose ShowAnalyzer.exe! ShowAnalyzer must be installed and registered.";
        } else if (setting.startsWith("ProfileLocation")) {
            return "If left blank none will be used.";
        } else if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {
            return "Choose the time of day that comskip should not be run.";
        } else if (setting.startsWith(SETTING_TIME_RATIOS)) {
            return "Format is Channel:Float,Channel:Float.";
        } else if (setting.startsWith(SETTING_DELETE_ORPHANS)) {
            return "Deletes all files ending with one of the cleanup extensions that have no corresponding recording.";
        } else if (setting.startsWith(SETTING_USE_INTELLIGENT_SCHEDULING)) {
            return "Run comskip only when nothing is recording. Will not start a job if it's not likely to complete before any upcoming recordings. Honors Restricted Times.";
        } else if (setting.startsWith(SETTING_SHOW_INTELLIGENT_TUNING)) {
            return "These are the parameters used to tune how system performance effects comskip execution time.  CHange at your own risk!";
        } else if (setting.startsWith("ShowChannels")) {
            return "Allows you to choose comskip or ShowAnalyzer on a per channel basis.";
        } else if (setting.startsWith("WineUser")) {
            if (SageUtil.GetBoolProperty("cd/running_as_root", true)) {
                return "Comskip should not be run as root.";
            } else {
                return "The user that is running Sage.";
            }        
        } else if (Names.contains(setting)) {
            return "Current setting is " + Configuration.GetServerProperty("cd/map_"+setting, "Default");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigHelpText: Unknown setting = " + setting);
            return null;
        }
    }

    // Returns the label used to present this setting to the user.
    @Override
    public String getConfigLabel(String setting) {

        if (setting.startsWith("LogLevel")) {
            return "Debug Logging Level";
        } else if (setting.startsWith("MaxJobs")) {
            return "Maximum Concurrent Jobs";
        } else if (setting.startsWith("StopAll")) {
            return "Stop Running Jobs";
        } else if (setting.startsWith("ClearQueue")) {
            return "Clear Job Queue";
        } else if (setting.startsWith(SETTING_SHOW_QUEUE)) {
            return "Show Jobs That Are Queued";
        } else if (setting.startsWith(SETTING_SHOW_RUNNING)) {
            return "Show Jobs That Are Running";
        } else if (setting.startsWith("Restart")) {
            return "Restart Queued Jobs";
        } else if (setting.startsWith("UNCMap")) {
            return "Drive Mapping";
        } else if (setting.startsWith("ComskipParms")) {
            return "Other comskip Parameters";
        } else if (setting.startsWith(SETTING_CLEANUP_EXTENSIONS)) {
            return "Cleanup Files With These Extensions";
        } else if (setting.startsWith("ComskipLocation")) {
            return "Location of comskip.exe";
        } else if (setting.startsWith("IniLocation")) {
            return "Location of comskip.ini";
        } else if (setting.startsWith("RunSlow")) {
            return "Run More Slowly";
        } else if (setting.startsWith("WineHome")) {
            return "Home directory for wine";
        } else if (setting.startsWith(SETTING_VIDEO_FILE_EXTENSIONS)) {
            return "File Extensions for Valid Video Files";
        } else if (setting.startsWith(SETTING_SKIP_CHANNELS)) {
            return "Do Not Run comskip on These Channels";
        } else if (setting.startsWith(SETTING_SKIP_CATEGORIES)) {
            return "Do Not Run comskip on These Categories";
        } else if (setting.startsWith("SetEnv")) {
            return "Environment Variables for Test Command";
        } else if (setting.startsWith("SetCommand")) {
            return "Test Command";
        } else if (setting.startsWith("ManualRun")) {
            return "Manually Run comskip";
        } else if (setting.startsWith("RunCommand")) {
            return "Run the Test Command";
        } else if (setting.startsWith("WineUser")) {
            if (SageUtil.GetBoolProperty("cd/running_as_root", true)) {
                return "User Account to Run comskip";
            } else {
                return "User Account That is Running SageTV";
            }           
        } else if (setting.startsWith("StartImm")) {
            return "Start comskip as Soon as Recording Starts";
        } else if (setting.startsWith("RunningAsRoot")) {
            return "SageTV is Running as root";
        } else if (setting.startsWith("ScanAll")) {
            return "Scan All Recordings Without comskip Info";
        } else if (setting.startsWith("ShowAdvanced")) {
            return "Show Advanced Options";
        } else if (setting.startsWith("UseShowAnalyzer")) {
            return "Use ShowAnalyzer";
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            return "Location of ShowAnalyzerEngine.exe";
        } else if (setting.startsWith("ProfileLocation")) {
            return "Location of ShowAnalyzer's Profiles";
        } else if (setting.startsWith("ShowChannels")) {
            return "Show All Channels";
        } else if (setting.startsWith(SETTING_RESTRICTED_TIMES)) {
            return "Restricted Times";
        } else if (setting.startsWith(SETTING_DELETE_ORPHANS)) {
            return "Delete All Orphaned and Extraneous Files";
        } else if (setting.startsWith(SETTING_TIME_RATIOS)) {
            return "Time Ratios";
        } else if (setting.startsWith(SETTING_USE_INTELLIGENT_SCHEDULING)) {
            return "Use Intelligent Scheduling";
        } else if (setting.startsWith(SETTING_SHOW_INTELLIGENT_TUNING)) {
            return "Show Intelligent Scheduling Tuning Parameters";
        } else if (Names!=null || Names.contains(setting)) {
            return setting + ": Pick Commercial Detection Program";
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getConfigLabel: Unknown setting = " + setting);
            return null;
        }
    }

    // Resets the configuration of this plugin.
    @Override
    public final void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "resetConfig: resetConfig received from Plugin Manager.");
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);

        NumberScanned = 0;
        NumberOfOrphans = -1;
        Integer processors = getAvailableProcessors();
        processors = (processors>1 ? processors-1 : processors);
        Configuration.GetServerProperty("cd/max_jobs", processors.toString());
        Configuration.SetServerProperty("cd/comskip_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        Configuration.SetServerProperty("cd/ini_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        Configuration.SetProperty(PROPERTY_DRIVE_MAP, PROPERTY_DEFAULT_DRIVE_MAP);
        Configuration.SetServerProperty(PROPERTY_CLEANUP_EXTENSIONS, PROPERTY_DEFAULT_CLEANUP_EXTENSIONS);
        Configuration.SetServerProperty("cd/comskip_parms", "");
        Configuration.SetServerProperty("cd/wine_home", getDefaultWineHome());
        Configuration.SetServerProperty("cd/wine_user", getDefaultWineUser());
        Configuration.SetServerProperty("cd/run_slow", "false");
        Configuration.SetServerProperty(PROPERTY_SKIP_CHANNELS, "");
        Configuration.SetServerProperty(PROPERTY_SKIP_CATEGORIES, "");
        Configuration.SetServerProperty("cd/start_imm", "false");
        Configuration.SetServerProperty("cd/running_as_root", "true");
        Configuration.SetServerProperty("cd/show_advanced", "false");
        Configuration.SetServerProperty("cd/use_showanalyzer", "false");
        Configuration.SetServerProperty("cd/showanalyzer_location", "Select");
        Configuration.SetServerProperty("cd/profile_location", "Select");
        Configuration.SetServerProperty("cd/show_channels", "false");
        Configuration.SetServerProperty(PROPERTY_VIDEO_FILE_EXTENSIONS, PROPERTY_DEFAULT_VIDEO_FILE_EXTENSIONS);
        Configuration.SetServerProperty(PROPERTY_RESTRICTED_TIMES, PROPERTY_DEFAULT_RESTRICTED_TIMES);
        Configuration.SetServerProperty(PROPERTY_TIME_RATIOS, PROPERTY_DEFAULT_TIME_RATIOS);
        Configuration.SetServerProperty(PROPERTY_USE_INTELLIGENT_SCHEDULING, PROPERTY_DEFAULT_USE_INTELLIGENT_SCHEDULING);
        Configuration.SetServerProperty(PROPERTY_SHOW_INTELLIGENT_TUNING, PROPERTY_DEFAULT_SHOW_INTELLIGENT_TUNING);
    }

    public static String getDefaultComskipLocation() {

        File PathFile = WidgetAPI.GetDefaultSTVFile();
        if (PathFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDefaultComskipLocation: null PathFile.");
            return null;
        }

        String STVPath = PathFile.getParent();
        if (STVPath==null || STVPath.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDefaultComskipLocation: null STVPath.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getDefaultComskipLocation: STVPath = " + STVPath);

        // ../SageTV||server/STVs/SageTV7
        // ../SageTV||server/comskip

        int STVsIndex = STVPath.indexOf("STVs");
        if (STVsIndex<1) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDefaultComskipLocation: Malformed STVPath.");
            return null;
        }

        return STVPath.substring(0, STVsIndex-1);
    }

    public static String getDefaultWineHome() {
        String WineUser = Configuration.GetServerProperty("cd/wine_user", null);
        if (WineUser==null || WineUser.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "plugin: null WineUser.");
            return "Enter";
        } else {
            return File.separator + "home" + File.separator + WineUser + File.separator + ".wine" + File.separator;
        }
    }

    public static String getDefaultWineUser() {

        if (Global.IsWindowsOS()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getDefaultWineUser: No Home for Windows.");
            return null;
        }

        String Home = "/home";  // Hardcode the separator, if run in Windows it returns the wrong character.
        File HomeFile = new File(Home);
        if (HomeFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDefaultWineUser: null HomeFile.");
            return null;
        }

        String[] Contents = HomeFile.list();
        if (Contents==null || Contents.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getDefaultWineUser: Nothing in " + HomeFile);
            return null;
        }

        for (String S : Contents) {
            S = S.toLowerCase();
            if (S.contains("comskip") || S.contains("sage") || S.contains("wine") || S.contains("commercial") || S.contains("detect")) {
                return S;
            }
        }

        return Contents[0];
    }

    private static Integer getAvailableProcessors() {
        int processors;
        processors = Runtime.getRuntime().availableProcessors();
        return (processors>=1 ? processors : 1);
    }

    private static void manualRun(String FileName) {

        Log.getInstance().write(Log.LOGLEVEL_ERROR, "manualRun: Manual rune for " + FileName);
        
        File F = new File(FileName);
        if (F==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "manualRun: null File for " + FileName);
            return;
        }

        Object MediaFile = MediaFileAPI.GetMediaFileForFilePath(F);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "manualRun: null MediaFile.");
            return;
        }

        api.DeleteComskipFiles(MediaFile);

        CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile);

        if (!CDMediaFile.queue()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "manualRun: queue failed.");
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
        if (!(  eventName.startsWith("RecordingCompleted") ||
                eventName.startsWith("RecordingStopped") ||
                eventName.startsWith("MediaFileRemoved") ||
                eventName.startsWith("RecordingScheduleChanged") ||
                eventName.startsWith("RecordingStarted"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
            return;
        }

        if (Log.getInstance().GetLogLevel()==Log.LOGLEVEL_TRACE || Log.getInstance().GetLogLevel()==Log.LOGLEVEL_VERBOSE) {
            SystemStatus.getInstance().printSystemStatus();
        }

        // If the Recording Schedule has changed we may need to start jobs that were waiting.
        if (eventName.startsWith("RecordingScheduleChanged")) {

            // Notify the Intelligent Scheduling Tuner, if it's active.
            TuneIntelligentScheduling ISTuner = ComskipManager.getInstance().getIntelligentTuner();
            if (ISTuner != null) {
                ISTuner.recScheduleHasChanged();
            }

            if (!SageUtil.GetBoolProperty(plugin.PROPERTY_USE_INTELLIGENT_SCHEDULING, plugin.PROPERTY_USE_INTELLIGENT_SCHEDULING)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Intelligent scheduling is disabled.");
                return;
            }

            ComskipManager.getInstance().startMaxJobs();
            return;
        }

        // Check that we have a valid MediaFile.
        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Error-null MediaFile");
            return;
        }

        if (eventName.startsWith("MediaFileRemoved")) {

            // If this file is being processed, stop it.
            ComskipJob Job = ComskipManager.getInstance().getJobForID(MediaFileAPI.GetMediaFileID(MediaFile));
            if (Job!=null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaFile is still being processed.");
                Job.stop();
            }

            CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile, true);

            if (CDMediaFile.cleanup()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaFile cleanup successful " + MediaFileAPI.GetMediaTitle(MediaFile));
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "sageEvent: Error-Could not cleanup MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        } else {

            // A recording is either starting or finishing.

            // Do nothing if it's on a channel that we are supposed to skip.
            // Issue restart() so any jobs waiting for a recording to complete will get restarted.
            if (skipThisChannel(MediaFile) || skipThisCategory(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because the channel or category is in the skip list.");
                ComskipManager.getInstance().startMaxJobs();
                return;
            }

            // Both RecordingStopped and RecordingCompleted is fired when a recording ends, so check that
            // it's not already queued.
            if (isRunningOrQueued(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because it's already running or queued.");
                ComskipManager.getInstance().startMaxJobs();
                return;
            }

            // Do nothing if it's the start of a recording and it's not set to start immediately.
            if (eventName.startsWith("RecordingStarted") && !SageUtil.GetBoolProperty("cd/start_imm", false)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because not set to start immediately.");
                return;
            }

            // If a recording has just stopped, and we are not in a restricted time,
            // and there is nothing running, and we have time to process a job... restart.
            //
            // This will make sure that if a recording is queued but has not been started because there
            // was not enough time, it will get restarted.
            //if (eventName.startsWith("RecordingStopped") && !ComskipManager.getInstance().inRestrictedTime() && (ComskipManager.getInstance().getNumberRunning()==0) && ComskipManager.getInstance().isEnoughTime(MediaFile)) {
            if (eventName.startsWith("RecordingStopped")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Starting ");
                ComskipManager.getInstance().startMaxJobs();
            }

            /*
             * There is a small window where recordings may be processed twice.  Namely:
             * - We are here because a recording has finished.
             * - CD is set to run comskip immediately.
             * - comskip has already completed the show that has just finished.
             *
             * We're relying on the supposition that Sage will fire RecordingStopped so soon after the
             * recording has actually stopped that comskip will not have a chance to finish processing it.
             *
             * We can't just ignore RecordingStopped when set to start comskip immediately because it may be
             * that the recording started during a restricted time and comskip is not actually processing it.
             */

            CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile);

            if (CDMediaFile.queue()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaFile queued " + MediaFileAPI.GetMediaTitle(MediaFile));
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "sageEvent: Error-Could not queue MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        }
          
        return;
    }

    private static boolean skipThisChannel(Object MediaFile) {

        String SkipList = Configuration.GetServerProperty(PROPERTY_SKIP_CHANNELS, "");
        if (SkipList==null || SkipList.isEmpty()) {
            return false;
        }

        String[] SkipArray = SkipList.split(",");
        if (SkipArray==null || SkipArray.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "skipThisChannel: Bad SkipList " + SkipList);
            return false;
        }

        String ChannelName = AiringAPI.GetAiringChannelName(MediaFile);
        String ChannelNumber = AiringAPI.GetAiringChannelNumber(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "skipThisChannel: ChannelName and ChannelNumber " + ChannelName + ":" + ChannelNumber);

        for (String Skip : SkipArray) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "skipThisChannel: Skip = " + Skip);

            // See if this is a range.
            String[] FirstLast = Skip.split("-");

            if (FirstLast==null || FirstLast.length==1) {

                // No range specified.  Check if the name or number matches.
                if (ChannelName.equalsIgnoreCase(Skip) || ChannelNumber.equalsIgnoreCase(Skip)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "skipThisChannel: Skipping " + Skip);
                    return true;
                }
            } else if (FirstLast.length==2) {

                // Range specified.  See if the channel number falls withing the range.
                int thisChannel = stringToInt(ChannelNumber);
                int firstChannel = stringToInt(FirstLast[0]);
                int lastChannel = stringToInt(FirstLast[1]);

                if (lastChannel < firstChannel) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "skipThisChannel: Range is backwards. Flipping.");
                    int t = firstChannel;
                    firstChannel = lastChannel;
                    lastChannel = t;
                }

                if (thisChannel >= firstChannel && thisChannel <= lastChannel) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "skipThisChannel: Skipping because channel is in skip range.");
                    return true;
                }

            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "skipThisChannel: Malformed range " + Skip);
            }

        }

        return false;
    }

    private static boolean skipThisCategory(Object MediaFile) {

        String SkipList = Configuration.GetServerProperty(PROPERTY_SKIP_CATEGORIES, "");
        if (SkipList==null || SkipList.isEmpty()) {
            return false;
        }

        String[] SkipArray = SkipList.split(",");
        
        if (SkipArray==null || SkipArray.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "skipThisCategory: Bad SkipList " + SkipList);
            return false;
        }

        // Create a list of user-supplied categories to skip.  Make them all lowercase.
        List<String> skipList = new ArrayList<String>();

        for (String skip : SkipArray) {
            if (skip!=null && !skip.isEmpty()) {
                String item = skip.toLowerCase().trim();
                if (item!=null && !item.isEmpty())
                    skipList.add(item);
            }
        }
System.out.println("CD:: skipList = " + skipList);

        // Create a List of Categories and Subcategories in this show.
        List<String> showCategoriesList = new ArrayList<String>();

        String[] showCategories = ShowAPI.GetShowCategoriesList(MediaFile);
for (String S : showCategories) System.out.println("CD:: showCategories " + S);
        if (showCategories!=null && showCategories.length>0) {

            // Some categories actually have two categories separated by a "/", such as House / garden.
            for (String category : showCategories) {
                showCategoriesList.addAll(parseCategories(category));
            }
        }
System.out.println("CD:: showCategoriesList 1 = " + showCategoriesList);

        String showSubcategory = ShowAPI.GetShowSubCategory(MediaFile);
System.out.println("CD:: showSubcategory " + showSubcategory);
        if (showSubcategory!=null && !showSubcategory.isEmpty())
            showCategoriesList.addAll(parseCategories(showSubcategory));
System.out.println("CD:: showCategoriesList 2 = " + showCategoriesList);

        // Check if any items in the skipList appear in the showCategoryList.
        for (String item : skipList) {
            if (showCategoriesList.contains(item)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "skipThisCategory: Skipping the category " + item);
                return true;
            }
        }

        return false;
    }

    private static List<String> parseCategories(String categories) {

        String SEPARATOR = "/";

        List<String> categoryList = new ArrayList<String>();

        if (categories==null || categories.isEmpty())
            return categoryList;

        if (!categories.contains(SEPARATOR)) {
            String item = categories.toLowerCase().trim();
            if (item!=null && !item.isEmpty())
                categoryList.add(item);
            return categoryList;
        }

        String[] categoryArray = categories.split(SEPARATOR);

        if (categoryArray==null || categoryArray.length==0)
            return categoryList;

        for (String category : categoryArray) {
            String item = category.toLowerCase().trim();
            if (item!=null && !item.isEmpty())
                categoryList.add(item);
        }

        return categoryList;
    }

    private static boolean isRunningOrQueued(Object MediaFile) {

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "isRunningOrQueued: null MediaFile");
            return false;
        }

        Integer ID = MediaFileAPI.GetMediaFileID(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "isRunningOrQueued: ID for job to check " + ID);

        Integer[] RunningJobs = ComskipManager.getInstance().getIDsForRunningJobs();

        if (RunningJobs != null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "isRunningOrQueued: Found running jobs " + RunningJobs.length);
            for (Integer I : RunningJobs) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isRunningOrQueued: Found ID  " + I);
                if (I.equals(ID)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "isRunningOrQueued: MediaFile is running.");
                    return true;
                }
            }
        }

        Integer[] QueuedJobs = ComskipManager.getInstance().getIDsForQueuedJobs();

        if (QueuedJobs != null) {
            for (Integer I : QueuedJobs) {
                if (I.equals(ID)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "isRunningOrQueued: MediaFile is queued.");
                    return true;
                }
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "isRunningOrQueued: MediaFile not running or queued.");
        return false;
    }

    private static void loadTimeRatios() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadTimeRatios: Loading time ratio information.");
        String S = Configuration.GetServerProperty(PROPERTY_TIME_RATIOS, PROPERTY_DEFAULT_TIME_RATIOS);

        if (S==null || S.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadTimeRatios: No ratio information to load.");
            return;
        }

        String[] Pairs = S.split(",");
        if (Pairs==null || Pairs.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadTimeRatios: No time pairs.");
            return;
        }

        for (String Pair : Pairs) {
            String[] ChannelRatio = Pair.split(":");

            if (ChannelRatio==null || ChannelRatio.length!=2) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "loadTimeRatios: Malformed Channel:Ratio " + Pair);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadTimeRatios: Found Channel:Ratio " + ChannelRatio[0] + ":" + ChannelRatio[1]);

                Float Ratio = 0.0F;
                
                try {
                    Ratio = Float.parseFloat(ChannelRatio[1]);
                } catch (NumberFormatException nfe) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "loadTimeRatios: Malformed Ratio " + ChannelRatio[1]);
                    Ratio = RATIO_DEFAULT;
                }

                ChannelTimeRatios.put(ChannelRatio[0], Ratio);
            }
        }
    }

    private static int stringToInt(String NumberString) {
        if (NumberString==null || NumberString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "stringToInt: null number.");
            return 0;
        }

        try {
            return Integer.parseInt(NumberString);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "stringToInt: Malformed number " + NumberString);
            return 0;
        }
    }

}
