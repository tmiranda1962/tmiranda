/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import java.util.concurrent.*;
import sage.media.rss.*;

/**
 *
 * @author Tom Miranda.
 */
public class DownloadThread extends Thread {

    private boolean                         stop;
    private BlockingQueue<RecordingEpisode> RecordingMaps;
    private RecordingEpisode                CurrentlyRecording;
    private boolean                         AbortCurrent;

    /*
     * Constructor.
     */
    public DownloadThread() {
        RecordingMaps = new LinkedBlockingQueue<RecordingEpisode>();
        stop = false;
        AbortCurrent = false;
        CurrentlyRecording = null;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Constructor completed.");
    }

    /**
     * Gets the status of the "Stop" flag.  If Stop is set to true the DownloadThread will exit normally
     * after finishing any download that is in progress.
     * <p>
     * @return The status of the Stop flag.
     */
    public boolean getStop() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: getStop.");
        return stop;
    }

    /**
     * Set the "Stop" flag to to value specified.
     * <p>
     * @param state The new value of the Stop flag.
     */
    public void setStop(boolean state) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: setStop.");
        stop = state;
        if (stop)
            this.interrupt();
    }

    /**
     * Add an item to be downloaded.  Details to follow....
     * <p>
     * @param info an array of strings ....
     * @return true if it succeeded, false otherwise.
     */
    public boolean addItem(RecordingEpisode episode) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: addItem.");
        return RecordingMaps.add(episode);
    }

   /**
    * Returns the number of items in the download queue. Does NOT include the currently downloading item, if any.
    * <p>
    * @return The number of items in the download queue.
    */
    public Integer getNumberOfQueuedItems() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: getNumberOfItems.");
        return RecordingMaps.size();
    }

    public boolean removeAllItems() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: removeAllItems.");
        RecordingMaps = new LinkedBlockingQueue<RecordingEpisode>();
        return true;
    }

    public Long getCurrentDownloadSize() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DT: getCurrentDownloadSize.");
        if (CurrentlyRecording==null) {
            return 0L;
        } else {
            return CurrentlyRecording.getBlocksRecorded() * RecordingEpisode.BLOCK_SIZE;
        }
    }

    public boolean getAbortCurrent() {
        return AbortCurrent;
    }

    public void removeItem(RecordingEpisode episode) {
        if (!RecordingMaps.remove(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: Failed to remove episode from RecordingMaps.");
        }
    }

    public void setAbortCurrent(boolean state) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: setAbortCurrent.");
        AbortCurrent = state;
    }

    public String getTitle() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: getTitle.");
        if (CurrentlyRecording==null) {
            return null;
        }
        return CurrentlyRecording.getRSSItemTitle();
    }

    public RecordingEpisode getCurrentlyRecording() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: getCurrentlyRecording.");
        return CurrentlyRecording;
    }

    public void abortCurrentDownload() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Aborting current download.");
        if (CurrentlyRecording != null)
            CurrentlyRecording.abortCurrentDownload();
    }

    void showCurrentlyRecording(RecordingEpisode currentlyRecording) {
        currentlyRecording.show();
    }

    /**
     * The main thread that does all of the downloading.
     */
    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Starting.");

        Thread.currentThread().setName("DownloadThread");

        while (!stop) {

            // Get the first item in the queue and then remove it from the queue.
            try {CurrentlyRecording = RecordingMaps.take();}
            catch (InterruptedException e) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "DT: Interrupted.  Terminating.");
                return;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Have work to do.");

            showCurrentlyRecording(CurrentlyRecording);

            // Make sure we have enough parameters.
            if (!CurrentlyRecording.isComplete()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: Not enough parameters.");
                CurrentlyRecording.failed();
                continue;
            }

            DownloadManager.getInstance().setCurrentlyRecordingID(CurrentlyRecording.getRequestID());

            // Get all of the RSSItems for the Feed Context.
            List<RSSItem> RSSItems = CurrentlyRecording.getRSSItems();
            if (RSSItems == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: null RSSItems.");
                CurrentlyRecording.failed();
                continue;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Found episodes for podcast = " + RSSItems.size());

            if (RSSItems.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: No RSSItems.");
                CurrentlyRecording.failed();
                continue;
            }

            // Get the one ChanItem (RSSItem) we are interested in.
            RSSItem ChanItem = CurrentlyRecording.getItemForID(RSSItems, CurrentlyRecording.getEpisodeID());
            if (ChanItem == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: null ChanItem.");
                CurrentlyRecording.failed();
                continue;
            }

            // Set the ChanItem.
            CurrentlyRecording.setChanItem(ChanItem);

            // Set the fileExt instance variable.
            CurrentlyRecording.setFileExt();

            // Create the tempfile where the episode will be downloaded to.
            if (!CurrentlyRecording.setTempFile()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: Failed to setTempFile.");
                CurrentlyRecording.failed();
                continue;
            }

            // Download the episode to the tempfile.
            if (!CurrentlyRecording.download()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: download failed.");
                CurrentlyRecording.failed();
                continue;
            }

            //Check for 0 size download.
            if (CurrentlyRecording.isZeroSizeDownload()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "DT: File is 0 bytes long.");
                CurrentlyRecording.failed();
                continue;
            }

            // Move the tempfile to the final location and rename it to the final name.
            if (!CurrentlyRecording.moveToFinalLocation()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: moveToFinalLocation failed.");
                CurrentlyRecording.failed();
                continue;
            }

            // Import the episode into the Sage database as an imported media file.
            if (CurrentlyRecording.importAsAiring() == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DT: importAsMediaFile failed.");
                CurrentlyRecording.failed();
                continue;
            }

            // It worked.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Completed successfully.");
            CurrentlyRecording.completed();
            CurrentlyRecording = null;

        } // While !stop

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DT: Fatal error. Ending.");
    } // Run

}
