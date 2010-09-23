/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.io.*;
import java.util.*;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class ComskipJob implements Runnable {

    private Process process = null;
    private boolean Stop = false;
    private int MediaFileID = 0;
    
    public static ComskipJob StartComskipJob() {
        ComskipJob t = new ComskipJob();
        new Thread(t).start();
        return t;
    }

    public int getMediaFileID() {
        return MediaFileID;
    }

    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Starting new ComskipJob");

        QueuedJob Job = ComskipManager.getInstance().getNextJob();
        if (Job==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Error getting next job.");
            ComskipManager.getInstance().jobComplete(this, false);
        }

        MediaFileID = Job.getMediaFileID();
        CSC.getInstance().addStatus("processing", MediaFileID);

        //String ChannelName = Job.getChannelName();
        //if (ChannelName==null || ChannelName.isEmpty()) {
            //Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null ChannelName, using default process "+ MediaFileID);
            //ChannelName = "<Unknown>";
        //}

        String ProgramToUse = Job.getProgramToUse();
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: ChannelName and Program = " + ChannelName + ":" + ProgramToUse);

        if (ProgramToUse.equalsIgnoreCase("comskip"))
            executeComskip(Job);
        else if (ProgramToUse.equalsIgnoreCase("showanalyzer"))
            executeShowAnalyzer(Job);
        else if (ProgramToUse.equalsIgnoreCase("default"))
            executeUsingDefault(Job);
        else if (ProgramToUse.equalsIgnoreCase("none"))
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Skipping, Channel overridden.");
        else {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "run: Unknown ProgramToUse " + ProgramToUse);
            executeUsingDefault(Job);
        }

        ComskipManager.getInstance().jobComplete(this, true);
        CSC.getInstance().removeStatus("processing", MediaFileID);
        //ComskipManager.getInstance().updateMediaFileIDsWithoutEDL(MediaFileID);
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Malformed paramter list " + OtherParms);
            } else {
                for (int i=0; i<Parms.length; i++) {
                    CommandList.add(Parms[i]);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Adding paramter " + Parms[i]);
                }
            }
        }

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Job parts " + Job.getFileName().size());

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "run: File no longer exists or has been processed " + FileName);
            } else {

                //String s = "\"" + FileName + "\"";
                CommandList.add(FileName);

                String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Command " + CommandList);

                try {process = Runtime.getRuntime().exec(Command); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Exception starting comskip " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "run: comskip.exe was interrupted " + e.getMessage());
                    jobInterrupted(Job, i);
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: comskip return code = " + status);
                if (status>1) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: comskip failed with return code = " + status);
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null wine_user.");
                return;
            }

            //CommandLine = CommandLine + User + " -s wine ";
            CommandLine = CommandLine + User + " wine ";
        } else {
            CommandLine = "wine ";
        }

        String ComskipLocation = Configuration.GetServerProperty("cd/comskip_location", plugin.getDefaultComskipLocation() + File.separator + "comskip" + File.separator + "comskip.exe");
        if (ComskipLocation==null || ComskipLocation.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null comskip_location.");
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Malformed paramter list " + OtherParms);
            } else {
                for (int i=0; i<Parms.length; i++) {
                    CommandLine = CommandLine + Parms[i] + " ";
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Adding paramter " + Parms[i]);
                }
            }
        }

        String WinePath = Configuration.GetServerProperty("cd/wine_home", plugin.getDefaultWineHome());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: WinePath " + WinePath);

        String[] env = {"WINEPREFIX="+WinePath,"WINEPATH="+WinePath};

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Job parts " + Job.getFileName().size());

        String CommandLineToExecute = null;

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "run: File no longer exists or has been processed " + FileName);
            } else {

                CommandLineToExecute = CommandLine + FileName;

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Command " + CommandLineToExecute);

                try {process = Runtime.getRuntime().exec(CommandLineToExecute); } catch (IOException e) {
                //try {process = Runtime.getRuntime().exec(CommandLineToExecute, env); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Exception starting comskip " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "run: comskip.exe was interrupted " + e.getMessage());
                    jobInterrupted(Job, i);
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: comskip return code = " + status);
                if (status>1) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: comskip failed with return code = " + status);
                }
            }
        }
    }

    private void executeShowAnalyzerWindows(QueuedJob Job) {

        // ShowAnalyzerEngine.exe FILE.MPG --profile FILE

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "Select");
        if (ShowAnalyzerLocation.startsWith("Select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: No ShowAnalyzer location has been set.");
            return;
        }

        List<String> FileNames = Job.getFileName();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Job parts " + Job.getFileName().size());

        String ProfileLocation = Job.getShowAnalyzerProfile();

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "run: File no longer exists or has been processed " + FileName);
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

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Command " + CommandList);

                try {process = Runtime.getRuntime().exec(Command); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Exception starting ShowAnalyer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "run: ShowAnalyzer was interrupted " + e.getMessage());
                    jobInterrupted(Job, i);
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: ShowAnalyzer return code = " + status);
            }
        }
    }

    private void executeShowAnalyzerWindowsString(QueuedJob Job) {

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "Select");
        if (ShowAnalyzerLocation.startsWith("Select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: No ShowAnalyzer location has been set.");
            return;
        }

        List<String> FileNames = Job.getFileName();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Job parts " + Job.getFileName().size());

        String ProfileLocation = Job.getShowAnalyzerProfile();

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "run: File no longer exists or has been processed " + FileName);
            } else {

                //String[] Command = (String[])CommandList.toArray(new String[CommandList.size()]);

                String CommandLine = ShowAnalyzerLocation + " " + FileName;

                if (ProfileLocation!=null) {
                    CommandLine = CommandLine + " --profile " + ProfileLocation;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Command " + CommandLine);

                try {process = Runtime.getRuntime().exec(CommandLine); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Exception starting ShowAnalyer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "run: ShowAnalyzer was interrupted " + e.getMessage());
                    jobInterrupted(Job, i);
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: ShowAnalyzer return code = " + status);
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null wine_user.");
                return;
            }

            CommandLine = CommandLine + User + " -s wine ";
        } else {
            CommandLine = "wine ";
        }

        String ShowAnalyzerLocation = Configuration.GetServerProperty("cd/showanalyzer_location", "");
        if (ShowAnalyzerLocation==null || ShowAnalyzerLocation.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null showanalyzer_location.");
            return;
        }

        CommandLine = CommandLine + ShowAnalyzerLocation + " ";

        String ProfileLocation = Configuration.GetServerProperty("cd/profile_location", "");
        if (ProfileLocation==null || ProfileLocation.isEmpty() || ProfileLocation.equalsIgnoreCase("select")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null profile_location.");
            ProfileLocation = null;
        }

        String WinePath = Configuration.GetServerProperty("cd/wine_home", plugin.getDefaultWineHome());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: WinePath " + WinePath);

        String[] env = {"WINEPREFIX="+WinePath,"WINEPATH="+WinePath};

        List<String> FileNames = Job.getFileName();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Job parts " + Job.getFileName().size());

        String CommandLineToExecute = null;

        // Process all the individual files that make up the MediaFile.
        for (int i=0; i<Job.getFileName().size() && !Stop; i++) {
            String FileName = FileNames.get(i);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Processing " + FileName);

            File F = new File(FileName);
            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: null File.");
                return;
            }

            if (!F.exists() || ComskipManager.getInstance().hasAnEdlOrTxtFile(FileName)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "run: File no longer exists or has been processed " + FileName);
            } else {

                CommandLineToExecute = CommandLine + FileName;
                if (ProfileLocation!=null) {
                    CommandLineToExecute = CommandLineToExecute + " --profile " + ProfileLocation;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Command " + CommandLineToExecute);

                try {process = Runtime.getRuntime().exec(CommandLineToExecute, env); } catch (IOException e) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Exception starting ShowAnalyzer " + e.getMessage());
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
                StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
                ErrorStream.start();
                OutputStream.start();

                int status = 0;
                try {status=process.waitFor(); } catch (InterruptedException e) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "run: ShowAnalyzer was interrupted " + e.getMessage());
                    jobInterrupted(Job, i);
                    ComskipManager.getInstance().jobComplete(this, false);
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: ShowAnalyzer return code = " + status);
            }
        }
    }

    public void stop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Stopping ComskipJob.");
        Stop = true;
        process.destroy();
    }

    private void jobInterrupted(QueuedJob Job, int Segment) {
        Job.setJobHasFailed(true);
        Job.setFailingSegment(Segment);
        if (!ComskipManager.getInstance().addToQueue(Job)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "run: Error adding back failed job.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "run: Adding interrupted job back to queue.");
        }
    }

}
