
package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 *
 * This class represents podcast episodes that have been recorded (downloaded) from the internet.  They
 * may or may not be physically on the filesystem.
 *
 */
public class Podcast implements Serializable {

    private Set<UnrecordedEpisode>  episodesOnWebServer = null;     // Episodes available on the web.
    private Set<Episode>            episodesEverRecorded = null;    // Complete Episode recording history.

    private boolean recordNew = false;                  // Record new episodes as they become available.
    private boolean isFavorite = false;                 // Is this a Favorite Podcast?
    private boolean deleteDuplicates = false;           // Delete duplicate Episodes on disk?
    private boolean keepNewest = true;                  // Keep the newest Episodes or the oldest?
    private boolean reRecordDeleted = false;            // Re-record Episodes that have been deleted?
    private int     maxToRecord = 0;                    // Maximum number of Episodes to keep on disk.
    private boolean autoDelete = false;                 // Can we delete Episodes to make room for newer?
    private long    lastChecked = 0L;                   // Last time we checked for new Episodes.
    private String  recDir = null;                      // Target recording directory.
    private String  recSubdir = null;                   // Target recording subdirectory.
    private String  showTitle = null;                   // The title of the Podcast.
    private String  onlineVideoType = null;             // Used in the STV to identify the Podcast.
    private String  onlineVideoItem = null;             // Used in the STV to identify the Podcast.
    private String  feedContext = null;                 // The Podcast feed context (URL).
    private boolean useShowTitleAsSubdir = false;       // Put the Podcast in its own subdir?
    private boolean useShowTitleInFileName = false;     // Make filename recognizable?
    private int     duplicatesDeleted = 0;              // How many duplicates have been deleted?

    // This is the name of the file that will store the serialized Podcast objects that are Favorites.
    public final static transient String FavoriteDB          = "PodcastRecorderFavoritePodcasts.DB";
    public final static transient String FavoriteDBBackup    = "PodcastRecorderFavoritePodcasts.bak";

    // Cache the Podcasts in memory for faster access. Date is the last time the cache was updated. It is used
    // to determine if the cache (in class API) on SageClients needs to be updated.
    //private static transient boolean        cacheIsDirty = true;
    //private static transient List<Podcast>  PodcastCache = new ArrayList<Podcast>();
    //private static transient Date           cacheDate = new Date();

    /**
     * Creates a new Podcast object.
     */
    public Podcast( boolean Favorite,
                    String OVT,
                    String OVI,
                    String Feed,
                    boolean RecNew,
                    boolean DeleteDupes,
                    boolean KeepNewest,
                    boolean ReRecord,
                    int Max,
                    boolean AutoDelete,
                    String Dir,
                    String Subdir,
                    String Title,
                    boolean TitleAsSubdir,
                    boolean TitleInName) {
        episodesOnWebServer = new HashSet<UnrecordedEpisode>();
        episodesEverRecorded = new HashSet<Episode>();
        recordNew = RecNew;
        isFavorite = Favorite;
        deleteDuplicates = DeleteDupes;
        keepNewest = KeepNewest;
        reRecordDeleted = ReRecord;
        maxToRecord = Max;
        autoDelete = AutoDelete;
        lastChecked = 0L;
        recDir = Dir;
        recSubdir = Subdir;
        showTitle = API.StripShowTitle(Title);
        onlineVideoType = OVT;
        onlineVideoItem = OVI;
        feedContext = Feed;
        useShowTitleAsSubdir = TitleAsSubdir;
        useShowTitleInFileName = TitleInName;
    }

    public Podcast() {}

    // Clone the provided Podcast.
    public Podcast(Podcast p) {

        recordNew               = p.recordNew;
        isFavorite              = p.isFavorite;
        deleteDuplicates        = p.deleteDuplicates;
        keepNewest              = p.keepNewest;
        reRecordDeleted         = p.reRecordDeleted;
        maxToRecord             = p.maxToRecord;
        autoDelete              = p.autoDelete;
        lastChecked             = p.lastChecked;
        recDir                  = p.recDir;
        recSubdir               = p.recSubdir;
        showTitle               = p.showTitle;
        onlineVideoType         = p.onlineVideoType;
        onlineVideoItem         = p.onlineVideoItem;
        feedContext             = p.feedContext;
        useShowTitleAsSubdir    = p.useShowTitleAsSubdir;
        useShowTitleInFileName  = p.useShowTitleInFileName;
        duplicatesDeleted       = p.duplicatesDeleted;

        episodesOnWebServer     = new HashSet<UnrecordedEpisode>();
        episodesEverRecorded    = new HashSet<Episode>();

        episodesOnWebServer.addAll(p.episodesOnWebServer);
        episodesEverRecorded.addAll(p.episodesEverRecorded);
    }

