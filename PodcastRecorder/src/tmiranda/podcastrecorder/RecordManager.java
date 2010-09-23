/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class RecordManager extends TimerTask {

    private static boolean isRunning = false;

    public RecordManager() {
        isRunning = false;
    }

    /**
     * This is the thread that looks for new Episodes to record.
     */
    public void run() {

        Thread.currentThread().setName("RecordManager");
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Starting RecordManager thread.");

        if (isRunning) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "SRM: Terminating because RecordManager is already running.");
            return;
        } else {
            isRunning = true;
        }

        // Get the favorite Podcasts from the database.
        List<Podcast> FavoritePodcasts = Podcast.readFavoritePodcasts();

        // Nothing to do if there are no favorites.
        if (FavoritePodcasts == null || FavoritePodcasts.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: No Favorite podcasts defined.");
            isRunning = false;
            return;
        }

        Podcast dummy = new Podcast();
        dummy.dumpFavorites();

        // Loop through all of the Podcast objects.
        for (Podcast podcast : FavoritePodcasts) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Processing Favorite podcast " + podcast.getOnlineVideoType() + ":" + podcast.getOnlineVideoItem());

            // There may already be too many episodes, but we will ignore that.

            // See if we need to check for new episodes on the server.
            if (podcast.isRecordNew() && podcast.isIsFavorite()) {

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Podcast is set to record new episodes.");

                // Set lastchecked to now.
                Long t = Utility.Time();
                podcast.setLastCheckedInDatabase(t);

                // See if we need to delete duplcates.
                if (podcast.isDeleteDuplicates()) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Checking for duplicates.");
                    podcast.deleteDuplicateEpisodes(podcast.isKeepNewest());
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Podcast is not set to delete duplicates.");
                }

                // Get the Episodes for this podcast that are on the web server.
                List<UnrecordedEpisode> EpisodesOnServer = podcast.getEpisodesOnWebServer();

                if (EpisodesOnServer==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: getEpisodeOnServer failed.");
                    EpisodesOnServer = new ArrayList<UnrecordedEpisode>();
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Found Episodes on web server = " + EpisodesOnServer.size());

                for (UnrecordedEpisode episode : EpisodesOnServer)
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, " -> " + episode.getEpisodeTitle());

                // No need to continue if no episodes are available.
                if (EpisodesOnServer.size() > 0) {

                    // Do the preparation that will be needed if we have to delete
                    // episodes that are already recorded.  We need to get them ordered
                    // oldest watched to newest watched followed by oldest unwatched to newest
                    // unwatched.

                    // Get all of the Episodes on the filesystem.
                    List<Episode> RecordedEpisodes = podcast.getEpisodesOnDisk();

                    if (RecordedEpisodes==null) {
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: getEpisodesOnDisk failed.");
                        RecordedEpisodes = new ArrayList<Episode>();
                    }

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Found episodes currently recorded = " + RecordedEpisodes.size());
                    for (Episode episode : RecordedEpisodes)
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, episode.getShowTitle() + ":" + episode.getShowEpisode());

                    // Sort RecordedEpisodes from oldest to newest.
                    RecordedEpisodes = Episode.sortByDateRecorded(RecordedEpisodes, false);

                    // Move all of the watched shows to the front.
                    List<Episode> WatchedEpisodes = Episode.filterByWatchedCompletely(RecordedEpisodes, true);
                    List<Episode> UnwatchedEpisodes = Episode.filterByWatchedCompletely(RecordedEpisodes, false);

                    RecordedEpisodes = WatchedEpisodes;
                    if (!RecordedEpisodes.addAll(UnwatchedEpisodes))
                        Log.getInstance().printStackTrace();

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Currently recorded after sort and filter = " + RecordedEpisodes.size());
                    for (Episode episode : RecordedEpisodes)
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, episode.getShowTitle() + ":" + episode.getShowEpisode());

                    // Remove reference so the GC can do its thing as needed.
                    WatchedEpisodes = null;
                    UnwatchedEpisodes = null;

                    // Filter the episodes by unrecorded.
                    List<UnrecordedEpisode> UnrecordedEpisodes = UnrecordedEpisode.filterByOnDisk(EpisodesOnServer, false);

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Found UnrecordedEpisodes = " + UnrecordedEpisodes.size());

                    for (UnrecordedEpisode episode : UnrecordedEpisodes)
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, episode.getEpisodeTitle() + " - " + episode.getCleanDescription());

                    // If we don't want to re-record deleted, remove them from the list.
                    if (!podcast.isReRecordDeleted()) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Filtering deleted episodes.");

                        // Filter out episodes that have already been recorded. (Return episodes that have never been recorded.)
                        UnrecordedEpisodes = UnrecordedEpisode.filterByEverRecorded(UnrecordedEpisodes, false);

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: UnrecordedEpisodes after Rerecord filter = " + UnrecordedEpisodes.size());
                        for (UnrecordedEpisode episode : UnrecordedEpisodes)
                            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, episode.getEpisodeTitle());
                    }

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Start recording Episodes.");

                    // Start recording.
                    while (UnrecordedEpisodes.size() > 0 && podcast.canRecordNew()) {

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Getting Episode from list.");

                        // Get the first one in the unrecorded list.
                        UnrecordedEpisode EpisodeToRecord = UnrecordedEpisodes.get(0);
                        if (!UnrecordedEpisodes.remove(EpisodeToRecord)) Log.getInstance().printStackTrace();

                        Log.getInstance().write(Log.LOGLEVEL_WARN,"SRM: Record Episode " + EpisodeToRecord.getEpisodeTitle());

                        // Record it.
                        Episode NewEpisode = EpisodeToRecord.record();

                        // Make sure it recorded OK.
                        if (NewEpisode != null) {

                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Recording complete.");

                            // Add this Episode to the recorded episode list.
                            // This is done after recoding completes.
                            //podcast.setEpisodeRecordedInDatabase(NewEpisode);

                            // See if we need to delete any episode now that a new episode has recorded.
                            if (podcast.needToDelete()) {

                                // Delete the first RecordedEpisode.
                                if (RecordedEpisodes.get(0).delete()) {
                                    RecordedEpisodes.remove(0);
                                } else {
                                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Failed to delete Episode.");
                                }
                            }
                        } else {
                            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Failed to record episode.");
                        }
                    }

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Recordings completed.");

                    // Show a warning if there are still unrecorded episodes.
                    if (UnrecordedEpisodes.size() > 0) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Unrecorded episodes that are still on the server: " + UnrecordedEpisodes.size());
                    }
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: No episodes on the server.");
                }
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Podcast is not set to record new episodes.");
            }
        }

        isRunning = false;
        return;
    }
}
