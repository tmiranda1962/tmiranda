
package tmiranda.podcastrecorder;

import java.util.*;
import java.io.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 * This class presents the API that can be used in a STV.
 *
 * For performance reasons a cache of all Podcasts is kept for each instance of the API, which
 * equates to each UI instance.
 *
 * @author Tom Miranda.
 */
public class API {

    // Singleton.
    private static final API instance = new API();

    /*
     * This is the object that co-ordinates sending and receiving data from a remote Sage server.  It's
     * only used when the API is being executed on a SageClient.
     */
    private MQDataGetter MQDataGetter;

    private static final String VERSION = Plugin.VERSION;

    private static final String THIS_CLASS = "tmiranda.podcastrecorder.API";
    private static final long   DEFAULT_TIMEOUT     = 5000L;
    private static final long   TEN_SECOND_TIMEOUT  = 10000L;
    //private static final long   HALF_SECOND_TIMEOUT = 500L;
    //private static final long   ONE_SECOND_TIMEOUT  = 1000L;

    /*
     * This is a cache that is kept on the local SageClient to increase performance.
     */
    private static List<Podcast> PodcastCache = new ArrayList<Podcast>();
    private static Date cacheDate = new Date(0L);

    /*
     * Note that in many places we use .toString() to get a String representation of a java.io.File.
     * This is because getAbsolutePath() will return a Windows formatted String ("C:\...") even if the
     * target filesystem (i.e. the Sage server) is Linux.
     */

    private API() {
        MQDataGetter = new MQDataGetter();
    }

    /**
     * This class is implemented as a Singleton because we need one, and exactly one, MQDataGetter per
     * instance of the user STV (may it be an extender, placeshifter or SageClient.
     */
    public static API getInstance() {
        return instance;
    }

    /**
     * Gets the MQDataGetter instance for this API instance.  This method should not be called 
     * directly by the Sage STV.
     */
    private static MQDataGetter GetMQDataGetter() {
        return API.getInstance().MQDataGetter;
    }