    public Podcast(PodcastData p) {
        recordNew               = p.recordNew;
        isFavorite              = p.isFavorite;
        deleteDuplicates        = p.deleteDuplicates;
        keepNewest              = p.keepNewest;
        reRecordDeleted         = p.reRecordDeleted;
        maxToRecord             = p.maxToRecord;
        autoDelete              = p.autoDelete;
        lastChecked             = p.lastChecked;
        recDir                  = p.recDir;
        recSubdir               = p.recSubdir;
        showTitle               = p.showTitle;
        onlineVideoType         = p.onlineVideoType;
        onlineVideoItem         = p.onlineVideoItem;
        feedContext             = p.feedContext;
        useShowTitleAsSubdir    = p.useShowTitleAsSubdir;
        useShowTitleInFileName  = p.useShowTitleInFileName;
        duplicatesDeleted       = p.duplicatesDeleted;

        episodesOnWebServer     = new HashSet<UnrecordedEpisode>();
        episodesEverRecorded    = new HashSet<Episode>();

        if (p.episodesOnWebServer != null) {
            for (UnrecordedEpisodeData eData : p.episodesOnWebServer) {
                UnrecordedEpisode e = new UnrecordedEpisode(this, eData);
                episodesOnWebServer.add(e);
            }
        }

        if (p.episodesEverRecorded != null) {
            for (EpisodeData eData : p.episodesEverRecorded) {
                Episode e = new Episode(eData);
                episodesEverRecorded.add(e);
            }
        }
    }

    public int getEpisodesOnWebServerSize() {
        return episodesOnWebServer.size();
    }

    public int getEpisodesEverRecordedSize() {
        return episodesEverRecorded.size();
    }

