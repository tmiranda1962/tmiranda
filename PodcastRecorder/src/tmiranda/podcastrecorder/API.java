/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import java.io.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class API {

    private static final API instance = new API();

    /*
     * This is the object that co-ordinates sending and receiving data from a remote Sage server.  It's
     * only used when the API is being executed on a SageClient.
     */
    private MQDataGetter MQDataGetter;

    private static final String THIS_CLASS = "tmiranda.podcastrecorder.API";
    private static final long   DEFAULT_TIMEOUT = 5000L;
    private static final long   TEN_SECOND_TIMEOUT = 10000L;

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

    /**
     * The class provides the Sage STV all of the functionality needed to:
     * - Manually record one or more podcasts.
     * - Define Favorite podcasts that may be automatically downloaded as new episodes become available.
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
     * Gets the MQDataGetter instance for this API instance.  This method should not be called directly by
     * the Sage STV.
     */
    private static MQDataGetter GetMQDataGetter() {
        return API.getInstance().MQDataGetter;
    }

    public static boolean IsServerAlive() {
        if (Global.IsClient()) {
            Boolean RC = (Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsServerAlive", DEFAULT_TIMEOUT);
            return (RC==null ? false : RC);
        } else {
            return true;
        }
    }

    public static String MakeHuluOVI(String SearchURL) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "MakeHuluOVI from " + SearchURL);

        String OVI = null;

        String FeedParts[] = SearchURL.split(",",3);
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
                    Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI Bad FeedParam.");
                }
                break;
            default:
                Log.Write(Log.LOGLEVEL_ERROR, "MakeHuluOVI Found bad SearchURL.");
                break;
        }

        return OVI;
    }

    public static void ShowRSSItem(RSSItem Item) {
        if (Item == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "ShowRSSItem null Item.");
            return;
        }
        System.out.println("PR: Showing RSSItem " + Item.getTitle() + ":" + Item.getCleanDescription() + ":" + Item.getLink());
    }

    public static void ShowRSSItems(List<RSSItem> Items) {
        if (Items == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "ShowRSSItems null Items.");
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
        Log.Write(Log.LOGLEVEL_TRACE, "Manually running RecordManager.");
        if (Global.IsClient()) {
            GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "RecordManagerManualRun");
        } else {
            Plugin.RecordManagerManualRun();
        }
    }

    public static boolean IsDynamicSubCat(String OVT, String OVI, RSSItem ChanItem) {

        if (OVI != null && OVT.startsWith("xChannelsDotCom") && OVI.startsWith("xChannelsDotComCatList")) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat ChannelsDotCom List. Returning true. " + OVI);
            return true;
        }

        RSSEnclosure Enclosure = ChanItem.getEnclosure();

        if (Enclosure==null) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat found null Enclosure. Returning false.");
            return false;
        }

        String Type = Enclosure.getType().toLowerCase();
        Log.Write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat type is " + Type);

        if (Type!=null && Type.contains("sagetv")) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsDynamicSubCat found sagetv Enclosure. Returning true.");
            return true;
        }

        return false;
    }
    
    public static String GetFeedName(RSSItem ChanItem) {
        if (ChanItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "GetFeedName null Item.");
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
            Log.Write(Log.LOGLEVEL_ERROR, "Null RSSItem passed to GetMediaFileForRSSItem");
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
            Log.Write(Log.LOGLEVEL_ERROR, "Null RSSItem passed to DeleteMediaFileForRSSItem");
            return false;
        }

        if (Global.IsClient()) {
            return (Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "DeleteMediaFileForRSSItem", new Object[] {Item}, TEN_SECOND_TIMEOUT);
        } else {
            Object MediaFile = RSSHelper.getMediaFileForRSSItem(Item);
            if (MediaFile==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Null MediaFile for RSSItem.");
                return false;           
            } else {
                return MediaFileAPI.DeleteFile(MediaFile);
            }
        }
    }

    public static boolean IsRSSItemOnDisk(RSSItem Item) {
        if (Item==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "Null RSSItem passed to IsRSSItemOnDisk");
            return false;
        }

        if (Global.IsClient()) {
            Boolean RC =(Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRSSItemOnDisk", new Object[] {Item}, TEN_SECOND_TIMEOUT);
            return (RC==null ? false : RC);
        } else {
            return (RSSHelper.getMediaFileForRSSItem(Item)!=null);
        }
    }

    /**
     * Checks to see if any episode is currently being recorded.
     * <p>
     * @return true if any podcast is being recorded, false otherwise.
     */
    public static boolean IsRecording() {
        if (Global.IsClient()) {
            Boolean RC = (Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRecording", DEFAULT_TIMEOUT);
            return (RC==null ? false : RC);
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
            Long RC = (Long)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetCurrentDownloadSize", DEFAULT_TIMEOUT);
            return (RC==null ? 0 : RC);
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
            return (List<RSSItem>)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForCurrent", DEFAULT_TIMEOUT);
        } else {
            return DownloadManager.getInstance().getRSSItemsForCurrent();
        }
    }

    public static boolean IsRecordingNow(RSSItem RSSItem) {
        if (RSSItem==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "Null RSSItem passed to IsRecordingNow");
            return false;
        }

        if (Global.IsClient()) {
            Boolean RC = (Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsRecordingNow", new Object[] {RSSItem}, DEFAULT_TIMEOUT);
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsRecordingNow returning " + RC);
            return (RC==null ? false : RC);
        } else {
            List<RSSItem> Items = DownloadManager.getInstance().getRSSItemsForCurrent();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsRecordingNow returning " + RSSHelper.RSSListContains(Items, RSSItem));
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
            Integer RC = (Integer)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeActive", DEFAULT_TIMEOUT);
            return (RC==null ? 0 : RC);
        } else {
            return DownloadManager.getInstance().getSizeActive();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<RSSItem> GetRSSItemsForActive() {
        if (Global.IsClient()) {
            return (List<RSSItem>)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForActive", DEFAULT_TIMEOUT);
        } else {
            return DownloadManager.getInstance().getRSSItemsForActive();
        }
    }

    public static boolean IsInQueue(RSSItem RSSItem) {
        if (RSSItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "IsInQueue null Item.");
            return false;
        }

        if (Global.IsClient()) {
            Boolean RC = (Boolean)GetMQDataGetter().getDataFromServer(THIS_CLASS, "IsInQueue", new Object[] {RSSItem}, DEFAULT_TIMEOUT);
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsInQueue returning " + RC);
            return (RC==null ? false : RC);
        } else {
            List<RSSItem>Items = DownloadManager.getInstance().getRSSItemsForActive();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "IsInQueue returning " + RSSHelper.RSSListContains(Items, RSSItem));
            return RSSHelper.RSSListContains(Items, RSSItem);
        }
    }

    public static void RemoveFromQueue(RSSItem RSSItem) {
        if (RSSItem == null) {
            Log.Write(Log.LOGLEVEL_ERROR, "RemoveFromQueue null Item.");
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
            return (List<RSSItem>)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForCompleted", DEFAULT_TIMEOUT);
        } else {
            return DownloadManager.getInstance().getRSSItemsForCompleted();
        }
    }
    
    public static Integer GetSizeCompleted() {
        if (Global.IsClient()) {
            Integer RC = (Integer)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeCompleted", DEFAULT_TIMEOUT);
            return (RC==null ? 0 : RC);
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
            return (List<RSSItem>)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetRSSItemsForFailed", DEFAULT_TIMEOUT);
        } else {
            return DownloadManager.getInstance().getRSSItemsForFailed();
        }
    }
   
    public static Integer GetSizeFailed() {
        if (Global.IsClient()) {
            Integer RC = (Integer)GetMQDataGetter().getDataFromServer(THIS_CLASS, "GetSizeFailed", DEFAULT_TIMEOUT);
            return (RC==null ? 0 : RC);
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
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return null;
        }
        return podcast.replaceAll("()", "");
    }

    public static String GetTextForPodcast(Properties properties, String podcast) {

        if (properties==null || podcast==null) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return null;
        }

        String Text = podcast;

        if (podcast.startsWith("ez")) {
            Text = properties.getProperty(podcast+"/Name", null);
            Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast ez returning " + Text);
            return (Text == null ? podcast : Text);
        } else {
            Text = properties.getProperty("Source/" + podcast + "/LongName", null);
            if (Text!=null) {
                Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast Source/LongName returning " + Text);
                return Text;
            } else {
                Text = properties.getProperty("Category/" + podcast + "/FullName", null);
                if (Text!=null) {
                    Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast FullName returning " + Text);
                    return Text;
                } else {
                    Text = properties.getProperty("Category/" + podcast + "/LongName", null);
                    Log.Write(Log.LOGLEVEL_VERBOSE, "GetTextForPodcast LongName returning " + Text);
                    return (Text==null ? podcast : Text);
                }
            }
        }
    }

    /*
     * Returns all Favorite Podcasts.
     */
    public static List<String> GetAllFavoritePodcasts() {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetAllFavoritePodcasts.");

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = Podcast.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        List<String> names = new ArrayList<String>();

        for (Podcast p : favoritePodcasts) {
            if (p.isIsFavorite())
                if (!names.add(p.getOnlineVideoItem()))
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "GetAllFavoritePodcasts failed to add.");
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

    public static synchronized List<String> IncreasePodcastPriority(String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "IncreasePodcastPriority.");

        List<Podcast> favoritePodcasts;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = Podcast.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        // This will be the new list of OnlineVideoItems. (Podcasst for the STV.)
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
        if (!Podcast.writeFavoritePodcasts(newPodcastList)) {
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
            favoritePodcasts = Podcast.readFavoritePodcasts();
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
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "DecreasePodcastPriority failed to add.");;

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
        if (!Podcast.writeFavoritePodcasts(newPodcastList)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Error writing favorite Podcasts.");
        }

        // Create the List of OVIs.
        for (Podcast p : newPodcastList) {
            if (!OVIs.add(p.getOnlineVideoItem()))
                Log.getInstance().printStackTrace();
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

    public static String GetOVTforPodcast(String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetOVTforPodcast.");

        if (SageUtil.isNull(OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return null;
        }

        List<Podcast> favoritePodcasts = null;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = Podcast.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        for (Podcast p : favoritePodcasts) {
            if (p.getOnlineVideoItem().equalsIgnoreCase(OVI)) {
                return p.getOnlineVideoType();
            }
        }

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetOVTforPodcast did not find match for " + OVI);
        return null;
    }

    public static String GetOVTforOVI(String OVI) {
        return GetOVTforPodcast(OVI);
    }

    public static Podcast GetPodcast(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_ALL, "GetPodcast.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return null;
        }

        List<Podcast> favoritePodcasts = null;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = Podcast.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            return null;
        }

        return Podcast.findPodcast(favoritePodcasts, OVT, OVI);
    }

    public static boolean PodcastExists(String OVT, String OVI) {
        return (GetPodcast(OVT,OVI)==null ? false : true);
    }

    public static long GetLastChecked(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetLastChecked.");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0L;
        else return podcast.getLastChecked();
    }

    public static int GetDuplicatesDeleted(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetDuplicatesDeleted.");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else return podcast.getDuplicatesDeleted();
    }

    public static int GetEpisodesOnServer(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesOnServer.");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else
            return podcast.getEpisodesOnServerSize();
    }

    public static int GetEpisodesOnDisk(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesOnDisk.");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else return podcast.getEpisodesOnDisk().size();
    }

    public static int GetEpisodesEverRecorded(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "GetEpisodesEverRecorded.");
        Podcast podcast = GetPodcast(OVT, OVI);
        if (podcast==null)
            return 0;
        else
            return podcast.getEpisodesEverRecordedSize();
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

        Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite2: Looking for " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_WARN, "null parameter.");
            return false;
        }

        List<Podcast> favoritePodcasts = null;

        if (Global.IsClient()) {
            favoritePodcasts = getFavoritePodcastsFromServer();
        } else {
            favoritePodcasts = Podcast.readFavoritePodcasts();
        }

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "IsPodcastFavorite: No favorites.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null || !podcast.isIsFavorite()) {
            Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite: Not a favorite.");
            return false;
        } else {
            Log.Write(Log.LOGLEVEL_ALL, "IsPodcastFavorite: Is a favorite.");
            return true;
        }
    }

    public synchronized static boolean _SetIsFavorite(String OVT, String OVI, Boolean Favorite) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetIsFavorite.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setIsFavoriteOnServer(OVT, OVI, Favorite);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_TRACE, "Failed to remove podcast.");
            return false;
        }

        podcast.setIsFavorite(Favorite);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_TRACE, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetIsFavorite(String OVT, String OVI, Object F) {
        return _SetIsFavorite(OVT, OVI, ObjToBool(F));
    }

    private static boolean setIsFavoriteOnServer(String OVT, String OVI, Boolean Favorite) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting Isfavorite on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetIsFavorite", new Object[] {OVT, OVI, Favorite});
        return true;
    }

    @SuppressWarnings("unchecked")
    public static List<Podcast> getFavoritePodcastsFromServer() {
        Log.Write(Log.LOGLEVEL_ALL, "Getting podcasts from server.");

        List<Podcast> Podcasts = (List<Podcast>)GetMQDataGetter().getDataFromServer("tmiranda.podcastrecorder.Podcast", "readFavoritePodcasts", new Object[] {cacheDate}, DEFAULT_TIMEOUT);

        // If it's not null it contains updated Podcasts.  If it is null the Podcasts in the local cache are still valid.
        if (Podcasts!=null) {
            Log.Write(Log.LOGLEVEL_VERBOSE, "API Podcast cache updated.");
            PodcastCache = Podcasts;
            cacheDate = new Date();
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

        Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Removing " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return removeFavoriteOnServer(OVT, OVI);
        }

        // Get the current Favorites.
        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: No favorites.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Is not a favorite.");
            return false;
        }

        // Remove the podcast.
        if (!favoritePodcasts.remove(podcast))
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RemoveFavorite failed to add.");

        Log.Write(Log.LOGLEVEL_TRACE, "RemoveFavorite: Removed.");

        // Save to disk.
        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean removeFavoriteOnServer(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Removing favorite on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "RemoveFavorite", new Object[] {OVT, OVI});
        return true;
    }

    public synchronized static boolean DisableFavorite(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_TRACE, "DisableFavorite: Disabling " + OVT + ":" + OVI);

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return disableFavoriteOnServer(OVT, OVI);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null || favoritePodcasts.isEmpty()) {
            Log.Write(Log.LOGLEVEL_TRACE, "No podcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "Did not findPodcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setIsFavorite(false);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean disableFavoriteOnServer(String OVT, String OVI) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Podcast: Disabling favorite on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "DisableFavorite", new Object[] {OVT, OVI});
        return true;
    }

    public static String GetFeedContext(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetFeedContext.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getFeedContext();
    }

    /*
     * RecordNew
     */
    public static boolean GetRecordNew(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetRecordNew.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isRecordNew();
    }

    public static boolean SetRecordNew(String OVT, String OVI, Object RN) {
        return _SetRecordNew(OVT, OVI, ObjToBool(RN));
    }

    public synchronized static boolean _SetRecordNew(String OVT, String OVI, Boolean RecordNew) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecordNew.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setRecordNewOnServer(OVT, OVI, RecordNew);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setRecordNew(RecordNew);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    private static boolean setRecordNewOnServer(String OVT, String OVI, Boolean RecordNew) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting record new on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetRecordNew", new Object[] {OVT, OVI, RecordNew});
        return true;
    }

    /*
     * ReRecord
     */
    public static boolean GetReRecord(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetReRecord.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isReRecordDeleted();
    }

    public synchronized static boolean _SetReRecord(String OVT, String OVI, Boolean ReRecord) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetReRecord.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setReRecordOnServer(OVT, OVI, ReRecord);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setReRecordDeleted(ReRecord);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetReRecord(String OVT, String OVI, Object RR) {
        return _SetReRecord(OVT, OVI, ObjToBool(RR));
    }

    private static boolean setReRecordOnServer(String OVT, String OVI, Boolean ReRecord) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting ReRecord on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetReRecord", new Object[] {OVT, OVI, ReRecord});
        return true;
    }

    /*
     * AutoDelete
     */
    public static boolean GetAutoDelete(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetAutoDelete.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isAutoDelete();
    }

    public synchronized static boolean _SetAutoDelete(String OVT, String OVI, Boolean AutoDelete) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetAutoDelete.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setAutoDeleteOnServer(OVT, OVT, AutoDelete);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setAutoDelete(AutoDelete);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetAutoDelete(String OVT, String OVI, Object AD) {
        return _SetAutoDelete(OVT, OVI, ObjToBool(AD));
    }

    private static boolean setAutoDeleteOnServer(String OVT, String OVI, Boolean AutoDelete) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting AutoDelete on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetAutoDelete", new Object[] {OVT, OVI, AutoDelete});
        return true;
    }

    /*
     * KeepNewest
     */
    public static boolean GetKeepNewest(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetKeepNewest.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isKeepNewest();
    }

    public synchronized static boolean _SetKeepNewest(String OVT, String OVI, Boolean Keep) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepNewest.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setKeepNewestOnServer(OVT, OVI, Keep);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setKeepNewest(Keep);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetKeepNewest(String OVT, String OVI, Object K) {
        return _SetKeepNewest(OVT, OVI, ObjToBool(K));
    }

    private static boolean setKeepNewestOnServer(String OVT, String OVI, Boolean Keep) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting KeepNewest on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetKeepNewest", new Object[] {OVT, OVI, Keep});
        return true;
    }

    /*
     * RemoveDuplicates
     */
    public static boolean GetRemoveDuplicates(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetRemoveDuplicates.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isDeleteDuplicates();
    }

    public synchronized static boolean _SetRemoveDuplicates(String OVT, String OVI, Boolean Remove) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRemoveDuplicates.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setRemoveDuplicatesOnServer(OVT, OVI, Remove);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setDeleteDuplicates(Remove);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetRemoveDuplicates(String OVT, String OVI, Object R) {
        return _SetRemoveDuplicates(OVT, OVI, ObjToBool(R));
    }

    private static boolean setRemoveDuplicatesOnServer(String OVT, String OVI, Boolean Remove) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting RemoveDuplicates on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetRemoveDuplicates", new Object[] {OVT, OVI, Remove});
        return true;
    }

    /*
     * UseTitleAsSubir
     */
    public static boolean GetUseTitleAsSubdir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetUseTitleAsSubdir.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isUseShowTitleAsSubdir();
    }

    public synchronized static boolean _SetUseTitleAsSubdir(String OVT, String OVI, Boolean Use) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleAsSubdir.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setUseTitleAsSubdirOnServer(OVT, OVI, Use);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setUseShowTitleAsSubdir(Use);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetUseTitleAsSubdir(String OVT, String OVI, Object U) {
        return _SetUseTitleAsSubdir(OVT, OVI, ObjToBool(U));
    }

    private static boolean setUseTitleAsSubdirOnServer(String OVT, String OVI, Boolean Use) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting UseTitleAsSubdir on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetUseTitleAsSubdir", new Object[] {OVT, OVI, Use});
        return true;
    }

    /*
     * UseShowTitleInFileName
     */
    public synchronized static boolean _SetUseTitleInFileName(String OVT, String OVI, Boolean Use) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetUseTitleInFileName.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setUseTitleInFileNameOnServer(OVT, OVI, Use);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setUseShowTitleInFileName(Use);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetUseTitleInFileName(String OVT, String OVI, Object U) {
        return _SetUseTitleInFileName(OVT, OVI, ObjToBool(U));
    }

    public static boolean GetUseTitleInFileName(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetUseTitleInFileName.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return false;
        else
            return podcast.isUseShowTitleInFileName();
    }

    private static boolean setUseTitleInFileNameOnServer(String OVT, String OVI, Boolean Use) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting UseTitleInFileName on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetUseTitleInFileName", new Object[] {OVT, OVI, Use});
        return true;
    }

    /*
     * ShowTitle
     */
    public static String GetShowTitle(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetShowTitle.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getShowTitle();
    }

    public synchronized static boolean SetShowTitle(String OVT, String OVI, String Title) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetShowTitle.");

        if (SageUtil.isNull(OVT, OVI, Title)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setShowTitleOnServer(OVT, OVI, Title);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setShowTitle(API.StripShowTitle(Title));

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetShowTitle(String OVT, String OVI, Object Title) {
        return SetShowTitle(OVT, OVI, (String)Title);
    }

    private static boolean setShowTitleOnServer(String OVT, String OVI, String Title) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting ShowTitle on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetShowTitle", new Object[] {OVT, OVI, Title});
        return true;
    }

    /*
     * KeepAtMost
     */
    public static int GetKeepAtMost(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetKeepAtMost.");

        Podcast podcast = Podcast.Find(OVT, OVI);

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

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetKeepAtMost.");

        if (SageUtil.isNull(OVT, OVI)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setKeepAtMostOnServer(OVT, OVI, Keep);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setMaxToRecord(Keep);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    private static boolean setKeepAtMostOnServer(String OVT, String OVI, Integer Keep) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting KeepAtMost on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "_SetKeepAtMost", new Object[] {OVT, OVI, Keep});
        return true;
    }

    /*
     * RecDir.
     */
    public static String GetRecDir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecDir.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getRecDir();
    }

    public synchronized static boolean SetRecDir(String OVT, String OVI, String Dir) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetRecDir.");

        if (SageUtil.isNull(OVT, OVI, Dir)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setRecDirOnServer(OVT, OVI, Dir);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setRecDir(Dir);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetRecDir(String OVT, String OVI, File Dir) {
        return SetRecDir(OVT, OVI, Dir.toString());
    }

    private static boolean setRecDirOnServer(String OVT, String OVI, String Dir) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting RecDir on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetRecDir", new Object[] {OVT, OVI, Dir});
        return true;
    }

    /*
     * Subdir
     */
    public static String GetSubdir(String OVT, String OVI) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "GetSubdir.");

        Podcast podcast = Podcast.Find(OVT, OVI);

        if (podcast==null)
            return null;
        else
            return podcast.getRecSubdir();
    }

    public synchronized static boolean SetSubdir(String OVT, String OVI, String Subdir) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "SetSubdir.");

        if (SageUtil.isNull(OVT, OVI, Subdir)) {
            Log.Write(Log.LOGLEVEL_ERROR, "null parameter.");
            return false;
        }

        if (Global.IsClient()) {
            return setSubdirOnServer(OVT, OVI, Subdir);
        }

        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "No favoritePodcasts.");
            return false;
        }

        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OVT, OVI);

        if (podcast == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "null podcast.");
            return false;
        }

        if (!favoritePodcasts.remove(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to remove podcast.");
            return false;
        }

        podcast.setRecSubdir(Subdir);

        if (!favoritePodcasts.add(podcast)) {
            Log.Write(Log.LOGLEVEL_ERROR, "Failed to add podcast.");
            return false;
        }

        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public static boolean SetSubdir(String OVT, String OVI, Object Subdir) {
        return SetSubdir(OVT, OVI, (String)Subdir);
    }

    private static boolean setSubdirOnServer(String OVT, String OVI, String Subdir) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Setting Subdir on server.");
        GetMQDataGetter().invokeMethodOnServer(THIS_CLASS, "SetSubdir", new Object[] {OVT, OVI, Subdir});
        return true;
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

        Log.Write(Log.LOGLEVEL_TRACE, "CreatePodcast parameters = " +
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

        // Get the current Favorites.
        List<Podcast> favoritePodcasts = Podcast.readFavoritePodcasts();

        // If there are no Favorites create a new list.
        if (favoritePodcasts == null) {
            Log.Write(Log.LOGLEVEL_TRACE, "CreatePodcast: Creating new Favorite array.");
            favoritePodcasts = new ArrayList<Podcast>();
        }

        // See if this one exists already.
        Podcast podcast = Podcast.findPodcast(favoritePodcasts, OnlineVideoType, OnlineVideoItem);

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
            if (!favoritePodcasts.remove(podcast))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Createpodcast failed to remove.");
        }

        // Add the new or updated podcast;
        if (!favoritePodcasts.add(podcast))
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Createpodcast failed to add.");

        // Save to disk.
        return Podcast.writeFavoritePodcasts(favoritePodcasts);
    }

    public synchronized static boolean CreatePodcast(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordNew, Object DeleteDuplicates, Object KeepNewest, Object ReRecordDeleted, Object MaxToRecord, Object AutoDelete, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName) {
        Boolean result = _CreatePodcast(ObjToString(OnlineVideoType), ObjToString(OnlineVideoItem), ObjToString(FeedContext), ObjToBool(RecordNew), ObjToBool(DeleteDuplicates), ObjToBool(KeepNewest), ObjToBool(ReRecordDeleted), ObjToInteger(MaxToRecord), ObjToBool(AutoDelete), ObjToString(RecordDir), ObjToString(RecordSubdir), ObjToString(ShowTitle), ObjToBool(UseShowTitleAsSubdir), ObjToBool(UseShowTitleInFileName));
        return (result==null ? false : result);
    }

    public synchronized static void ShowClass(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object bRecordNew, Object bDeleteDuplicates, Object bKeepNewest, Object bReRecordDeleted, Object MaxToRecord, Object bAutoDelete, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object bUseShowTitleAsSubdir, Object bUseShowTitleInFileName) {
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
        Log.Write(Log.LOGLEVEL_VERBOSE, "Creating podcast on server.");
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
        return (result==null ? false : (Boolean)result);
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

        Podcast podcast = Podcast.Find(OnlineVideoType, OnlineVideoItem);

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
        }

        UnrecordedEpisode episode = new UnrecordedEpisode(podcast, ChanItem);

        return episode.startRecord().startsWith("REQ");
}

    public static boolean Record(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, RSSItem ChanItem) {
        return _Record(ObjToString(OnlineVideoType), ObjToString(OnlineVideoItem), ObjToString(FeedContext), ObjToString(RecDir), ObjToString(RecordSubdir), ObjToString(ShowTitle), ObjToBool(UseShowTitleAsSubdir), ObjToBool(UseShowTitleInFileName), ChanItem);
    }

    public static boolean recordOnServer(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, Object ChanItem) {

        Log.Write(Log.LOGLEVEL_VERBOSE, "Recording on server.");
        Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, 
                                                            "_Record",
                                                            new Object[] {OnlineVideoType, OnlineVideoItem, FeedContext, RecordDir, RecordSubdir, ShowTitle, UseShowTitleAsSubdir, UseShowTitleInFileName, ChanItem},
                                                            DEFAULT_TIMEOUT);

        return (result==null ? false : (Boolean)result);
    }


    public static void ShowClass(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object UseShowTitleAsSubdir, Object UseShowTitleInFileName, Object ChanItem) {
        System.out.println("Class = " + OnlineVideoType.getClass());
        System.out.println("Class = " + OnlineVideoItem.getClass());
        System.out.println("Class = " + FeedContext.getClass());
        System.out.println("Class = " + RecordDir.getClass());
        System.out.println("Class = " + RecordSubdir.getClass());
        System.out.println("Class = " + ShowTitle.getClass());
        System.out.println("Class = " + UseShowTitleAsSubdir.getClass());
        System.out.println("Class = " + UseShowTitleInFileName.getClass());
        System.out.println("Class = " + ChanItem.getClass());
        return;
    }
   
    public static boolean OLDRecordAllEpisodes(String OnlineVideoType, String OnlineVideoItem, String FeedContext, String RecordDir, String RecordSubdir, String ShowTitle, Boolean bUseShowTitleAsSubdir) {


        if (Global.IsClient()) {
            return recordAllEpisodesOnServer(OnlineVideoType, OnlineVideoItem, FeedContext, RecordDir, RecordSubdir, ShowTitle, bUseShowTitleAsSubdir);
        }

        Podcast podcast = new Podcast(  false,                  // Is Not a Favorite
                                        OnlineVideoType.toString(),
                                        OnlineVideoItem.toString(),
                                        FeedContext.toString(),
                                        false,                  // Do not record new
                                        false,                  // Do not delete duplicates
                                        true,                   // Keep the newest
                                        true,                   // Rerecord if deleted
                                        0,                      // Unlimited number of recordings
                                        false,                  // Do not auto delete
                                        RecordDir.toString(),
                                        RecordSubdir.toString(),
                                        ShowTitle.toString(),
                                        SageUtil.StringToBool(bUseShowTitleAsSubdir.toString()),
                                        true);                  // Use ShowTitle in FileName

        Set<UnrecordedEpisode> UnrecordedEpisodes = podcast.getEpisodesOnWebServer();

        if (UnrecordedEpisodes == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RecordAllEpisodes: Failed to getEpisodesOnWebServer.");
            return false;
        }

        boolean failed = false;

        for(UnrecordedEpisode episode : UnrecordedEpisodes) {
            if (!episode.startRecord().startsWith("REQ")) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RecordAllEpisodes: Failed to startRecord.");
                failed = true;
            }
        }

        return failed;
    }

    public static boolean recordAllEpisodesOnServer(Object OnlineVideoType, Object OnlineVideoItem, Object FeedContext, Object RecordDir, Object RecordSubdir, Object ShowTitle, Object bUseShowTitleAsSubdir) {
        Log.Write(Log.LOGLEVEL_VERBOSE, "Recording All on server.");
        Object result = GetMQDataGetter().getDataFromServer(THIS_CLASS, "RecordAllEpisodes",
                                                            new Object[] {OnlineVideoType, OnlineVideoItem,
                                                                            FeedContext, RecordDir, RecordSubdir,
                                                                            ShowTitle, bUseShowTitleAsSubdir},
                                                            DEFAULT_TIMEOUT);

        return (result==null ? false : (Boolean)result);
    }

    public static boolean OLDRecordAllEpisodes(String OnlineVideoType, String OnlineVideoItem, String FeedContext, File RecordDir, String RecordSubdir, String ShowTitle, Boolean UseShowTitleAsSubdir) {
        return OLDRecordAllEpisodes(OnlineVideoType, OnlineVideoItem, FeedContext, RecordDir.toString(), RecordSubdir, ShowTitle, UseShowTitleAsSubdir);
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
}
