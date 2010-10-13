/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.io.*;
import java.util.*;
import java.text.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class ComskipManager {

    private static ComskipManager instance = new ComskipManager();

    private static List<ComskipJob> CurrentJobs = new ArrayList<ComskipJob>();

    private static final String RESOURCES = "CommercialDetector";
    private static final String COMSKIP = "comskip";
    //private static final String SHOWANALYZER = "ShowAnalyzer";

    private static final String QUEUE = RESOURCES + File.separator + "CommercialDetector.DB";
    public static final String COMSKIP_INI_DIR = RESOURCES + File.separator + COMSKIP;

    //private static List<Integer> MediaFileIDsWithoutEDL;

    private ComskipManager(){};

    public static ComskipManager getInstance() {
        return instance;
    }

    public void destroy() {
        stopAll();
        instance = null;
    }

    public void makeResources() {

        // Delete the old Database if it is there.
        File OldDB = new File("CommercialDetector.DB");
        if (OldDB!=null) {
            if (OldDB.exists()) {
                OldDB.delete();
            }
        }

        File Directory = new File(RESOURCES);
        if (Directory==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "makeResources: null Directory, fatal error.");
            return;
        }

        if (Directory.exists() && Directory.isDirectory()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "makeResources: Directory exists.");
            return;
        }

        if (!Directory.mkdir()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "makeResources: Directory could not be created, fatal error");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "makeResources: Directory created.");
    }

    public int getNumberRunning() {
        return CurrentJobs.size();
    }

    // Return total size of queue EXCLUDING NumberRunning.
    public int getQueueSize(boolean Failed) {
        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty())
            return 0;

        int Count = 0;
        for (QueuedJob J : Jobs)
            if (Failed) {
                if (J.getJobHasFailed()) Count++;
            } else {
                if (!J.getJobHasFailed()) Count++;
            }
         
        return Count;
    }

    public synchronized boolean queue(Object MediaFile) {

        File Parent = MediaFileAPI.GetParentDirectory(MediaFile);
        File[] files = MediaFileAPI.GetSegmentFiles(MediaFile);
        int ID = MediaFileAPI.GetMediaFileID(MediaFile);

        if (files[0]==null || files.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null or empty segment 0.");
            return false;
        }

        String Path = Parent.getAbsolutePath();
        if (Path==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null path.");
            return false;
        }

        // Check if we need to remap drives to UNC paths.
        String UNCMap = Configuration.GetProperty("cd/UNC_map", "");
        if (!(UNCMap==null || UNCMap.isEmpty())) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Found UNC mappings " + UNCMap);
            Path = remapDrive(Path, UNCMap);
        }

        // Get a list of all the files that make up this MediaFile.
        List<String> FilesToProcess = new ArrayList<String>();

        for (File f : files) {
            String name = CreateFullPath(Path, f.getName());
            if (name==null)
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager: null name skipped.");
            else
                FilesToProcess.add(name);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: FilesToProcess = " + FilesToProcess);

        // Add this job to the queue on disk.
        QueuedJob NewJob = new QueuedJob(FilesToProcess, ID);

        if (!addToQueue(NewJob)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error from addToQueue.");
            return false;
        }

        // If we are below the job execution limit, start this job.
        int MaxJobs = SageUtil.GetIntProperty("cd/max_jobs", 1);

        if (getNumberRunning()<MaxJobs && !inRestrictedTime()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Below MaxJobs threshhold.");
            return jobStart();
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: MaxJobs already running " + getNumberRunning() + MaxJobs);
            return true;
        }
    }

    public boolean inRestrictedTime() {
        String S = Configuration.GetServerProperty("cd/restricted_times", null);

        if (S==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No Restricted times.");
            return false;
        }

        String[] Hours = S.split(",");

        if (Hours==null || Hours.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Malformed restricted_times " + S);
            return false;
        }

        // Get the current time and format it so we only get the hour of the day.
        Date Now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("H");

        String CurrentHour = formatter.format(Now);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Current hour is " + CurrentHour);

        for (String Hour : Hours) {
            String H = Hour.substring(0,2);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager: Restricted hour is " + H);

            if (CurrentHour.equals(H)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Current hour is restricted.");
                return true;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Current hour is not restricted.");
        return false;
    }

    private String remapDrive(String Path, String UNCMap) {

        // Split the string into Drive-Path pairs;
        String[] Pairs = UNCMap.split(",");
        if (Pairs==null || Pairs.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Invalid mappings.");
            return Path;
        }

        String[] DrivePath = Path.split(":");
        if (DrivePath==null || DrivePath.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Invalid Path.");
            return Path;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Drive = " + DrivePath[0]);

        for (String Pair : Pairs) {

            // Split the string into a Drive and a UNC Path.
            String[] Map = Pair.split("-");
            if (Map==null || Map.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: No mappings.");
                return Path;
            }

            // See if the drive matches the drive in Path.
            if (Map[0].equalsIgnoreCase(DrivePath[0])) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Found Drive match. Remapping " + DrivePath[0] + "->"+ Map[1] + DrivePath[1]);
                return Map[1] + DrivePath[1];
            }
        }

        return Path;
    }

    public synchronized boolean jobStart() {

        ComskipJob job = ComskipJob.StartComskipJob();
        if (job==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error-null job.");
            return false;
        }

        CurrentJobs.add(job);
        return true;
    }

    // The ComskipJob calls this when completed. Check to see if another job can, and needs to be, started.
    public void jobComplete(ComskipJob Job, boolean success) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: job has completed with status " + success);
        CurrentJobs.remove(Job);

        startFirstInQueue();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No more jobs to process.");
        return;
    }

    private synchronized void startFirstInQueue() {
        if (getQueueSize(false)>0) {
            int MaxJobs = SageUtil.GetIntProperty("cd/max_jobs", 1);
            if (getNumberRunning()<MaxJobs) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Below MaxJobs threshhold.");
                jobStart();
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: MaxJobs already running " + getNumberRunning());
                return;
            }
        }
    }

    // See if we need to restart items that were queued but never processed (because Sage Stopped.)
    public boolean restart() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Restarting queued jobs.");
        startFirstInQueue();
        return true;
    }

    // Stop any jobs that are running.
    public boolean stopAll() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Stopping all queued jobs.");
        for (ComskipJob job : CurrentJobs) {
            job.stop();
        }
        return true;
    }

    public synchronized boolean clearQueue(boolean ClearFailed) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Clearing all queued jobs " + ClearFailed);

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error adding job.");
            return false;
        }

        List<QueuedJob> NewJobList = new ArrayList<QueuedJob>();

        for (QueuedJob J : Jobs) {
            if (ClearFailed) {
                if (!J.getJobHasFailed()) NewJobList.add(J);
            } else {
                if (J.getJobHasFailed()) NewJobList.add(J);
            }
        }

        return writeQueuedJobs(NewJobList);
    }

    private static String CreateFullPath(String Path, String FileName) {
        if (Path.contains("/"))
            return Path + "/" + FileName;
        else
            return Path + "\\" + FileName;
    }

    public synchronized List<QueuedJob> readQueuedJobs() {

        // Create the database file if it does not exist.
        File file = new File(QUEUE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "readQueuedJobs: Error creating new DB file.");
                return null;
            }
        }

        // Create the List to hold the elements.
        List<QueuedJob> jobs = new ArrayList<QueuedJob>();

        FileInputStream fileStream = null;
        ObjectInputStream objectStream = null;

        try {

            // Open an ObjectInputStream to deserialize the object.
            fileStream = new FileInputStream(QUEUE);

            // If there are no jobs in the DB return an empty List.
            try {
                objectStream = new ObjectInputStream(fileStream);
            } catch (EOFException eof) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "readQueuedJobs: No jobs to read. " + eof.getMessage());
                inputStreamClose(objectStream, fileStream);
                return jobs;
            }

            // Read each object and add it to the List.
            Object p = null;

            try {
                while ((p=objectStream.readObject()) != null) {
                    jobs.add((QueuedJob)p);
                }
            } catch(EOFException eof) {
                // Good, we read all of the objects.
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readQueuedJobs: complete. " + eof.getMessage());
                inputStreamClose(objectStream, fileStream);
            } catch (InvalidClassException ic) {
                // If we have an invalid class just delete the DB and return an empty List.
                Log.getInstance().write(Log.LOGLEVEL_WARN, "readQueuedJobs: Objects in DB are invalid. " + ic.getMessage());
                inputStreamClose(objectStream, fileStream);
                file.delete();
                return jobs;
            }

            // Close the stream.
            inputStreamClose(objectStream, fileStream);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readQueuedJobs: found " + jobs.size());

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readTuners: Exception in readQueuedJobs. " + e.getMessage());
            e.printStackTrace();
            inputStreamClose(objectStream, fileStream);
        }

        inputStreamClose(objectStream, fileStream);
        return jobs;
    }

    public synchronized boolean writeQueuedJobs(List<QueuedJob> Jobs) {

        // Make sure the List is not null.
        if (Jobs==null) {
            return false;
        }

        File file = new File(QUEUE);
        if (file.exists())
            file.delete();

        try {
            file.createNewFile();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "writeQueuedJobs: Error creating new DB file. " + e.getMessage());
            return false;
        }

        try {

            FileOutputStream fileStream = new FileOutputStream(QUEUE);
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);

            for (QueuedJob p : Jobs) {
                objectStream.writeObject(p);
            }

            outputStreamClose(objectStream, fileStream);

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "writeQueuedJobs: Exception. " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public synchronized boolean addToQueue(QueuedJob NewJob) {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error adding job.");
            return false;
        }

        Jobs.add(NewJob);

        return writeQueuedJobs(Jobs);
    }

    // Removes the first non-failed job from the queue.
    public synchronized QueuedJob getNextJob() {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error getting next job.");
            return null;
        }

        if (Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No more jobs.");
            return null;
        }

        boolean Found = false;
        QueuedJob Job = null;

        for (QueuedJob J : Jobs) {
            if (!J.getJobHasFailed()) {
                Found = true;
                Job = J;
            }
        }

        if (Found) {
            Jobs.remove(Job);
            if (!writeQueuedJobs(Jobs)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error writing jobs.");
                return null;
            } else {
                return Job;
            }
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No more jobs.");
            return null;
        }
    }

    private void inputStreamClose(ObjectInputStream ois, FileInputStream fis) {
        try {
            if (ois!=null) ois.close();
            if (fis!=null) fis.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "inputStreamClose: IOException " + e.getMessage());
        }
    }

    private void outputStreamClose(ObjectOutputStream ois, FileOutputStream fis) {
        try {
            if (ois!=null) ois.close();
            if (fis!=null) fis.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "outputStreamClose: IOException " + e.getMessage());
        }
    }
    
    public String[] getQueuedFileNames(boolean CountFailed) {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error getting next job.");
            return null;
        }

        List<String> GoodList = new ArrayList<String>();

        for (QueuedJob J : Jobs) {
            if (CountFailed) {
                if (J.getJobHasFailed()) GoodList.add(getFileNameForJob(J));
            } else {
                if (!J.getJobHasFailed()) GoodList.add(getFileNameForJob(J));
            }
        }

        String[] GoodArray = new String[GoodList.size()];

        for (int i=0; i<GoodList.size(); i++)
            GoodArray[i] = GoodList.get(i);

        return GoodArray;
    }

    private String getFileNameForJob(QueuedJob Job) {
        List<String> FileNames = Job.getFileName();

        if (FileNames==null || FileNames.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFileNameForJob: null or empty FileNames.");
            return "<Unknown>";
        }

        File F = new File(FileNames.get(0));
        if (F==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFileNameForJob: null File for " + FileNames.get(0));
            return "<Unknown>";
        }

        Object MediaFile = MediaFileAPI.GetMediaFileForFilePath(F);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFileNameForJob: null MediaFile for " + F);
            return "<Unknown>";
        }

        String Name = MediaFileAPI.GetMediaTitle(MediaFile);
        if (Name==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFileNameForJob: null MediaTitle.");
            return "<Unknown>";
        }

        return Name;
    }

    public boolean cleanup(Object MediaFile) {

        String VideoExt = Configuration.GetServerProperty("cd/video_ext", plugin.PROPERTY_VIDEO_FILE_EXTENSIONS);

        if (VideoExt==null || VideoExt.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No video extensions specified. Assuming mpg and ts");
            VideoExt = "mpg,ts";
        }

        String[] VideoExtensions = VideoExt.split(",");

        if (VideoExt==null || VideoExt.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Malformed video extension. Assuming mpg and ts " + VideoExt);
            VideoExtensions = new String[] {"mpg", "ts"};
        }

        String CleanupExt = Configuration.GetServerProperty("cd/cleanup_ext", "edl,txt,log,logo");

        if (CleanupExt==null || CleanupExt.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: No extensions to cleanup.");
            return true;
        }

        String[] Extensions = CleanupExt.split(",");

        if (CleanupExt==null || Extensions.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Invalid cleanup extension string " + CleanupExt);
            return false;
        }

        File Parent = MediaFileAPI.GetParentDirectory(MediaFile);
        if (Parent==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null Parent.");
            return false;
        }

        String Path = Parent.getAbsolutePath();
        if (Path==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null Path.");
            return false;
        }

        // Check if we need to remap drives to UNC paths.
        String UNCMap = Configuration.GetProperty("cd/UNC_map", "");
        if (!(UNCMap==null || UNCMap.isEmpty())) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Found UNC mappings " + UNCMap);
            Path = remapDrive(Path, UNCMap);
        }        

        File FileName = MediaFileAPI.GetFileForSegment(MediaFile,0);
        if (FileName==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null FileName.");
            return false;
        }

        String FullName = CreateFullPath(Path, FileName.getName());

        String[] NameExt = FullName.split("\\.");

        String FileNameToDelete = null;
        for (String Extension : Extensions) {

            if (NameExt.length==2) {

                // Most common case: "filename.ext"
                FileNameToDelete = NameExt[0] + "." + Extension;

            } else if (NameExt.length>=2) {

                // Name has multiple "." in it: "filename.x.y.ext"
                FileNameToDelete = NameExt[0] + ".";
                for (int i=1; i<(NameExt.length); i++) {
                    FileNameToDelete = FileNameToDelete + NameExt[i] + ".";
                }
                FileNameToDelete = FileNameToDelete + Extension;
            } else {

                // Something bad happened.
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Malformed FullName " + FullName);
                FileNameToDelete = "ERROR.ERROR";
            }

            if (hasAnyVideoFiles(FileNameToDelete, VideoExtensions)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: MediaFile still has corresponding video file.");
                return true;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Attempting to delete " + FileNameToDelete);

            File FileToDelete = new File(FileNameToDelete);
            if (FileToDelete==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null FileToDelete.");
            } else {
                if (FileToDelete.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: File deleted.");
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: File was not deleted.");
                }
            }
        }

        return true;
    }

    private boolean hasAnyVideoFiles(String FileName, String[] VideoExtensions) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager: Looking for video file for " + FileName);

        int dotPos = FileName.lastIndexOf(".");

        if (dotPos<0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Failed to find extension." + FileName);
            return false;
        }

        String BaseFileName = FileName.substring(0, dotPos);

        BaseFileName = BaseFileName + ".";

        for (String Extension : VideoExtensions) {
            File FileToTry = new File(BaseFileName+Extension);

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager: Looking for " + BaseFileName+Extension);

            if (FileToTry==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: null FileToTry.");   
            } else {
                if (FileToTry.exists()) {
                    return true;
                }
            }
        }

        // No matches found.
        return false;
    }

    public ComskipJob getJobForID(int ID) {
        for (ComskipJob job : CurrentJobs) {
            if (job.getMediaFileID() == ID) {
                return job;
            }
        }
        return null;
    }

    public void runTestCommand(String envString, String commandString) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "executeCommands: " + envString + ":" + commandString);

        if (commandString==null || commandString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "runTestCommand: null commandSrting");
            return;
        }

        String[] envArray = null;
        if (!(envString==null || envString.isEmpty())) {
            envArray = envString.split(",");
            if (envArray==null || envArray.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "executeCommands: Bad environemnt string.  Must contain at least one comma.");
                return;
            }
        }

        executeCommandLine(commandString, envArray);
        return;
    }

    public void executeCommands(String[] Command, String[]env) {

        Process process = null;
        try {process = Runtime.getRuntime().exec(Command, env); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "executeCommands: Exception " + e.getMessage());
        }

        StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        ErrorStream.start();
        OutputStream.start();

        int status = 0;
        try {status=process.waitFor(); } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "executeCommands: comskip.exe was interrupted " + e.getMessage());
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "executeCommands: Return code = " + status);
    }

    public void executeCommandLine(String CommandLine, String[] env) {
        Process process = null;
        try {process = Runtime.getRuntime().exec(CommandLine, env); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "executeCommands: Exception " + e.getMessage());
        }

        StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        ErrorStream.start();
        OutputStream.start();

        int status = 0;
        try {status=process.waitFor(); } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "executeCommands: comskip.exe was interrupted " + e.getMessage());
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "executeCommands: Return code = " + status);
    }

    public Integer[] getIDsForRunningJobs() {

        if (CurrentJobs==null || CurrentJobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getIDsForRunningJobs: No jobs running");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getIDsForRunningJobs: Found running jobs " + CurrentJobs.size());

        List<Integer> IDList = new ArrayList<Integer>();
        for (ComskipJob J : CurrentJobs) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getIDsForRunningJobs: Adding ID = " + J.getMediaFileID());
            IDList.add(J.getMediaFileID());
        }
        return (Integer[])IDList.toArray(new Integer[IDList.size()]);
    }

    public Integer[] getIDsForQueuedJobs() {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getIDsForQueuedJobs: No queued jobs.");
            return null;
        }

        List<Integer> IDList = new ArrayList<Integer>();
        for (QueuedJob J : Jobs) {
            IDList.add(J.getMediaFileID());
        }
        return (Integer[])IDList.toArray(new Integer[IDList.size()]);
    }

    public List<Object> getMediaFilesWithout(String MediaMask) {

        // Create the return Array.
        List<Object> MediaFilesWithout = new ArrayList<Object>();

        // Get MediaFiles according to the mask.
        Object[] MediaFiles = MediaFileAPI.GetMediaFiles(MediaMask);
        if (MediaFiles==null || MediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getMediaFilesWithout: No MediaFiles.");
            return MediaFilesWithout;
        }

        // Loop through each MediaFile.
        for (Object MediaFile : MediaFiles) {

            // See how many segments (files) are in the MediaFile.
            File[] Files = MediaFileAPI.GetSegmentFiles(MediaFile);
            if (Files==null || Files.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "getMediaFilesWithout: No Files.");
            } else {

                // If any of the files do not have an .edl or .txt consider it unprocessed.
                for (File F : Files) {
                    String FileName = F.getAbsolutePath();
                    if (F==null) {
                        Log.getInstance().write(Log.LOGLEVEL_WARN, "getMediaFilesWithout: No Files.");
                    } else {

                        // If this file does not have an .edl or .txt AND it's not already in the list, add it.
                        if (!hasAnEdlOrTxtFile(FileName)) {
                            if (!MediaFilesWithout.contains(MediaFile)) {
                                MediaFilesWithout.add(MediaFile);
                            }
                        }
                    }
                }
            }
        }

        return MediaFilesWithout;
    }

    public boolean hasAnEdlOrTxtFile(String FileName) {
        String BaseName = FileName.substring(0, FileName.lastIndexOf("."));

        String[] Extensions = {".edl",".txt"};

        for (String Ext : Extensions) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "hasAnEdlOrTxtFile: Looking for existing file " + BaseName+Ext);
            File F = new File(BaseName+Ext);

            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "hasAnEdlOrTxtFile: null File " + BaseName+Ext);
            } else {
                if (F.exists()) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "hasAnEdlOrTxtFile: File exists " + F.getAbsolutePath());
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isMediaFileRunning(Object MediaFile) {

        if (CurrentJobs==null || CurrentJobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isMediaFileRunning: No jobs running");
            return false;
        }

        int ID = MediaFileAPI.GetMediaFileID(MediaFile);

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isMediaFileRunning: Found running jobs " + CurrentJobs.size());

        for (ComskipJob J : CurrentJobs) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getIDsForRunningJobs: Adding ID = " + J.getMediaFileID());
            if (ID == J.getMediaFileID()) {
                return true;
            }
        }

        return false;
    }

    /*
    public void loadMediaFileIDWithoutEDL(String Mask) {

        MediaFileIDsWithoutEDL = new ArrayList<Integer>();

        List<Object> MediaFiles = getMediaFilesWithout(Mask);

        if (MediaFiles==null || MediaFiles.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadMediaFileIDWithoutEDL: None found.");
            return;
        }

        for (Object MediaFile : MediaFiles) {
            MediaFileIDsWithoutEDL.add(MediaFileAPI.GetMediaFileID(MediaFile));
        }

        return;
    }

    public boolean hasEdlOrTxt(Object MediaFile) {
        if (MediaFile==null) {
            return false;
        }

        Integer ID = MediaFileAPI.GetMediaFileID(MediaFile);

        return MediaFileIDsWithoutEDL.contains(ID);
    }

    public boolean updateMediaFileIDsWithoutEDL(Integer ID) {

        Object MediaFile = MediaFileAPI.GetMediaFileForID(ID);

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "updateMediaFileIDsWithoutEDL: null MediaFile.");
            return false;
        }

        if (hasEdlOrTxt(MediaFile) && MediaFileIDsWithoutEDL.contains(ID)) {
            MediaFileIDsWithoutEDL.remove(ID);
            return true;
        }

        return false;
    }
     */
}
