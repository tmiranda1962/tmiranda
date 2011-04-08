
package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import java.net.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 * This class represents podcast episodes that are still on the internet and have not been recorded.
 *
 * @author Tom Miranda.
 *

 */
 public class UnrecordedEpisode extends Episode implements Serializable {


    /**
     * Constructor.
     * <p>
     * @param p The Podcast to which this UnrecordedEpisode belongs.  Each UnrecordedEpisode has exactly
     * one Podcast but each Podcast can have multiple UnrecordedEpisodes.
     */
    public UnrecordedEpisode(Podcast p, RSSItem Item) {
        super(p, RSSHelper.makeID(Item));
        ChanItem = Item;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "UnrecordedEpisode: FeedContext=" + podcast.getFeedContext());
    }

    public UnrecordedEpisode(Podcast p, UnrecordedEpisodeData e) {
        super(p,RSSHelper.makeID(e.ChanItem));
        ChanItem = e.ChanItem;
        SPRRequestID = e.SPRRequestID;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "UnrecordedEpisode: FeedContext=" + podcast.getFeedContext());
    }

    /*
     **********************
     * Instance variables.*
     * ********************
     */

    RSSItem ChanItem;           // The RSSItem for this particular Episode.
    String SPRRequestID;        // A unique ID supplied by the SagePodcastRecorder.

    /**
     * Get the ChanItem (RSSItem) for this UnrecordedPodcast.
     * <p>
     * @return The RSSItem (ChanItem) for this UnrecordedPodcast.
     */
    public RSSItem getChanItem() {
        return ChanItem;
    }

    /**
     * Sets the default Video URL for this Episode.
     * <p>
     * @return true if success, false otherwise.
     */
    public List<String> setDefaultVideoURL(String OVT, String OVI) {

        List<String> URLs = new ArrayList<String>();

        RSSEnclosure Enclosure = ChanItem.getEnclosure();

        if (Enclosure == null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: null Enclosure.");
        } else {
            String Type = Enclosure.getType();
            if (Type!=null && Type.toLowerCase().contains("sagetv")) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.setDefaultVideoURL: Error - Found SageTV custom Enclosure.");
                return URLs;
            }
        }

        if (OVT.startsWith("xPodcast")) {

            if (OVI.endsWith("_GOO")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found xPodcast _GOO.");
                URLs = handleGoogle();
            } else if (OVI.endsWith("_YTV")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found xPodcast _YTV.");
                URLs = handleYouTube();
            } else if (OVI.endsWith("_YTC")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found xPodcast _YTC.");
                URLs = handleYouTubeChannel();
            } else {

                // Handle a video podcast link.

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found xPodcast " + OVT);
                String URL = (Enclosure==null ? ChanItem.getLink() : Enclosure.getUrl());

                if (URL!=null && !URL.isEmpty()) {
                    if (!URLs.add(URL))
                        Log.printStackTrace();
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: found for xPodcast = " + URL);
                    return URLs;
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.setDefaultVideoURL: null URL for xPodcast.");
                }

            }

        } else if (OVT.startsWith("xYouTube")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found YouTube " + OVT);
            URLs = handleYouTube();
        } else if (OVT.startsWith("xYouTubeChannels")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found YouTube Channel " + OVT);
            URLs = handleYouTubeChannel();
        } else if (OVT.startsWith("xChannelsDotCom")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found Channels.com " + OVT);
            URLs = handleChannelsDotCom(OVI);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.setDefaultVideoURL: Found Google " + OVT);
            URLs = handleGoogle();
        }

        return URLs;
    }

    private List<String> handleChannelsDotCom(String OVI) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleChannelsDotCom: Found Channels.com " + OVI);

        List<String> URLs = new ArrayList<String>();

        if (OVI.startsWith("xChannelsDotComCatList") || OVI.startsWith("xChannelsDotComList")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleChannelsDotCom: Error - Unexpectedly found ChannelsDotComCatList " + OVI);
            return URLs;

        }

        // VideoURL = sage_media_rss_RSSItem_getLink(RSSItem)

        String VideoURL = ChanItem.getLink();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleChannelsDotCom: Found VideoURL from link " + OVI + ":" + VideoURL);
        if (VideoURL==null || VideoURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleChannelsDotCom: null VideoURL from xChannelsDotComFeedContent.");
            return URLs;
        }

        if (!URLs.add(VideoURL))
            Log.printStackTrace();

        return URLs;
    }

    private List<String> handleYouTube() {
        List<String> URLs = new ArrayList<String>();

        RSSEnclosure Enclosure = ChanItem.getEnclosure();

        String VideoID = null;

        if (Enclosure==null) {
            VideoID = ytGetVideoIDFromLink();
            if (VideoID==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: null VideoID after null Enclosure.");
                return URLs;
            }
        } else {
            String URL = Enclosure.getUrl();
            if (URL==null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: null URL from Enclosure.");
                return URLs;
            }

            VideoID = URL.substring(URL.lastIndexOf("/")+1, URL.lastIndexOf("."));
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: VideoID = " + VideoID);
            // VideoID = Substring(EnclosureURL, StringLastIndexOf(EnclosureURL, "/") + 1, StringLastIndexOf(EnclosureURL, "."))

            if (VideoID==null || VideoID.isEmpty()) {
                VideoID = ytGetVideoIDFromLink();
                if (VideoID==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: null VideoID.");
                    return URLs;
                }
            }
        }

        Properties Props = loadProperties();
        if (Props==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: null Props.");
            return URLs;
        }

        String VideoFinderLineStart = Props.getProperty("xYouTube/VideoLinkFinder/LineStart");
        String VideoFinderLinkStart = Props.getProperty("xYouTube/VideoLinkFinder/LinkStart");
        String VideoFinderLinkEnd = Props.getProperty("xYouTube/VideoLinkFinder/LinkEnd");
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: VideoFinders = " + VideoFinderLineStart + "|" + VideoFinderLinkStart + "|" + VideoFinderLinkEnd);

        // PageURL = new_java_net_URL("http://www.youtube.com/watch?v=" + VideoID)
        URL PageURL = null;
        try {
            PageURL = new URL("http://www.youtube.com/watch?v=" + VideoID);
        } catch (MalformedURLException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: malformedURLException " + "http://www.youtube.com/watch?v=" + VideoID);
            return URLs;
        }

        // URLReader = new_java_io_BufferedReader(new_java_io_InputStreamReader(java_net_URL_openStream(PageURL)))

        InputStream is = null;
        try {
            is = PageURL.openStream();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: IOException on openStream " + e.getMessage());
            return URLs;
        }

        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader URLReader = new BufferedReader(isr);

        String tstring = null;
        String Currline = null;

        try {
            while ((Currline=URLReader.readLine())!=null && tstring==null) {

                Log.getInstance().write(Log.LOGLEVEL_ALL, "UnrecordedEpisode.handleYouTube: Currline = " + Currline);

                // playerswfidx = StringIndexOf(CurrLine, VideoFinderLineStart)
                int playerswfidx = Currline.indexOf(VideoFinderLineStart);
                if (playerswfidx != -1) {
                    
                    // CurrLine = Substring(CurrLine, playerswfidx, -1)
                    Currline = Currline.substring(playerswfidx);

                    // playerswfidx = StringIndexOf(CurrLine, VideoFinderLinkStart)
                    playerswfidx = Currline.indexOf(VideoFinderLinkStart);

                    // CurrLine = Substring(CurrLine, playerswfidx + Size(VideoFinderLinkStart), -1)
                    Currline = Currline.substring(playerswfidx + VideoFinderLinkStart.length());

                    // tstring = Substring(CurrLine, 0, StringIndexOf(CurrLine, VideoFinderLinkEnd))
                    tstring = Currline.substring(0, Currline.indexOf(VideoFinderLinkEnd));
                }
            }
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: IOException on readLine " + e.getMessage());
            return URLs;
        }

        if (tstring==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: Failed to find tstring.");
            return URLs;
        }

        // ResString = java_net_URLDecoder_decode(tstring)
        String ResString = null;
        try {
            ResString = URLDecoder.decode(tstring, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleYouTube: Exception decoding tstring " + tstring);
            return URLs;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: ResString = " + ResString);

        // ResList = DataUnion(java_lang_String_split(ResString,","))
        List<String> ResList = Arrays.asList(ResString.split(","));
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: ResList = " + ResList.toArray());


        // GetProperty("online_video/YouTube/default_quality2","xHD1080")
        List<String> ResSkipList = new ArrayList<String>();

        String Quality = Configuration.GetProperty("online_video/YouTube/default_quality2","xHD1080");

        if (Quality.equals("xHD720")) {
            if (!ResSkipList.add("37")) Log.printStackTrace();
        } else if (Quality.equals("xHiRes")) {
            if (!ResSkipList.add("37")) Log.printStackTrace();
            if (!ResSkipList.add("22")) Log.printStackTrace();
        } else if (Quality.equals("xLowRes")) {
            if (!ResSkipList.add("37")) Log.printStackTrace();
            if (!ResSkipList.add("22")) Log.printStackTrace();
            if (!ResSkipList.add("35")) Log.printStackTrace();
            if (!ResSkipList.add("18")) Log.printStackTrace();
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: ResSkipList = " + ResSkipList.toArray());

        // "REM Convert URL resolution list to URLs, skipping those not to be used."
        for (String ThisRes : ResList) {
            int SepPos = ThisRes.indexOf("|");
            String ThisResNum = ThisRes.substring(0, SepPos);
            String ThisResURL = ThisRes.substring(SepPos+1);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: ThisResNum and ThisResURL = " + ThisResNum + ":" + ThisResURL);

            if (!ResSkipList.contains(ThisResNum)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.handleYouTube: Adding URL = " + ThisResURL);
                if (!URLs.add(ThisResURL))
                    Log.printStackTrace();
            }
        }

        return URLs;
    }

    private List<String> handleYouTubeChannel() {
        return handleYouTube();
    }

    private Properties loadProperties() {

        File STVFile = WidgetAPI.GetDefaultSTVFile();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.loadProperties: STVFile = " + STVFile.toString());

        File STV = Utility.GetPathParentDirectory(STVFile);
        String STVString = STV.getAbsolutePath();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.loadProperties: STVString = " + STVString);

        String PropFilePath = STVString + File.separator + "OnlineVideos" + File.separator + "OnlineVideoLinks.properties";
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.loadProperties: Loading " + PropFilePath);

        File PropFile = new File(PropFilePath);

        // PropInput = new_java_io_BufferedInputStream( new_java_io_FileInputStream( PropCacheFile ) )

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(PropFile);
        } catch (FileNotFoundException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.loadProperties: Property file not found " + PropFile);
            return null;
        }

        BufferedInputStream bis = new BufferedInputStream(fis);

        Properties Props = new Properties();

        try {
            Props.load(bis);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.loadProperties: IO Exception " + e.getMessage());
        }

        try {bis.close();} catch (IOException e) {Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.loadProperties: Error bis on close " + e.getMessage());}
        try {fis.close();} catch (IOException e) {Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.loadProperties: Error fis on close " + e.getMessage());}
        return Props;
    }

    private String ytGetVideoIDFromLink() {
        String URLLink = ChanItem.getLink();
        if (URLLink==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.ytGetVideoIDFromLink: null URLLink.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.ytGetVideoIDFromLink: URLLink = " + URLLink);

        String VideoID = null;

        int IDidx = URLLink.indexOf("v=");

        if (IDidx != -1) {
            String URLLink2 = URLLink.substring(IDidx+2);
            if (URLLink2==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.ytGetVideoIDFromLink: null URLLink2.");
                return null;
            }
            int Endidx = URLLink2.indexOf("&");
            VideoID = (Endidx==-1 ? URLLink2.substring(0, Endidx) : URLLink2);
        } else {
            VideoID = URLLink.substring(URLLink.lastIndexOf("="));
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.ytGetVideoIDFromLink: VideoID = " + VideoID);
        return VideoID;
    }

    private List<String> handleGoogle() {
        List<String> URLs = new ArrayList<String>();

        RSSMediaGroup MediaGroup = ChanItem.getMediaGroup();
        if (MediaGroup==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleGoogle: null MediaGroup.");
            return URLs;
        }

        Vector ContentsVector = MediaGroup.getContent();
        if (ContentsVector==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleGoogle: null ContentsVector.");
            return URLs;
        }

        Object[] Contents = ContentsVector.toArray();
        Object FilteredContents = Database.FilterByMethod(Contents, "sage_media_rss_RSSMediaContent_getType", "application/x-shockwave-flash", true);
        if (FilteredContents!=null && ((Object[])FilteredContents).length!=0) {
            RSSMediaContent ShockWaveObject = ((RSSMediaContent[])FilteredContents)[0];
            String URL = ShockWaveObject.getUrl();
            if (URL==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.handleGoogle: null URL from ShockWaveObject.");
                return URLs;
            }

            URL = Translate.decode(URL);

            if (URL.toLowerCase().contains("youtube.com")) {
                // VideoID = Substring(VideoURL, StringLastIndexOf(VideoURL, "/") + 1, -1)
            }
        }

        return URLs;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UnrecordedEpisode other = (UnrecordedEpisode) obj;
        if (this.ChanItem != other.ChanItem && (this.ChanItem == null || !this.ChanItem.equals(other.ChanItem))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.ChanItem != null ? this.ChanItem.hashCode() : 0);
        return hash;
    }


    /**
     * Gets the Episodes that are already on the disk.
     * <p>
     * @param episodes A List of UnrecordedEpisodes that may or may not actually be in the filesystem.
     * @param onDisk true to return the UnrecordedEpisodes that are actually on the filesystem, false to
     * return the UnrecordedEpisodes that are not on the filesystem.
     * @return A List of UnrecordedEpisodes that meets the criteria.
     */
    public static Set<UnrecordedEpisode> filterByOnDisk(Set<UnrecordedEpisode> episodes, boolean onDisk) {

        // Create the List.
        Set<UnrecordedEpisode> ReturnedEpisodes = new HashSet<UnrecordedEpisode>();

        if (episodes==null || episodes.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "UnrecordedEpisode.filterByOnDisk: null or 0 size episodes.");
            return null;
        }

        // Add those that are currently on the filesystem.
        for (UnrecordedEpisode episode : episodes) {
            if (episode.getID()==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.filterByOnDisk: null ID.");
            } else if (episode.isOnDisk() == onDisk) {
                if (!ReturnedEpisodes.add(episode))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.filterByOnDisk Element already in set.");
            }
        }

        return ReturnedEpisodes;
    }


    /**
     * Filter a List of UnrecordedEpisodes based on if they have ever been recorded.  The episode may have been
     * recorded and deleted, the method does not take into account if the UnrecordedEpisode is physically on the
     * filesystem or not.
     * <p>
     * @param episodes The List of unfiltered Episodes.
     * @param everRecorded true to return those items that were ever recorded, false to return those that were
     * never recorded.
     * @return A List of filtered UnrecordedEpisodes.
     */
    public static Set<UnrecordedEpisode> filterByEverRecorded(Set<UnrecordedEpisode> episodes, boolean everRecorded) {

        Set<UnrecordedEpisode> FilteredEpisodes = new HashSet<UnrecordedEpisode>();

        for (UnrecordedEpisode episode : episodes) {
            if (episode.hasBeenRecorded() == everRecorded) {
                if (!FilteredEpisodes.add(episode))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.filterByEverRecorded: Element already in set.");
            }
        }

        return FilteredEpisodes;
    }

    private boolean hasBeenRecorded() {

        Podcast p = getPodcast();

        Set<Episode> recorded = p.getEpisodesEverRecorded();

        if (recorded==null) {
            return false;
        }

        String xID = RSSHelper.makeID(ChanItem);

        for (Episode e : recorded) {
            if (xID.equals(e.getID())) {
                return true;
            }
        }

        return false;
    }

    /*
     *************
     * Recording.*
     *************
     */


    /**
     * Record the UnrecordedEpisode.
     * <p>
     * @return The recorded episode if success, null otherwise.
     */
    public Episode record() {

        String ReqID = startRecord();
        if (!ReqID.startsWith("REQ")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "record; Failed to start recording. Status = " + ReqID);
            return null;
        }

        SPRRequestID = ReqID;

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "record: Waiting for recording to complete.");

        while (isRecording(ReqID) || isInQueue(ReqID)) {
            try {
                Thread.sleep(SageUtil.GetLongProperty("podcastrecorder/rm_wait_for_recording", 5L * 1000L));
                Log.getInstance().write(Log.LOGLEVEL_ALL, "record checking....");
            } catch (InterruptedException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "record interrupted.");
                Thread.currentThread().interrupt();
            }
        }

        if (!recordedSuccessfully()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "record: Recording failed.");
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM record: Recording has completed.");

        // Create a new Episode and set its ID.
        Podcast p = getPodcast();

        Episode episode = new Episode(p, getID());

        // Set the AiringID.
        if (episode.findAiring()==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "SRM record: Did not find correcponding Airing.");
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SRM record: Found corresponding Airing.");
        }

        p.addEpisodeEverRecorded(episode);
    
        return episode;
    }

    /**
     * Starts the recording process by passing all of the necessary parameters to the SagePodcastRecorder.
     * <p>
     * @return The RecordingID of the started recording is success.  If an error occurs the RecordingIS will start
     * with "ERROR", otherwise the RecordingID will start with "REQ".
     */
     public String startRecord() {

         Podcast p = getPodcast();

         Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "UnrecordedEpisode.startRecord: FeedContext = " + p.getFeedContext());

         String xID = getNewSPRRequestID();
                
         List<String> VideoURLs = setDefaultVideoURL(p.getOnlineVideoType(), p.getOnlineVideoItem());

         RecordingEpisode episode = new RecordingEpisode(   xID,
                                                            p.getFeedContext(),
                                                            p.getOnlineVideoType(),
                                                            p.getOnlineVideoItem(),
                                                            p.isFavorite(),
                                                            RSSHelper.makeID(ChanItem),
                                                            ChanItem,
                                                            p.getRecDir(),
                                                            (p.isUseShowTitleAsSubdir() ? p.getShowTitle() : p.getRecSubdir()),
                                                            p.getShowTitle(),
                                                            getEpisodeTitle(),
                                                            makeFileName().replaceAll("[^A-Za-z0-9_-]", ""),
                                                            VideoURLs);

         if (!episode.isComplete()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "UnrecordedEpisode.startRecord: Incomplete recording map for " + xID);
            return "Incomplete Data.";
         }

         DownloadManager.getInstance().addRecording(xID, episode);
         DownloadManager.getInstance().addActiveDownloads(xID);
         DownloadManager.getInstance().addItem(episode);
         Log.getInstance().write(Log.LOGLEVEL_TRACE, "UnrecordedEpisode.startRecord: Queued to DownloadThread " + xID);
         return xID;
     }

    /**
     * Check to see if the Episode is currently recording.
     * <p>
     * @return true if the Episode is curerntly recording or waiting to be recorded, false otherwise.
     */
    private boolean isRecording(String ReqID) {
        if (DownloadManager.getInstance().getCurrentlyRecordingID()!=null && DownloadManager.getInstance().getCurrentlyRecordingID().compareToIgnoreCase(ReqID)==0) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInQueue(String ReqID) {
        List<String> ActiveDownloads = DownloadManager.getInstance().getActiveDownloads();
        if (ActiveDownloads==null) {
            return false;
        }

        return ActiveDownloads.contains(ReqID);
    }

    /**
     * Check if the Episode was successfully recorded.
     * <p>
     * @return true if it was successfully recorded, false otherwise.
     */
     private boolean recordedSuccessfully() {
         List<String> CompletedList = DownloadManager.getInstance().getCompletedDownloads();
         Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "UnrecordedEpisoderecordedSuccessfully: recorded successfully checking for " + SPRRequestID);
         return CompletedList.contains(this.SPRRequestID);
     }
 
    /**
     * Sends a command to the podcast recorder requesting a new download ID.
     * <p>
     * @return The ID if successful, otherwise a string that starts with "ERROR".
     */
    private String getNewSPRRequestID() {
        return "REQ" + UUID.randomUUID().toString();
    }

    /**
     * Make a file name for the Episode.
     * <p>
     * @return A file name to use.
     */
    public String makeFileName() {
        String Name = null;
        Podcast podcast = this.getPodcast();

        if (podcast.isUseShowTitleInFileName()) {
            Name = podcast.getShowTitle() + " - " + this.getEpisodeTitle();
        } else {
            Name = this.getEpisodeTitle();
        }

        return Name;
    }

    /**
     * Gets the Episode Title from the RSSItem.
     * <p>
     * @return The Episode Title.  May contain strange characters so be sure to strip them out as needed.
     */
    public String getEpisodeTitle() {
        return ChanItem.getTitle();
    }

    public String getCleanDescription() {
        return ChanItem.getCleanDescription();
    }

}
