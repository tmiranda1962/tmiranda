
package tmiranda.podcastrecorder;

import java.util.*;
import sagex.api.*;

/**
 * This class is responsible for downloading Podcasts that have been defined as Favorites. It
 * enforces all recording limits
 * @author Tom Miranda.
 */
public class RecordManager extends TimerTask {

    private static boolean isRunning = false;

    public RecordManager() {}

    /**
     * This is the thread that looks for new Episodes to record.
     */
    @Override
    public void run() {

        try {

            Thread.currentThread().setName("RecordManager");
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Starting RecordManager thread.");

            //showDatabaseInfo("thread start");

            if (isRunning) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "SRM: Terminating because RecordManager is already running.");
                return;
            }

            isRunning = true;
            DownloadManager.getInstance().setRecMgrStatus(isRunning);

            // Update the database with any manual recordings that were made since the last run.
            //DownloadManager.getInstance().updateDatabase();

            //showDatabaseInfo("after updateDatabase");

            // Get the Podcasts from the database.
            List<Podcast> podcasts = DataStore.getAllPodcasts();
            //List<PodcastKey> podcastKeys = DataStore.getAllPodcastKeys();

            // Nothing to do if there are no favorites.
            if (podcasts == null || podcasts.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: No Favorite podcasts defined.");
                isRunning = false;
                DownloadManager.getInstance().setRecMgrStatus(isRunning);
                return;
            }

            Podcast dummy = new Podcast();
            dummy.dumpFavorites();

            // Loop through all of the Podcast objects.
            for (Podcast podcast : podcasts) {

                if (podcast==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Error reading Podcast.");
                    continue;
                }

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Processing Favorite podcast " + podcast.getOnlineVideoType() + ":" + podcast.getOnlineVideoItem());

                // There may already be too many episodes, but we will ignore that.

                // See if we need to check for new episodes on the server.
                if (podcast.isRecordNew() && podcast.isFavorite()) {

                    //showDatabaseInfo("record new for Favorite");

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Podcast is set to record new episodes.");

                    // Set lastchecked to now.
                    Long t = Utility.Time();
                    podcast.setLastChecked(t);

                    // See if we need to delete duplcates.
                    if (podcast.isDeleteDuplicates()) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Checking for duplicates.");
                        podcast.deleteDuplicateEpisodes(podcast.isKeepNewest());
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Podcast is not set to delete duplicates.");
                    }

                    //showDatabaseInfo("before getEpisodesOnWebServer");

                    // Update the Podcast with the latest info from the Web.
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Updating with latest Episode information.");
                    podcast.updateEpisodesOnWebServerAndEverRecorded();

                    // Get the Episodes for this podcast that are on the web server.
                    Set<UnrecordedEpisode> EpisodesOnServer = podcast.getEpisodesOnWebServer();

                    if (EpisodesOnServer==null) {
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: getEpisodeOnServer failed.");
                        EpisodesOnServer = new HashSet<UnrecordedEpisode>();
                    }

                    //showDatabaseInfo("after getEpisodesOnWebServer");

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Found Episodes on web server = " + EpisodesOnServer.size());

                    for (UnrecordedEpisode episode : EpisodesOnServer)
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, " -> " + episode.getEpisodeTitle());

                    // No need to continue if no episodes are available.
                    if (EpisodesOnServer.size() > 0) {

                        //showDatabaseInfo("still have spisodes");

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

                        //showDatabaseInfo("after getEpisodesOnDisk");

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Found episodes currently recorded = " + RecordedEpisodes.size());
                        for (Episode episode : RecordedEpisodes)
                            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, episode.getShowTitle() + ":" + episode.getShowEpisode());

                        // Sort RecordedEpisodes from oldest to newest.
                        RecordedEpisodes = Episode.sortByDateRecorded(RecordedEpisodes, false);

                        //showDatabaseInfo("after soerByDateRecorded");

                        // Move all of the watched shows to the front.
                        List<Episode> WatchedEpisodes = Episode.filterByWatchedCompletely(RecordedEpisodes, true);
                        List<Episode> UnwatchedEpisodes = Episode.filterByWatchedCompletely(RecordedEpisodes, false);

