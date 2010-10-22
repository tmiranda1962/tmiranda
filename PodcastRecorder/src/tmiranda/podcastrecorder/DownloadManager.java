/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import sagex.api.*;
import sage.media.rss.*;

/**
 *
 * @author Tom Miranda.
 * <p>
 * Singleton that handles all aspects of downloading podcasts.
 */
public class DownloadManager {

    private static final DownloadManager instance = new DownloadManager();

    private MQDataGetter MQDataGetter;  // If any DownloadManager method is invoked from a client this will be
                                        // used to get data from the server or invoke methods on the server.
    
    private static DownloadThread DT;

    private static boolean recMgrRunning = false;

    private static List<String> ActiveDownloads = new ArrayList<String>();      // Queued to DownloadThread
    private static List<String> FailedDownloads = new ArrayList<String>();      // Tried and failed.
    private static List<String> CompletedDownloads = new ArrayList<String>();   // Tried and completed.
    private static String CurrentlyRecordingID = null;

    // Map so we can convert from RequestID to RecordingEpisode.
    private static HashMap<String, RecordingEpisode> Recordings = new HashMap<String, RecordingEpisode>();

    private static final String THIS_CLASS = "tmiranda.podastrecorder.DownloadManager";
    private static long DefaultMQTimeout;

    private DownloadManager() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DownloadManager: Initializing.");

