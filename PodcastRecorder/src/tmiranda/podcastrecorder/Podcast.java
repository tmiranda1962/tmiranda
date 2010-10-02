/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import java.net.*;
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

    private List<UnrecordedEpisode> episodesOnServer = null;    // Episodes available on the web.
    private List<Episode> episodesEverRecorded = null;          // Complete Episode recording history.

    private boolean recordNew = false;
    private boolean isFavorite = false;
    private boolean deleteDuplicates = false;
    private boolean keepNewest = true;
    private boolean reRecordDeleted = false;
    private int     maxToRecord = 0;
    private boolean autoDelete = false;
    private long    lastChecked = 0L;
    private String  recDir = null;
    private String  recSubdir = null;
    private String  showTitle = null;
    private String  onlineVideoType = null;
    private String  onlineVideoItem = null;
    private String  feedContext = null;
    private boolean useShowTitleAsSubdir = false;
    private boolean useShowTitleInFileName = false;
    private int duplicatesDeleted = 0;

    // This is the name of the file that will store the serialized Podcast objects that are Favorites.
    private final static String FavoriteDB          = "PodcastRecorderFavoritePodcasts.DB";
    private final static String FavoriteDBBackup    = "PodcastRecorderFavoritePodcasts.bak";

    // Cache the Podcasts in memory for faster access. Date is the last time the cache was updated. It is used
    // to determine if the cache (in class API) on SageClients needs to be updated.
    private static boolean cacheIsDirty = true;
    private static List<Podcast> PodcastCache = new ArrayList<Podcast>();
    private static Date cacheDate = new Date();

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
        episodesOnServer = new ArrayList<UnrecordedEpisode>();
        episodesEverRecorded = new ArrayList<Episode>();
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

    public int getEpisodesOnServerSize() {
        return episodesOnServer.size();
    }

    public int getEpisodesEverRecordedSize() {
        return episodesEverRecorded.size();
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
    public List<Episode> getEpisodesEverRecorded() {
        return this.episodesEverRecorded;
    }

    private synchronized boolean setEpisodeRecordedInDatabase(Episode episode) {

        if (episode==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast: null parameter.");
            return false;
        }

        List<Podcast> Favorites = readFavoritePodcasts();

        for (Podcast p : Favorites) {
            if (p.equals(this)) {
                if (!p.episodesEverRecorded.add(episode)) Log.getInstance().printStackTrace();
                return writeFavoritePodcasts(Favorites);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM setEpisodeRecorded: Did not find Podcast.");
        return false;
    }

    public void setLastChecked(Long t) {
        this.lastChecked = t;
    }

    public synchronized boolean setLastCheckedInDatabase(Long t) {

        if (t==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast: null parameter.");
            return false;
        }

        List<Podcast> Favorites = readFavoritePodcasts();

        for (Podcast p : Favorites) {
            if (p.equals(this)) {
                p.setLastChecked(t);
                return writeFavoritePodcasts(Favorites);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM setLastCheckedInDatbase: Did not find Podcast.");
        return false;
    }


    /*
     * Gets the text name associated with a Podcast. Will never return null;
     */
    private String getText(Properties properties) {

        if (properties==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast: null parameter.");
            return null;
        }

        String Text = null;

        String OVI = this.getOnlineVideoItem();

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast getText looking for " + OVI);

        if (OVI.startsWith("ez")) {
            Text = properties.getProperty(OVI+"/Name", null);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast getText ez returning " + Text);
            return (Text == null ? " " : Text);
        } else {
            Text = properties.getProperty("Source/" + OVI + "/LongName", null);
            if (Text!=null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast getText Source/LogName returning " + Text);
                return Text;
            } else {
                Text = properties.getProperty("Category/" + OVI + "/FullName", null);
                if (Text!=null) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast getText FullName returning " + Text);
                    return Text;
                } else {
                    Text = properties.getProperty("Category/" + OVI + "/LongName", null);
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast getText LongName returning " + Text);
                    return (Text==null ? " " : Text);
                }
            }
        }
    }

    public static Podcast Find(String OVT, String OVI) {

        Log.getInstance().write(Log.LOGLEVEL_ALL, "Podcast: Find.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast: null parameter.");
            return null;
        }

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = API.getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = readFavoritePodcasts();
        }


        if (favoritePodcasts == null) {
            return null;
        }

        Podcast podcast = findPodcast(favoritePodcasts, OVT, OVI);

        return (podcast==null ? null : podcast);
    }

    /**
     * Finds a Podcast based on OVT and OVI.
     * <p>
     * @param favoritePodcasts
     * @param OVI
     * @param OVT
     * @return
     */
    public static Podcast findPodcast(List<Podcast> favoritePodcasts, String OVT, String OVI) {

        for (Podcast podcast : favoritePodcasts) {
            if (podcast.onlineVideoType.equals(OVT) && podcast.onlineVideoItem.equals(OVI)) {
                return podcast;
            }
        }

        return null;
    }

    /**
     * Method that can be used from the STV to print a list of the favorite podcasts to the logfile.
     */
    public void dumpFavorites() {

        // Get the current Favorites.
        List<Podcast> favoritePodcasts = readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM DumpFavorites: No Favorites defined.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM DumpFavorites:");

        for (Podcast podcast : favoritePodcasts) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, " Podcast \n" +
                "OVT="+podcast.onlineVideoType + ":" +
                "OVI="+podcast.onlineVideoItem + ":" +
                "FeedContext="+podcast.feedContext + ":" +
                "RecordNew="+podcast.recordNew + ":" +
                "DeleteDupes="+podcast.deleteDuplicates + ":" +
                "KeepNewest="+podcast.keepNewest + ":" +
                "ReRecDeleted="+podcast.reRecordDeleted + ":" +
                "Max="+podcast.maxToRecord + ":" +
                "AutoDelete="+podcast.autoDelete + ":" +
                "RecDir="+podcast.recDir + ":" +
                "recSubdir="+podcast.recSubdir + ":" +
                "ShowTitle="+podcast.showTitle + ":" +
                "ShowTitleAsSubdir="+podcast.useShowTitleAsSubdir + ":" +
                "ShowTitleInFileName="+podcast.useShowTitleInFileName);

            if (episodesOnServer == null || episodesOnServer.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "  No episodesOnServer.");
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "  episodesOnServer:");

                for (UnrecordedEpisode e : episodesOnServer) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "    Title = " + e.getEpisodeTitle());
                }
            }

             if (episodesEverRecorded == null || episodesEverRecorded.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "  No episodesEverRecorded.");
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "  episodesEverRecorded:");

                for (Episode e : episodesEverRecorded) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "    Title = " + e.getShowEpisode());
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

    /**
    * Reads the Favorite Podcasts form the disk.
    * <p>
    * @return   A List of Podcasts.  null indicates an error.
    */
    public synchronized static List<Podcast> readFavoritePodcasts() {

        // If the Podcasts have not changed just return what we have in the cache.
        if (!cacheIsDirty) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "readFavoritePodcasts cache is clean");
            return PodcastCache;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readFavoritePodcasts cache is dirty");

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "readFavoritePodcasts: Error creating new FavoriteDB file.");
                return null;
            }
        }


        // Create the List to hold the elements.
        List<Podcast> favoritePodcasts = new ArrayList<Podcast>();

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(FavoriteDB);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readFavoritePodcasts: Error opening FileInputStream.");
            e.printStackTrace();
            return null;
        }

        ObjectInputStream objectStream;

        try {
            objectStream = new ObjectInputStream(fileStream);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readFavoritePodcasts: No Podcasts to read.");
            try {fileStream.close();} catch (Exception ex) {}
            return favoritePodcasts;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readFavoritePodcasts: Exception " + e.getMessage());
            e.printStackTrace();
            try {fileStream.close();} catch (Exception ex) {}
            return favoritePodcasts;
        }

        Object p = null;

        try {
            while ((p=objectStream.readObject()) != null) {
                if (!favoritePodcasts.add((Podcast)p))
                    Log.getInstance().printStackTrace();
        }

        } catch(EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "readFavoritePodcasts complete.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "readFavoritePodcasts: Objects in DB are invalid.");
            ic.printStackTrace();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readFavoritePodcasts exception " + e.getMessage());
            e.printStackTrace();
        }

        try {
            objectStream.close();
            fileStream.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readFavoritePodcasts. Exception closing. " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "readFavoritePodcasts: found " + favoritePodcasts.size());

        // Update the cache.
        PodcastCache = favoritePodcasts;
        cacheDate = new Date();
        cacheIsDirty = false;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readFavoritePodcasts updated Podcast cache.");

        return favoritePodcasts;
    }

    /*
     * Convenience method for API class. A return of null signifies that the cache in the API class is still valid.
     */
    public synchronized static List<Podcast> readFavoritePodcasts(Date clientCacheDate) {
        if (clientCacheDate.compareTo(cacheDate) < 0) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "readFavoritePodcasts Date cache needs to be updated.");
            return readFavoritePodcasts();
        }

        return null;
    }

    //public synchronized static List<Podcast> readFavoritePodcasts(boolean fromCache) {
       // return readFavoritePodcasts(fromCache);
    //}


    /**
    * Saves the Favorite Podcasts to disk.
    * <p>
     * @param A List of Podcasts.
    * @return   true if the operation succeded, false otherwise.
    */
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
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "writeFavoritePodcasts: Error creating new FavoriteDB file.");
                return false;
            }
        }

        // Write the new database file.
        try {
            FileOutputStream fileStream = new FileOutputStream(FavoriteDB);
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);

            // Write all Podcasts to disk.
            for (Podcast p : favoritePodcasts) {
                objectStream.writeObject(p);
            }

            objectStream.close();
            fileStream.close();

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "writeFavoritePodcasts Exception.");;
            return false;
        }

        cacheIsDirty = true;
        return true;
    }

    /*
     * ***************************************
     * Methods that have to do with Episodes.*
     * ***************************************
     */

    /**
     * Gets an array of Episodes that are on the web server.  Does NOT download them, just gets the RSSItems.
     * <p>
     * @return A List of the episodes that are available on the web.
     */
    public List<UnrecordedEpisode> getEpisodesOnWebServer() {

        // Make sure in here we set the proper variables in the Episode object.

        // Make sure we follow URL redirects.
        RSSHelper.setRedirects();

        // Get the RSSItems for this Podcast.
        List<RSSItem> RSSItems = this.getRSSItems();

        // Abort if error.
        if (RSSItems == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getEpisodesOnWebServer Error getting RSSItems for Podcast.");
            return null;
        }

        List<UnrecordedEpisode> unrecorded = new ArrayList<UnrecordedEpisode>();

        // Loop through all of the RSSItems.
        for (RSSItem Item : RSSItems) {

            // Create a new UnrecordedEpisode.
            UnrecordedEpisode episode = new UnrecordedEpisode(this, Item);

            // Add it the the List.
            if (!unrecorded.add(episode))
                Log.getInstance().printStackTrace();
        }

        setEpisodesOnWebServerInDatabase(unrecorded);

        return unrecorded;
    }

    public synchronized boolean setEpisodesOnWebServerInDatabase(List<UnrecordedEpisode> unrecorded) {

        if (unrecorded==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast: null parameter.");
            Log.getInstance().printStackTrace();
            return false;
        }

        List<Podcast> Favorites = readFavoritePodcasts();

        for (Podcast p : Favorites) {
            if (p.equals(this)) {
                p.episodesOnServer = unrecorded;
                return writeFavoritePodcasts(Favorites);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setEpisodesOnWebServerInDatabase: Did not find Podcast.");
        return false;
    }

    /**
     * Gets the Episodes that are curerntly on the filesystem.
     * <p>
     * @return An List of the episodes on the filesystem. A zero sized list if there are none.
     */
    public List<Episode> getEpisodesOnDisk() {

        List<Episode> OnDisk = new ArrayList<Episode>();

        if (episodesEverRecorded == null || episodesEverRecorded.size()==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getEpisodeOnDisk: No episodesEverRecorded.");
            return OnDisk;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getEpisodeOnDisk: episodesEverRecorded = " + episodesEverRecorded.size());

        for (Episode e : episodesEverRecorded) {
            if (e.isOnDisk()) {
                if (!OnDisk.add(e))
                    Log.getInstance().printStackTrace();
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getEpisodeOnDisk: Found = " + OnDisk.size());
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
            duplicatesDeleted++;
            deleted++;
        }

        setDuplicatesDeletedInDatabase(duplicatesDeleted);
        return deleted;
    }

    public synchronized boolean setDuplicatesDeletedInDatabase(int duplicatesDeleted) {

        List<Podcast> Favorites = readFavoritePodcasts();

        for (Podcast p : Favorites) {
            if (p.equals(this)) {
                p.duplicatesDeleted = duplicatesDeleted;
                return writeFavoritePodcasts(Favorites);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setDuplicatesDeletedInDatabase: Did not find Podcast.");
        return false;
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

        // Make sure we have Episodes to process.
        if (episodes == null || episodes.size()==0) {
            return 0;
        }

        // Get a sorted list of episodes.
        episodes = Episode.sortByDateRecorded(episodes, !KeepNewest);

        // Make sure we have Episodes to process.
        if (episodes == null || episodes.size()==0) {
            return 0;
        }

        // Loop through all of the sorted episodes.
        for (Episode episode : episodes) {

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
                if (!e.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM deleteDuplcateEpisodes: Error deleting.");
                }

                return 1;
            }
        }

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
    private List<RSSItem> getRSSItems() {
        return RSSHelper.getRSSItems(feedContext);
    }

    private List<RSSItem> OLDgetRSSItems() {

        // Create the new RSSHandler and check for error.
        RSSHandler hand = new RSSHandler();

        String SearchURL = this.feedContext.toLowerCase();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: SearchURL = " + SearchURL);

        if (SearchURL == null || SearchURL.length() == 0 || SearchURL.startsWith("xurlnone")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: Bad SearchURL");
            return null;
        }

        if (SearchURL.startsWith("external")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: Parsing external feed.");
            String FeedParts[] = SearchURL.split(",",3);

            String FeedEXE = null;
            String FeedParamList[] = null;

            switch (FeedParts.length) {
                case 2:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: FeedEXE = " + FeedEXE);
                    break;
                case 3:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: FeedEXE = " + FeedEXE);
                    String FeedParam = FeedParts[2];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: FeedParam = " + FeedParam);
                    if (FeedParam.length() > 0) {
                        FeedParamList = FeedParam.split("\\|\\|");
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: Found parameters.");
                    }
                    break;
                default:
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: Bad SearchURL = " + SearchURL);
                    return null;
            }

            String feedText = Utility.ExecuteProcessReturnOutput(FeedEXE, FeedParamList, null, true, true);

            if (feedText.length() == 0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: No results from ExecuteProcess.");
                return null;
            }

            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems:  UPnP2Podcast not implemented.");
            Log.getInstance().printStackTrace();
            return null;
        }

        // Create the new URL from the String and check for errors.
        URL url;
        try {
            url = new URL(SearchURL);
            if (!(url instanceof URL)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: Bad new url");
                Log.getInstance().printStackTrace();
                return null;
            }
        } catch (MalformedURLException urle) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: null URL.");
            return null;
        }


        // Create the new Parser and check for errors.
        RSSParser parser = new RSSParser();
        if (!(parser instanceof RSSParser)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: Bad new parser.");
            return null;
        }

        // Parse the XML file pointed to be the URL.
        try {
            parser.parseXmlFile(url, hand, false);
        } catch (RSSException rsse) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: Exception parsing URL. " + rsse.getMessage());
            return null;
        }

        // Create the new Channel and check for errors.
        RSSChannel rsschan = new RSSChannel();
        if (!(rsschan instanceof RSSChannel)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: Bad new chan.");
            return null;
        }

        // Get the Channel for this handle.
        rsschan = hand.getRSSChannel();
        if (rsschan == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM getRSSItems: null chan.");
            return null;
        }

        // Get the RSSItems in a LinkedList.
        LinkedList<RSSItem> ChanItems = new LinkedList<RSSItem>();
        ChanItems = rsschan.getItems();

        // Create the List to hold the results.
        List<RSSItem> ItemArray = new ArrayList<RSSItem>();

        // Loop through all the ChanItems and convert to a List.
        for (RSSItem item : ChanItems) {
            if (!ItemArray.add(item))
                Log.getInstance().printStackTrace();
        }

        // Done at last.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM getRSSItems: Returning ChanItems = " + ItemArray.size());
        return ItemArray;
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

    public List<UnrecordedEpisode> getEpisodesOnServer() {
        return episodesOnServer;
    }

    public String getFeedContext() {
        return feedContext;
    }

    public boolean isIsFavorite() {
        return isFavorite;
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
    }

    public void setDeleteDuplicates(boolean deleteDuplicates) {
        this.deleteDuplicates = deleteDuplicates;
    }

    public void setDuplicatesDeleted(int duplicatesDeleted) {
        this.duplicatesDeleted = duplicatesDeleted;
    }

    public void addEpisodeEverRecorded(Episode episode) {
        if (!episodesEverRecorded.contains(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Adding episodeEverRecorded " + episode.getShowTitle());
            if (!episodesEverRecorded.add(episode)) Log.getInstance().printStackTrace();
            setEpisodeRecordedInDatabase(episode);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode has already been recorded " + episode.getShowTitle());
        }
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void setKeepNewest(boolean keepNewest) {
        this.keepNewest = keepNewest;
    }

    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
    }

    public void setMaxToRecord(int maxToRecord) {
        this.maxToRecord = maxToRecord;
    }

    public void setOnlineVideoItem(String onlineVideoItem) {
        this.onlineVideoItem = onlineVideoItem;
    }

    public void setOnlineVideoType(String onlineVideoType) {
        this.onlineVideoType = onlineVideoType;
    }

    public void setReRecordDeleted(boolean reRecordDeleted) {
        this.reRecordDeleted = reRecordDeleted;
    }

    public void setRecDir(String recDir) {
        this.recDir = recDir;
    }

    public void setRecSubdir(String recSubdir) {
        this.recSubdir = recSubdir;
    }

    public void setRecordNew(boolean recordNew) {
        this.recordNew = recordNew;
    }

    public void setShowTitle(String showTitle) {
        this.showTitle = showTitle;
    }

    public void setUseShowTitleAsSubdir(boolean useShowTitleAsSubdir) {
        this.useShowTitleAsSubdir = useShowTitleAsSubdir;
    }

    public void setUseShowTitleInFileName(boolean useShowTitleInFileName) {
        this.useShowTitleInFileName = useShowTitleInFileName;
    }

}
