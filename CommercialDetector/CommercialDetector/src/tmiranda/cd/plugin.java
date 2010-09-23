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

    private final String                VERSION = "0.70";
    private sage.SageTVPluginRegistry   registry;
    private sage.SageTVEventListener    listener;
    private static Integer              NumberScanned;
    private static Map<String, String>  ChannelNames;
    private static Set<String>          Names;

    public static final String          PROPERTY_VIDEO_FILE_EXTENSIONS = "mpg,mp4,avi,ts,mkv,m4v";

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
    }

    public plugin(sage.SageTVPluginRegistry Registry, boolean Reset) {
        registry = Registry;
        listener = this;
        NumberScanned = 0;
        ChannelNames = new HashMap<String, String>();
        if (Reset && !Global.IsClient())
            resetConfig();
    }

    // This method is called when the plugin should startup
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
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Running in Client mode.");
            //registry.eventSubscribe(listener, "MyEvent");
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
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: No Channels defined.");
        } else {
            for (Object Channel : Channels) {
                String Name = ChannelAPI.GetChannelName(Channel);
                String Number = ChannelAPI.GetChannelNumber(Channel);
                if (Name==null || Number==null || Name.isEmpty() || Number.isEmpty()) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: null Name or Number ");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: Found channel " + Name + ":" + Number);
                    ChannelNames.put(Name, Number);
                }
            }
            Names = ChannelNames.keySet();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Channelnames created. Size = " + ChannelNames.size());
        }

        // Clear the properties that we use to communicate with SageClients.
        // - processing is a list of the MediaFileIDs of jobs that are being processed.
        // - queue is a list of MediaFileIDs that SageClients want to have processed.
        CSC.getInstance().setStatus("processing", null);
        CSC.getInstance().setStatus("queue", null);

        // Make sure the DB file and all necessary directories exist.
        ComskipManager.getInstance().makeResources();

        // Restart any jobs that were queued but not completed.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Restarting any pending jobs.");

        if (!ComskipManager.getInstance().inRestrictedTime() && ComskipManager.getInstance().getNumberRunning()==0) {
            ComskipManager.getInstance().restart();
        }

        // Print debug info if needed.
        if (Log.getInstance().GetLogLevel()==Log.LOGLEVEL_ALL) {
            List<Object> MediaFilesToQueue = ComskipManager.getInstance().getMediaFilesWithout("T");
            for (Object MediaFile : MediaFilesToQueue) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "PlugIn: No edl for " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        }

        // Subscribe to what we need.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Subscribing to events.");
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingStarted");
        registry.eventSubscribe(listener, "MediaFileRemoved");

        // Start the task that will monitor the queue that SageClients use to request jobs.
        if (SageUtil.GetBoolProperty("cd/monitor_clients", "true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Starting MonitorClient.");
            long MonitorClientPeriod = SageUtil.GetLongProperty("cd/monitor_client_period", 60*1000);
            TimerTask MonitorClient = new MonitorClient();
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(MonitorClient, MonitorClientPeriod, MonitorClientPeriod);
        }

        // Start the task that wakes up every hour to see if jobs waiting because they were queued in
        // restricted times should be started.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Starting RestartRestricted.");

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
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Minutes until first RestartRestricted " + MinutesToNextHour + ":" + MillisToNextHour);
    }

    // This method is called when the plugin should shutdown
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Stop received from Plugin Manager.");

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Running in Client mode.");
            return;
        }

        registry.eventUnsubscribe(listener, "RecordingCompleted");
        registry.eventUnsubscribe(listener, "RecordingStopped");
        ComskipManager.getInstance().stopAll();
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    public void destroy() {
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Running in Client mode.");
            CSC.getInstance().destroy();
            Log.getInstance().destroy();
            return;
        }

        ComskipManager.getInstance().destroy();
        CSC.getInstance().destroy();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Destroy received from Plugin Manager.");
        Log.getInstance().destroy();
        ChannelNames = null;
        Names = null;
    }

    // These methods are used to define any configuration settings for the
    // plugin that should be presented in the UI. If your plugin does not
    // need configuration settings; you may simply return null or zero from
    // these methods.

    // Returns the names of the settings for this plugin
    public String[] getConfigSettings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigSetting received from Plugin Manager.");
        boolean TestMode = SageUtil.GetBoolProperty("cd/test_mode", false);

        List<String> CommandList = new ArrayList<String>();

        // These options are allowed under all circumstances.
        CommandList.add("MaxJobs");
        CommandList.add("ComskipLocation");
        CommandList.add("IniLocation");
        CommandList.add("RunSlow");
        CommandList.add("RestrictTimes");
        CommandList.add("ComskipParms");
        CommandList.add("CleanupExt");
        CommandList.add("VideoExt");
        CommandList.add("SkipChannels");
        CommandList.add("StartImmediately");
        CommandList.add("ScanAll");

        String ServerOS = Configuration.GetServerProperty("cd/server_is", "unknown");

        // These options are only allowed if the server in linux.  It does not matter what the client is.
        if (ServerOS.equalsIgnoreCase("linux")) {
            CommandList.add("RunningAsRoot");
            CommandList.add("WineUser");
            CommandList.add("WineHome");
        }

        // Allowed for all.
        CommandList.add("LogLevel");

        // Do not allow these options if the server is linux.  It does not matter what the client is.
        if (!ServerOS.equalsIgnoreCase("linux")) {
            CommandList.add("ShowAdvanced");
        }

        // Only show these options if show_advanced and the server is not linux.  It does not matter what the client is.
        if (SageUtil.GetBoolProperty("cd/show_advanced", false) && !ServerOS.equalsIgnoreCase("linux")) {
            CommandList.add("UseShowAnalyzer");
            CommandList.add("ShowAnalyzerLocation");
            CommandList.add("ProfileLocation");
            CommandList.add("ShowChannels");
        }

        // Dependent on show_advanced which is controlled above.
        if (SageUtil.GetBoolProperty("cd/show_channels", false) && SageUtil.GetBoolProperty("cd/show_advanced", false)) {
            for (String Name : Names) {
                CommandList.add(Name);
            }
        }

        if (TestMode) {
            CommandList.add("ManualRun");
            CommandList.add("StopAll");
            CommandList.add("Restart");
            CommandList.add("ShowQueue");
            CommandList.add("ClearQueue");
            CommandList.add("ShowFailed");
            CommandList.add("ClearFailed");
            CommandList.add("SetEnv");
            CommandList.add("UNCMap");
            CommandList.add("SetCommand");
            CommandList.add("RunCommand");
        }

        return (String[])CommandList.toArray(new String[CommandList.size()]);
    }

    // Returns the current value of the specified setting for this plugin
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
        } else if (setting.startsWith("ClearQueue") || setting.startsWith("ShowQueue") || setting.startsWith("Restart")) {
            Integer n = ComskipManager.getInstance().getQueueSize(false);
            return n.toString();
        } else if (setting.startsWith("ShowFailed") || setting.startsWith("ClearFailed")) {
            Integer n = ComskipManager.getInstance().getQueueSize(true);
            return n.toString();
        } else if (setting.startsWith("ComskipLocation")) {
            return Configuration.GetServerProperty("cd/comskip_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        } else if (setting.startsWith("IniLocation")) {
            return Configuration.GetServerProperty("cd/ini_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        } else if (setting.startsWith("UNCMap")) {
            return Configuration.GetProperty("cd/UNC_map", "");
        } else if (setting.startsWith("ComskipParms")) {
            return Configuration.GetServerProperty("cd/comskip_parms", "");
        } else if (setting.startsWith("VideoExt")) {
            return Configuration.GetServerProperty("cd/video_ext", PROPERTY_VIDEO_FILE_EXTENSIONS);
        } else if (setting.startsWith("CleanupExt")) {
            return Configuration.GetServerProperty("cd/cleanup_ext", "edl,log");
        } else if (setting.startsWith("WineHome")) {
            return Configuration.GetServerProperty("cd/wine_home", getDefaultWineHome());
        } else if (setting.startsWith("WineUser")) {
            return Configuration.GetServerProperty("cd/wine_user", getDefaultWineUser());
        } else if (setting.startsWith("RunSlow")) {
            return Configuration.GetServerProperty("cd/run_slow", "false");
        } else if (setting.startsWith("SkipChannels")) {
            return Configuration.GetServerProperty("cd/skip_channels", "");
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
        } else if (setting.startsWith("ScanAll")) {
            if (NumberScanned==0) {
                return "Queue Files";
            } else {
                return NumberScanned.toString() + " Queued";
            }
        } else if (setting.startsWith("RestrictTimes")) {
            return "Select";
        } else if (Names.contains(setting)) {
            return Configuration.GetServerProperty("cd/map_"+setting, "Default");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Unknown setting from getConfigValue = " + setting);
            return "UNKNOWN";
        }
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin
    public String[] getConfigValues(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigValues received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith("RestrictTimes")) {
            String S = Configuration.GetServerProperty("cd/restricted_times", null);

            if (S==null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: No Restricted times.");
                String[] v = {};
                return v;
            }

            String[] Hours = S.split(",");

            if (Hours==null || Hours.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "PlugIn: Malformed restricted_times " + S);
                String[] v = {};
                return v;
            }

            return Hours;
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
    // is used for a specific settings
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
        else if (setting.startsWith("ShowQueue"))
            return CONFIG_CHOICE;
        else if (setting.startsWith("ClearQueue"))
            return CONFIG_BUTTON;
        else if (setting.startsWith("ShowFailed"))
            return CONFIG_CHOICE;
        else if (setting.startsWith("ClearFailed"))
            return CONFIG_CHOICE;
        else if (setting.startsWith("ComskipLocation"))
            return CONFIG_FILE;
        else if (setting.startsWith("IniLocation"))
            return CONFIG_FILE;
        else if (setting.startsWith("UNCMap"))
            return CONFIG_TEXT;
        else if (setting.startsWith("ComskipParms"))
            return CONFIG_TEXT;
        else if (setting.startsWith("CleanupExt"))
            return CONFIG_TEXT;
        else if (setting.startsWith("VideoExt"))
            return CONFIG_TEXT;
        else if (setting.startsWith("SkipChannels"))
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
        else if (setting.startsWith("RestrictTimes"))
            return CONFIG_MULTICHOICE;
        else if (setting.startsWith("ShowChannels"))
            return CONFIG_BOOL;
        else if (Names.contains(setting)) {
            return CONFIG_CHOICE;
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Unknown setting from getConfigType = " + setting);
            return CONFIG_TEXT;
        }
    }

    // Sets a configuration value for this plugin
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "PlugIn: Invalid MaxJobs entry " + value);
            }
            if (v<=0) value = "1";
            if (v==5309) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: Toggling TestMode.");
                boolean TestMode = SageUtil.GetBoolProperty("cd/test_mode", false);
                Configuration.SetServerProperty("cd/test_mode", (TestMode ? "false":"true"));
            } else {
                Configuration.SetServerProperty("cd/max_jobs", value);
                ComskipManager.getInstance().restart();
            }
        } else if (setting.startsWith("ComskipLocation")) {
            Configuration.SetServerProperty("cd/comskip_location", value);
        } else if (setting.startsWith("IniLocation")) {
            Configuration.SetServerProperty("cd/ini_location", value);
        } else if (setting.startsWith("UNCMap")) {
            Configuration.SetProperty("cd/UNC_map", value);
        } else if (setting.startsWith("ComskipParms")) {
            Configuration.SetServerProperty("cd/comskip_parms", value);
        } else if (setting.startsWith("CleanupExt")) {
            Configuration.SetServerProperty("cd/cleanup_ext", value);
        } else if (setting.startsWith("VideoExt")) {
            Configuration.SetServerProperty("cd/video_ext", value);
        } else if (setting.startsWith("RunSlow")) {
            Configuration.SetServerProperty("cd/run_slow", value);
        } else if (setting.startsWith("SkipChannels")) {
            Configuration.SetServerProperty("cd/skip_channels", value);
        } else if (setting.startsWith("WineHome")) {
            Configuration.SetServerProperty("cd/wine_home", value);
        } else if (setting.startsWith("StartImm")) {
            Configuration.SetServerProperty("cd/start_imm", value);
        } else if (setting.startsWith("Restart")) {
            ComskipManager.getInstance().restart();
        } else if (setting.startsWith("StopAll")) {
            ComskipManager.getInstance().stopAll();
        } else if (setting.startsWith("ClearQueue")) {
            ComskipManager.getInstance().clearQueue(false);
        } else if (setting.startsWith("ClearFailed")) {
            ComskipManager.getInstance().clearQueue(true);
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
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            if (value.contains("ShowAnalyzer.exe")) {
                String newValue = value.replace("ShowAnalyzer.exe", "ShowAnalyzerEngine.exe");
                Configuration.SetServerProperty("cd/showanalyzer_location", newValue);
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "PlugIn: User choose ShowAnalyzer.exe, fixing " + newValue);
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
                if (!ComskipManager.getInstance().queue(MediaFile)) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "PlugIn: Error queuing file " + MediaFileAPI.GetMediaTitle(MediaFile));
                }
            }
        } else if (setting.startsWith("WineUser")) {
            if (!value.equalsIgnoreCase("root")) {
                Configuration.SetServerProperty("cd/wine_user", value);
                Configuration.SetServerProperty("cd/wine_home", getDefaultWineHome());
            }
        } else if (setting.startsWith("ShowFailed") || setting.startsWith("ShowQueue")) {
            // Do nothing.
        } else if (Names.contains(setting)) {
            Configuration.SetServerProperty("cd/map_"+setting, value);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Unknown setting from setConfigValue = " + setting);
        }
        
    }

    // Sets a configuration values for this plugin for a multiselect choice
    public void setConfigValues(String setting, String[] values) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: setConfigValues received from Plugin Manager. Setting = " + setting);

        if (setting.startsWith("RestrictTimes")) {

            if (values==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "PlugIn: null values from setConfigValues.");
                return;
            }

            if (values.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: setConfigValues resetting restricted_times.");
                Configuration.SetServerProperty("cd/restricted_times", null);
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

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Plugin: setConfigValues setting restricted_times to " + NewString);
            Configuration.SetServerProperty("cd/restricted_times", NewString);
            return;
        }

        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    public String[] getConfigOptions(String setting) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PlugIn: getConfigOptions received from Plugin Manager. Setting = " + setting);
        if (setting.startsWith("LogLevel")) {
            String[] values = {"None", "Error", "Warn", "Trace", "Verbose", "Maximum"};
            return values;
        } else if (setting.startsWith("RestrictTimes")) {
            String[] values = { "00:00 - 00:59", "01:00 - 01:59", "02:00 - 02:59", "03:00 - 03:59", "04:00 - 04:59",
                                "05:00 - 05:59", "06:00 - 06:59", "07:00 - 07:59", "08:00 - 08:59", "09:00 - 09:59",
                                "10:00 - 10:59", "11:00 - 11:59", "12:00 - 12:59", "13:00 - 13:59", "14:00 - 14:59",
                                "15:00 - 15:59", "16:00 - 16:59", "17:00 - 17:59", "18:00 - 18:59", "19:00 - 19:59",
                                "20:00 - 20:59", "21:00 - 21:59", "22:00 - 22:59", "23:00 - 23:59"};
            return values;
        } else if (setting.startsWith("ShowQueue")) {
            String[] values = ComskipManager.getInstance().getQueuedFileNames(false);
            if (values==null || values.length==0) {
                String[] v = {"None"};
                return v;
            } else {
                return values;
            }
        } else if (setting.startsWith("ShowFailed")) {
            String[] values = ComskipManager.getInstance().getQueuedFileNames(true);
            if (values==null || values.length==0) {
                String[] v = {"None"};
                return v;
            } else {
                return values;
            }
        } else if (Names.contains(setting)) {
            String[] v = {"Comskip","ShowAnalyzer","None","Default"};
            return v;
        } else {
            return null;
        }
    }

    // Returns the help text for a configuration setting
    public String getConfigHelpText(String setting) {
        if (setting.startsWith("LogLevel")) {
            return "Set the Debug Logging Level.";
        } else if (setting.startsWith("MaxJobs")) {
            return "The maximum number of detection jobs to run at the same time.";
        } else if (setting.startsWith("StopAll")) {
            return "Stop the currently running jobs immediately. There is NO confirmation dialog.";
        } else if (setting.startsWith("ClearQueue")) {
            return "Remove all jobs from the queue.";
        } else if (setting.startsWith("ShowQueue")) {
            return "Show which recordings are waiting to be processed.";
        } else if (setting.startsWith("Restart")) {
            return "Restart the jobs in the queue.";
        } else if (setting.startsWith("ShowFailed")) {
            return "Show the jobs that failed.";
        } else if (setting.startsWith("ClearFailed")) {
            return "Clear the list of failed jobs.";
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
        } else if (setting.startsWith("CleanupExt")) {
            return "When Recordings are deleted, the corresponding files with these extensions will be deleted.";
        } else if (setting.startsWith("WineHome")) {
            return "Override for non standard installs.";
        } else if (setting.startsWith("SetEnv")) {
            return "Delimit using a comma.";
        } else if (setting.startsWith("SkipChannels")) {
            return "Enter channel names or numbers delimited by a comma.";
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
            return "Configure ShowAnalyzer and Channel Mapping.";
        } else if (setting.startsWith("UseShowAnalyzer")) {
            return "Use ShowAnalyzer Instead of comskip.";
        } else if (setting.startsWith("VideoExt")) {
            return "No files will be cleaned up if a corresponding video file exists.";
        } else if (setting.startsWith("ShowAnalyzerLocation")) {
            return "Do NOT choose ShowAnalyzer.exe! ShowAnalyzer must be installed and registered.";
        } else if (setting.startsWith("ProfileLocation")) {
            return "If left blank none will be used.";
        } else if (setting.startsWith("RestrictTimes")) {
            return "Choose the time of day that comskip should not be run.";
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
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Unknown setting from getConfigHelpText = " + setting);
            return null;
        }
    }

    // Returns the label used to present this setting to the user
    public String getConfigLabel(String setting) {

        if (setting.startsWith("LogLevel")) {
            return "Debug Logging Level";
        } else if (setting.startsWith("MaxJobs")) {
            return "Maximum Concurrent Jobs";
        } else if (setting.startsWith("StopAll")) {
            return "Stop Running Jobs";
        } else if (setting.startsWith("ClearQueue")) {
            return "Clear Job Queue";
        } else if (setting.startsWith("ShowQueue")) {
            return "Show the Job Queue";
        } else if (setting.startsWith("Restart")) {
            return "Restart Queued Jobs";
        } else if (setting.startsWith("ShowFailed")) {
            return "Show the Failed Jobs";
        } else if (setting.startsWith("ClearFailed")) {
            return "Clear the Failed Job Queue";
        } else if (setting.startsWith("UNCMap")) {
            return "Drive Mapping";
        } else if (setting.startsWith("ComskipParms")) {
            return "Other comskip Parameters";
        } else if (setting.startsWith("CleanupExt")) {
            return "Cleanup Files With These Extensions";
        } else if (setting.startsWith("ComskipLocation")) {
            return "Location of comskip.exe";
        } else if (setting.startsWith("IniLocation")) {
            return "Location of comskip.ini";
        } else if (setting.startsWith("RunSlow")) {
            return "Run More Slowly";
        } else if (setting.startsWith("WineHome")) {
            return "Home directory for wine";
        } else if (setting.startsWith("VideoExt")) {
            return "File Extensions for Valid Video Files";
        } else if (setting.startsWith("SkipChannels")) {
            return "Do Not Run comskip on These Channels";
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
        } else if (setting.startsWith("RestrictTimes")) {
            return "Restricted Times";
        } else if (Names.contains(setting)) {
            return setting + ": Pick Commercial Detection Program";
        } else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlugIn: Unknown setting from getConfigLabel = " + setting);
            return null;
        }
    }

    // Resets the configuration of this plugin
    public void resetConfig() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlugIn: resetConfig received from Plugin Manager.");
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_WARN);

        NumberScanned = 0;
        Integer processors = getAvailableProcessors();
        processors = (processors>1 ? processors-1 : processors);
        Configuration.GetServerProperty("cd/max_jobs", processors.toString());
        Configuration.SetServerProperty("cd/comskip_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        Configuration.SetServerProperty("cd/ini_location", getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.ini");
        Configuration.SetProperty("cd/UNC_map", "");
        Configuration.SetServerProperty("cd/cleanup_ext", "edl,log,txt");
        Configuration.SetServerProperty("cd/comskip_parms", "");
        Configuration.SetServerProperty("cd/wine_home", getDefaultWineHome());
        Configuration.SetServerProperty("cd/wine_user", getDefaultWineUser());
        Configuration.SetServerProperty("cd/run_slow", "false");
        Configuration.SetServerProperty("cd/skip_channels", "");
        Configuration.SetServerProperty("cd/start_imm", "false");
        Configuration.SetServerProperty("cd/running_as_root", "true");
        Configuration.SetServerProperty("cd/show_advanced", "false");
        Configuration.SetServerProperty("cd/use_showanalyzer", "false");
        Configuration.SetServerProperty("cd/showanalyzer_location", "Select");
        Configuration.SetServerProperty("cd/profile_location", "Select");
        Configuration.SetServerProperty("cd/show_channels", "false");
        Configuration.SetServerProperty("cd/video_ext", PROPERTY_VIDEO_FILE_EXTENSIONS);
        Configuration.SetServerProperty("cd/restricted_times", null);
    }

    public static String getDefaultComskipLocation() {

        File PathFile = WidgetAPI.GetDefaultSTVFile();
        if (PathFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: null PathFile.");
            return null;
        }

        String STVPath = PathFile.getParent();
        if (STVPath==null || STVPath.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: null STVPath.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "plugin: STVPath = " + STVPath);

        // ../SageTV||server/STVs/SageTV7
        // ../SageTV||server/comskip

        int STVsIndex = STVPath.indexOf("STVs");
        if (STVsIndex<1) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: Malformed STVPath.");
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
        String Home = "/home";  // Hardcode the separator, if run in Windows it returns the wrong character.
        File HomeFile = new File(Home);
        if (HomeFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: null HomeFile.");
            return null;
        }

        String[] Contents = HomeFile.list();
        if (Contents==null || Contents.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: Nothing in " + HomeFile);
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

    private Integer getAvailableProcessors() {
        int processors;
        processors = Runtime.getRuntime().availableProcessors();
        return (processors>=1 ? processors : 1);
    }

    private void manualRun(String FileName) {

        Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: Manual rune for " + FileName);
        
        File F = new File(FileName);
        if (F==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: null File for " + FileName);
            return;
        }

        Object MediaFile = MediaFileAPI.GetMediaFileForFilePath(F);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: null MediaFile.");
            return;
        }

        api.DeleteComskipFiles(MediaFile);

        if (!ComskipManager.getInstance().queue(MediaFile)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "plugin: queue failed.");
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

    public synchronized void sageEvent(String eventName, java.util.Map eventVars) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: event received = " + eventName);

        // Check that we have the right event.
        if (!(  eventName.startsWith("RecordingCompleted") ||
                eventName.startsWith("RecordingStopped") ||
                eventName.startsWith("MediaFileRemoved") ||
                eventName.startsWith("RecordingStarted"))) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "sageEvent: Unexpected event received = " + eventName);
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
            
            if (ComskipManager.getInstance().cleanup(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaFile cleanup successful " + MediaFileAPI.GetMediaTitle(MediaFile));
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "sageEvent: Error-Could not cleanup MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        } else {

            // A recording is either starting or finishing.

            // Do nothing if it's on a channel that we are supposed to skip.
            if (skipThisChannel(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because it's a skip_channel.");
                return;
            }

            // Both RecordingStopped and RecordingCompleted is fired when a recording ends, so check that
            // it's not already queued.
            if (isRunningOrQueued(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because it's already running or queued.");
                return;
            }

            // Do nothing if it's the start of a recording and it's not set to start immediately.
            if (eventName.startsWith("RecordingStarted") && !SageUtil.GetBoolProperty("cd/start_imm", false)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping because not set to start immediately.");
                return;
            }

            /*
             * There is a small window where recordings may be processed twice.  Namely:
             * - We are here because a recording has finsished.
             * - CD is set to run comskip immediately.
             * - comskip has already completed the show that has just finished.
             *
             * We're relying on the supposition that Sage will fire RecordingStopped so soon after the
             * recording has actually stopped that comskip will not have a chance to finish processing it.
             *
             * We can't just ignore RecordingStopped when set to start comskip immediately because it may be
             * that the recording started during a restricted time and comskip is not actually processing it.
             */

            if (ComskipManager.getInstance().queue(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: MediaFile queued " + MediaFileAPI.GetMediaTitle(MediaFile));
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "sageEvent: Error-Could not queue MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            }
        }
          
        return;
    }

    private boolean skipThisChannel(Object MediaFile) {

        String SkipList = Configuration.GetServerProperty("cd/skip_channels", "");
        if (SkipList==null || SkipList.isEmpty()) {
            return false;
        }

        String[] SkipArray = SkipList.split(",");
        if (SkipArray==null || SkipArray.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "sageEvent: Bad SkipList " + SkipList);
            return false;
        }

        String ChannelName = AiringAPI.GetAiringChannelName(MediaFile);
        String ChannelNumber = AiringAPI.GetAiringChannelNumber(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "sageEvent: ChannelName and ChannelNumber " + ChannelName + ":" + ChannelNumber);

        for (String Skip : SkipArray) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "sageEvent: Skip = " + Skip);
            if (ChannelName.equalsIgnoreCase(Skip) || ChannelNumber.equalsIgnoreCase(Skip)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "sageEvent: Skipping " + Skip);
                return true;
            }
        }

        return false;
    }

    private boolean isRunningOrQueued(Object MediaFile) {

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

}