                        RecordedEpisodes = WatchedEpisodes;
                        if (!UnwatchedEpisodes.isEmpty() && !RecordedEpisodes.addAll(UnwatchedEpisodes))
                            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Error adding all.");

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Currently recorded after sort and filter = " + RecordedEpisodes.size());
                        for (Episode episode : RecordedEpisodes)
                            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, episode.getShowTitle() + ":" + episode.getShowEpisode());

                        // Remove reference so the GC can do its thing as needed.
                        WatchedEpisodes = null;
                        UnwatchedEpisodes = null;

                        //showDatabaseInfo("before filterByOnDisk");

                        // Filter the episodes by unrecorded.
                        Set<UnrecordedEpisode> UnrecordedEpisodes = UnrecordedEpisode.filterByOnDisk(EpisodesOnServer, false);

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

                        //showDatabaseInfo("just before recording");

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Start recording Episodes.");

                        // Start recording.
                        boolean recordedAShow = false;
                        String ShowName = null;

                        while (UnrecordedEpisodes.size() > 0 && podcast.canRecordNew()) {

                            //showDatabaseInfo("can record new");

                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Getting Episode from list.");

                            // Get the first one in the unrecorded list.
                            Iterator<UnrecordedEpisode> i = UnrecordedEpisodes.iterator();

                            UnrecordedEpisode EpisodeToRecord = i.next();

                            if (!UnrecordedEpisodes.remove(EpisodeToRecord))
                                Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Error removing.");

                            Log.getInstance().write(Log.LOGLEVEL_WARN,"SRM: Record Episode " + EpisodeToRecord.getEpisodeTitle());

                            //showDatabaseInfo("before record");

                            // Record it.
                            Episode NewEpisode = EpisodeToRecord.record();

                            //showDatabaseInfo("after record");

                            // Make sure it recorded OK.
                            if (NewEpisode != null) {

                                recordedAShow = true;
                                ShowName = NewEpisode.getShowTitle();

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
                                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Failed to get Episode.");
                                    }
                                }
                            } else {
                                Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Failed to record episode.");
                            }
                        }

                        // Optionally display a system message if any shows were recorded.
                        if (recordedAShow && SageUtil.GetBoolProperty(Plugin.PROPERTY_MESSAGE_AFTER_RECORD, false)) {
                            //Properties props = new Properties();
                            //props.setProperty("ShowName", ShowName);
                            SystemMessageAPI.PostSystemMessage(1204, 1, "New recorded Podcasts are available for " + ShowName, null);
                        }

                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Recordings completed.");

                        // Show a warning if there are still unrecorded episodes.
                        if (UnrecordedEpisodes.size() > 0) {
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Unrecorded episodes that are still on the server: " + UnrecordedEpisodes.size());
                        }
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: No episodes on the server.");
                    }
                }

                // See if there are more episodes on the web server for this favorite.
                if (podcast.isFavorite() && SageUtil.GetBoolProperty(Plugin.PROPERTY_MESSAGE_IF_NEW_AVAIL, false)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Checking for new Episodes to see if a system message should be sent.");

                    if (podcast.hasUnrecordedEpisodes()) {
                        SystemMessageAPI.PostSystemMessage(1204, 1, "New Podcasts are available for " + podcast.getShowTitle(), null);
                    }
                }
            }
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SRM: Terminating because of exception " + e.getMessage());
            isRunning = false;
            DownloadManager.getInstance().setRecMgrStatus(isRunning);
            return;
        } finally {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM: Terminating.");
            isRunning = false;
            DownloadManager.getInstance().setRecMgrStatus(isRunning);
            return;
        }
    }

    private void showDatabaseInfo(String place) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RecordManager.showDatabaseInfo: Examining sizes " + place);

        List<Podcast> podcasts = DataStore.getAllPodcasts();

        for (Podcast p : podcasts) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RecordManager.showDatabaseInfo: Show " + p.getShowTitle());
            int onWeb = p.getEpisodesOnWebServerSize();
            int everRecorded = p.getEpisodesEverRecordedSize();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RecordManager.showDatabaseInfo: OnWeb : EverRecorded " + onWeb + ":" + everRecorded);
        }
    }
}
