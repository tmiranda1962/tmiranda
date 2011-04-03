/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 *
 * <p>
 * This class represents a java thread that will scan the temp directory looking for old files whose
 * name begins with a specific string.  It is intended to be used to cleanup old files left behind by
 * Sage's Online Services as well as the Malore Online Browser.
 *
 */
public class CleanupThread extends TimerTask {

    public static final String  DOWNLOAD_FILE_PREFIX_PR = "MaloreOnlineVideo";
    public static final String  DOWNLOAD_FILE_PREFIX_SAGE = "onlinevideo";

    /**
     * Creates a new CleanupThread.  No parameters required.
     */
    public CleanupThread() {
        setLastRunTime(0L);
    }

    /**
     * Used to keep track of the last time the CleanupThread was run.
     */
    private Long lastruntime = 0L;

    /**
     * Sets the lastruntime.
     * <p>
     * @param t The time, in ms since 1/1/1970, that the CleaupThread was last run.
     */
    private void setLastRunTime(Long t) {
        lastruntime = t;
    }

    /**
     * Get the time that the Cleanup Thread was last run.
     * <p>
     * @return The time, in ms since 1/1/1970, that the CleanupThread was last run.
     */
    public Long getLastRunTime() {
        return lastruntime;
    }

    /**
     * Method to count the number of files that will be deleted if the deleteFilesStartingWith method is
     * used with the same parameters.
     * <p>
     * @param files An array of Files to process.
     * @param startingwith A String that is used to identify which files to count.  Files starting with this string
     * and are older than the minumum age will be counted.
     * @param minage The minimum age (in ms) of a file before it would be deleted.  Files less than minage will
     * not be counted.
     * @return The number of files on the array starting with "startingwith" and older than "minage".
     */
    public int countFilesToDelete(File[] files, String startingwith, Long minage) {
        return deleteFilesStartingWith(files, startingwith, minage, true);
    }

    /**
     * Delete files starting with a specific string and older than a minimum age.
     * <p>
     * @param files An array of Files to process.
     * @param startingwith A string used to identify the files that will be inspected.  Files that do not
     * start with this string will be ignored.
     * @param minage The minimum age (in ms) of a file before it is deleted.  Files newer than minage will
     * be ignored.
     * @param justcount If set to true no files will be deleted but the number of files that would have been
     * deleted is returned. If set to false all Files in the array that start with "startingwith" and are
     * older than "minage" will be deleted.
     * @return The number of files deleted or, if justcount is set to true the number of files that
     * would have been deleted.
     */
    private int deleteFilesStartingWith(File[] files, String startingwith, Long minage, boolean justcount) {

        int count = 0;
        Long time = Utility.Time();

        for (File file : files) {
            String name = file.getName().toLowerCase();

            if (!file.isDirectory() && name.startsWith(startingwith)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.deleteFilesStartingWith: Found potential file " + name);
                Long LastModified = file.lastModified();

                if (LastModified + minage < time) {
                    if (justcount) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.deleteFilesStartingWith: Counting file " + name);
                        count++;
                    } else {
                        if(!file.delete()) {
                            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CleanupThread.deleteFilesStartingWith: Could not delete locked file " + name);
                        } else {
                            Log.getInstance().write(Log.LOGLEVEL_WARN, "CleanupThread.deleteFilesStartingWith: Deleted old file " + name);
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * Method used to start the Cleanup Thread.
     */
    @Override
    public void run() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Starting Cleanup thread.");

        Thread.currentThread().setName("CleanupThread");

        // Get the location of the directory where temporary files are stored.
        File tempdir = getTempDir();
        if (tempdir == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CleanupThread.run: Could not get temporary file directory.  Terminating Cleanup thread.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Will monitor " + tempdir);

        setLastRunTime(Utility.Time());

        // Get the maxage.
        long maxage = SageUtil.GetLongProperty(Plugin.PROPERTY_CLEANUP_MAX_AGE_OF_TEMPFILE, 60L * 60L * 1000L);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Min age before deleting files = " + maxage);

        File[] FileList = tempdir.listFiles();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Looking for old Malore Online files.");
        deleteFilesStartingWith(FileList, DOWNLOAD_FILE_PREFIX_PR, maxage, false);

        if (SageUtil.GetBoolProperty(Plugin.PROPERTY_CLEANUP_ONLINE_VIDEO, true)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Looking for old Online Video.");
            deleteFilesStartingWith(FileList, DOWNLOAD_FILE_PREFIX_SAGE, maxage, false);
        }

        if (SageUtil.GetBoolProperty(Plugin.PROPERTY_CLEANUP_RSS, false)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Looking for old RSS files.");
            deleteFilesStartingWith(FileList, ".rsslib4jbug", maxage, false);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CleanupThread.run: Done.");
    }

    /**
     * Gets the temp directory on this system.
     * <p>
     * @return a File that represents the temp directory on this system.
     */
    private File getTempDir() {

        File tempfile = null;

        try {
            tempfile = java.io.File.createTempFile(DOWNLOAD_FILE_PREFIX_PR,".cln");
        } catch(IOException e) {
            return null;
        }

        tempfile.deleteOnExit();

        return Utility.GetPathParentDirectory(tempfile);
    }
}
