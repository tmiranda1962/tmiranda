/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.util.*;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class SystemStatus {

    /**
     * Constructor.
     */
    private SystemStatus() {}

    private static SystemStatus instance = new SystemStatus();

    public static SystemStatus getInstance() {
        return instance;
    }
    
    /**
     * Prints to the Sage logfile the complete system status.
     */
    public void printSystemStatus() {
        printNumberRunning();
        printJobsRunning();
        printJobQueue();
    }

    /**
     * Prints to the Sage logfile details about the jobs that are queued.
     */
    public void printJobQueue() {
        List<QueuedJob> Jobs = ComskipManager.getInstance().readQueuedJobs();

        if (Jobs==null || Jobs.isEmpty()) {
            System.out.println("CD: SystemStatus: No jobs in queue.");
            return;
        }

        System.out.println("CD: SystemStatus: Jobs in queue = " + Jobs.size());

        for (QueuedJob Job : Jobs) {

            Object MediaFile = Job.getMediaFile();

            if (MediaFile==null) {
                System.out.println("CD: SystemStatus:   null MediaFile.");
            } else {
                String Title = ShowAPI.GetShowTitle(MediaFile);
                String Episode = ShowAPI.GetShowEpisode(MediaFile);
                System.out.println("CD: SystemStatus:   Title : Episode = " + Title + " : " + Episode);
            }

            // Print all the file names.
            List<String> FileNames = Job.getFileName();

            if (FileNames==null || FileNames.isEmpty()) {
                System.out.println("CD: SystemStatus:   null FileNames.");
            } else {
                for (String Name : FileNames) {
                    System.out.println("CD: SystemStatus:   FileName = " + Name);
                }
            }

            //System.out.println("CD: SystemStatus:   JobHasFailed = " + Job.getJobHasFailed());
        }
    }

    /**
     * Prints to the Sage logfile the number of jobs currently running.
     */
    public void printNumberRunning() {
        Integer N = ComskipManager.getInstance().getNumberRunning();
        System.out.println("CD: SystemStatus: Number running = " + N.toString());
    }

    /**
     * Prints to the Sage logfile details about the jobs that are currently running.
     */
    public void printJobsRunning() {
        List<ComskipJob> RunningJobs = ComskipManager.getInstance().getRunningJobs();

        if (RunningJobs==null || RunningJobs.isEmpty()) {
            System.out.println("CD: SystemStatus: No jobs running.");
            return;
        }

        System.out.println("CD: SystemStatus: Jobs running = " + RunningJobs.size());

        for (ComskipJob Job : RunningJobs) {
            Integer MediaFileID = Job.getMediaFileID();

            System.out.println("CD: SystemStatus:   MediaFileID = " + MediaFileID.toString());

            Object MediaFile = MediaFileAPI.GetMediaFileForID(MediaFileID);

            if (MediaFile==null) {
                System.out.println("CD: SystemStatus:   null MediaFile.");
                continue;
            }

            String Title = ShowAPI.GetShowTitle(MediaFile);
            String Episode = ShowAPI.GetShowEpisode(MediaFile);
            System.out.println("CD: SystemStatus:   Title : Episode = " + Title + " : " + Episode);
        }
    }

    /**
     * Prints to the Sage logfile the time ratio property string.
     */
    public void printTimeRatios() {
        String S = Configuration.GetServerProperty(plugin.PROPERTY_TIME_RATIOS, plugin.PROPERTY_DEFAULT_TIME_RATIOS);
        System.out.println("CD: SystemStatus: Time Ratios = " + S);
    }

    public void printNumberOfOrphans() {
        Integer N = ComskipManager.getInstance().countAllOrphans();
        System.out.println("CD: SystemStatus: Number of orphans = " + N.toString());
    }

}
