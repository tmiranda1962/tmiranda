/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class PodcastData implements Serializable {
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
}