        /*
         * If the DownloadThread is already running or this is a SageClient, go no further.
         */
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DownloadManager: Attempt to access Downloadmanager on SageClient.");
            MQDataGetter = new MQDataGetter();
            DefaultMQTimeout = SageUtil.GetLongProperty("podcastrecorder/default_mq_timeout", 1000L);
            return;
        }

        /*
         * Start the DownloadThread.
         */
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DownloadManager: Starting DownloadThread.");
        DT = new DownloadThread();
        DT.start();

        /*
        if (t==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DownloadManager: Error starting DownloadThread.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "DownloadManager: Starting DownloadThread.");
            DT = t;
        }
        */
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DownloadManager: Initialization complete.");
    }

    public static DownloadManager getInstance() {
        return instance;
    }

    public Object Clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public boolean getRecMgrStatus() {
        return recMgrRunning;
    }

    public void setRecMgrStatus(boolean status) {
        recMgrRunning = status;
    }

    public void destroy() {
        if (DT!=null) {
            DT.abortCurrentDownload();
            DT.setStop(true);
            DT = null;
        }

    }

    /*
     * Active Dowloads. (In queue.)
     */
    public List<String> getActiveDownloads() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getActiveDownloads.");
        return ActiveDownloads;
    }

    public boolean addActiveDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "AddActiveDownloads added " + ID + ":" + (ActiveDownloads.size()+1));
        return ActiveDownloads.add(ID);
    }

    public boolean removeActiveDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RemoveActiveDownloads removed " + ID + ":" + (ActiveDownloads.size()-1));
        return ActiveDownloads.remove(ID);
    }

    public void removeAllActiveDownloads() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: removeAllActiveDownloads.");
        ActiveDownloads = new ArrayList<String>();
    }

    public List<RSSItem> getRSSItemsForActive() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: GetRSSItemsForActive.");

        List<RSSItem> ItemList = new ArrayList<RSSItem>();

        for (String ID : ActiveDownloads) {

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ID = " + ID);
            RecordingEpisode episode = Recordings.get(ID);

            if (episode == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Error in GetRSSItemsForActive.  Failed to find " + ID);
            } else {
                RSSItem I = episode.getOrigChanItem();
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getRSSItemsForActive Adding Item " + I.getDescription());
                if (!ItemList.add(episode.getOrigChanItem()))
                    Log.printStackTrace();
            }

        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Found items = " + ItemList.size());
        return ItemList;
    }

    public int getSizeActive() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "GetSizeActive = " + ActiveDownloads.size());
        return ActiveDownloads.size();
    }

    public void removeFromQueue(RSSItem Item) {

        String ID = RSSHelper.makeID(Item);

        Set<String> RequestIDs = Recordings.keySet();

        for (String RequestID : RequestIDs) {
            RecordingEpisode episode = Recordings.get(RequestID);
            RSSItem ChanItem = episode.getOrigChanItem();
            if (ID.equalsIgnoreCase(RSSHelper.makeID(ChanItem))) {
                DT.removeItem(episode);                 // Remove from DownloadThread queue
                if (!ActiveDownloads.remove(RequestID))
                    Log.printStackTrace();// remove from DownloadManager queue
                Recordings.remove(RequestID);

                return;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_ERROR, "removeFromQueue: Failed to find RSSItem");
        return;
    }


    /*
     * FailedDownloads.
     */
    public List<String> getFailedDownloads() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getFailedDownloads.");
        return FailedDownloads;
    }

    public boolean addFailedDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: addFailedDownloads.");
        return FailedDownloads.add(ID);
    }

    public boolean removeFailedDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: removeFailedDownloads.");
        return FailedDownloads.remove(ID);
    }

    public List<RSSItem> getRSSItemsForFailed() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: GetRSSItemsforFailed.");

        List<RSSItem> ItemList = new ArrayList<RSSItem>();

        for (String ID : FailedDownloads) {

            RecordingEpisode episode = Recordings.get(ID);

            if (episode == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Error in GetRSSItemsForFailed.  Failed to find " + ID);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Adding Item.");
                if (!ItemList.add(episode.getChanItem()))
                    Log.printStackTrace();
            }

        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Found items = " + ItemList.size());
        return ItemList;
    }

    public int getSizeFailed() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: GetSizeFailed = " + FailedDownloads.size());
        return FailedDownloads.size();
    }


    /*
     * CompletedDownloads.
     */
    public List<String> getCompletedDownloads() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getCompletedDownloads.");
        return CompletedDownloads;
    }

    public boolean addCompletedDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: AddCompletedDownloads added " + ID + ":" + (CompletedDownloads.size()+1));
        return CompletedDownloads.add(ID);
    }

    public boolean removeCompletedDownloads(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: removeCompletedDownloads.");
        return CompletedDownloads.remove(ID);
    }

    public List<RSSItem> getRSSItemsForCompleted() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: GetRSSItemsForCompleted.");

        List<RSSItem> ItemList = new ArrayList<RSSItem>();

        for (String ID : CompletedDownloads) {

            RecordingEpisode episode = Recordings.get(ID);

            if (episode == null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Error in GetRSSItemsForCompleted.  Failed to find " + ID);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Adding Item.");
                if (!ItemList.add(episode.getChanItem()))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Element already in set.");
            }

        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Found items = " + ItemList.size());
        return ItemList;
    }

    public int getSizeCompleted() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DownloadManager: GetSizeCompleted = " + CompletedDownloads.size());
        return CompletedDownloads.size();
    }


    /*
     * CurrentlyRecordingID.
     */
    public String getCurrentlyRecordingID() {
        Log.getInstance().write(Log.LOGLEVEL_ALL, "DownloadManager: getCurrentlyRecordingID.");
        return CurrentlyRecordingID;
    }

    public void setCurrentlyRecordingID(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: setCurrentlyRecordingID.");
        CurrentlyRecordingID = ID;
    }

    public List<RSSItem> getRSSItemsForCurrent() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: GetRSSItemsForCurrent.");

        List<RSSItem> ItemList = new ArrayList<RSSItem>();

        if (CurrentlyRecordingID==null) {
            return ItemList;
        }

        RecordingEpisode episode = Recordings.get(CurrentlyRecordingID);

        if (episode == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Error in GetRSSItemsForCurrent.  Failed to find " + CurrentlyRecordingID);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Adding Item.");
            if (!ItemList.add(episode.getChanItem()))
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Element already in set.");
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Found items = " + ItemList.size());
        return ItemList;
    }

    /*
     * Recordings is a Map that corelates RequestIDs to RecordingEpisodes.  The RecordingEpisodes may
     * be in ActiveDownloads, CompletedDownloads or FailedDownloads.
     */
    public HashMap<String, RecordingEpisode> getRecordings() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getRecordings.");
        return Recordings;
    }

    public RecordingEpisode addRecording(String ID, RecordingEpisode episode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: addRecordings.");
        return Recordings.put(ID, episode);
    }

    public RecordingEpisode getRecording(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getRecordingID.");
        return Recordings.get(ID);
    }

    public RecordingEpisode removeRecording(String ID) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: removeRecording.");
        return Recordings.remove(ID);
    }

    /*
     * Interaction with DownloadThread.
     */

    public long getCurrentDownloadSize() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: getCurrentDownloadSize.");

        if (Global.IsClient())
            return (Long)MQDataGetter.getDataFromServer(THIS_CLASS, "getCurrentDownloadSize", DefaultMQTimeout);
        else
            return DT.getCurrentDownloadSize();
    }

    public String getTitle() {
        if (Global.IsClient()) {
            return (String)MQDataGetter.getDataFromServer(THIS_CLASS, "getTitle", DefaultMQTimeout);
        } else {
            return DT.getTitle();
        }
    }

    public boolean getStop() {
        if (Global.IsClient()) {
            return (Boolean)MQDataGetter.getDataFromServer(THIS_CLASS, "getStop", DefaultMQTimeout);
        } else {
            return DT.getStop();
        }
    }

    public void setStop(boolean state) {
        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PR: Podcast: Removing all items on server.");
            MQDataGetter.invokeMethodOnServer(THIS_CLASS, "setStop", new Object[] {state});
        } else {
            DT.setStop(state);
        }     
    }

    public boolean addItem(RecordingEpisode episode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: Adding item to download queue.");
        
        if (episode==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "addItem: null episode");
            return false;
        }

        return DT.addItem(episode);
    }

    public void abortCurrentDownload() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DownloadManager: abortCurrentDownload.");

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Aborting download on server.");
            MQDataGetter.invokeMethodOnServer(THIS_CLASS, "abortCurrentDownload");
        } else {
            DT.abortCurrentDownload();
        }
    }

    public Integer getNumberOfQueuedItems() {
        if (Global.IsClient()) {
            return (Integer)MQDataGetter.getDataFromServer(THIS_CLASS, "getNumberOfQueuedItems", DefaultMQTimeout);
        } else {
            return DT.getNumberOfQueuedItems();
        }
    }

    public boolean removeAllItems() {

        if (Global.IsClient()) {
            MQDataGetter.invokeMethodOnServer(THIS_CLASS, "removeAllItems");
            return true;
        } else {
            return DT.removeAllItems();
        }
    }

    public synchronized boolean updateDatabase() {

        List<Podcast> Podcasts = Podcast.readFavoritePodcasts();
        if (Podcasts==null || Podcasts.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "updateDatabase: No favorite podcasts.");
            return true;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "updateDatabase: Scanning.");

        List<Podcast> newPodcasts = new ArrayList<Podcast>();

        for (Podcast podcast : Podcasts) {
            newPodcasts.add(updatePodcast(podcast));
        }

        Podcasts = null;

        if (!Podcast.writeFavoritePodcasts(newPodcasts)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "updateDatabase: Error writing favorite podcasts.");
            return false;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "updateDatabase: Done.");
        return true;
    }
    
    private Podcast updatePodcast(Podcast podcast) {

        // Clone the current Podcast.
        Podcast newPodcast = new Podcast(podcast);

        Set<UnrecordedEpisode> EpisodesOnWebServer = newPodcast.getEpisodesOnWebServer();

        if (EpisodesOnWebServer==null || EpisodesOnWebServer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "updatePodcast: No Episodes on Web.");
            return newPodcast;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "updatePodcast: Found Episodes on Web " + EpisodesOnWebServer.size());

        // Scan all MediaFiles to see if any match.
        for (UnrecordedEpisode episode : EpisodesOnWebServer) {

            // Make sure this UnrecordedEpisode is remembered.
            newPodcast.addEpisodesOnServer(episode);

            RSSItem ChanItem = episode.getChanItem();

            // Try to find a matching MediaFile
            Object MediaFile = RSSHelper.getMediaFileForRSSItem(ChanItem);

            // If we found one, see if it's already in the database.
            if (MediaFile != null) {

                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "updatePodcast: Found recorded episode " + episode.getEpisodeTitle());
                
                Episode newEpisode = new Episode(newPodcast, RSSHelper.makeID(ChanItem));
                
                // If it's not already in the database, add it.
                if (!newPodcast.hasEpisodeEverBeenRecorded(newEpisode)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "updatePodcast: Adding recorded episode to database.");
                    newPodcast.addEpisodesEverRecorded(newEpisode);
                }

            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "updatePodcast: Done.");
        return newPodcast;
    }

}
