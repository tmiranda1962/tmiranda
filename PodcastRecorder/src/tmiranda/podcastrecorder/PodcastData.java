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

    boolean recordNew               = false;
    boolean isFavorite              = false;
    boolean deleteDuplicates        = false;
    boolean keepNewest              = true;
    boolean reRecordDeleted         = false;
    int     maxToRecord             = 0;
    boolean autoDelete              = false;
    long    lastChecked             = 0L;
    String  recDir                  = null;
    String  recSubdir               = null;
    String  showTitle               = null;
    String  onlineVideoType         = null;
    String  onlineVideoItem         = null;
    String  feedContext             = null;
    boolean useShowTitleAsSubdir    = false;
    boolean useShowTitleInFileName  = false;
    int     duplicatesDeleted       = 0;

    /**
     * Constructor - Creates a new PodcastData Object suitable for serialization.
     * @param p A Podcast Object.
     */
    public PodcastData(Podcast p) {
        recordNew               = p.isRecordNew();
        isFavorite              = p.isFavorite();
        deleteDuplicates        = p.isDeleteDuplicates();
        keepNewest              = p.isKeepNewest();
        reRecordDeleted         = p.isReRecordDeleted();
        maxToRecord             = p.getMaxToRecord();
        autoDelete              = p.isAutoDelete();
        lastChecked             = p.getLastChecked();
        recDir                  = p.getRecDir();
        recSubdir               = p.getRecSubdir();
        showTitle               = p.getShowTitle();
        onlineVideoType         = p.getOnlineVideoType();
        onlineVideoItem         = p.getOnlineVideoItem();
        feedContext             = p.getFeedContext();
        useShowTitleAsSubdir    = p.isUseShowTitleAsSubdir();
        useShowTitleInFileName  = p.isUseShowTitleInFileName();
        duplicatesDeleted       = p.getDuplicatesDeleted();

        episodesOnWebServer     = new HashSet<UnrecordedEpisodeData>();
        episodesEverRecorded    = new HashSet<EpisodeData>();

        if (p.getEpisodesOnWebServer() != null) {
            for (UnrecordedEpisode e : p.getEpisodesOnWebServer()) {
                UnrecordedEpisodeData eData = new UnrecordedEpisodeData(e);
                episodesOnWebServer.add(eData);
            }
        }

        if (p.getEpisodesEverRecorded() != null) {
            for (Episode e : p.getEpisodesEverRecorded()) {
                EpisodeData eData = new EpisodeData(e);
                episodesEverRecorded.add(eData);
            }
        }
    }
}
