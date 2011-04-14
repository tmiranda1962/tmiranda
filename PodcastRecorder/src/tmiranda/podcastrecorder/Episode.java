
package tmiranda.podcastrecorder;

import java.util.*;
import java.io.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 * <p>
 * This class represents recorded episodes for Podcasts.  The Episode may or may not be physically
 * on the disk (filesystem).
 */
 public class Episode implements Serializable {

    private static final long serialVersionUID = 1L;

    Podcast podcast;            // The Podcast that contains this Episode.
    String  ID;                 // A unique String that identifies each Episode.
    int     AiringID;           // The ID of the Airing corresponding to this Episode.
                                // - 0 means uninitialized, -1 means it's never been downloaded
                                //   and the user marked it as downloaded.

    /**
     * Constructor for an Episode object.
     */
    public Episode(Podcast OwningPodcast, String EpID) {
        podcast = OwningPodcast;
        ID = EpID;
        AiringID = 0;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Episode: FeedContext=" + podcast.getFeedContext());
    }

    public Episode(EpisodeData e) {
        podcast = e.podcast;
        ID = e.ID;
        AiringID = e.AiringID;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Episode: FeedContext=" + podcast.getFeedContext());
    }

    public Podcast getPodcast() {
        return podcast;
    }

    /**
     * Gets the Show Title of a recorded Episode.
     * <p>
     * @return The Show Title.
     */
    public String getShowTitle() {

        if (AiringID == -1)
            return podcast.getShowTitle();
        
        // Try to get the title from the Airing.
        if (AiringID > 0) {
            Object Airing = AiringAPI.GetAiringForID(AiringID);
            if (Airing != null) {
                return ShowAPI.GetShowTitle(Airing);
            }
        }

        // Try to get the show title from the MediaFile.  If it's null it means the Episode
        // is probably deleted so get the title from the Podcast.
        Object MF = this.fetchMediaFile();
        if (MF!=null)
            return ShowAPI.GetShowTitle(MF);
        else
            //return null;
            return podcast.getShowTitle();
    }

    /**
     * Gets the Episode Title of a recorded Episode.
     * <p>
     * @return The title.
     */
    public String getShowEpisode() {
        Object MF = this.fetchMediaFile();
        if (MF==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Episode.getShowEpisode: null MediaFile.");
            return null;
        }

        return ShowAPI.GetShowEpisode(MF);
    }


    /**
     * Checks to see if the Episode is physically on the disk. Note that if the file is successfully
     * downloaded but it is 0 bytes long, getMediaFile() will return null.  We try to avoid this
     * situation by not importing 0 length downloads into the Sage database.
     * <p>
     * @return true if the Episode is on the disk, false otherwise.
     */
    public boolean isOnDisk() {
        if (this.fetchMediaFile() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks to see if the Episode has been deleted.
     * <p>
     * @return true if it has been deleted, false if not.  Note that if the Episode has never been recorded this
     * method will return false.
     */
    public boolean hasBeenDeleted() {

        // If it's on the disk it's not deleted.
        if (this.isOnDisk()) {
            return false;
        }

        // If it has been recorded, but it's not on the disk, it's been deleted.
        if (this.hasBeenRecorded()) {
            return true;
        }

        // It's not on the disk and it's never been recorded.
        return false;
    }

    /**
     * Checks to see if the Episode has EVER been recorded.  The method does not check to see if the Episode is
     * currently on the disk.
     * <p>
     * @return true if the Episode has ever been recorded, false otherwise.
     */
    private boolean hasBeenRecorded() {

        Set<Episode> recorded = podcast.getEpisodesEverRecorded();

        if (recorded==null) {
            return false;
        }

        return recorded.contains(this);
    }

    /**
     * Checks if the Episode is completely watched.
     * <p>
     * @return true if the Episode is completely watched, false otherwise.
     */
    public boolean isWatchedCompletely() {

        Object MediaFile = this.fetchMediaFile();

        if (MediaFile==null) {
            return false;
        }

        return AiringAPI.IsWatchedCompletely(MediaFile);
    }

    public boolean isFavorite() {
        return podcast.isFavorite();
    }

    public String getID() {
        return ID;
    }

    public void setID(String NewID) {
        ID = NewID;
    }

    public void setAiringID(int AirID) {
        AiringID = AirID;
    }


    /*
     * *****************
     * Sorting methods.*
     * *****************
     */


    /**
     * Sort a List of Episodes by date recorded.
     * <p>
     * @param episodes A List of Episodes to be sorted.
     * @param descending false to sort from oldest to newest, true to sort from newest to oldest.
     * @return A sorted List or null if error.
     */
    public static List<Episode> sortByDateRecorded(List<Episode> episodes, boolean descending) {

        Collections.sort(episodes, new DateRecorded());

        if (descending)
            Collections.reverse(episodes);

        return episodes;
    }


    /**
     * Gets the date that the Episode was recorded.
     * <p>
     * @return The original recording date, in ms since 1/1/1970.
     */
    public long getDateRecorded() {
        Object MediaFile = this.fetchMediaFile();

        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Episode.getDateRecorded: MediaFile not found.");
            return 0L;
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "Episode.getDateRecorded: Found MediaFile.");
        return ShowAPI.GetOriginalAiringDate(MediaFile);
    }


    /**
     * Filter a List of Episodes by WatchedCompletely.
     * <p>
     * @param episodes The List of unfiltered Episodes.
     * @param watched true to return those Episodes that are WatchedCompletely, false to return those that are
     * not WatchedCompletely.
     * @return The filtered List.
     */
    public static List<Episode> filterByWatchedCompletely(List<Episode> episodes, boolean watched) {

        List<Episode> FilteredEpisodes = new ArrayList<Episode>();

        for (Episode episode : episodes) {
            if (episode.isWatchedCompletely() == watched) {
                if (!FilteredEpisodes.add(episode))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.getDateRecorded: Element already in set.");
            }
        }

        return FilteredEpisodes;
    }

    /**
     * Delete the Episode from the filesystem.
     * <p>
     * @return true if it was successfully deleted, false otherwise.
     */
    public boolean delete() {

        Object MediaFile = this.fetchMediaFile();

        if (MediaFile == null) {
            return false;
        }

        return MediaFileAPI.DeleteFile(MediaFile);
    }

    public Object findAiring() {

        if (AiringID == -1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: Episode manually marked as recorded.");
            return null;
        }

        if (AiringID > 0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: Returning Airing for AiringID.");
            return AiringAPI.GetAiringForID(AiringID);
        }

        Object MediaFile = fetchMediaFile();

        if (MediaFile != null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: Returning Airing for MediaFile.");
            Object Airing = MediaFileAPI.GetMediaFileAiring(MediaFile);
            AiringID = AiringAPI.GetAiringID(Airing);
            return Airing;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: No MediaFile.  Manually searching for Airing.");

        Object[] allAirings = getAllSageAirings();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: All Airings " + allAirings.length);

        String OVT = podcast.getOnlineVideoType();
        String OVI = podcast.getOnlineVideoItem();
        String FeedContext = podcast.getFeedContext();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: OVT:OVI:FeedContext " + OVT + " : " + OVI + " : " + FeedContext);

        for (Object Airing : allAirings) {
            String thisOVT = AiringAPI.GetManualRecordProperty(Airing, RecordingEpisode.METADATA_OVT);
            String thisOVI = AiringAPI.GetManualRecordProperty(Airing, RecordingEpisode.METADATA_OVI);
            String thisFeedContext = AiringAPI.GetManualRecordProperty(Airing, RecordingEpisode.METADATA_FEEDCONTEXT);

            if (OVT.equals(thisOVT) && OVI.equals(thisOVI) && FeedContext.equals(thisFeedContext)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: Found Airing.");
                AiringID = AiringAPI.GetAiringID(Airing);
                return Airing;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode.findAiring: Did not find a matching Airing.");
        return null;
    }

    static Object[] getAllSageAirings() {
        Vector V = Database.SearchSelectedFields(" ", false, true, true, true, true, true, true, true, true, true);

        if (V==null || V.isEmpty())
            return null;
        else
            return V.toArray();
    }


    /**
    * Returns a MediaFile object corresponding to an Episode.
    * <p>
    * @return       MediaFile object corresponding to the Podcast ID or null if it does not exist.
    */
    public Object fetchMediaFile() {
        Object[] MediaFilesAll = MediaFileAPI.GetMediaFiles("VM");

        if (MediaFilesAll==null || MediaFilesAll.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Episode.getMediaFile: null or 0 size MediaFilesAll.");
            return null;
        }

        for (Object MF : MediaFilesAll) {
            String Misc = ShowAPI.GetShowMisc(MF);
            if (Misc!=null && this.ID.compareTo(Misc)==0) {
                return MF;
            }
        }

        // Log.getInstance().write(Log.LOGLEVEL_ALL, "getMediaFile: Failed to find MediaFile.");
        return null;
    }


    /**
     * This method prints all of the Episodes that are currently on the disk to the log file.
     * <p>
     */
    public static void DumpAllEpisodesOnDisk() {

        Object[] MediaFilesAll = MediaFileAPI.GetMediaFiles("VM");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Episode: Dumping all Episodes:");

        for (Object MF : MediaFilesAll) {
            if (ShowAPI.GetShowExternalID(MF).startsWith("ONL")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "*** Found " + ShowAPI.GetShowTitle(MF) + ":" + ShowAPI.GetShowEpisode(MF));
            }
        }

        return;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Episode other = (Episode) obj;
        if (this.podcast != other.podcast && (this.podcast == null || !this.podcast.equals(other.podcast))) {
            return false;
        }
        if ((this.ID == null) ? (other.ID != null) : !this.ID.equals(other.ID)) {
            return false;
        }

System.out.println("EPISODES ARE EQUAL.");
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.podcast != null ? this.podcast.hashCode() : 0);
        hash = 97 * hash + (this.ID != null ? this.ID.hashCode() : 0);
        return hash;
    }

}


/**
 * Comparator that compares two Episodes by the date they were recorded.
 * <p>
 * @author Tom Miranda.
 */
class DateRecorded implements Comparator<Episode> {
    @Override
    public int compare(Episode Ep1, Episode Ep2) {
        long d1 = Ep1.getDateRecorded();
        long d2 = Ep2.getDateRecorded();

        if (d1 > d2) return 1;
        else if (d1 < d2) return -1;
        else return 0;
    }
}