    /**
     * Checks to see if the Sage server is alive and the PodcastRecorder Plugin is loaded.
     *
     * @return true if the Sage server is responding and has the PodcastRecorder plugin installed,
     * false otherwise.
     */
    public static boolean IsServerAlive() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsServerAlive", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            return true;
        }
    }

    /**
     * Checks to see if the Plugin on the server and the Plugin on the Client are the same
     * version.  Generally speaking, the Plugin will not work correctly if the two versions
     * are not the same.
     *
     * @return true if the Plugins are the same version, false otherwise.
     */
    public static boolean IsPluginCorrectVersion() {
        return GetAPIVersion().equals(GetServerVersion());
    }

    public static String GetServerVersion() {

        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetServerVersion", new Object[] {}, DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof String) ? "ERROR" : (String)RC);
        } else {
            return Plugin.VERSION;
        }
    }

    public static String GetAPIVersion() {
        return API.VERSION;
    }

    public static String MakeHuluOVI(String SearchURL) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI: Making from " + SearchURL);

        if (SearchURL==null) {
            Log.Write(Log.LOGLEVEL_WARN, "MakeHuluOVI: null SearchURL.");
            return null;
        }

        String OVI = null;

        String FeedParts[] = SearchURL.split(",",3);

        if (FeedParts==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI: null FeedParts " + SearchURL);
            return null;
        }

        String FeedEXE = null;
        String FeedParamList[] = null;

        switch (FeedParts.length) {
            case 2:
                // FeedEXE = FeedParts[1];
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MakeHuluOVI: Only two FeedParts.");
                break;
            case 3:
                FeedEXE = FeedParts[1];
                Log.Write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI: FeedEXE = " + FeedEXE);

                String FeedParam = FeedParts[2];
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI: FeedParam = " + FeedParam);

                if (FeedParam.length() > 0) {
                    Log.Write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI: Found parameters.");

                    FeedParamList = FeedParam.split("\\|\\|");

                    for (int i=0; i<(FeedParamList.length-1); i++) {
                        String p = FeedParamList[i];
                        Log.Write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI: Parameter " + p);

                        if (p.toLowerCase().contains("path")) {
                            String path = FeedParamList[i+1];
                            OVI = path.replaceAll("[^A-Za-z0-9_-]", "_");
                            Log.Write(Log.LOGLEVEL_TRACE, "MakeHuluOVI: Returning " + OVI);
                            return OVI;
                        }
                    }
                    Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI: Failed to find path.");

                } else {
                    Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI: Bad FeedParam.");
                }
                break;
            default:
                Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI: Found bad SearchURL.");
                break;
        }

        return OVI;
    }

    public static String MakeChannelsDotComOVI(String OnlineVideoItem, String UnstrippedTitle, RSSItem ChanItem) {

        if (OnlineVideoItem.equalsIgnoreCase("xChannelsDotComFeedContent")) {
            return UnstrippedTitle == null ? null : StripShowTitle(UnstrippedTitle);
        } else if (OnlineVideoItem.equalsIgnoreCase("xChannelsDotComFeedList")) {
            if (ChanItem==null)
                return null;
            String title = ChanItem.getTitle();
            return title == null ? null : StripShowTitle(title);
        } else if (OnlineVideoItem.equalsIgnoreCase("xChannelsDotCatList")) {
            return OnlineVideoItem;
        } else {
            return null;
        }
    }

    public static void ShowRSSItem(RSSItem Item) {
        if (Item == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "ShowRSSItem: null Item.");
            return;
        }

        System.out.println("ShowRSSItem: Showing RSSItem " + Item.getTitle() + ":" + Item.getCleanDescription() + ":" + Item.getLink());
    }

    public static void ShowRSSItems(List<RSSItem> Items) {
        if (Items == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "ShowRSSItems: null Items.");
            return;
        }

        for (RSSItem Item : Items) {
            ShowRSSItem(Item);
        }
    }

    /**
     * Causes the RecordManager to run immediatly.  The RecordManager is what checks for new podcast episodes
     * and downloads them as defined by the favorite options.  This process happens infrequently (the default
     * setting is once per day) so this method should be called after adding a favorite or modifying a
     * favorite.
     */
    public static void RecordManagerManualRun() {
        Log.Write(Log.LOGLEVEL_TRACE, "RecordManagerManualRun: Running RecordManager.");
        if (Global.IsClient()) {
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "RecordManagerManualRun");
        } else {
            Plugin.RecordManagerManualRun();
        }
    }

    public static boolean IsDynamicSubCat(String OVT, String OVI, RSSItem ChanItem) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat: Checking " + OVT + ":" + OVI);
        
        if (OVI != null && OVT.startsWith("xChannelsDotCom") && OVI.startsWith("xChannelsDotComCatList")) {
            Log.Write(Log.LOGLEVEL_TRACE, "IsDynamicSubCat: ChannelsDotCom List. Returning true. " + OVI);
            return true;
        }

        RSSEnclosure Enclosure = ChanItem.getEnclosure();

        if (Enclosure==null) {
            Log.Write(Log.LOGLEVEL_TRACE, "IsDynamicSubCat: found null Enclosure. Returning false.");
            return false;
        }

        String Type = Enclosure.getType().toLowerCase();
        Log.Write(Log.LOGLEVEL_TRACE, "IsDynamicSubCat: Type is " + Type);

        if (Type!=null && Type.contains("sagetv")) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat: Found sagetv Enclosure. Returning true.");
            return true;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat: Is not a dynamic subcat.");
        return false;
    }
    
    public static String GetFeedName(RSSItem ChanItem) {
        if (ChanItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetFeedName: null Item.");
            return null;
        }

        String name = ChanItem.getTitle();
        name.replaceAll(" ", "_");
        name.replaceAll("[^A-Za-z0-9_-]", "");
        name.replaceAll("_", " ");

        int maxlen = SageUtil.GetIntProperty(Plugin.PROPERTY_FILE_MAX_LENGTH, 75);
        if (name.length() > maxlen) {
            name = name.substring(0, maxlen-1);
        }

        return name;
    }

    /**
     * Returns the MediaFile Object corresponding to the given RSSItem.
     * <p>
     * @param Item The RSSItem.
     * @return The MediaFile Object corresponding to this RSSItem or null if there is none.  No MediaFile
     * implies that the RSSItem has not been downloaded and imported into the SageTV core.
     */
    public static Object GetMediaFileForRSSItem(RSSItem Item) {
        if (Item==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetMediaFileForRSSItem: null RSSItem.");
            return false;
        }

        if (Global.IsClient()) {
            return GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetMediaFileForRSSItem", new Object[] {Item}, TEN_SECOND_TIMEOUT);
        } else {
            return RSSHelper.getMediaFileForRSSItem(Item);
        }
    }

    public static boolean DeleteMediaFileForRSSItem(RSSItem Item) {
        if (Item==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "DeleteMediaFileForRSSItem: null RSSItem.");
            return false;
        }

        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "DeleteMediaFileForRSSItem", new Object[] {Item}, TEN_SECOND_TIMEOUT);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            Object MediaFile = RSSHelper.getMediaFileForRSSItem(Item);
            if (MediaFile==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DeleteMediaFileForRSSItem: null MediaFile for RSSItem.");
                return false;           
            } else {
                return MediaFileAPI.DeleteFile(MediaFile);
            }
        }
    }

    public static boolean IsRSSItemOnDisk(RSSItem Item) {
        if (Item==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "IsRSSItemOnDisk: null RSSItem.");
            return false;
        }

        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRSSItemOnDisk", new Object[] {Item}, TEN_SECOND_TIMEOUT);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            return (RSSHelper.getMediaFileForRSSItem(Item)!=null);
        }
    }

    public static boolean IsAiringPodcast(Object MediaFile) {
        String ID = ShowAPI.GetShowMisc(MediaFile);

        if (ID==null || ID.isEmpty()) {
            return false;
        }

        String Category = ShowAPI.GetShowCategory(MediaFile);

        if (Category==null || Category.isEmpty()) {
            return false;
        }
        
        return Category.equalsIgnoreCase("PodcastRecorder");
    }

    /**
     * Checks to see if any episode is currently being recorded.
     * <p>
     * @return true if any podcast is being recorded, false otherwise.
     */
    public static boolean IsRecording() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRecording", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            return (DownloadManager.getInstance().getCurrentlyRecordingID()==null ? false : true);
        }
    }

    /*
     ********************************
     * Current = What's recording now.
     *********************************
     */

    /**
     * Gets the number of bytes that have already been downloaded for the currently recording episode.
     * <p>
     * @return The number of bytes downloaded.
     */
    public static Long GetCurrentDownloadSize() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetCurrentDownloadSize", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Long) ? 0 : (Long)RC);
        } else {
            return DownloadManager.getInstance().getCurrentDownloadSize();
        }
    }

    /**
     * Gets a List containing the RSSItem for the currently recording download.
     * <p>
     * @return A List containing the RSSItem for the currently recording episode.
     */
    @SuppressWarnings("unchecked")
    public static List<RSSItem> GetRSSItemsForCurrent() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForCurrent", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof List) ? null : (List<RSSItem>)RC);
        } else {
            return DownloadManager.getInstance().getRSSItemsForCurrent();
        }
    }

    public static boolean IsRecordingNow(RSSItem RSSItem) {
        if (RSSItem==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "IsRecordingNow: null RSSItem.");
            return false;
        }

        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRecordingNow", new Object[] {RSSItem}, DEFAULT_TIMEOUT);
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsRecordingNow: Returning " + RC);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            List<RSSItem> Items = DownloadManager.getInstance().getRSSItemsForCurrent();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsRecordingNow: Returning " + RSSHelper.RSSListContains(Items, RSSItem));
            return RSSHelper.RSSListContains(Items, RSSItem);
        }
    }

    public static void AbortCurrentDownload() {
        if (Global.IsClient()) {
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "AbortCurrentDownload");
        } else {
            DownloadManager.getInstance().abortCurrentDownload();
        }
    }


    /*
     * Active = In the download queue.
     */
    public static Integer GetSizeActive() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeActive", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Integer) ? 0 : (Integer)RC);
        } else {
            return DownloadManager.getInstance().getSizeActive();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<RSSItem> GetRSSItemsForActive() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForActive", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof List) ? null : (List<RSSItem>)RC);
        } else {
            return DownloadManager.getInstance().getRSSItemsForActive();
        }
    }

    public static boolean IsInQueue(RSSItem RSSItem) {
        if (RSSItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "IsInQueue: null RSSItem.");
            return false;
        }

        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsInQueue", new Object[] {RSSItem}, DEFAULT_TIMEOUT);
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsInQueue: Returning " + RC);
            return (RC==null || !(RC instanceof Boolean) ? false : (Boolean)RC);
        } else {
            List<RSSItem>Items = DownloadManager.getInstance().getRSSItemsForActive();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsInQueue: Returning " + RSSHelper.RSSListContains(Items, RSSItem));
            return RSSHelper.RSSListContains(Items, RSSItem);
        }
    }

    public static void RemoveFromQueue(RSSItem RSSItem) {
        if (RSSItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "RemoveFromQueue: null Item.");
            return;
        }

        if (Global.IsClient()) {
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "RemoveFromQueue", new Object[] {RSSItem});
        } else {
            DownloadManager.getInstance().removeFromQueue(RSSItem);
        }
    }
    
    /*
     * Completed = Succesfully downloaded.
     */
    @SuppressWarnings("unchecked")
    public static List<RSSItem> GetRSSItemsForCompleted() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForCompleted", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof List) ? null : (List<RSSItem>)RC);
        } else {
            return DownloadManager.getInstance().getRSSItemsForCompleted();
        }
    }
    
    public static Integer GetSizeCompleted() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeCompleted", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Integer) ? 0 : (Integer)RC);
        } else {
            return DownloadManager.getInstance().getSizeCompleted();
        }
    }


    /*
     * Failed = Attempt to download failed.
     */
    @SuppressWarnings("unchecked")
    public static List<RSSItem> GetRSSItemsForFailed() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForFailed", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof List) ? null : (List<RSSItem>)RC);
        } else {
            return DownloadManager.getInstance().getRSSItemsForFailed();
        }
    }
   
    public static Integer GetSizeFailed() {
        if (Global.IsClient()) {
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeFailed", DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Integer) ? 0 : (Integer)RC);
        } else {
            return DownloadManager.getInstance().getSizeFailed();
        }
    }

    public static String StripShowTitle(String Title) {
        if (Title==null) {
            return "";
        }
        String S = Title.replaceAll(" ", "_");
        S = S.replaceAll("[^A-Za-z0-9_-]", "");
        S = S.replaceAll("_", " ");
        return S;
    }

    /*
     * Strips all illegal characters from the podcast.
     */
    public static String StripPodcastText(String podcast) {
        if (podcast==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter 1.");
            return null;
        }
        return podcast.replaceAll("()", "");
    }

    public static String GetTextForPodcast(Properties properties, String podcast) {

        if (properties==null || podcast==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter 2.");
            return null;
        }

        String Text = podcast;

        if (podcast.startsWith("ez")) {
            Text = properties.getProperty(podcast+"/Name", null);
            Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast: ez returning " + Text);
            return (Text == null ? podcast : Text);
        } else {
            Text = properties.getProperty("Source/" + podcast + "/LongName", null);
            if (Text!=null) {
                Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast: Source/LongName returning " + Text);
                return Text;
            } else {
                Text = properties.getProperty("Category/" + podcast + "/FullName", null);
                if (Text!=null) {
                    Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast: FullName returning " + Text);
                    return Text;
                } else {
                    Text = properties.getProperty("Category/" + podcast + "/LongName", null);
                    Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast: LongName returning " + Text);
                    return (Text==null ? podcast : Text);
                }
            }
        }
    }

    /*
     * Returns all Favorite Podcasts.
     */
    public static List<String> GetAllFavoritePodcasts() {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetAllFavoritePodcasts: Begin.");

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = DataStore.getAllPodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        List<String> names = new ArrayList<String>();

        for (Podcast p : favoritePodcasts) {
            if (p.isFavorite())
                if (!names.add(p.getOnlineVideoItem()))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "GetAllFavoritePodcasts: Failed to add.");
        }
        return names;
    }

    public static String[] SortPodcastsByName(Properties properties, String[] UnsortedPodcasts) {
        if (UnsortedPodcasts==null || UnsortedPodcasts.length==0) {
            return UnsortedPodcasts;
        }

        // Temp holding place for the Names that correspond to the Unsorted OVI's.
        String[] Names = new String[UnsortedPodcasts.length];

        // Key = Name. Value = OVI. Will be used to go from Name back to OVI.
        Map<String, String> NameMap = new HashMap<String, String>();

        // Get the Name, put it in the map and save it in the unsorted Names array.
        for (int i=0; i<UnsortedPodcasts.length; i++) {
            String Name = GetTextForPodcast(properties, UnsortedPodcasts[i]);
            NameMap.put(Name, UnsortedPodcasts[i]);
            Names[i] = Name;
        }

        // Sort the Names array.
        Arrays.sort(Names);

        // This is where we will store the sorted OVI's.
        String[] SortedPodcasts = new String[UnsortedPodcasts.length];

        // For each sorted Name put in the corresponding OVI.
        for (int i=0; i<SortedPodcasts.length; i++) {
            SortedPodcasts[i] = NameMap.get(Names[i]);
        }

        return SortedPodcasts;
    }

    public static String[] SortPodcastsByName(Properties properties, List<String>UnsortedPodcasts) {
        if (properties==null || UnsortedPodcasts==null) {
            return null;
        }

        // Convert the List to an Array.  There is probably a better way...
        String[] UP = new String[UnsortedPodcasts.size()];
        for (int i=0; i<UnsortedPodcasts.size(); i++) {
            UP[i] = UnsortedPodcasts.get(i);
        }

        return SortPodcastsByName(properties, UP);
    }

    /**
    public static synchronized List<String> IncreasePodcastPriority(String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "IncreasePodcastPriority.");

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = DataStore.getAllPodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        // This will be the new list of OnlineVideoItems. (Podcast for the STV.)
        List<String> OVIs = new ArrayList<String>();

        // Handle special case of only having 1 Favorite.
        if (favoritePodcasts.size()==1) {
            if (!OVIs.add(OVI))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "IncreasePodcastPriority failed to add.");
            return OVIs;
        }

        // This is where we will store the new list of Podcasts with the new priorities.
        List<Podcast> newPodcastList = new ArrayList<Podcast>();

        boolean found = false;

        int i = 0;
        for (i=0; i<favoritePodcasts.size() && !found; i++) {

            // Get a Podcast.
            Podcast p = favoritePodcasts.get(i);

            //  See if it's the one we're interesed in. If it is, swap it down, if not just add it to the new List.
            if (p.getOnlineVideoItem().equalsIgnoreCase(OVI)) {

                found = true;

                // Add it to the new List.
                if (!newPodcastList.add(p))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "IncreasePodcastPriority failed to add.");

                // If it's already first there is nothing to do.
                if (i!=0) {

                    // Swap them.
                    newPodcastList = swapListElements(newPodcastList, i-1, i);
                }
            } else {
                if (!newPodcastList.add(p))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "IncreasePodcastPriority failed to add.");
            }
        }

        // Add any remaining.
        for (; i<favoritePodcasts.size(); i++) {
            if (!newPodcastList.add(favoritePodcasts.get(i)))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "IncreasePodcastPriority failed to add.");
        }

        // Write the new List out to disk.
        if (!DataStore.writeFavoritePodcasts(newPodcastList)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Error writing favorite Podcasts.");
        }

        // Create the List of OVIs.
        for (Podcast p : newPodcastList) {
            if (!OVIs.add(p.getOnlineVideoItem()))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "IncreasePodcastPriority failed to add.");
        }

        return OVIs;
    }

    public static synchronized List<String> DecreasePodcastPriority(String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "DecreasePodcastPriority.");

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = DataStore.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        // This will be the new list of OnlineVideoItems. (Podcasst for the STV.)
        List<String> OVIs = new ArrayList<String>();

        // Handle special case of only having 1 Favorite.
        if (favoritePodcasts.size()==1) {
            if (!OVIs.add(OVI))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");
            return OVIs;
        }

        // This is where we will store the new list of Podcasts with the new priorities.
        List<Podcast> newPodcastList = new ArrayList<Podcast>();

        boolean found = false;

        int i = 0;
        for (i=0; i<favoritePodcasts.size() && !found; i++) {

            // Get a Podcast.
            Podcast p = favoritePodcasts.get(i);

            //  See if it's the one we're interesed in. If it is, swap it down, if not just add it to the new List.
            if (p.getOnlineVideoItem().equalsIgnoreCase(OVI)) {

                found = true;

                // Add it to the new List.
                if (!newPodcastList.add(p))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");

                // If it's already last there is nothing to do.
                if (i!=favoritePodcasts.size()-1) {

                    // Get the next one.
                    if (!newPodcastList.add(favoritePodcasts.get(i+1)))
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");

                    // Swap them.
                    newPodcastList = swapListElements(newPodcastList, i, i+1);

                    // Skip the next element in the List because we already added it.
                    i++;
                }
            } else {
                if (!newPodcastList.add(p))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");
            }
        }

        // Add any remaining.
        for (; i<favoritePodcasts.size(); i++) {
            if (!newPodcastList.add(favoritePodcasts.get(i)))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");
        }

        // Write the new List out to disk.
        if (!DataStore.writeFavoritePodcasts(newPodcastList)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Error writing favorite Podcasts.");
        }

        // Create the List of OVIs.
        for (Podcast p : newPodcastList) {
            if (!OVIs.add(p.getOnlineVideoItem()))
                Log.Write(Log.LOGLEVEL_ERROR, "Error adding favorite Podcasts.");
        }

        return OVIs;
    }

    // first+1 MUST = second. oldList MUST have at least two elements.
    private static List<Podcast> swapListElements(List<Podcast> oldList, int first, int second) {

        // Where the results will be returned.
        List<Podcast> newList = new ArrayList<Podcast>();

        // Copy from 0 to the first item.
        for (int i=0; i<first; i++) {
            if (!newList.add(oldList.get(i)))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "swapListElements failed to add.");
        }

        if (!newList.add(oldList.get(second))) Log.getInstance().write(Log.LOGLEVEL_ERROR, "swapListElements failed to add.");
        if (!newList.add(oldList.get(first))) Log.getInstance().write(Log.LOGLEVEL_ERROR, "swapListElements failed to add.");

        // Now copy the rest.
        for (int i=second+1; i<oldList.size(); i++) {
            if (!newList.add(oldList.get(i)))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "swapListElements failed to add.");
        }

        return newList;
    }
     */

    public static String GetOVTforPodcast(String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetOVTforPodcast: Begin.");

        if (SageUtil.isNull(OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetOVTforPodcast: null parameter.");
            return null;
        }

        List<Podcast> favoritePodcasts = null;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = DataStore.getAllPodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        for (Podcast p : favoritePodcasts) {
            if (p.getOnlineVideoItem().equalsIgnoreCase(OVI)) {
                return p.getOnlineVideoType();
            }
        }

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetOVTforPodcast: Did not find match for " + OVI);
        return null;
    }

    public static String GetOVTforOVI(String OVI) {
        return GetOVTforPodcast(OVI);
    }

    /**
     * Retrieves the specified Podcast.
     * - Does not lock the Podcast for update.
     * - Always fetches the Podcast from the server.
     *
     * @param OVT
     * @param OVI
     * @return
     */
    public static Podcast GetPodcast(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_ALL, "GetPodcast: Begin");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetPodcast: null parameter " + OVT + ":" + OVI);
            return null;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "GetPodcast: Executing GetPodcast on server.");
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetPodcast", new Object[] {OVT, OVI}, DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Podcast) ? null : (Podcast)RC);
        } else
            return DataStore.getPodcast(OVT, OVI);
    }

    public static Podcast GetPodcast(String FeedContext) {

        Log.Write(Log.LOGLEVEL_ALL, "GetPodcast: Begin");

        if (FeedContext==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetPodcast: null parameter " + FeedContext);
            return null;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "GetPodcast: Executing GetPodcast on server.");
            Object RC = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetPodcast", new Object[] {FeedContext}, DEFAULT_TIMEOUT);
            return (RC==null || !(RC instanceof Podcast) ? null : (Podcast)RC);
        } else
            return DataStore.getPodcast(FeedContext);
    }

    public static boolean PodcastExists(String OVT, String OVI) {
        return (GetPodcast(OVT,OVI)==null ? false : true);
    }

    public static long GetLastChecked(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetLastChecked:");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0L;
        else return podcast.getLastChecked();
    }

    public static int GetDuplicatesDeleted(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetDuplicatesDeleted:");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else return podcast.getDuplicatesDeleted();
    }

    public static int GetEpisodesOnServer(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesOnServer:");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else
            return podcast.getEpisodesOnWebServerSize();
    }

    public static int GetEpisodesOnDisk(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesOnDisk:");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else return podcast.getEpisodesOnDisk().size();
    }

    public static int GetEpisodesEverRecorded(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesEverRecorded:");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else
            return podcast.getEpisodesEverRecordedSize();
    }

    public static boolean MarkAsRecorded(String OVT, String OVI, RSSItem ChanItem) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MarkAsRecorded: Enter.");

        if (SageUtil.isNull(OVT, OVI) || ChanItem==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "MarkAsRecorded: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MarkAsRecorded: Marking as Recorded on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "MarkAsRecorded", new Object[] {OVT, OVI, ChanItem});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "MarkAsRecorded: null podcast.");
            return false;
        }

        Episode episode = new Episode(podcast, RSSHelper.makeID(ChanItem));

        if (podcast.hasEpisodeEverBeenRecorded(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MarkAsRecorded: Episode has already been recorded.");
            return true;
        }

        episode.setAiringID(-1);
        
        podcast.addEpisodeEverRecorded(episode);
        return true;
    }

    public static boolean MarkAsUnrecorded(String OVT, String OVI, RSSItem ChanItem) {
System.out.println("MARKASUNREC: " + OVT + ":" + OVI + ":" + RSSHelper.makeID(ChanItem));
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MarkAsUnrecorded: Enter.");

        if (SageUtil.isNull(OVT, OVI) || ChanItem==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "MarkAsUnrecorded: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MarkAsUnecorded: Marking as Unrecorded on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "MarkAsUnrecorded", new Object[] {OVT, OVI, ChanItem});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);
System.out.println("MARKASUNREC: podcast EER Size " + podcast.getEpisodesEverRecordedSize());

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "MarkAsUnrecorded: null podcast.");
            return false;
        }

        Episode episode = new Episode(podcast, RSSHelper.makeID(ChanItem));

        if (!podcast.hasEpisodeEverBeenRecorded(episode)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MarkAsUnrecorded: Episode has never been recorded " + RSSHelper.makeID(ChanItem));
            return true;
        }

        podcast.removeEpisodeEverRecorded(episode);
        return true;
    }

    public static boolean HasBeenRecorded(String OVT, String OVI, RSSItem ChanItem) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "HasBeenRecorded: Enter.");

        if (SageUtil.isNull(OVT, OVI) || ChanItem==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "HasBeenRecorded: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, "HasBeenRecorded", new Object[] {OVT, OVI, ChanItem}, DEFAULT_TIMEOUT);
            return (result==null || !(result instanceof Boolean) ? false : (Boolean)result);
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "HasBeenRecorded: null podcast.");
            return false;
        }

        Episode episode = new Episode(podcast, RSSHelper.makeID(ChanItem));

        return podcast.hasEpisodeEverBeenRecorded(episode);
    }

    public static Map<String, Boolean> GetHasBeenRecorded(RSSChannel Channel) {

        Map<String, Boolean> recordedMap = new HashMap<String, Boolean>();

        if (Channel == null)
            return recordedMap;

        if (Global.IsClient()) {
            Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetHasBeenRecorded", new Object[] {Channel}, DEFAULT_TIMEOUT);

            //@SuppressWarnings("unchecked")
            return (result==null || !(result instanceof Map) ? new HashMap<String, Boolean>() : (Map<String, Boolean>)result);
        }

        List<RSSItem> RSSItems = new LinkedList<RSSItem>();
        RSSItems = Channel.getItems();

        if (RSSItems==null || RSSItems.isEmpty())
            return recordedMap;

        List<Podcast> podcasts = DataStore.getAllPodcasts();

        if (podcasts == null || podcasts.isEmpty())
            return recordedMap;

        for (RSSItem thisItem : RSSItems) {
            
            for (Podcast podcast : podcasts) {

                Set<Episode> episodes = podcast.getEpisodesEverRecorded();

                for (Episode episode : episodes) {

                    if (episode.getID().equals(RSSHelper.makeID(thisItem))) {
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "GetHasBeenRecorded: Found recorded RSSItem " + thisItem.getTitle() + " : " + RSSHelper.makeID(thisItem));
                        recordedMap.put(RSSHelper.makeID(thisItem), Boolean.TRUE);
                    }
                }
            }
        }

        return recordedMap;
    }
    
    /*
     * Isfavorite
     */

    /**
     * Method for Sage STV to check if a Podcast is a favorite.
     * <p>
     * @param OVT The OnlineVideoType as used in the STV.
     * @param OVI The OnlineVideoItem as used on the STV
     * @return true if the Podcast is a favorite, false otherwise.
     */
    public static boolean IsPodcastFavorite(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite: Looking for " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsPodcastFavorite: null parameter " + OVT + ":" + OVI);
            return false;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null || !podcast.isFavorite()) {
            Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite: Not a favorite.");
            return false;
        } else {
            Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite: Is a favorite.");
            return true;
        }
    }

    /**
     * Checks if the Airing is a Favorite Podcast.
     * @param Airing
     * @return true if it's a favorite, false otherwise.
     */
    public static boolean IsPodcastFavorite(Object Airing) {

        if (Airing==null) {
            return false;
        }

        Object MediaFile = AiringAPI.GetMediaFileForAiring(Airing);

        if (MediaFile==null) {
            return false;
        }

        String fav = MediaFileAPI.GetMediaFileMetadata(MediaFile, RecordingEpisode.METADATA_FAVORITE);

        return fav!=null && fav.equalsIgnoreCase("true");
    }

    public synchronized static boolean _SetIsFavorite(String OVT, String OVI, Boolean Favorite) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "_SetIsFavorite: Enter.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "_SetIsFavorite: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "_SetIsFavoriteOnServer: Setting Isfavorite on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetIsFavorite", new Object[] {OVT, OVI, Favorite});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "_SetIsFavorite: null podcast.");
            return false;
        }

        podcast.setIsFavorite(Favorite);
        return true;
    }

    public static boolean SetIsFavorite(String OVT, String OVI, Object F) {
        return _SetIsFavorite(OVT, OVI, ObjToBool(F));
    }

    public static synchronized List<Podcast> getFavoritePodcastsFromServer() {
        Log.getInstance().write(Log.LOGLEVEL_ALL, "getFavoritePodcastsFromServer: Getting podcasts from server.");

        Date newDate = new Date();

        Object RC = GetMQDataGetter().getDataFromServer("tmiranda.podcastrecorder.DataStore", "readFavoritePodcasts", new Object[] {cacheDate}, DEFAULT_TIMEOUT);

        @SuppressWarnings("unchecked")
        List<Podcast> Podcasts = (RC==null || !(RC instanceof List) ? null : (List<Podcast>)RC);

        // If it's not null it contains updated Podcasts.  If it is null the Podcasts in the local cache are still valid.
        if (Podcasts!=null) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "API getFavoritePodcastsFromServer: Podcast cache updated.");
            PodcastCache.clear();
            PodcastCache = Podcasts;
            cacheDate = newDate;
        }

        return PodcastCache;
    }

    /**
     * Method that the Sage STV should use to remove a Favorite Podcast.
     * <p>
     * @param OVT The OnlineVideoType as used in the STV.
     * @param OVI The OnlineVideoItem as used in the STV.
     * @return true if the Favorite was successfully removed.  false otherwise.
     */
    public synchronized static boolean RemoveFavorite(String OVT, String OVI) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Removing " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RemoveFavorite: null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "RemoveFavorite: Removing favorite on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "RemoveFavorite", new Object[] {OVT, OVI});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Is not a favorite.");
            return false;
        }

        Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Removed.");
        return DataStore.removePodcast(podcast);
    }


    public synchronized static boolean DisableFavorite(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_TRACE, "DisableFavorite: Disabling " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "DisableFavorite: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "DisableFavorite: Disabling favorite on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "DisableFavorite", new Object[] {OVT, OVI});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "DisableFavorite: Is not a favorite.");
            return false;
        }

        podcast.setIsFavorite(false);
        return true;
    }

    public static String GetFeedContext(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetFeedContext:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getFeedContext();
    }

    /*
     * RecordNew
     */
    public static boolean GetRecordNew(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetRecordNew:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null) {
            return false;
        } else {
            return podcast.isRecordNew();
        }
    }

    public static boolean SetRecordNew(String OVT, String OVI, Object RN) {
        return _SetRecordNew(OVT, OVI, ObjToBool(RN));
    }

    public synchronized static boolean _SetRecordNew(String OVT, String OVI, Boolean RecordNew) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecordNew:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetRecordNew: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecordNew: Setting record new on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetRecordNew", new Object[] {OVT, OVI, RecordNew});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null) {
            Log.Write(Log.LOGLEVEL_TRACE, "_SetIsFavorite: Could not get Podcast.");
            return false;
        }
        
        podcast.setRecordNew(RecordNew);
        return true;
    }

    /*
     * ReRecord
     */
    public static boolean GetReRecord(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetReRecord:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isReRecordDeleted();
    }

    public synchronized static boolean _SetReRecord(String OVT, String OVI, Boolean ReRecord) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetReRecord:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetReRecord: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetReRecord: Setting ReRecord on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetReRecord", new Object[] {OVT, OVI, ReRecord});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetReRecord: null podcast.");
            return false;
        }

        podcast.setReRecordDeleted(ReRecord);

        return true;
    }

    public static boolean SetReRecord(String OVT, String OVI, Object RR) {
        return _SetReRecord(OVT, OVI, ObjToBool(RR));
    }

    /*
     * AutoDelete
     */
    public static boolean GetAutoDelete(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetAutoDelete:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isAutoDelete();
    }

    public synchronized static boolean _SetAutoDelete(String OVT, String OVI, Boolean AutoDelete) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetAutoDelete:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetAutoDelete: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetAutoDelete: Setting AutoDelete on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetAutoDelete", new Object[] {OVT, OVI, AutoDelete});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetAutoDelete: Could not get Podcast.");
            return false;
        }

        podcast.setAutoDelete(AutoDelete);

        return true;
    }

    public static boolean SetAutoDelete(String OVT, String OVI, Object AD) {
        return _SetAutoDelete(OVT, OVI, ObjToBool(AD));
    }

    /*
     * KeepNewest
     */
    public static boolean GetKeepNewest(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetKeepNewest:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isKeepNewest();
    }

    public synchronized static boolean _SetKeepNewest(String OVT, String OVI, Boolean Keep) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepNewest:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetKeepNewest: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepNewest: Setting KeepNewest on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetKeepNewest", new Object[] {OVT, OVI, Keep});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetKeepNewest: null podcast.");
            return false;
        }

        podcast.setKeepNewest(Keep);

        return true;
    }

    public static boolean SetKeepNewest(String OVT, String OVI, Object K) {
        return _SetKeepNewest(OVT, OVI, ObjToBool(K));
    }


    /*
     * RemoveDuplicates
     */
    public static boolean GetRemoveDuplicates(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetRemoveDuplicates:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isDeleteDuplicates();
    }

    public synchronized static boolean _SetRemoveDuplicates(String OVT, String OVI, Boolean Remove) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRemoveDuplicates:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetRemoveDuplicates: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetRemoveDuplicates: Setting RemoveDuplicates on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetRemoveDuplicates", new Object[] {OVT, OVI, Remove});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetRemoveDuplicates: null podcast.");
            return false;
        }

        podcast.setDeleteDuplicates(Remove);

        return true;
    }

    public static boolean SetRemoveDuplicates(String OVT, String OVI, Object R) {
        return _SetRemoveDuplicates(OVT, OVI, ObjToBool(R));
    }


    /*
     * UseTitleAsSubir
     */
    public static boolean GetUseTitleAsSubdir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetUseTitleAsSubdir:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isUseShowTitleAsSubdir();
    }

    public synchronized static boolean _SetUseTitleAsSubdir(String OVT, String OVI, Boolean Use) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleAsSubdir:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetUseTitleAsSubdir: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleAsSubdir: Setting UseTitleAsSubdir on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetUseTitleAsSubdir", new Object[] {OVT, OVI, Use});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetUseTitleAsSubdir: null podcast.");
            return false;
        }

        podcast.setUseShowTitleAsSubdir(Use);

        return true;
    }

    public static boolean SetUseTitleAsSubdir(String OVT, String OVI, Object U) {
        return _SetUseTitleAsSubdir(OVT, OVI, ObjToBool(U));
    }


    /*
     * UseShowTitleInFileName
     */
    public synchronized static boolean _SetUseTitleInFileName(String OVT, String OVI, Boolean Use) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleInFileName:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetUseTitleInFileName: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleInFileName: Setting UseTitleInFileName on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetUseTitleInFileName", new Object[] {OVT, OVI, Use});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetUseTitleInFileName: null podcast.");
            return false;
        }

        podcast.setUseShowTitleInFileName(Use);

        return true;
    }

    public static boolean SetUseTitleInFileName(String OVT, String OVI, Object U) {
        return _SetUseTitleInFileName(OVT, OVI, ObjToBool(U));
    }

    public static boolean GetUseTitleInFileName(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetUseTitleInFileName:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isUseShowTitleInFileName();
    }


    /*
     * ShowTitle
     */
    public static String GetShowTitle(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetShowTitle:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getShowTitle();
    }

    public synchronized static boolean SetShowTitle(String OVT, String OVI, String Title) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetShowTitle:");

        if (SageUtil.isNull(OVT, OVI, Title)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetShowTitle: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetShowTitle: Setting ShowTitle on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetShowTitle", new Object[] {OVT, OVI, Title});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetShowTitle: null podcast.");
            return false;
        }

        podcast.setShowTitle(API.StripShowTitle(Title));

        return true;
    }

    public static boolean SetShowTitle(String OVT, String OVI, Object Title) {
        return SetShowTitle(OVT, OVI, (String)Title);
    }


    /*
     * KeepAtMost
     */
    public static int GetKeepAtMost(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetKeepAtMost:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return 0;
        else
            return podcast.getMaxToRecord();
    }

    public static boolean SetKeepAtMost(String OVT, String OVI, Integer Keep) {
        return _SetKeepAtMost(OVT, OVI, Keep.intValue());
    }

    public static boolean SetKeepAtMost(String OVT, String OVI, Object Keep) {
        return _SetKeepAtMost(OVT, OVI, ObjToInteger(Keep));
    }

    public synchronized static boolean _SetKeepAtMost(String OVT, String OVI, Integer Keep) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepAtMost:");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetKeepAtMost: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepAtMost:  Setting on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetKeepAtMost", new Object[] {OVT, OVI, Keep});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetKeepAtMost: null podcast.");
            return false;
        }

        podcast.setMaxToRecord(Keep);
        return true;
    }


    /*
     * RecDir.
     */
    public static String GetRecDir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecDir:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getRecDir();
    }

    public synchronized static boolean SetRecDir(String OVT, String OVI, String Dir) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecDir:");

        if (SageUtil.isNull(OVT, OVI, Dir)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetRecDir: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecDir:  Setting on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetRecDir", new Object[] {OVT, OVI, Dir});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetRecDir: null podcast.");
            return false;
        }

        podcast.setRecDir(Dir);

        return true;
    }

    public static boolean SetRecDir(String OVT, String OVI, File Dir) {
        return SetRecDir(OVT, OVI, Dir.toString());
    }


    /*
     * Subdir
     */
    public static String GetSubdir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetSubdir:");

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getRecSubdir();
    }

    public synchronized static boolean SetSubdir(String OVT, String OVI, String Subdir) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetSubdir:");

        if (SageUtil.isNull(OVT, OVI, Subdir)) {
            Log.Write(Log.LOGLEVEL_ERROR, "SetSubdir: null parameter " + OVT + ":" + OVI);
            return false;
        }

        if (Global.IsClient()) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "SetSubdir: Setting on server.");
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetSubdir", new Object[] {OVT, OVI, Subdir});
            return true;
        }

        Podcast podcast = GetPodcast(OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "SetSubdir: null podcast.");
            return false;
        }

        podcast.setRecSubdir(Subdir);

        return true;
    }

    public static boolean SetSubdir(String OVT, String OVI, Object Subdir) {
        return SetSubdir(OVT, OVI, (String)Subdir);
    }


    /*
     * Creates a new Podcast in the database.  Does NOT mark the Podcast as a Favorite.
     */
    public synchronized static boolean _CreatePodcast(String OnlineVideoType,
                                                    String OnlineVideoItem,
                                                    String FeedContext,
                                                    Boolean bRecordNew,
                                                    Boolean bDeleteDuplicates,
                                                    Boolean bKeepNewest,
                                                    Boolean bReRecordDeleted,
                                                    Integer MaxToRecord,
                                                    Boolean bAutoDelete,
                                                    String RecordDir,
                                                    String RecordSubdir,
                                                    String ShowTitle,
                                                    Boolean bUseShowTitleAsSubdir,
                                                    Boolean bUseShowTitleInFileName) {

        Log.Write(Log.LOGLEVEL_TRACE, "CreatePodcast: Parameters = " +
                                        OnlineVideoType + ":" +
                                        OnlineVideoItem + ":" +
                                        FeedContext + ":" +
                                        bRecordNew + ":" +
                                        bDeleteDuplicates + ":" +
                                        bKeepNewest + ":" +
                                        bReRecordDeleted + ":" +
                                        MaxToRecord + ":" +
                                        bAutoDelete + ":" +
                                        RecordDir + ":" +
                                        RecordSubdir + ":" +
                                        ShowTitle + ":" +
                                        bUseShowTitleAsSubdir + ":" +
                                        bUseShowTitleInFileName);

        if (Global.IsClient()) {
            return createPodcastOnServer(OnlineVideoType, OnlineVideoItem, FeedContext, bRecordNew, bDeleteDuplicates, bKeepNewest, bReRecordDeleted, MaxToRecord, bAutoDelete, RecordDir, RecordSubdir, ShowTitle, bUseShowTitleAsSubdir, bUseShowTitleInFileName);
        }

        // See if the Podcast already exists in the database.
        Podcast podcast = DataStore.getPodcast(OnlineVideoType, OnlineVideoItem);

        // If Podcast does not already exist, create one. Otherwise remove the existing.
        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "CreatePodcast: Creating new Podcast.");
            podcast = new Podcast(false,
                                OnlineVideoType,
                                OnlineVideoItem,
                                FeedContext,
                                bRecordNew,
                                bDeleteDuplicates,
                                bKeepNewest,
                                bReRecordDeleted,
                                MaxToRecord,
                                bAutoDelete,
                                RecordDir,
                                RecordSubdir,
                                ShowTitle,
                                bUseShowTitleAsSubdir,
                                bUseShowTitleInFileName);
        } else {
            
            // Lock the existing podcast.
            //if (DataStore.getPodcastForUpdate(podcast, HALF_SECOND_TIMEOUT)==null) {
                //Log.getInstance().write(Log.LOGLEVEL_ERROR, "CreatePodcast: Failed to lock Podcast.");
                //return false;
            //}

            // Now remove it.
            if (!DataStore.removePodcast(podcast))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "CreatePodcast: Failed to remove.");
        }

        // Add the new or updated podcast;
        boolean result = DataStore.addPodcast(podcast);

        if (!result)
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "CreatePodcast: Failed to add.");

        return result;
    }

    public synchronized static boolean CreatePodcast(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordNew, Object DeleteDuplicates, Object KeepNewest, Object ReRecordDeleted, Object MaxToRecord, Object AutoDelete, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName) {
        Boolean result = _CreatePodcast(ObjToString(OnlineVideoType), ObjToString(OnlineVideoItem), ObjToString(FeedContext), ObjToBool(RecordNew), ObjToBool(DeleteDuplicates), ObjToBool(KeepNewest), ObjToBool(ReRecordDeleted), ObjToInteger(MaxToRecord), ObjToBool(AutoDelete), ObjToString(RecordDir), ObjToString(RecordSubdir), ObjToString(ShowTitle), ObjToBool(UseShowTitleAsSubdir), ObjToBool(UseShowTitleInFileName));
        return (result==null ? false : result);
    }

    public synchronized static void ShowClass(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object bRecordNew, Object bDeleteDuplicates, Object bKeepNewest, Object bReRecordDeleted, Object MaxToRecord, Object bAutoDelete, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object bUseShowTitleAsSubdir, Object bUseShowTitleInFileName) {
        if (Log.getInstance().GetLogLevel() >= Log.LOGLEVEL_TRACE)
            return;

        System.out.println("Class = " + OnlineVideoType.getClass());
        System.out.println("Class = " + OnlineVideoItem.getClass());
        System.out.println("Class = " + FeedContext.getClass());
        System.out.println("Class = " + bRecordNew.getClass());
        System.out.println("Class = " + bDeleteDuplicates.getClass());
        System.out.println("Class = " + bKeepNewest.getClass());
        System.out.println("Class = " + bReRecordDeleted.getClass());
        System.out.println("Class = " + MaxToRecord.getClass());
        System.out.println("Class = " + bAutoDelete.getClass());
        System.out.println("Class = " + RecordDir.getClass());
        System.out.println("Class = " + RecordSubdir.getClass());
        System.out.println("Class = " + ShowTitle.getClass());
        System.out.println("Class = " + bUseShowTitleAsSubdir.getClass());
        System.out.println("Class = " + bUseShowTitleInFileName.getClass());
}

    public static boolean createPodcastOnServer(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object bRecordNew, Object bDeleteDuplicates, Object bKeepNewest, Object bReRecordDeleted, Integer MaxToRecord, Object bAutoDelete, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object bUseShowTitleAsSubdir, Object bUseShowTitleInFileName) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "createPodcastOnServer: Creating podcast on server.");
        Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, "_CreatePodcast",
                                    new Object[] {  OnlineVideoType,
                                                    OnlineVideoItem,
                                                    FeedContext,
                                                    bRecordNew,
                                                    bDeleteDuplicates,
                                                    bKeepNewest,
                                                    bReRecordDeleted,
                                                    MaxToRecord,
                                                    bAutoDelete,
                                                    RecordDir,
                                                    RecordSubdir,
                                                    ShowTitle,
                                                    bUseShowTitleAsSubdir,
                                                    bUseShowTitleInFileName}, DEFAULT_TIMEOUT);
        return (result==null || !(result instanceof Boolean) ? false : (Boolean)result);
    }



    /**
     * Records a specific episode of a Podcast.  The podcast specified by OnlineVideoType and OnlineVideoItem
     * should already exist.  If it does not a new Podcast is created.
     * @param OnlineVideoType
     * @param OnlineVideoItem
     * @param FeedContext
     * @param RecordDir
     * @param RecordSubdir
     * @param ShowTitle
     * @param bUseShowTitleAsSubdir
     * @param ChanItem
     * @return
     */
    public static boolean _Record(String OnlineVideoType, String OnlineVideoItem, String FeedContext, String RecordDir, String RecordSubdir, String ShowTitle, Boolean UseShowTitleAsSubdir, Boolean UseShowTitleInFileName, RSSItem ChanItem) {
       
        if (Global.IsClient()) {
            return recordOnServer(OnlineVideoType, OnlineVideoItem, FeedContext, RecordDir, RecordSubdir, ShowTitle, UseShowTitleAsSubdir, UseShowTitleInFileName, ChanItem);
        }

        Podcast podcast = DataStore.getPodcast(OnlineVideoType, OnlineVideoItem);

        if (podcast==null) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Record: Creating new Podcast for " + OnlineVideoType + ":" + OnlineVideoItem);
            podcast = new Podcast(  false,                      // Is Not a Favorite
                                    OnlineVideoType,
                                    OnlineVideoItem,
                                    FeedContext,
                                    false,                      // Do not record new
                                    false,                      // Do not delete duplicates
                                    true,                       // Keep newest
                                    true,                       // ReRecord if deleted
                                    0,                          // Unlimited recordings
                                    false,                      // Do not autodelete
                                    RecordDir,
                                    RecordSubdir,
                                    ShowTitle,
                                    UseShowTitleAsSubdir,
                                    UseShowTitleInFileName);    // Use ShowTitle in FileName

        } else {
            
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Record: Found existing Podcast for " + OnlineVideoType + ":" + OnlineVideoItem);

            // ChannelsDotCom Podcasts can have the same OnlineVideoItem yet different FeedContexts.
            if (!podcast.getFeedContext().equalsIgnoreCase(FeedContext)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Record: Updating FeedContext " + podcast.getFeedContext() + "->" + FeedContext);
                podcast.setFeedContext(FeedContext);
            }
        }

        UnrecordedEpisode episode = new UnrecordedEpisode(podcast, ChanItem);

        return episode.startRecord().startsWith("REQ");
}

    public static boolean Record(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, RSSItem ChanItem) {
        return _Record(ObjToString(OnlineVideoType), ObjToString(OnlineVideoItem), ObjToString(FeedContext), ObjToString(RecDir), ObjToString(RecordSubdir), ObjToString(ShowTitle), ObjToBool(UseShowTitleAsSubdir), ObjToBool(UseShowTitleInFileName), ChanItem);
    }

    public static boolean recordOnServer(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, Object ChanItem) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "recordOnServer: Recording on server.");
        Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, 
                                                            "_Record",
                                                            new Object[] {OnlineVideoType, OnlineVideoItem, FeedContext, RecordDir, RecordSubdir, ShowTitle, UseShowTitleAsSubdir, UseShowTitleInFileName, ChanItem},
                                                            DEFAULT_TIMEOUT);

        return (result==null || !(result instanceof Boolean) ? false : (Boolean)result);
    }


    public static void ShowClass(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, Object ChanItem) {
        if (Log.getInstance().GetLogLevel() >= Log.LOGLEVEL_TRACE)
            return;

        System.out.println("Class = " + OnlineVideoType.getClass() + OnlineVideoType.toString());
        System.out.println("Class = " + OnlineVideoItem.getClass() + OnlineVideoItem.toString());
        System.out.println("Class = " + FeedContext.getClass() + FeedContext.toString());
        System.out.println("Class = " + RecordDir.getClass() + RecordDir.toString());
        System.out.println("Class = " + RecordSubdir.getClass() + RecordSubdir.toString());
        System.out.println("Class = " + ShowTitle.getClass() + ShowTitle.toString());
        System.out.println("Class = " + UseShowTitleAsSubdir.getClass() + UseShowTitleAsSubdir.toString());
        System.out.println("Class = " + UseShowTitleInFileName.getClass() + UseShowTitleInFileName.toString());
        System.out.println("Class = " + ChanItem.getClass() + ChanItem.toString());
        return;
    }
   
    public static boolean recordAllEpisodesOnServer(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object bUseShowTitleAsSubdir) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "recordAllEpisodesOnServer: Recording All on server.");
        Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, "RecordAllEpisodes",
                                                            new Object[] {OnlineVideoType, OnlineVideoItem,
                                                                            FeedContext, RecordDir, RecordSubdir,
                                                                            ShowTitle, bUseShowTitleAsSubdir},
                                                            DEFAULT_TIMEOUT);

        return (result==null || !(result instanceof Boolean) ? false : (Boolean)result);
    }


    private static Boolean ObjToBool(Object O) {
        if (O == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter in ObjToBool.");
            return false;
        } else if (O instanceof Boolean) {
            return (Boolean)O;
        } else if (O instanceof String) {
            String S = (String)O;
            return Boolean.valueOf(S);
        } else {
            return (Boolean)O;
        }
    }

    private static String ObjToString(Object O) {
        if (O == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter in ObjToString.");
            return "NULL";
        } else if (O instanceof String) {
            return (String)O;
        } else if (O instanceof File) {
            File F = (File)O;
            return F.toString();
        } else
            return O.toString();
    }

    private static Integer ObjToInteger(Object Obj) {
        if (Obj == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter in ObjToInteger.");
            return 0;
        } else if (Obj instanceof Integer) {
            return (Integer)Obj;
        } else if (Obj instanceof String) {
            try {
                Integer I = Integer.parseInt((String)Obj);
                return I;
            } catch (NumberFormatException e) {
                return 0;
            }
        } else
            return 0;
    }

    public static void showFavoriteDatabase() {
        Podcast podcast = new Podcast();
        podcast.dumpFavorites();
    }
}
