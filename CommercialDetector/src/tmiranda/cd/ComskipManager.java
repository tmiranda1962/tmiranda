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

    private static final String QUEUE = RESOURCES + File.separator + "CommercialDetector.DB";
    public static final String COMSKIP_INI_DIR = RESOURCES + File.separator + COMSKIP;

    private TuneIntelligentScheduling     ISTuner = null;

    private ComskipManager(){};

    public static ComskipManager getInstance() {
        return instance;
    }

    public void destroy() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.destroy: Stopping all jobs.");
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.makeResources: null Directory, fatal error.");
            return;
        }

        if (Directory.exists() && Directory.isDirectory()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.makeResources: Directory exists.");
            return;
        }

        if (!Directory.mkdir()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.makeResources: Directory could not be created, fatal error");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.makeResources: Directory created.");
    }

    public int getNumberRunning() {
        return CurrentJobs.size();
    }

    public List<ComskipJob> getRunningJobs() {
        return CurrentJobs;
    }

    // Return total size of queue EXCLUDING NumberRunning.
    public synchronized int getQueueSize(boolean Failed) {
        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty())
            return 0;
        else
            return Jobs.size();
    }

    public boolean inRestrictedTime() {
        String S = Configuration.GetServerProperty(plugin.PROPERTY_RESTRICTED_TIMES, plugin.PROPERTY_DEFAULT_RESTRICTED_TIMES);

        if (S==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.inRestrictedTime: No Restricted times.");
            return false;
        }

        String[] Hours = S.split(",");

        if (Hours==null || Hours.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.inRestrictedTime: Malformed restricted_times " + S);
            return false;
        }

        // Get the current time and format it so we only get the hour of the day.
        Date Now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("H");

        String CurrentHour = formatter.format(Now);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.inRestrictedTime: Current hour is " + CurrentHour);

        for (String Hour : Hours) {
            String H = Hour.substring(0,2);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.inRestrictedTime: Restricted hour is " + H);

            if (CurrentHour.equals(H)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.inRestrictedTime: Current hour is restricted.");
                return true;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.inRestrictedTime: Current hour is not restricted.");
        return false;
    }

    private String remapDrive(String Path, String UNCMap) {

        // Split the string into Drive-Path pairs;
        String[] Pairs = UNCMap.split(",");
        if (Pairs==null || Pairs.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.remapDrive: Invalid mappings.");
            return Path;
        }

        String[] DrivePath = Path.split(":");
        if (DrivePath==null || DrivePath.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.remapDrive: Invalid Path.");
            return Path;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.remapDrive: Drive = " + DrivePath[0]);

        for (String Pair : Pairs) {

            // Split the string into a Drive and a UNC Path.
            String[] Map = Pair.split("-");
            if (Map==null || Map.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.remapDrive: No mappings.");
                return Path;
            }

            // See if the drive matches the drive in Path.
            if (Map[0].equalsIgnoreCase(DrivePath[0])) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.remapDrive: Found Drive match. Remapping " + DrivePath[0] + "->"+ Map[1] + DrivePath[1]);
                return Map[1] + DrivePath[1];
            }
        }

        return Path;
    }

    public synchronized boolean jobStart() {

        ComskipJob job = ComskipJob.StartComskipJob();
        if (job==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.jobStart: Error-null job.");
            return false;
        }

        CurrentJobs.add(job);

        return true;
    }

    // The ComskipJob calls this when completed. Check to see if another job can, and needs to be, started.
    public synchronized void jobComplete(ComskipJob Job, boolean success) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.jobComplete: Job has completed with status " + success);
        CurrentJobs.remove(Job);

        if (!startFirstInQueue()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.jobComplete: Did not start next job.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.jobComplete: Successfully started next job.");
        }

        return;
    }

    /**
     * Starts the first queued job if:
     * - There is a job in the queue.
     * - We are not in a restricted time.
     * - Less than MaxJobs jobs are already running.
     * - There is enough time to process the job.
     *
     * @return true if a job was started, false if not.
     */
    private synchronized boolean startFirstInQueue() {

        // Check if there are any jobs in the queue.
        if (getQueueSize(false)==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: No jobs queued.");
            return false;
        }

        if (ComskipManager.getInstance().inRestrictedTime()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Time is restricted.");
            return false;
        }

        // Check if we are already at MaxJobs.
        int MaxJobs = SageUtil.GetIntProperty("cd/max_jobs", 1);

        if (getNumberRunning()>=MaxJobs) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Above MaxJobs threshhold.");
            return false;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Below MaxJobs threshhold.");

        // Look through the queue to see if any can be started in the time we have available. It is
        // possible that the Queue will be modified while this method is being run so make sure nothing
        // precludes that condition. It is possible that a job is added to the end of the queue while this
        // method is running that can be run, but won't because it is never checked.

        int QueueSize = getQueueSize(false);

        for (Integer i=0; i<QueueSize; i++) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Checking job " + i.toString());

            // Check if there is enough time to complete the next job.
            QueuedJob Job = peekNextJob();

            // Job can be null due to an error, or the Queue shrunk while this method is running, or
            // we removed a job because the MediaFile was null (next check.)
            if (Job==null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: null QueuedJob.");
                continue;
            }

            Object MediaFile = Job.getMediaFile();

            // The MediaFile can be null for a variety of reasons.  Most likely it's because Sage deleted the
            // recording after it was queued to get processed.  Handle this situation by removing the job from
            // the queue and processing the next file.
            if (MediaFile==null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: null MediaFile. Removing job from the queue.");
                getNextJob();
                QueueSize -= 1;
                continue;
            }

            if (isEnoughTime(MediaFile)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Enough time to process job " + i.toString());
                jobStart();
                return true;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Not enough time to process this job. Cycling Queue.");
            
            if (!cycleQueue()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.startFirstInQueue: cycleQueue failed.");
            }
        }

        // Could not find any jobs to process.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startFirstInQueue: Not enough time to process any jobs.");

        if (QueueSize != getQueueSize(false)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.startFirstInQueue: Queue size changed while method was running.");
        }
        
        return false;
    }

    /**
     * Starts as many jobs as possible. Checks for jobs in queue, below max jobs threshhold, restricted time and enough time.
     *
     * @return true if success, false otherwise.
     */
    public synchronized boolean startMaxJobs() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startMaxJobs: Restarting queued jobs.");
        while (startFirstInQueue()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.startMaxJobs: Started Job.");
            SystemStatus.getInstance().printSystemStatus();
        }
        return true;
    }

    // Stop any jobs that are running.
    public boolean stopAll() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.stopAll: Stopping all queued jobs.");

        // Get a copy of the CurrentJobs to avoid a concurrent modification error that would be caused when
        // job.stop() is invoked.
        List<ComskipJob> Jobs = new ArrayList<ComskipJob>();
        Jobs.addAll(CurrentJobs);

        for (ComskipJob job : Jobs) {
            job.stop();
        }
        return true;
    }

    public synchronized boolean clearQueue() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.clearQueue: Clearing all queued jobs.");

        List<QueuedJob> NewJobList = new ArrayList<QueuedJob>();

        return writeQueuedJobs(NewJobList);
    }

    private static String CreateFullPath(String Path, String FileName) {
        if (Path.contains("/"))
            return Path + "/" + FileName;
        else
            return Path + "\\" + FileName;
    }

    public synchronized List<QueuedJob> readQueuedJobs() {

        // Create the List to hold the elements.
        List<QueuedJob> jobs = new ArrayList<QueuedJob>();

        // Create the database file if it does not exist.
        File file = new File(QUEUE);
        if (!file.exists()) {
            try {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.readQueuedJobs: DB file does not exist, creating it.");
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.readQueuedJobs: Error creating new DB file.");
                return jobs;
            }
        }

        FileInputStream fileStream = null;
        ObjectInputStream objectStream = null;

        try {

            // Open an ObjectInputStream to deserialize the object.
            fileStream = new FileInputStream(QUEUE);

            // If there are no jobs in the DB return an empty List.
            try {
                objectStream = new ObjectInputStream(fileStream);
            } catch (EOFException eof) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "ComskipManager.readQueuedJobs: No jobs to read. " + eof.getMessage());
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
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.readQueuedJobs: complete. " + eof.getMessage());
                inputStreamClose(objectStream, fileStream);
            } catch (InvalidClassException ic) {
                // If we have an invalid class just delete the DB and return an empty List.
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.readQueuedJobs: Objects in DB are invalid. " + ic.getMessage());
                inputStreamClose(objectStream, fileStream);
                file.delete();
                return jobs;
            }

            // Close the stream.
            inputStreamClose(objectStream, fileStream);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.readQueuedJobs: found " + jobs.size());

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.readQueuedJobs: Exception. " + e.getMessage());
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.writeQueuedJobs: Error creating new DB file. " + e.getMessage());
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.writeQueuedJobs: Exception. " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public synchronized boolean addToQueue(QueuedJob NewJob) {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.addToQueue: Error adding job.");
            return false;
        }

        Jobs.add(NewJob);

        return writeQueuedJobs(Jobs);
    }

    // Removes the first non-failed job from the queue.
    public synchronized QueuedJob getNextJob() {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getNextJob: Error getting next job.");
            return null;
        }

        if (Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getNextJob: No more jobs.");
            return null;
        }

        QueuedJob Job = Jobs.remove(0);

        if (!writeQueuedJobs(Jobs)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getNextJob: Error writing jobs.");
        }

        return Job;
    }

    // Gets the first non-failed job from the queue.  Doe not remove it.
    public synchronized QueuedJob peekNextJob() {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.peekNextJob: Error getting next job.");
            return null;
        }

        if (Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.peekNextJob: No more jobs.");
            return null;
        }

        return Jobs.get(0);
    }

    /**
     * Moves a ComskipJob to the front of the queue.
     *
     * @param ID The MediaFile ID of the job to move.
     * @return true if success, false otherwise.
     */
    public synchronized boolean moveToFrontOfQueue(int ID) {
        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.moveToFrontOfQueue: Error reading queue.");
            return false;
        }

        if (Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.moveToFrontOfQueue: No jobs.");
            return false;
        }

        // Placeholder.
        QueuedJob FoundJob = null;

        // Scan List for the correct Job.
        for (QueuedJob Job : Jobs) {
            if (Job.getMediaFileID() == ID) {
                if (FoundJob==null) {
                    FoundJob = Job;
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.moveToFrontOfQueue: Found duplicate.");
                }
            }
        }

        if (FoundJob==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.moveToFrontOfQueue: Did not find ID " + ID);
            return false;
        }
        
        // Remove if from where it was, add it to the end, then reverse the list.
        Jobs.remove(FoundJob);
        Jobs.add(FoundJob);
        Collections.reverse(Jobs);

        return writeQueuedJobs(Jobs);
    }

    /**
     * Cycles through the job queue by moving the last item to the front.
     *
     * @return true if success, false otherwise
     */
    private synchronized boolean cycleQueue() {
        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.cycleQueue: Error reading queue.");
            return false;
        }

        if (Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.cycleQueue: No jobs.");
            return false;
        }
        
        if (Jobs.size()==1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.cycleQueue: Only one job.");
            return false;
        }        
        
        QueuedJob SecondJob = Jobs.get(1);
        int ID = SecondJob.getMediaFileID();
        return moveToFrontOfQueue(ID);
    }

    private void inputStreamClose(ObjectInputStream ois, FileInputStream fis) {
        try {
            if (ois!=null) ois.close();
            if (fis!=null) fis.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.inputStreamClose: IOException " + e.getMessage());
        }
    }

    private void outputStreamClose(ObjectOutputStream ois, FileOutputStream fis) {
        try {
            if (ois!=null) ois.close();
            if (fis!=null) fis.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.outputStreamClose: IOException " + e.getMessage());
        }
    }
    
    public String[] getQueuedFileNames(boolean CountFailed) {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty()) {
            //Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager: Error getting next job.");
            return null;
        }

        List<String> GoodList = new ArrayList<String>();

        for (QueuedJob J : Jobs) {
            GoodList.add(getFileNameForJob(J));
        }

        String[] GoodArray = new String[GoodList.size()];

        for (int i=0; i<GoodList.size(); i++)
            GoodArray[i] = GoodList.get(i);

        return GoodArray;
    }

    private String getFileNameForJob(QueuedJob Job) {
        List<String> FileNames = Job.getFileName();

        if (FileNames==null || FileNames.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getFileNameForJob: null or empty FileNames.");
            return "<Unknown>";
        }

        File F = new File(FileNames.get(0));
        if (F==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getFileNameForJob: null File for " + FileNames.get(0));
            return "<Unknown>";
        }

        Object MediaFile = MediaFileAPI.GetMediaFileForFilePath(F);
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getFileNameForJob: null MediaFile for " + F);
            return "<Unknown>";
        }

        String Name = MediaFileAPI.GetMediaTitle(MediaFile);
        if (Name==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getFileNameForJob: null MediaTitle.");
            return "<Unknown>";
        }

        return Name;
    }

    private boolean hasAnyVideoFiles(String FileName, String[] VideoExtensions, String MediaFileExt) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.hasAnyVideoFiles: Looking for video file for " + FileName + ":" + MediaFileExt);

        int dotPos = FileName.lastIndexOf(".");

        if (dotPos<0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.hasAnyVideoFiles: Failed to find extension." + FileName);
            return false;
        }

        String BaseFileName = FileName.substring(0, dotPos);

        BaseFileName = BaseFileName + ".";

        for (String Extension : VideoExtensions) {

            if (Extension.equalsIgnoreCase(MediaFileExt)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.hasAnyVideoFiles: Skipping deleted file extension " + MediaFileExt);
            } else {
                File FileToTry = new File(BaseFileName+Extension);

                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.hasAnyVideoFiles: Looking for " + BaseFileName+Extension);

                if (FileToTry==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.hasAnyVideoFiles: null FileToTry.");
                } else {
                    if (FileToTry.exists()) {
                        return true;
                    }
                }
            }
        }

        // No matches found.
        return false;
    }

    public synchronized ComskipJob getJobForID(int ID) {
        for (ComskipJob job : CurrentJobs) {
            if (job.getMediaFileID() == ID) {
                return job;
            }
        }
        return null;
    }

    public void runTestCommand(String envString, String commandString) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.runTestCommand: " + envString + ":" + commandString);

        if (commandString==null || commandString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.runTestCommand: null commandSrting");
            return;
        }

        String[] envArray = null;
        if (!(envString==null || envString.isEmpty())) {
            envArray = envString.split(",");
            if (envArray==null || envArray.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.runTestCommand: Bad environemnt string.  Must contain at least one comma.");
                return;
            }
        }

        executeCommandLine(commandString, envArray);
        return;
    }

    public void executeCommands(String[] Command, String[]env) {

        Process process = null;
        try {process = Runtime.getRuntime().exec(Command, env); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.executeCommands: Exception " + e.getMessage());
        }

        StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        ErrorStream.start();
        OutputStream.start();

        int status = 0;
        try {status=process.waitFor(); } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.executeCommands: comskip.exe was interrupted " + e.getMessage());
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.executeCommands: Return code = " + status);
    }

    public void executeCommandLine(String CommandLine, String[] env) {
        Process process = null;
        try {process = Runtime.getRuntime().exec(CommandLine, env); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.executeCommandLine: Exception " + e.getMessage());
        }

        StreamGetter ErrorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter OutputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        ErrorStream.start();
        OutputStream.start();

        int status = 0;
        try {status=process.waitFor(); } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.executeCommandLine: comskip.exe was interrupted " + e.getMessage());
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.executeCommandLine: Return code = " + status);
    }

    public Integer[] getIDsForRunningJobs() {

        if (CurrentJobs==null || CurrentJobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getIDsForRunningJobs: No jobs running");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getIDsForRunningJobs: Found running jobs " + CurrentJobs.size());

        List<Integer> IDList = new ArrayList<Integer>();
        for (ComskipJob J : CurrentJobs) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.getIDsForRunningJobs: Adding ID = " + J.getMediaFileID());
            IDList.add(J.getMediaFileID());
        }
        return (Integer[])IDList.toArray(new Integer[IDList.size()]);
    }

    public Integer[] getIDsForQueuedJobs() {

        List<QueuedJob> Jobs = readQueuedJobs();
        if (Jobs==null || Jobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getIDsForQueuedJobs: No queued jobs.");
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
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getMediaFilesWithout: No MediaFiles.");
            return MediaFilesWithout;
        }

        // Loop through each MediaFile.
        for (Object MediaFile : MediaFiles) {

            // See how many segments (files) are in the MediaFile.
            File[] Files = MediaFileAPI.GetSegmentFiles(MediaFile);
            if (Files==null || Files.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getMediaFilesWithout: No Files.");
            } else {

                // If any of the files do not have an .edl or .txt consider it unprocessed.
                for (File F : Files) {
                    String FileName = F.getAbsolutePath();
                    if (F==null) {
                        Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getMediaFilesWithout: No Files.");
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

    public int countAllOrphans() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.countAllOrphans: Counting all orphaned files.");

        List<File> OrphanedFiles = getOrphanedFiles();

        if (OrphanedFiles == null || OrphanedFiles.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.countAllOrphans: Nothing to count.");
            return 0;
        } else {
            return OrphanedFiles.size();
        }
    }

    public void deleteAllOrphans() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.deleteAllOrphans: Deleting all orphaned files.");

        List<File> OrphanedFiles = getOrphanedFiles();

        if (OrphanedFiles == null || OrphanedFiles.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.deleteAllOrphans: Nothing to delete.");
            return;
        }

        for (File ThisFile : OrphanedFiles) {
            if (ThisFile.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.deleteAllOrphans: Deleted " + ThisFile.getAbsolutePath());
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.deleteAllOrphans: Failed to deleted " + ThisFile.getAbsolutePath());
            }
        }
    }
    
    private List<File> getOrphanedFiles() {

        // Create the return Array.
        List<File> OrphanedFiles = new ArrayList<File>();

        // Get all of the Sage MediaFiles.
        Object[] MediaFiles = MediaFileAPI.GetMediaFiles("VM");
        if (MediaFiles==null || MediaFiles.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getOrphanedFiles: No MediaFiles.");
            return OrphanedFiles;
        }

        List<Object> MediaFileList = Arrays.asList(MediaFiles);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getOrphanedFiles: Found MediaFiles " + MediaFileList.size());

        // Get all of the extenstions that we can cleanup.
        String S = Configuration.GetServerProperty(plugin.PROPERTY_CLEANUP_EXTENSIONS, plugin.PROPERTY_DEFAULT_CLEANUP_EXTENSIONS);

        if (S==null || S.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getOrphanedFiles: No cleanup_ext.");
            return OrphanedFiles;
        }

        String[] CleanupExtensions = S.split(",");

        if (CleanupExtensions==null || CleanupExtensions.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getOrphanedFiles: Nothing to clean up " + S);
            return OrphanedFiles;
        }

        // Convert to a List.
        List<String> ExtensionList = Arrays.asList(CleanupExtensions);

        // Get every file in every Sage recording directory.
        List<File> AllFiles = getAllFilesInAllRecordingDirectories();

        if (AllFiles==null || AllFiles.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getOrphanedFiles: No Files in any Sage recording directory.");
            return OrphanedFiles;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getOrphanedFiles: Total files in recording directories " + AllFiles.size());

        // Loop through all known files to see if there is a corresponding video file.
        for (File ThisFile : AllFiles) {

            if (isCleanable(ThisFile, ExtensionList) && !hasMediaFile(ThisFile, MediaFileList)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getOrphanedFiles: Found orphan " + ThisFile.getAbsolutePath());
                OrphanedFiles.add(ThisFile);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getOrphanedFiles: Total orphans " + OrphanedFiles.size());

        return OrphanedFiles;
    }

    private List<File> getAllFilesInAllRecordingDirectories() {
        List<File> AllFiles = new ArrayList<File>();

        File[] AllDirectories = Configuration.GetVideoDirectories();

        if (AllDirectories==null || AllDirectories.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getAllFilesInAllDirectories: No recording directories configured.");
            return AllFiles;
        }

        for (File ThisDirectory : AllDirectories) {
            if (!ThisDirectory.isDirectory()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getAllFilesInAllDirectories: Found non-directory " + ThisDirectory.toString());
                continue;
            }

            // Get the path name for this directory.
            String Path = ThisDirectory.getPath();

            // Get all of the file names in this directory.
            String[] FilesInDirectory = ThisDirectory.list();

            if (FilesInDirectory==null || FilesInDirectory.length==0) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getAllFilesInAllDirectories: No files in directory " + ThisDirectory.toString());
                continue;
            }

            for (String FileName : FilesInDirectory) {
                if (FileName==null || FileName.isEmpty()) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getAllFilesInAllDirectories: null FileName " + FileName);
                    continue;
                }

                File NewFile = new File(Path + File.separator + FileName);

                if (NewFile==null) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getAllFilesInAllDirectories: null NewFile.");
                    continue;
                }

                AllFiles.add(NewFile);
            }
        }

        return AllFiles;
    }

    /*
     * Returns true if any segment of any MediaFile matches.
     */
    private boolean hasMediaFile(File ThisFile, List<Object> MediaFiles) {
        
        String ThisFileName = ThisFile.getName();

        int dotPos = ThisFileName.lastIndexOf(".");
        
        if (dotPos<0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.hasMediaFile: Failed to find extension." + ThisFileName);
            return true;
        }
        
        String ThisFileBaseName = ThisFileName.substring(0, dotPos).toLowerCase();

        // Look through the MediaFiles to see if one of them matches ThisFile.
        for (Object MediaFile : MediaFiles) {

            int Segments = MediaFileAPI.GetNumberOfSegments(MediaFile);

            for (int Segment=0; Segment<Segments; Segment++) {
                
                File MF = MediaFileAPI.GetFileForSegment(MediaFile, Segment);

                if (MF==null) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.hasMediaFile: null File for " + MediaFileAPI.GetMediaTitle(MediaFile));
                    continue;
                }

                // Get a String representation of the FileName.
                String FileName = MF.getName();
  
                dotPos = FileName.lastIndexOf(".");

                if (dotPos<0) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.hasMediaFile: Failed to find extension." + FileName);
                    continue;
                }

                String BaseFileName = FileName.substring(0, dotPos).toLowerCase();
 
                if (ThisFileBaseName.equals(BaseFileName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * Return true if ThisFile ends with a valid CleanupExtension, false otherwise.
     */
    private boolean isCleanable(File ThisFile, List<String> CleanupExtensions) {
        String FileName = ThisFile.toString().toLowerCase();

        for (String Ext : CleanupExtensions) {
            if (FileName.endsWith(Ext.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    public boolean hasAnEdlOrTxtFile(String FileName) {
        String BaseName = FileName.substring(0, FileName.lastIndexOf("."));

        String[] Extensions = {".edl",".txt"};

        for (String Ext : Extensions) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.hasAnEdlOrTxtFile: Looking for existing file " + BaseName+Ext);
            File F = new File(BaseName+Ext);

            if (F==null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.hasAnEdlOrTxtFile: null File " + BaseName+Ext);
            } else {
                if (F.exists()) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.hasAnEdlOrTxtFile: File exists " + F.getAbsolutePath());
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * Methods for various Intelligent Scheduling operations.
     */
    public synchronized boolean isMediaFileRunning(Object MediaFile) {

        if (CurrentJobs==null || CurrentJobs.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.isMediaFileRunning: No jobs running");
            return false;
        }

        int ID = MediaFileAPI.GetMediaFileID(MediaFile);

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.isMediaFileRunning: Found running jobs " + CurrentJobs.size());

        for (ComskipJob J : CurrentJobs) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ComskipManager.getIDsForRunningJobs: Adding ID = " + J.getMediaFileID());
            if (ID == J.getMediaFileID()) {
                return true;
            }
        }

        return false;
    }

    // For now this will assume that if ANY jobs are running or anything is recording we can't start another.
    public boolean isEnoughTime(Object MediaFile) {

        if (!SageUtil.GetBoolProperty(plugin.PROPERTY_USE_INTELLIGENT_SCHEDULING, plugin.PROPERTY_USE_INTELLIGENT_SCHEDULING)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Intelligent scheduling is disabled.");
            return true;
        }

        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.isEnoughTime: null MediaFile.");
            return true;
        }

        // If anything is recording we can't start a job.
        Object[] Recording = Global.GetCurrentlyRecordingMediaFiles();
        if (Recording!=null && Recording.length > 0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Recording is in progress.");
            return false;
        }

        // Get the total length of the MediaFile.
        long MediaFileDuration = MediaFileAPI.GetFileDuration(MediaFile);
        if (MediaFileDuration==0 || MediaFileDuration<0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.isEnoughTime: No duration.");
            return true;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Duration for MediaFile is " + MediaFileDuration + ":" + MediaFileDuration/1000/60 + " minutes.");

        // Get the time ratio for the channel.
        float Ratio = getRatioForMediaFile(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Ratio for this MediaFile is " + Ratio);

        // See how many jobs are already running.
        int JobsRunning = ComskipManager.getInstance().getNumberRunning();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Jobs currently running " + JobsRunning);

        // Adjust the Ratio given the number of jobs that are running.
        if (JobsRunning>0) {
            float RatioIncreaser = 1.0F + (1.0F - ComskipJob.PROPERTY_DEFAULT_RUNNING_IMPACT_FLOAT);
            Ratio = Ratio * RatioIncreaser;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Increasing Ratio " + RatioIncreaser + ":" + Ratio);
        }

        // Estimate how long it will take to process the file.
        float timeToProcessFileFloat = MediaFileDuration * Ratio;
        long timeToProcessFile = (long)timeToProcessFileFloat;

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Time to process estimate is " + timeToProcessFile + ":" + timeToProcessFile/1000/60 + " minutes.");

        // Get all of the recordings that will happen between now and the time we can expect the job to finish.
        long Now = Utility.Time();

        // See what's scheduled to record between now and when we expect the job to complete.  Pad the
        // start time by a few seconds just in case something is just about to end.
        Object[] Scheduled = Global.GetScheduledRecordingsForTime(Now+5000L, Now+timeToProcessFile);
        
        // If there is nothing scheduled we have the time to run this job.
        if (Scheduled==null || Scheduled.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Nothing scheduled to record.");
            return true;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Not enough time " + Scheduled.length);

        for (Object Airing : Scheduled) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.isEnoughTime: Scheduled show " + ShowAPI.GetShowTitle(Airing) + " : " + ShowAPI.GetShowEpisode(Airing));
        }

        return false;
    }

    // Ratio is the amount of time it takes to process a file vs the file length.  So if a 60 minute
    // recording takes 15 minutes to process, the Ration will be 0.25.
    private float getRatioForMediaFile(Object MediaFile) {
        String Channel = AiringAPI.GetAiringChannelName(MediaFile);

        if (Channel==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ComskipManager.getRatioForMediaFile: null Channel.");
            return plugin.RATIO_DEFAULT;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getRatioForMediaFile: Channel is " + Channel);

        Float Ratio = plugin.ChannelTimeRatios.get(Channel);

        if (Ratio==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ComskipManager.getRatioForMediaFile: No ratio found " + Ratio);
            Ratio = plugin.RATIO_DEFAULT;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ComskipManager.getRatioForMediaFile: Ratio is " + Ratio);

        return Ratio;
    }

    

    /*
     * Methods to take care of various Intelligent Scheduling Tuner functions.
     */
    public TuneIntelligentScheduling getIntelligentTuner() {
        return ISTuner;
    }

    public void createIntelligentSchedulingTuner() {
        ISTuner = new TuneIntelligentScheduling();
    }

    public boolean initIntelligentSchedulingTuner() {
        if (ISTuner==null) {
            return false;
        } else {
            ISTuner.startInitialize();
            return true;
        }
    }

}