    public boolean hasEpisodeEverBeenRecorded(Episode episode) {

        if (episodesEverRecorded==null || episodesEverRecorded.isEmpty()) {
            return false;
        }

        for (Episode e : episodesEverRecorded) {
            if (episode.getID().equals(e.getID())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasUnrecordedEpisodes() {
        Set<UnrecordedEpisode> unrecordedEpisodes = fetchEpisodesOnWebServer();

        // If there are no episodes on the web return false.
        if (unrecordedEpisodes==null || unrecordedEpisodes.isEmpty()) {
            return false;
        }

        Set<Episode> recordedEpisodes = getEpisodesEverRecorded();

        // If there are episodes on the web, but none recorded, return true.
        if (recordedEpisodes==null || recordedEpisodes.isEmpty()) {
            return true;
        }

        // If any unrecorded episode is not recorded return true.
        for (UnrecordedEpisode unrecorded : unrecordedEpisodes) {

            boolean thisOneIsRecorded = false;
            String unrecordedID = unrecorded.getID();

            for (Episode recorded : recordedEpisodes) {

                String recordedID = recorded.getID();

                if (unrecordedID.compareTo(recordedID) == 0) {
                    thisOneIsRecorded = true;
                }
            }

            if (!thisOneIsRecorded) {
                return true;
            }
        }

        // All episodes on the web have been recorded.
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Podcast other = (Podcast) obj;
        if (this.recordNew != other.recordNew) {
            return false;
        }
        if (this.deleteDuplicates != other.deleteDuplicates) {
            return false;
        }
        if (this.keepNewest != other.keepNewest) {
            return false;
        }
        if (this.reRecordDeleted != other.reRecordDeleted) {
            return false;
        }
        if ((this.onlineVideoType == null) ? (other.onlineVideoType != null) : !this.onlineVideoType.equals(other.onlineVideoType)) {
            return false;
        }
        if ((this.onlineVideoItem == null) ? (other.onlineVideoItem != null) : !this.onlineVideoItem.equals(other.onlineVideoItem)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.recordNew ? 1 : 0);
        hash = 83 * hash + (this.deleteDuplicates ? 1 : 0);
        hash = 83 * hash + (this.keepNewest ? 1 : 0);
        hash = 83 * hash + (this.reRecordDeleted ? 1 : 0);
        hash = 83 * hash + (this.onlineVideoType != null ? this.onlineVideoType.hashCode() : 0);
        hash = 83 * hash + (this.onlineVideoItem != null ? this.onlineVideoItem.hashCode() : 0);
        return hash;
    }


    /**
     * Gets all of the Episodes ever recorded.  The Episodes may or may not be available on the web and
     * they may or may not be available on the filesystem.
     * <p>
     * @return A List of all the episodes ever recorded.
     */
    public Set<Episode> getEpisodesEverRecorded() {
        return episodesEverRecorded;
    }


    /*
     * Gets the text name associated with a Podcast. Will never return null.
     */
    private String getText(Properties properties) {

        if (properties==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getText: null parameter.");
            return null;
        }

        String Text = null;

        String OVI = this.getOnlineVideoItem();

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getText: Looking for " + OVI);

        if (OVI.startsWith("ez")) {
            Text = properties.getProperty(OVI+"/Name", null);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getText: Returning " + Text);
            return (Text == null ? " " : Text);
        } else {
            Text = properties.getProperty("Source/" + OVI + "/LongName", null);
            if (Text!=null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getText: Source/LogName returning " + Text);
                return Text;
            } else {
                Text = properties.getProperty("Category/" + OVI + "/FullName", null);
                if (Text!=null) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getText: FullName returning " + Text);
                    return Text;
                } else {
                    Text = properties.getProperty("Category/" + OVI + "/LongName", null);
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getText: LongName returning " + Text);
                    return (Text==null ? " " : Text);
                }
            }
        }
    }

    /**
     * Method that can be used from the STV to print a list of the favorite podcasts to the logfile.
     */
    public void dumpFavorites() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpFavorites: Displaying contents of database.");

        // Get the current Favorites.
        List<Podcast> favoritePodcasts = DataStore.getAllPodcasts();

        if (favoritePodcasts == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpFavorites: No Favorites defined.");
            return;
        }

        File file = new File(FavoriteDB);
        //if (file==null || !file.exists()) {
            //Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.dumpFavorites: Fatal error, no FavoriteDB");
            //favoritePodcasts = null;
            //return;
        //}

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpFavorites: FavoriteDB size " + file.length());

        dumpList(favoritePodcasts);
        favoritePodcasts = null;
    }

    public void dumpList(List<Podcast> favoritePodcasts) {
        if (favoritePodcasts == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: No Favorites defined.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: " + favoritePodcasts.size());

        for (Podcast podcast : favoritePodcasts) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: Podcast " +
                                                "OVT="+podcast.onlineVideoType + " : " +
                                                "OVI="+podcast.onlineVideoItem + " : " +
                                                "FeedContext="+podcast.feedContext + " : " +
                                                "RecordNew="+podcast.recordNew + " : " +
                                                "DeleteDupes="+podcast.deleteDuplicates + " : " +
                                                "KeepNewest="+podcast.keepNewest + " : " +
                                                "ReRecDeleted="+podcast.reRecordDeleted + " : " +
                                                "Max="+podcast.maxToRecord + " : " +
                                                "AutoDelete="+podcast.autoDelete + " : " +
                                                "RecDir="+podcast.recDir + " : " +
                                                "recSubdir="+podcast.recSubdir + " : " +
                                                "ShowTitle="+podcast.showTitle + " : " +
                                                "ShowTitleAsSubdir="+podcast.useShowTitleAsSubdir + " : " +
                                                "ShowTitleInFileName="+podcast.useShowTitleInFileName);

            if (podcast.episodesOnWebServer == null || podcast.episodesOnWebServer.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: No episodesOnWebServer.");
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: EpisodesOnWebServer " + podcast.episodesOnWebServer.size());

                for (UnrecordedEpisode e : podcast.episodesOnWebServer) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: Title for Episode on Web = " + e.getEpisodeTitle());
                }
            }

             if (podcast.episodesEverRecorded == null || podcast.episodesEverRecorded.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: No episodesEverRecorded.");
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: episodesEverRecorded:" + podcast.episodesEverRecorded.size());

                for (Episode e : podcast.episodesEverRecorded) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.dumpList: Title for recorded Episode = " + e.getShowEpisode());
                }
            }
        }
    }

    /**
     * Checks if a Podcast is a Favorite.
     * <p>
     * @return true if it's a Favorite, false otherwise.
     */
    public boolean isFavorite() {
        return isFavorite;
    }

    public PodcastKey getKey() {
        return new PodcastKey(this.onlineVideoType, this.onlineVideoItem, this.feedContext);
    }


    /*
     * ***************************************
     * Methods that have to do with Episodes.*
     * ***************************************
     */

    public void updateEpisodesOnWebServerAndEverRecorded() {

        // Start with a clean slate.
        clearEpisodesOnWebServer();

        // Get all of the Episodes that are on the web server.
        Set<UnrecordedEpisode> EpisodesOnWebServer = fetchEpisodesOnWebServer();

        if (EpisodesOnWebServer==null || EpisodesOnWebServer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.updateEpisodesOnWebServerAndEverRecorded: No Episodes on Web.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.updateEpisodesOnWebServerAndEverRecorded: Found Episodes on Web " + EpisodesOnWebServer.size());

        // Scan all MediaFiles to see if any match.
        for (UnrecordedEpisode episode : EpisodesOnWebServer) {

            // Make sure this UnrecordedEpisode is remembered.
            addEpisodesOnWebServer(episode);

            RSSItem ChanItem = episode.getChanItem();

            // Try to find a matching MediaFile
            Object MediaFile = RSSHelper.getMediaFileForRSSItem(ChanItem);

            // If we found one, see if it's already in the database.
            if (MediaFile != null) {

                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.updateEpisodesOnWebServerAndEverRecorded: Found recorded episode " + episode.getEpisodeTitle());

                Episode newEpisode = new Episode(this, RSSHelper.makeID(ChanItem));

                // If it's not already in the database, add it.
                if (!hasEpisodeEverBeenRecorded(newEpisode)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.updateEpisodesOnWebServerAndEverRecorded: Adding recorded episode to database.");
                    addEpisodeEverRecorded(newEpisode);
                }

            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.updateEpisodesOnWebServerAndEverRecorded: Done.");
        return;
    }

    /**
     * Gets an array of Episodes that are on the web server.  Does NOT download them, just gets 
     * the RSSItems.
     *
     * Also updates the database with the unrecorded episodes.
     *
     * <p>
     * @return A List of the episodes that are available on the web.
     */
    public Set<UnrecordedEpisode> fetchEpisodesOnWebServer() {

        // Make sure in here we set the proper variables in the Episode object.

        // Make sure we follow URL redirects.
        RSSHelper.setRedirects();

        // Get the RSSItems for this Podcast.
        List<RSSItem> RSSItems = this.fetchRSSItems();

        // Abort if error.
        if (RSSItems == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.fetchEpisodesOnWebServer: Error getting RSSItems for Podcast.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.fetchEpisodesOnWebServer: Found RSSItems = " + RSSItems.size());

        Set<UnrecordedEpisode> unrecorded = new HashSet<UnrecordedEpisode>();

        // Loop through all of the RSSItems.
        for (RSSItem Item : RSSItems) {

            // Create a new UnrecordedEpisode.
            UnrecordedEpisode episode = new UnrecordedEpisode(this, Item);

            // Add it the the List.
            if (!unrecorded.add(episode))
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.fetchEpisodesOnWebServer: Element already in set.");
        }

        return unrecorded;
    }

    /**
     * Gets the Episodes that are curerntly on the filesystem.
     * <p>
     * @return An List of the episodes on the filesystem. A zero sized list if there are none.
     */
    public List<Episode> getEpisodesOnDisk() {

        List<Episode> OnDisk = new ArrayList<Episode>();

        if (episodesEverRecorded == null || episodesEverRecorded.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.getEpisodesOnDisk: No episodesEverRecorded.");
            return OnDisk;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.getEpisodesOnDisk: episodesEverRecorded = " + episodesEverRecorded.size());

        for (Episode e : episodesEverRecorded) {
            if (e.isOnDisk()) {
                if (!OnDisk.add(e))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.getEpisodesOnDisk: Element already in set.");
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.getEpisodesOnDisk: Found = " + OnDisk.size());
        return OnDisk;
    }


    /**
     * This method deletes all duplicate recordings of the Podcast.  If KeepNewest is true the Episode that
     * was recorded last is kept otherwise the Episode that was recorded first is kept.
     * <p>
     * The date specified in the Episode itself has no bearing on the behavior of this method.
     * <p>
     * @param KeepNewest true to keep the most recently recorded Episode, false to keep the Episode recorded first.
     * @return The number of duplicate Episodes actually deleted.
     */
    public int deleteDuplicateEpisodes(boolean KeepNewest){

        int deleted = 0;

        while (deleteDuplicateEpisode(KeepNewest) != 0) {          
            setDuplicatesDeleted(duplicatesDeleted++);
            deleted++;
        }

        //setDuplicatesDeletedInDatabase(duplicatesDeleted);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplicateEpisodes: Deleted " + duplicatesDeleted);
        return deleted;
    }

    /**
     * Deletes a single duplicate Episode.
     * @param KeepNewest true to keep the Episode recorded last, false to keep the Episode recorded first.
     * Like deleteDuplicateEpisodes the date in the Episode has no bearing on the behavior of the method.
     * @return The number of Episodes deleted (0 or 1).
     */
    private int deleteDuplicateEpisode(boolean KeepNewest) {

        // Get all of the Episodes that are physically on the disk.
        List<Episode> episodes = this.getEpisodesOnDisk();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplicateEpisode: Episode on disk = " + episodes.size());

        // Make sure we have Episodes to process.
        if (episodes == null || episodes.isEmpty()) {
            return 0;
        }

        // Get a sorted list of episodes.
        episodes = Episode.sortByDateRecorded(episodes, !KeepNewest);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplicateEpisode: Episode on disk after sort = " + episodes.size());

        // Make sure we have Episodes to process.
        if (episodes == null || episodes.isEmpty()) {
            return 0;
        }

        // Loop through all of the sorted episodes.
        for (Episode episode : episodes) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplcateEpisodes: Checking for duplicates " + episode.getID() + ":" + episode.getShowEpisode());

            // Look for duplicates.
            boolean foundFirst = false;
            boolean foundSecond = false;
            Episode e = null;
            String episodeID = episode.getID();

            for (int i=0; i<episodes.size() && !foundSecond; i++) {
                e = episodes.get(i);
                String eID = e.getID();

                if (episodeID.equalsIgnoreCase(eID)) {
                    if (foundFirst) {
                        foundSecond = true;
                    } else {
                        foundFirst = true;
                    }
                }
            }

            // If we found a second Episode, delete it.
            if (foundSecond) {

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplcateEpisodes: Found duplicate " + e.getShowEpisode());
                if (!e.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.deleteDuplcateEpisodes: Error deleting.");
                }

                return 1;
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplcateEpisodes: No duplicate.");
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.deleteDuplcateEpisodes: No duplicates for any episode.");
        return 0;
    }

    /**
     * Method used to determine if we can proceed with recording a new episode.
     * <p>
     * @return true if it's OK to record another Episode.  Note that it may be necessary to delete an Episode
     * after a new one is recorded. Use needToDelete() to determine if an Episode should be deleted.
     */
    public boolean canRecordNew() {

        // OK to record if MaxToRecord set to unlimited (0).
        if (this.maxToRecord == 0) {
            return true;
        }

        // OK to record if what we have on disk is still below the threshhold.
        if (getEpisodesOnDisk().size() < this.maxToRecord) {
            return true;
        }

        // OK to record if we can autodelete.
        if (this.autoDelete) {
            return true;
        }

        return false;
    }

    /**
     * Determines if any Episodes need to be deleted based on MaxToRecord and NumberOnDisk.
     * @return true if MaxToRecord is not unlimited and the number on disk is greater than MaxToRecord.
     */
    public boolean needToDelete() {

        // No need to delete if MaxToRecord is unlimited.
        if (this.maxToRecord == 0) {
            return false;
        }

        List<Episode> OnDisk = this.getEpisodesOnDisk();

        // No need to delete if there are none on disk.
        if (OnDisk == null || OnDisk.isEmpty()) {
            return false;
        }

        return (OnDisk.size() > this.maxToRecord);
    }

    /**
     * Gets the RSSItems for the Podcast.
     * <p>
     * @return A List of RSSItems fot this Podcast.  null if error.
     */
    private List<RSSItem> fetchRSSItems() {
        return RSSHelper.getRSSItems(feedContext);
    }

    public static String getFavoriteDB() {
        return FavoriteDB;
    }

    public static String getFavoriteDBBackup() {
        return FavoriteDBBackup;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public boolean isDeleteDuplicates() {
        return deleteDuplicates;
    }

    public int getDuplicatesDeleted() {
        return duplicatesDeleted;
    }

    public Set<UnrecordedEpisode> getEpisodesOnWebServer() {
        return episodesOnWebServer;
    }

    public void clearEpisodesOnWebServer() {
        if (episodesOnWebServer==null) {
            return;
        }

        episodesOnWebServer.clear();
        updateDatabase();
    }

    public void clearEpisodesEverRecorded() {
        if (episodesEverRecorded==null) {
            return;
        }

        episodesEverRecorded.clear();
        updateDatabase();
    }

    public void addEpisodesOnWebServer(UnrecordedEpisode episode) {

        if (episodesOnWebServer.contains(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.addEpisodesOnWebServer: UnrecordedEpisode already in set " + episode.getShowTitle());
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.addEpisodesOnWebServer: Adding episodesOnWebServer " + episode.getShowTitle());
        if (!episodesOnWebServer.add(episode))
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.addEpisodesOnWebServer: Failed to add " + episode.getShowEpisode());

        updateDatabase();
    }

    public String getFeedContext() {
        return feedContext;
    }

    public void setFeedContext(String NewContext) {
        feedContext = NewContext;
        updateDatabase();
    }

    public boolean isKeepNewest() {
        return keepNewest;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public int getMaxToRecord() {
        return maxToRecord;
    }

    public String getOnlineVideoItem() {
        return onlineVideoItem;
    }

    public String getOnlineVideoType() {
        return onlineVideoType;
    }

    public boolean isReRecordDeleted() {
        return reRecordDeleted;
    }

    public String getRecDir() {
        return recDir;
    }

    public String getRecSubdir() {
        return recSubdir;
    }

    public boolean isRecordNew() {
        return recordNew;
    }

    public String getShowTitle() {
        return showTitle;
    }

    public boolean isUseShowTitleAsSubdir() {
        return useShowTitleAsSubdir;
    }

    public boolean isUseShowTitleInFileName() {
        return useShowTitleInFileName;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
        updateDatabase();
    }

    public void setDeleteDuplicates(boolean deleteDuplicates) {
        this.deleteDuplicates = deleteDuplicates;
        updateDatabase();
    }

    public void setDuplicatesDeleted(int duplicatesDeleted) {
        this.duplicatesDeleted = duplicatesDeleted;
        updateDatabase();
    }

    public void addEpisodeEverRecorded(Episode episode) {

        if (episodesEverRecorded.contains(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.addEpisodeEverRecorded: Episode has already been recorded " + episode.getShowTitle());
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.addEpisodeEverRecorded: Adding episodeEverRecorded " + episode.getShowTitle());
        if (!episodesEverRecorded.add(episode))
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.addEpisodeEverRecorded: Error adding element " + episode.getShowTitle());

        updateDatabase();
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
        changeEpisodeFavoriteStatus(isFavorite);
        updateDatabase();
    }

    /**
     * Change the status of all episodesEverRecorded to indicate if they are a favorite
     * or not.  Will update the MediaFile metadata properties.
     *
     * @param favorite true if the Episodes should be marked as a Favorite, false if they
     * should be marked as not a Favorite.
     * 
     */
    private void changeEpisodeFavoriteStatus(boolean favorite) {

        for (Episode episode : episodesEverRecorded) {
            Object MediaFile = episode.fetchMediaFile();
            if (MediaFile != null) {
                MediaFileAPI.SetMediaFileMetadata(MediaFile, RecordingEpisode.METADATA_FAVORITE, favorite ? "true" : "false");

                Object Airing = MediaFileAPI.GetMediaFileEncoding(MediaFile);
                if (Airing != null) {
                    AiringAPI.SetManualRecordProperty(Airing, RecordingEpisode.METADATA_FAVORITE, favorite ? "true" : "false");
                }
            }
        }
    }

    public void setKeepNewest(boolean keepNewest) {
        this.keepNewest = keepNewest;
        updateDatabase();
    }

    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
        updateDatabase();
    }

    public void setMaxToRecord(int maxToRecord) {
        this.maxToRecord = maxToRecord;
        updateDatabase();
    }

    public void setOnlineVideoItem(String onlineVideoItem) {
        this.onlineVideoItem = onlineVideoItem;
        updateDatabase();
    }

    public void setOnlineVideoType(String onlineVideoType) {
        this.onlineVideoType = onlineVideoType;
        updateDatabase();
    }

    public void setReRecordDeleted(boolean reRecordDeleted) {
        this.reRecordDeleted = reRecordDeleted;
        updateDatabase();
    }

    public void setRecDir(String recDir) {
        this.recDir = recDir;
        updateDatabase();
    }

    public void setRecSubdir(String recSubdir) {
        this.recSubdir = recSubdir;
        updateDatabase();
    }

    public void setRecordNew(boolean recordNew) {
        this.recordNew = recordNew;
        updateDatabase();
    }

    public void setShowTitle(String showTitle) {
        this.showTitle = showTitle;
        updateDatabase();
    }

    public void setUseShowTitleAsSubdir(boolean useShowTitleAsSubdir) {
        this.useShowTitleAsSubdir = useShowTitleAsSubdir;
        updateDatabase();
    }

    public void setUseShowTitleInFileName(boolean useShowTitleInFileName) {
        this.useShowTitleInFileName = useShowTitleInFileName;
        updateDatabase();
    }

    private void updateDatabase() {
        if (DataStore.getPodcastForUpdate(this)==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.updateDatabase: Podcast is locked. Not committing to database. " + this.getShowTitle());
            return;
        }

        if (!DataStore.updatePodcast(this)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.updateDatabase: Failed to update database for " + this.getShowTitle());
        }
    }
}

/*
 * Old stuff that can be deleted.
 */

    /*
     * Convenience method for API class. A return of null signifies that the cache in the API class is still valid.
     */
    /**
    public synchronized static List<Podcast> readFavoritePodcasts(Date clientCacheDate) {
        if (cacheIsDirty || clientCacheDate.compareTo(cacheDate) < 0) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readFavoritePodcasts: Date cache needs to be updated.");
            return readFavoritePodcasts();
        }

        return null;
    }
     */

    /**
    * Saves the Favorite Podcasts to disk.
    * <p>
     * @param A List of Podcasts.
    * @return   true if the operation succeded, false otherwise.
    */
    /**
    public synchronized static boolean writeFavoritePodcasts(List<Podcast> favoritePodcasts) {

        // Make sure the List is not null.
        if (favoritePodcasts==null) {
            return false;
        }

        // Backup the current database file and delete the original.
        SageUtil.RenameFile(FavoriteDB, FavoriteDBBackup);

        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writeFavoritePodcasts: Error creating new FavoriteDB file.");
                return false;
            }
        }

        // Write the new database file.
        try {
            FileOutputStream fileStream = new FileOutputStream(FavoriteDB);
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);

            // Write all Podcasts to disk.
            for (Podcast p : favoritePodcasts) {
                PodcastData pData = new PodcastData(p);
                objectStream.writeObject(pData);
            }

            objectStream.close();
            fileStream.close();

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writeFavoritePodcasts: Exception " + e.getMessage());
            return false;
        }

        cacheIsDirty = true;
        cacheDate = new Date();
        return true;
    }

    public synchronized static boolean writePodcast(Podcast podcast) {

        // Make sure the List is not null.
        if (podcast==null) {
            return false;
        }

        // Delete the backup.
        File backupDB = new File(FavoriteDBBackup);

        if (backupDB.exists()) {
            if (!backupDB.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writePodcast: Error deleting backup file.");
            }
        }

        // Backup the current database file and delete the original.
        if (!SageUtil.RenameFile(FavoriteDB, FavoriteDBBackup)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writePodcast: Error backing up file.");
            return false;
        }

        // Create a new database file.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writePodcast: Error creating new FavoriteDB file.");
                return false;
            }
        }

        // Write the new database file replacing the existing Podcast with the new one.

        cacheIsDirty = true;
        cacheDate = new Date();
        return true;
    }
*/

    /**
    * Reads the Favorite Podcasts form the disk.
    * <p>
    * @return   A List of Podcasts.  null indicates an error.
    */
    /**
    public synchronized static List<Podcast> readFavoritePodcasts() {

        // If the Podcasts have not changed just return what we have in the cache.
        if (!cacheIsDirty) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.readFavoritePodcasts: cache is clean");
            return PodcastCache;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readFavoritePodcasts: cache is dirty");

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readFavoritePodcasts: Error creating new FavoriteDB file.");
                return null;
            }
        }

        // Clear the cache to reclaim the memory.
        PodcastCache.clear();
        PodcastCache = null;
        cacheIsDirty = true;
        cacheDate = new Date();

        // Create the List to hold the elements.
        List<Podcast> favoritePodcasts = new ArrayList<Podcast>();

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(FavoriteDB);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readFavoritePodcasts: Error opening FileInputStream.");
            PodcastCache = favoritePodcasts;
            cacheIsDirty = false;
            cacheDate = new Date();
            return null;
        }

        ObjectInputStream objectStream;

        try {
            objectStream = new ObjectInputStream(fileStream);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readFavoritePodcasts: No Podcasts to read.");
            try {fileStream.close();} catch (Exception ex) {}
            cacheIsDirty = false;
            PodcastCache = favoritePodcasts;
            cacheDate = new Date();
            return favoritePodcasts;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readFavoritePodcasts: Exception " + e.getMessage());
            try {fileStream.close();} catch (Exception ex) {}
            cacheIsDirty = false;
            PodcastCache = favoritePodcasts;
            cacheDate = new Date();
            return favoritePodcasts;
        }

        Object p = null;
        Podcast podcast = null;

        try {
            while ((p=objectStream.readObject()) != null) {
                podcast = new Podcast((PodcastData)p);
                if (!favoritePodcasts.add(podcast))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.readFavoritePodcasts: Element already in set.");
        }

        } catch(EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.readFavoritePodcasts: Complete.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Podcast.readFavoritePodcasts: Objects in DB are invalid.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readFavoritePodcasts: Exception " + e.getMessage());
        }

        try {
            objectStream.close();
            fileStream.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readFavoritePodcasts: Exception closing. " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.readFavoritePodcasts: found " + favoritePodcasts.size());

        // Update the cache.
        PodcastCache = favoritePodcasts;
        cacheDate = new Date();
        cacheIsDirty = false;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readFavoritePodcasts: Updated Podcast cache.");

        return favoritePodcasts;
    }

     */

    /**
    public synchronized static List<PodcastKey> getAllPodcastKeys() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getAllPodcastKeys: Looking.");

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: Error creating new FavoriteDB file.");
                return null;
            }
        }

        // Create the List to hold the elements.
        List<PodcastKey> keyList = new ArrayList<PodcastKey>();

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(FavoriteDB);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: Error opening FileInputStream.");
            return null;
        }

        ObjectInputStream objectStream;

        try {
            objectStream = new ObjectInputStream(fileStream);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getAllPodcastKeys: No Podcasts to read.");
            try {fileStream.close();} catch (Exception ex) {}
            return keyList;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: Exception " + e.getMessage());
            try {fileStream.close();} catch (Exception ex) {}
            return keyList;
        }

        Object p = null;
        Podcast podcast = null;

        try {
            while ((p=objectStream.readObject()) != null) {
                podcast = new Podcast((PodcastData)p);
                PodcastKey key = new PodcastKey(podcast.getOnlineVideoType(), podcast.getOnlineVideoItem(), podcast.getFeedContext());
                if (!keyList.add(key))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: Failed to add key.");
        }

        } catch(EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.getAllPodcastKeys: complete.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Podcast.getAllFavoritePodcastKeys: Objects in DB are invalid.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: exception " + e.getMessage());
        }

        try {
            objectStream.close();
            fileStream.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.getAllPodcastKeys: Exception closing. " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.getAllPodcastKeys: found " + keyList.size());
        return keyList;
    }

    public synchronized static Podcast readPodcastFromDataBase(PodcastKey key) {

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readPodcastFromDataBase: Error creating new FavoriteDB file.");
                return null;
            }
        }

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(FavoriteDB);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readPodcastFromDataBase: Error opening FileInputStream.");
            return null;
        }

        ObjectInputStream objectStream;

        try {
            objectStream = new ObjectInputStream(fileStream);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readPodcastFromDataBase: No Podcasts to read.");
            try {fileStream.close();} catch (Exception ex) {}
            return null;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readPodcastFromDataBase: Exception " + e.getMessage());
            try {fileStream.close();} catch (Exception ex) {}
            return null;
        }

        Object p = null;
        Podcast podcast = null;

        try {
            boolean found = false;
            while ((p=objectStream.readObject()) != null && !found) {
                podcast = new Podcast((PodcastData)p);
                PodcastKey thisKey = podcast.getKey();
                if (key.equals(thisKey)) {
                    found = true;
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Podcast.readPodcastFromDataBase: Found Podcast");
                }
            }
        } catch(EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast.readPodcastFromDataBase: complete.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Podcast.readPodcastFromDataBase: Objects in DB are invalid.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readPodcastFromDataBase: exception " + e.getMessage());
        }

        try {
            objectStream.close();
            fileStream.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.readPodcastFromDataBase: Exception closing. " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.readPodcastFromDataBase: Done.");

        return podcast;
    }
     */