/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class PodcastData implements Serializable {

    Set<UnrecordedEpisodeData>  episodesOnWebServer = null;
    Set<EpisodeData>            episodesEverRecorded = null;

    boolean recordNew = false;
    boolean isFavorite = false;
    boolean deleteDuplicates = false;
    boolean keepNewest = true;
    boolean reRecordDeleted = false;
    int     maxToRecord = 0;
    boolean autoDelete = false;
    long    lastChecked = 0L;
    String  recDir = null;
    String  recSubdir = null;
    String  showTitle = null;
    String  onlineVideoType = null;
    String  onlineVideoItem = null;
    String  feedContext = null;
    boolean useShowTitleAsSubdir = false;
    boolean useShowTitleInFileName = false;
    int     duplicatesDeleted = 0;

    /**
     * Constructor - Creates a new PodcastData Object suitable for serialization.
     * @param p A Podcast Object.
     */
    public PodcastData(Podcast p) {
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

        episodesOnWebServer     = new HashSet<UnrecordedEpisodeData>();
        episodesEverRecorded    = new HashSet<EpisodeData>();

        if (p.episodesOnWebServer != null) {
            for (UnrecordedEpisode e : p.episodesOnWebServer) {
                UnrecordedEpisodeData eData = new UnrecordedEpisodeData(e);
                episodesOnWebServer.add(eData);
            }
        }

        if (p.episodesEverRecorded != null) {
            for (Episode e : p.episodesEverRecorded) {
                EpisodeData eData = new EpisodeData(e);
                episodesEverRecorded.add(eData);
            }
        }
    }
}
