/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.io.*;
import java.util.*;
import java.lang.Math;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class ComskipJob implements Runnable {

    private Process     process = null;
    private boolean     Stop = false;
    private int         MediaFileID = 0;
    private Timer       timer = null;
    private QueuedJob   Job = null;

    /*
     * Map to keep the raw data needed to calculate the completion ratio.  The Map key is the time
     * the data was gathered and JobSnapshot is the actual data.
     */
    Map<Long, JobSnapshot>  JobStats = null;

    // Delay a second before we start gathering statistics.
    public final static String      PROPERTY_JOBPROFILER_DELAY = "cd/jobprofiler_delay";
    public final static String      PROPERTY_JOBPROFILER_DELAY_DEFAULT = "1000";

    // By default look every 15 seconds to see how many jobs are running and how many recordings are in progress.
    public final static String      PROPERTY_JOBPROFILER_PERIOD = "cd/jobprofiler_period";
    public final static String      PROPERTY_JOBPROFILER_PERIOD_DEFAULT = "15000";
    public final static long        PROPERTY_JOBPROFILER_PERIOD_DEFAULT_LONG = 15000L;

    // By default each additional comskip jobs that is running will increase execution time by 30%
    public final static String      SETTING_RUNNING_IMPACT = "RunningImpact";
    public final static String      PROPERTY_RUNNING_IMPACT = "cd/jobprofiler_cpu_impact";
    public final static String      PROPERTY_DEFAULT_RUNNING_IMPACT = "0.7";
    public final static Float       PROPERTY_DEFAULT_RUNNING_IMPACT_FLOAT = 0.7F;

    // By default each additional recording in progress will increase excution by 30%
    public final static String      SETTING_RECORD_IMPACT = "RecordImpact";
    public final static String      PROPERTY_RECORD_IMPACT = "cd/jobprofiler_record_impact";
    public final static String      PROPERTY_DEFAULT_RECORD_IMPACT = "0.7";
    public final static Float       PROPERTY_DEFAULT_RECORD_IMPACT_FLOAT = 0.7F;

    // By default the data from each new recording will have a 10% impact on the overall channel ratio.
    public final static String      SETTING_RECORDING_IMPACT_ON_CHANNEL = "RecordChanImpact";
    public final static String      PROPERTY_RECORDING_IMPACT_ON_CHANNEL = "cd/jobprofiler_recording_impact";
    public final static Float       PROPERTY_DEFAULT_RECORDING_IMPACT_ON_CHANNEL = 0.10F;

    /**
     * Constructor.  Creates a new ComskipJob and initializes all internal variables.  Removes the first
     * job from the queue.
     */
    public ComskipJob() {
        Stop = false;
        Job = ComskipManager.getInstance().getNextJob();
        JobStats = new HashMap<Long, JobSnapshot>();

        if (Job!=null) {
            MediaFileID = Job.getMediaFileID();
            CSC.getInstance().addStatus(CSC.STATUS_PROCESSING, MediaFileID);
        }
    }
    
    /**
     * Static method that creates a new ComskipJob thread of execution (job).
     *
     * @return Reference to the newly created job.
     */
    public static ComskipJob StartComskipJob() {
        ComskipJob t = new ComskipJob();
        new Thread(t).start();
        return t;
    }

    /**
     * Get the MediaFile ID for the job that is currently running.
     *
     * @return The MediaFile ID.
     */
    public int getMediaFileID() {
        return MediaFileID;
    }

    public Object getMediaFile() {
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.getMediaFile: null MediaFile for ID " + MediaFileID);
        }
        return MediaFile;
    }

    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob: Starting new ComskipJob");

        // Stop this job immediately if soemthing went wrong in the constructor.
        if (Job==null || JobStats==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob: Error in constructor.");
            ComskipManager.getInstance().jobComplete(this, false);
            return;
        }

        // Keep track of when the ob started.
        long jobStartTime = Utility.Time();

        // Start the task that will monitor the job and supply the raw data needed to calculate the Ratio.
        long Delay = SageUtil.GetLongProperty(PROPERTY_JOBPROFILER_DELAY, PROPERTY_JOBPROFILER_DELAY_DEFAULT);
        long Period = SageUtil.GetLongProperty(PROPERTY_JOBPROFILER_PERIOD, PROPERTY_JOBPROFILER_PERIOD_DEFAULT);
        TimerTask JobProfiler = new JobProfiler(this);
        timer = new Timer();
        timer.scheduleAtFixedRate(JobProfiler, Delay, Period);

        String ProgramToUse = Job.getProgramToUse();
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: ChannelName and Program = " + ChannelName + ":" + ProgramToUse);

        if (ProgramToUse.equalsIgnoreCase("comskip"))
            executeComskip(Job);
        else if (ProgramToUse.equalsIgnoreCase("showanalyzer"))
            executeShowAnalyzer(Job);
        else if (ProgramToUse.equalsIgnoreCase("default"))
            executeUsingDefault(Job);
        else if (ProgramToUse.equalsIgnoreCase("none"))
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob: Skipping, Channel overridden.");
        else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob: Unknown ProgramToUse " + ProgramToUse);
            executeUsingDefault(Job);
        }

        // Stop the monitoring.
        timer.cancel();

        // Mark this job as complete.
        ComskipManager.getInstance().jobComplete(this, true);

        // Tell the SageClients that the job is ocmplete.
        CSC.getInstance().removeStatus(CSC.STATUS_PROCESSING, MediaFileID);

        // Adjust the ratio using the stats from the job that just completed.
        updateRatio(jobStartTime);

        // If the job was stopped or interrupted, delete any exisitng .edl files and then add the job back
        // to the queue for reprocessing.
        if (Stop) {
            
            Object MediaFile = getMediaFile();
            if (MediaFile!=null) {

                CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile);
                CDMediaFile.cleanup();
            }

            if (!ComskipManager.getInstance().addToQueue(Job)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob: Error adding back failed job.");
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob: Adding stopped job back to queue.");
            }
        }
    }

    private void executeComskip(QueuedJob Job) {
        if (Global.IsWindowsOS())
            executeComskipWindows(Job);
        else
            executeComskipLinux(Job);
    }

    private void executeShowAnalyzer(QueuedJob Job) {
        if (Global.IsWindowsOS())
            executeShowAnalyzerWindows(Job);
        else
            executeShowAnalyzerLinux(Job);
    }

    private void executeUsingDefault(QueuedJob Job) {
        if (SageUtil.GetBoolProperty("cd/use_showanalyzer", false)) {
            executeShowAnalyzer(Job);
        } else {
            executeComskip(Job);
        }
    }

    private void executeComskipWindows(QueuedJob Job) {

        List<String> CommandList = new ArrayList<String>();

        String ComskipLocation = Configuration.GetServerProperty("cd/comskip_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        CommandList.add(ComskipLocation);

        if (SageUtil.GetBoolProperty("cd/run_slow", false)) {
            CommandList.add("--playnice");
        }

        String IniLocation = Job.getComskipIni();
        if (IniLocation!=null && !IniLocation.isEmpty()) {
            CommandList.add("--ini="+IniLocation);
        }
        

        // Now add in any other paremters specified.
        String OtherParms = Configuration.GetServerProperty("cd/comskip_parms", "");
        if (!(OtherParms==null || OtherParms.isEmpty())) {
            String[] Parms = OtherParms.split(" ");
            if (Parms==null || Parms.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipWindows: Malformed paramter list " + OtherParms);
            } else {
                for (int i=0; i<Parms.length; i++) {
                    CommandList.add(Parms[i]);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipWindows: Adding paramter " + Parms[i]);
                }
            }
        }

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipWindows: Job parts " + Job.getFileName().size());

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipWindows: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipWindows: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeComskipWindows: File no longer exists or has been processed " + FileName);
            } else {

                //String s = "\"" + FileName + "\"";
                CommandList.add(FileName);

                String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipWindows: Command " + CommandList);

                try {process = Runtime.getRuntime().exec(Command); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipWindows: Exception starting comskip " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeComskipWindows: comskip.exe was interrupted " + e.getMessage());
                    stop();
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                // If the Stop flag is set do nothing else.  Sage is probably shuttoing down.
                if (Stop) {
                    return;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipWindows: comskip return code = " + status);
                if (status>1) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipWindows: comskip failed with return code = " + status);
                }
            }

            CommandList.remove(FileName);
        }
    }

    private void executeComskipLinux(QueuedJob Job) {

        // If RunningAsRoot -> sudo -H -u <USER> -s wine comskip.exe --ini=comskip.ini FILE.MPG
        // If running as a user -> wine comskip.exe ...

        String CommandLine = null;

        if (SageUtil.GetBoolProperty("cd/running_as_root", true)) {
            CommandLine = "sudo -H -u ";

            String User = Configuration.GetServerProperty("cd/wine_user", "");
            if (User==null || User.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: null wine_user.");
                return;
            }

            //CommandLine = CommandLine + User + " -s wine ";
            CommandLine = CommandLine + User + " wine ";
        } else {
            CommandLine = "wine ";
        }

        String ComskipLocation = Configuration.GetServerProperty("cd/comskip_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        if (ComskipLocation==null || ComskipLocation.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: null comskip_location.");
            return;
        }

        CommandLine = CommandLine + ComskipLocation + " ";

        if (SageUtil.GetBoolProperty("cd/run_slow", false)) {
            CommandLine = CommandLine + "--playnice ";
        }

        String IniLocation = Job.getComskipIni();
        if (IniLocation!=null && !IniLocation.isEmpty()) {
            CommandLine = CommandLine + "--ini=" + IniLocation + " ";
        }

        // Now add in any other paremters specified.
        String OtherParms = Configuration.GetServerProperty("cd/comskip_parms", "");
        if (!(OtherParms==null || OtherParms.isEmpty())) {
            String[] Parms = OtherParms.split(" ");
            if (Parms==null || Parms.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: Malformed paramter list " + OtherParms);
            } else {
                for (int i=0; i<Parms.length; i++) {
                    CommandLine = CommandLine + Parms[i] + " ";
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: Adding paramter " + Parms[i]);
                }
            }
        }

        String WinePath = Configuration.GetServerProperty("cd/wine_home", plugin.getDefaultWineHome());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: WinePath " + WinePath);

        String[] env = {"WINEPREFIX="+WinePath,"WINEPATH="+WinePath};

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: Job parts " + Job.getFileName().size());

        String CommandLineToExecute = null;

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeComskipLinux: File no longer exists or has been processed " + FileName);
            } else {

                CommandLineToExecute = CommandLine + FileName;

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: Command " + CommandLineToExecute);

                try {process = Runtime.getRuntime().exec(CommandLineToExecute); } catch (IOException e) {
                //try {process = Runtime.getRuntime().exec(CommandLineToExecute, env); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: Exception starting comskip " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeComskipLinux: comskip.exe was interrupted " + e.getMessage());
                    stop();
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                // If the Stop flag is set do nothing else.  Sage is probably shuttoing down.
                if (Stop) {
                    return;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeComskipLinux: comskip return code = " + status);
                if (status>1) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeComskipLinux: comskip failed with return code = " + status);
                }
            }
        }
    }

    private void executeShowAnalyzerWindows(QueuedJob Job) {

        // ShowAnalyzerEngine.exe FILE.MPG --profile FILE

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "Select");
        if (ShowAnalyzerLocation.startsWith("Select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComkipJob.executeShowAnalyzerWindows: No ShowAnalyzer location has been set.");
            return;
        }

        List<String> FileNames = Job.getFileName();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComkipJob.executeShowAnalyzerWindows: Job parts " + Job.getFileName().size());

        String ProfileLocation = Job.getShowAnalyzerProfile();

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComkipJob.executeShowAnalyzerWindows: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComkipJob.executeShowAnalyzerWindows: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComkipJob.executeShowAnalyzerWindows: File no longer exists or has been processed " + FileName);
            } else {

                //String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);
                List<String> CommandList = new ArrayList<String>();

                //String CommandLine = ShowAnalyzerLocation + " " + FileName;
                CommandList.add(ShowAnalyzerLocation);
                CommandList.add(FileName);

                if (ProfileLocation!=null) {
                    //CommandLine = CommandLine + " --profile " + ProfileLocation;
                    CommandList.add("--profile");
                    CommandList.add(ProfileLocation);
                }

                String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComkipJob.executeShowAnalyzerWindows: Command " + CommandList);

                try {process = Runtime.getRuntime().exec(Command); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComkipJob.executeShowAnalyzerWindows: Exception starting ShowAnalyer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComkipJob.executeShowAnalyzerWindows: ShowAnalyzer was interrupted " + e.getMessage());
                    stop();
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                // If the Stop flag is set do nothing else.  Sage is probably shuttoing down.
                if (Stop) {
                    return;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComkipJob.executeShowAnalyzerWindows: ShowAnalyzer return code = " + status);
            }
        }
    }

    private void executeShowAnalyzerWindowsString(QueuedJob Job) {

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "Select");
        if (ShowAnalyzerLocation.startsWith("Select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerWindowsString: No ShowAnalyzer location has been set.");
            return;
        }

        List<String> FileNames = Job.getFileName();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerWindowsString: Job parts " + Job.getFileName().size());

        String ProfileLocation = Job.getShowAnalyzerProfile();

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerWindowsString: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerWindowsString: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeShowAnalyzerWindowsString: File no longer exists or has been processed " + FileName);
            } else {

                //String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);

                String CommandLine = ShowAnalyzerLocation + " " + FileName;

                if (ProfileLocation!=null) {
                    CommandLine = CommandLine + " --profile " + ProfileLocation;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerWindowsString: Command " + CommandLine);

                try {process = Runtime.getRuntime().exec(CommandLine); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerWindowsString: Exception starting ShowAnalyer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeShowAnalyzerWindowsString: ShowAnalyzer was interrupted " + e.getMessage());
                    stop();
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                // If the Stop flag is set do nothing else.  Sage is probably shutting down.
                if (Stop) {
                    return;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerWindowsString: ShowAnalyzer return code = " + status);
            }
        }
    }

    private void executeShowAnalyzerLinux(QueuedJob Job) {

        // If RunningAsRoot -> sudo -H -u <USER> -s wine ShowAnalyzerEngine.exe FILE.MPG --profile FILE
        // If running as a user -> wine ShowAnalyzerEngine.exe ...

        String CommandLine = null;

        if (SageUtil.GetBoolProperty("cd/running_as_root", true)) {
            CommandLine = "sudo -H -u ";

            String User = Configuration.GetServerProperty("cd/wine_user", "");
            if (User==null || User.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerLinux: null wine_user.");
                return;
            }

            CommandLine = CommandLine + User + " -s wine ";
        } else {
            CommandLine = "wine ";
        }

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "");
        if (ShowAnalyzerLocation==null || ShowAnalyzerLocation.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerLinux: null showanalyzer_location.");
            return;
        }

        CommandLine = CommandLine + ShowAnalyzerLocation + " ";

        String ProfileLocation = Configuration.GetServerProperty("cd/profile_location", "");
        if (ProfileLocation==null || ProfileLocation.isEmpty() || ProfileLocation.equalsIgnoreCase("select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerLinux: null profile_location.");
            ProfileLocation = null;
        }

        String WinePath = Configuration.GetServerProperty("cd/wine_home", plugin.getDefaultWineHome());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerLinux: WinePath " + WinePath);

        String[] env = {"WINEPREFIX="+WinePath,"WINEPATH="+WinePath};

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerLinux: Job parts " + Job.getFileName().size());

        String CommandLineToExecute = null;

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerLinux: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerLinux: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeShowAnalyzerLinux: File no longer exists or has been processed " + FileName);
            } else {

                CommandLineToExecute = CommandLine + FileName;
                if (ProfileLocation!=null) {
                    CommandLineToExecute = CommandLineToExecute + " --profile " + ProfileLocation;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerLinux: Command " + CommandLineToExecute);

                try {process = Runtime.getRuntime().exec(CommandLineToExecute, env); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.executeShowAnalyzerLinux: Exception starting ShowAnalyzer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.executeShowAnalyzerLinux: ShowAnalyzer was interrupted " + e.getMessage());
                    stop();
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                // If the Stop flag is set do nothing else.  Sage is probably shuttoing down.
                if (Stop) {
                    return;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.executeShowAnalyzerLinux: ShowAnalyzer return code = " + status);
            }
        }
    }

    /**
     * Stops the ComskipJob by destroying the comskip or ShowAnalyzer process that is running
     * and setting a flag telling the ComskipJob to terminate gracefully.
     */
    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.stop: Stopping ComskipJob.");

        // Stop the thread from processing any more.
        Stop = true;

        // Destroy the running comskip or ShowAnalyzer process.
        if (process!=null)
            process.destroy();
    }

    private void updateRatio(long jobStartTime) {

        // See how logn the job took to complete.
        long jobTime = Utility.Time() - jobStartTime;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatio: jobTime " + jobTime + ":" + jobTime/1000/60 + " minutes.");

        // Adjust the time depending on what was happening while the job was being processed.
        long adjustedJobTime = adjustJobTime(jobTime);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatio: adjustedJobTime " + adjustedJobTime);

        // Get the total length of the MediaFile.
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.updateRatio: null MediaFile for ID " + MediaFileID);
            return;
        }

        long MediaFileDuration = MediaFileAPI.GetFileDuration(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatio: MediaFile duration " + MediaFileDuration + ":" + MediaFileDuration/1000/60 + " minutes.");

        if (MediaFileDuration==0 || MediaFileDuration<0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.updateRatio: No duration.");
            return;
        }

        // Calculate the ratio for this recording.
        Float RecordingRatio = (float)adjustedJobTime / (float)MediaFileDuration;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatio: Actual ratio " + RecordingRatio);

        String Channel = AiringAPI.GetAiringChannelName(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatio: Channel for this MediaFile " + Channel);

        if (Channel==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.updateRatio: null Channel.");
            return;
        }

        // Adjust the existing Ratio
        Float ChannelRatio = plugin.ChannelTimeRatios.get(Channel);

        if (ChannelRatio==null || ChannelRatio==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.updateRatio: null ChannelRatio.");
            ChannelRatio = plugin.RATIO_DEFAULT;
        }

        Float RecordImpactOnChannel = SageUtil.GetFloatProperty(PROPERTY_RECORDING_IMPACT_ON_CHANNEL, PROPERTY_DEFAULT_RECORDING_IMPACT_ON_CHANNEL);
        Float NewRatio = ((1.0F-RecordImpactOnChannel) * ChannelRatio ) + (RecordImpactOnChannel * RecordingRatio);
        Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.updateRatio: Old, Current and new Ratios " + ChannelRatio + ":" + RecordingRatio + ":" + NewRatio);

        plugin.ChannelTimeRatios.put(Channel, NewRatio);

        updateRatioProperties();
    }

    private synchronized void updateRatioProperties() {

        if (plugin.ChannelTimeRatios.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.updateRatioProperties: ChannelTimeRatios is empty.");
            return;
        }

        Set<String> Channels = plugin.ChannelTimeRatios.keySet();

        if (Channels==null && Channels.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.updateRatioProperties: null Channels.");
            return;
        }
        
        String NewProperty = null;

        for (String Channel : Channels) {

            // Get the Ratio for this Channel.
            Float Ratio = plugin.ChannelTimeRatios.get(Channel);

            String RatioString = Ratio.toString();

            String PropElement = Channel + ":" + RatioString;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipJob.updateRatioProperties: PropElement " + PropElement);

            if (NewProperty==null) {
                NewProperty = PropElement;
            } else {
                NewProperty = NewProperty + "," + PropElement;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.updateRatioProperties: NewProperty " + NewProperty);

        Configuration.SetServerProperty(plugin.PROPERTY_TIME_RATIOS, NewProperty);
    }

    /**
     * Add a jobSnapshot to the JobStats Map.  The key to the Map will be the current time. (i.e. the time that
     * this method is invoked.)
     *
     * @param Snapshot The JobSnapshot to add.
     */
    public void addSnapshot(JobSnapshot Snapshot) {
        JobStats.put(Utility.Time(), Snapshot);
    }

    private long adjustJobTime(long actualTime) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.adjustJobTime: Have JobStats " + JobStats.size());

        String S = Configuration.GetServerProperty(PROPERTY_RUNNING_IMPACT, PROPERTY_DEFAULT_RUNNING_IMPACT);

        Float RunningImpact = PROPERTY_DEFAULT_RUNNING_IMPACT_FLOAT;
        try {
            RunningImpact = Float.parseFloat(S);
        } catch (NumberFormatException nfe) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.adjustJobTime: Malformed CPU impact.");
            Configuration.SetServerProperty(PROPERTY_RUNNING_IMPACT, PROPERTY_DEFAULT_RUNNING_IMPACT);
            RunningImpact = PROPERTY_DEFAULT_RUNNING_IMPACT_FLOAT;
        }

        S = Configuration.GetServerProperty(PROPERTY_RECORD_IMPACT, PROPERTY_DEFAULT_RECORD_IMPACT);

        Float RecordImpact = PROPERTY_DEFAULT_RECORD_IMPACT_FLOAT;
        try {
            RecordImpact = Float.parseFloat(S);
        } catch (NumberFormatException nfe) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.adjustJobTime: Malformed disk impact.");
            Configuration.SetServerProperty(PROPERTY_RECORD_IMPACT, PROPERTY_DEFAULT_RECORD_IMPACT);
            RecordImpact = PROPERTY_DEFAULT_RECORD_IMPACT_FLOAT;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.adjustJobTime: CPU and Record impact " + RunningImpact + ":" + RecordImpact);

        // Double check that neither of our impact factors are 0.
        RunningImpact = (RunningImpact==0 ? PROPERTY_DEFAULT_RUNNING_IMPACT_FLOAT : RunningImpact);
        RecordImpact = (RecordImpact==0 ? PROPERTY_DEFAULT_RECORD_IMPACT_FLOAT : RecordImpact);

        long Period = SageUtil.GetLongProperty(PROPERTY_JOBPROFILER_PERIOD, PROPERTY_JOBPROFILER_PERIOD);
        if (Period==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipJob.adjustJobTime: Malformed Period.");
            Configuration.SetServerProperty(PROPERTY_JOBPROFILER_PERIOD, PROPERTY_JOBPROFILER_PERIOD);
            Period = PROPERTY_JOBPROFILER_PERIOD_DEFAULT_LONG;
        }

        Set<Long> Times = JobStats.keySet();

        if (Times==null || Times.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.adjustJobTime: Null Times.");
            return actualTime;
        }

        long adjustedTime = 0L;

        for (Long Time : Times) {
            JobSnapshot Snapshot = JobStats.get(Time);

            if (Snapshot==null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipJob.adjustJobTime: null Snapshot.");
                continue;
            }

            int NumberRunning = Snapshot.getNumberComskipRunning() - 1; // Don't count the one we're on.
            int NumberRecording = Snapshot.getNumberRecording();

            float adjustmentRatio = 1.0F;

            for (int i=0; i<NumberRunning; i++) {
                adjustmentRatio = adjustmentRatio * RunningImpact;
            }

            for (int i=0; i<NumberRecording; i++) {
                adjustmentRatio = adjustmentRatio * RecordImpact;
            }

            // Lower the execution time depending on how many other comskip jobs were running and
            // how many recordings were in progress.
            adjustedTime = adjustedTime + (long)(Period * adjustmentRatio);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.adjustJobTime: Returning " + adjustedTime);
        float percent = (actualTime - adjustedTime) / actualTime;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipJob.adjustJobTime: Percent adjustment " + percent);
        
        return adjustedTime;
    }

    public String getShowTitle() {
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        return (MediaFile==null ? "<Unknown>" : ShowAPI.GetShowTitle(MediaFile));
    }

    public String getShowEpisode() {
        Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);
        return (MediaFile==null ? "<Unknown>" : ShowAPI.GetShowEpisode(MediaFile));
    }

    public String getShowTitleEpisode() {
        return getShowTitle() + ":" + getShowEpisode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ComskipJob other = (ComskipJob) obj;
        if (this.Job != other.Job && (this.Job == null || !this.Job.equals(other.Job))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.Job != null ? this.Job.hashCode() : 0);
        return hash;
    }
}

/**
 * Raw data that is used to calibrate how long jobs actually take to complete vs. the length of the job.
 *
 * @author Tom Miranda
 */
class JobSnapshot {
    private int NumberComskipRunning;
    private int NumberRecording;

    /**
     * Constructor.
     *
     * @param NumComskipJobs The number of comskip or ShowAnalyzer jobs that are currently running.
     * @param NumRec The number of recordings that are in progress.
     */
    public JobSnapshot(int NumComskipJobs, int NumRec) {
        NumberComskipRunning = NumComskipJobs;
        NumberRecording = NumRec;
    }

    public int getNumberComskipRunning() {
        return NumberComskipRunning;
    }

    public int getNumberRecording() {
        return NumberRecording;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JobSnapshot other = (JobSnapshot) obj;
        if (this.NumberComskipRunning != other.NumberComskipRunning) {
            return false;
        }
        if (this.NumberRecording != other.NumberRecording) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + this.NumberComskipRunning;
        hash = 43 * hash + this.NumberRecording;
        return hash;
    }

}
