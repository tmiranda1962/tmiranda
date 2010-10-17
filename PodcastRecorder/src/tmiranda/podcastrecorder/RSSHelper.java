/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import java.net.*;
import java.io.*;
import sagex.api.*;
import sage.media.rss.*;

/**
 *
 * @author Tom Miranda.
 */
public class RSSHelper {

    public static void setRedirects() {
        java.net.HttpURLConnection.setFollowRedirects(true);
    }

    public static String makeID(RSSItem ChanItem) {

        String RSSString =  ChanItem.getTitle() +
                            ChanItem.getDate() +
                            ChanItem.getCleanDescription() +
                            ChanItem.getComments() +
                            ChanItem.getLink() +
                            ChanItem.getAuthor() +
                            ChanItem.getDuration();

        return RSSString.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public static RSSItem makeDummyRSSItem() {
        RSSItem Item = new RSSItem();
        Item.setTitle("Dummy Title");
        Item.setDate("Dummy Date");
        Item.setDescription("Dummy Description");
        Item.setComments("Dummy Comments");
        Item.setLink("Dummy Link");
        Item.setAuthor("Dummy Author");
        Item.setDuration(0);
        return Item;
    }


    /**
    * Returns a MediaFile object corresponding to a Podcast ID.
    * <p>
    * @param  Item a String that is the Podcast ID.
    * @return      MediaFile object corresponding to the Podcast ID or null if it does not exist.
    */
    public static Object getMediaFileForRSSString(String RSSString) {
        Object[] MediaFilesAll = MediaFileAPI.GetMediaFiles("VM");

        if (MediaFilesAll == null || RSSString == null)
            return null;

        for (int i=0; i < MediaFilesAll.length; i++) {
            if (RSSString.compareTo(ShowAPI.GetShowMisc(MediaFilesAll[i])) == 0) {
                return MediaFilesAll[i];
            }
        }

        return null;
    }

    /**
    * Returns a MediaFile object corresponding to an RSSItem.
    * <p>
    * @param  Item an RSSItem
    * @return      MediaFile object corresponding to the RSSItem or null if it does not exist.
    */
    public static Object getMediaFileForRSSItem(RSSItem Item) {
        return getMediaFileForRSSString(makeID(Item));
    }

    /*
     * Duplicate what happens when item is selected in the Online Video Menu. First we need to get the FeedContext.
     */
    public static String getFeedContext(Properties Props, String OnlineVideoType, String OnlineVideoItem) {

        if (Props==null || OnlineVideoType==null || OnlineVideoItem==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFeedContext found null parameter.");
            return null;
        }

        String OVT = OnlineVideoType;
        String OVI = OnlineVideoItem;
        String FeedContext = null;

        if (OVI.startsWith("xBack")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found xBack.");
            return null;
        }

        String IsCat = Props.getProperty(OVI+"/IsCategory");
        if (IsCat!=null && IsCat.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found Subcategory.");
            return null;
        }

        String TrackingCategory = Props.getProperty(OVI+"/TrackingCat");
        if (TrackingCategory !=null && !TrackingCategory.isEmpty()) {
            OVI = Configuration.GetProperty("online_video/last_sub_category/" + TrackingCategory, TrackingCategory);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found TrackingCategory " + TrackingCategory + ":" + OVI);
        }

        String IsSearch = Props.getProperty(OVI+"/IsSearch");
        if (IsSearch!=null && IsSearch.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found Search.");
            return null;
        }

        // ActionAfterRSSDownload = "xJumpToResultsScreen"

        String IsSingleSourceSubCat = Props.getProperty(OVT + "/IsSingleSourceSubCat");
        if (IsSingleSourceSubCat!=null && IsSingleSourceSubCat.equalsIgnoreCase("true")) {
            FeedContext = OVI;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found FeedContext from OVI " + FeedContext);
        } else {
            if (OVT.startsWith("xPodcast")) {
                FeedContext = Props.getProperty(OVI + "/URLContext");
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found FeedContext from /URLContext " + FeedContext);

                if (FeedContext==null || FeedContext.isEmpty()) {

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext 0 size FeedContext.");

                    String FeedPropValue = Props.getProperty("xFeedPodcast/" + OVI);
                    if (FeedPropValue==null || FeedPropValue.isEmpty()) {
                        FeedPropValue = Props.getProperty("xFeedPodcastCustom/" + OVI);
                        if (FeedPropValue==null || FeedPropValue.isEmpty()) {
                            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFeedContext can't find FeedContext. Aborting.");
                            return null;
                        }
                    }

                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getFeedContext FeedPropValue " + FeedPropValue);

                    String[] s = FeedPropValue.split(";");

                    if (s.length < 2) {
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "getFeedContext Bad FeedPropValue. Aborting.");
                        return null;
                    }

                    FeedContext = s[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found FeedContext from FeedPropValue " + FeedContext);
                }

            } else if (OVT.startsWith("xChannelsDotCom")) {
                // LangList = GetProperty("ui/ChannelsDotCom/Langs","en")
                String LangList = Configuration.GetProperty("ui/ChannelsDotCom/Langs","en");

                // FeedContext = FeedContext + java_util_Properties_getProperty( gOnlineVideoListProps, OnlineVideoType + "/URLLangPrefix", "" ) + LangList

                FeedContext = Props.getProperty(OnlineVideoType + "/URLLangPrefix");

                if (SageUtil.GetBoolProperty("ui/ChannelsDotCom/AllowAdultContent",false)) {
                    // FeedContext = FeedContext + java_util_Properties_getProperty( gOnlineVideoListProps, OnlineVideoType + "/URLAdultPrefix", "" )
                    FeedContext = FeedContext + Props.getProperty(OnlineVideoType + "/URLAdultPrefix");

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found FeedContext from xChannelsDotCom " + FeedContext);
                }

            } else {
                FeedContext = Props.getProperty(OVI + "/URLContext");
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getFeedContext found FeedContext from non-xPodcast " + FeedContext);
            }
        }

        return FeedContext;
    }


    /*
     * Duplicate what happens when item is selected in the Online Video Menu. Have FeedContext, get the full SearchURL.
     */
    public static String getSearchURL(Properties Props, String OVT, String OVI, String FeedContext) {

        if (Props==null || OVT==null || OVI==null || FeedContext==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getSearchURL found null parameter.");
            return null;
        }

        String IsSingleSourceSubCat = Props.getProperty(OVT + "/IsSingleSourceSubCat");
        String IsMultiPage = null;

        if (IsSingleSourceSubCat!=null && IsSingleSourceSubCat.equalsIgnoreCase("true")) {
            IsMultiPage = Props.getProperty(OVT + "/IsMultiPage");
        } else {
            IsMultiPage = Props.getProperty(OVI + "/IsMultiPage");
        }

        Integer NewResultsIndex = (IsMultiPage!=null && IsMultiPage.equalsIgnoreCase("true") ? 1 : 0);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL NewResultsIndex " + NewResultsIndex);

        String URLPrefix = Props.getProperty(OVT + "/URLPrefix");
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL URLPrefix " + URLPrefix);

        String s = Props.getProperty(OVT + "/NeedsPageNumPostfix");

        boolean NeedsPageNumPostfix = (s!=null && s.equalsIgnoreCase("true"));
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL NeedsPageNumPostfix " + NeedsPageNumPostfix);

        s = Props.getProperty(OVT + "/ItemsPerPage", "20");
        int NumItemsPerPage = Integer.parseInt(s);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL NumItemsPerPage " + NumItemsPerPage);

        s = Props.getProperty(OVT + "/PageNumPostfixBase", "0");
        Integer PageIndexBase = Integer.parseInt(s);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL pageIndexBase " + PageIndexBase);

        String URLPostfix = Props.getProperty(OVT + "/URLPostfix", "");
        if (NeedsPageNumPostfix) {
            Integer I = ((NewResultsIndex-1) * NumItemsPerPage + PageIndexBase);
            URLPostfix = URLPostfix + I.toString();
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL URLPostfix " + URLPostfix);

        String SearchURL = (URLPrefix==null ? FeedContext : URLPrefix + FeedContext);

        if (URLPostfix!=null) {
            SearchURL = SearchURL + URLPostfix;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getSearchURL SearchURL " + SearchURL);

        return SearchURL;
    }

    /*
     * Duplicate what happens when item is selected in the Online Video Menu. Have FeedContext and SearchURL, download.
     */
    public static List<RSSItem> getRSSItems(String SearchURL) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems SearchURL = " + SearchURL);

        List<RSSItem> RSSItems = new ArrayList<RSSItem>();

        if (SearchURL==null || SearchURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems null or empty SearchURL.");
            return RSSItems;
        }

        RSSHandler hand = new RSSHandler();
        if (hand==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems null handler.");
            return RSSItems;
        }

        String SearchURLlc = SearchURL.toLowerCase();

        if (SearchURLlc.startsWith("xurlnone")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems found xurlnone");
            return RSSItems;
        }

        if (SearchURLlc.startsWith("external")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems found external feed " + SearchURL);

            String FeedParts[] = SearchURL.split(",",3);
            String FeedEXE = null;
            String FeedParamList[] = null;

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getRSSItems FeedParts = " + FeedParts);

            // Parse the various parts.
            switch (FeedParts.length) {
                case 2:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: FeedEXE = " + FeedEXE);
                    break;
                case 3:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: FeedEXE = " + FeedEXE);

                    String FeedParam = FeedParts[2];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: FeedParam = " + FeedParam);

                    if (FeedParam.length() > 0) {
                        FeedParamList = FeedParam.split("\\|\\|");
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Found parameters " + FeedParamList.length);

                        // "REM Walk through parameter list to check for any special cases."
                        for (int i=0; i<FeedParamList.length; i++) {
                            String Param = FeedParamList[i];
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Parameters " + Param);

                            if (Param!=null && Param.startsWith("%%") && Param.endsWith("%%")) {
                                // ThisParam = Substring(ThisParam, 2, Size(ThisParam) - 2)
                                String ThisParam = Param.substring(2, Param.length()-2);
                                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Found special parameter " + ThisParam);

                                String ThisParamLC = ThisParam.toLowerCase();

                                if (ThisParamLC.startsWith("property=")) {

                                    // ThisParam = Substring(ThisParam, Size("property="), -1)
                                    String S = "property=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Property " + ThisParam);

                                    String NewVal = Configuration.GetProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("serverproperty=")) {

                                    // ThisParam = Substring(ThisParam, Size("serverproperty="), -1)
                                    String S = "serverproperty=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: ServerProperty " + ThisParam);

                                    String NewVal = Configuration.GetServerProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("getuserinput=")) {
                                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Parameter requires user input.");
                                }
                            }
                        }
                    }
                    break;
                default:
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Bad SearchURL = " + SearchURL);
                    return RSSItems;

            }

            // Execute the command.  If we are not running on Windows we need to execute the command remotely.

            String feedText = null;

            if (Global.IsWindowsOS()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Execute " + FeedEXE + " " + StringArrayToString(FeedParamList));
                feedText = Utility.ExecuteProcessReturnOutput(FeedEXE, FeedParamList, null, true, false);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: RemoteExecute " + FeedEXE + " " + FeedParamList);
                //feedText = SageUtil.ExecuteUPnPBrowser(FeedEXE, FeedParamList);
                feedText = Utility.ExecuteProcessReturnOutput("/opt/sagetv/server/SageOnlineServicesEXEs/UPnPBrowser.out", FeedParamList, null, true, false);
            }

            if (feedText==null || feedText.isEmpty() || feedText.length() == 0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: No results from ExecuteProcess.");
                return RSSItems;
            }

           Log.getInstance().write(Log.LOGLEVEL_ALL, "getRSSItems: feedtext " + feedText);

            String RSSWriteFilePath = GetWriteFilePath();

            if (RSSWriteFilePath==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Failed to get RSSWriteFilePath.");
                return RSSItems;
            }

            String RSSReadFilePath = "file:" + RSSWriteFilePath;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: RSSReadFilePath = " + RSSReadFilePath);

            // Write the text to a file.
            if (!WriteText(RSSWriteFilePath, feedText)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Failed to write text.");
                return RSSItems;
            }
            
            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(RSSReadFilePath, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }

        } else {

            // Create the new URL from the String and check for errors.
            URL url;
            try {
                url = new URL(SearchURL);
            } catch (MalformedURLException urle) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems malformed URL. " + SearchURL + " - "+ urle.getMessage());
                return RSSItems;
            }

            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(url, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }
        }

        // Create the new Channel and check for errors.
        RSSChannel rsschan = new RSSChannel();

        // Get the Channel for this handle.
        rsschan = hand.getRSSChannel();
        if (rsschan == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems null chan.");
            return null;
        }

        // Get the RSSItems in a LinkedList.
        @SuppressWarnings("unchecked")
        LinkedList<RSSItem> ChanItems = rsschan.getItems();

        // Loop through all the ChanItems and convert to a List.
        for (RSSItem item : ChanItems) {
            if (!RSSItems.add(item)) Log.getInstance().printStackTrace();
        }

        // Done at last.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: Returning ChanItems = " + RSSItems.size());       
        return RSSItems;
    }


    private static String StringArrayToString(String[] StringArray) {
        if (StringArray==null || StringArray.length==0) {
            return null;
        }

        String BigString = "[";

        for (String S : StringArray) {
            BigString = BigString + S + ":";
        }

        BigString = BigString + "]";

        return BigString;
    }

    private static String GetWriteFilePath() {
            //String STVFileString = WidgetAPI.GetDefaultSTVFile();
            File STVFile = WidgetAPI.GetDefaultSTVFile();
            //if (STVFileString==null || STVFileString.isEmpty()) {
                //Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Null STVFileString.");
                //return null;
            //}

            //File STVFile = new File(STVFileString);
            File PathParent = Utility.GetPathParentDirectory(STVFile);

            // Don't use getAbsolutePath() because it will return Windows formatted string if executed on
            // a client even if Sage server is Linux.
            File OnlineVideoBasePath = Utility.CreateFilePath(PathParent.toString(), "OnlineVideos" );

            String FeedBaseFilename = "TempFeed_PodcastRecorder";

            //if (Global.IsRemoteUI()) {
                //FeedBaseFilename = FeedBaseFilename + "_" + Global.GetUIContextName();
            //} else {
                //String p = Configuration.GetProperty("client","");
                //FeedBaseFilename = FeedBaseFilename + (p!=null && p.equals("true") ? "_client" : "");
            //}

            File RSSWriteFilePath = Utility.CreateFilePath(OnlineVideoBasePath.getAbsolutePath(), FeedBaseFilename + ".xml" );
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRSSItems: RSSWriteFilePath = " + RSSWriteFilePath.toString());

            return RSSWriteFilePath.toString();
    }

    private static boolean WriteText(String WriteFilePath, String text) {

        File f = new File(WriteFilePath);

        FileOutputStream fos;

        try {
            fos = new FileOutputStream(f, false);
        } catch (FileNotFoundException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Exception opening fos " + WriteFilePath);
            return false;
        }

        OutputStreamWriter osw;

        try {
            osw = new OutputStreamWriter(fos, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Exception opening osw " + e.getMessage());
            try {fos.close();} catch (IOException ioe) {}
            return false;
        }

        try {
            osw.write(text);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Exception writing " + e.getMessage());
            try {
                osw.close();
                fos.close();
            } catch (IOException ioe) {}
            return false;
        }

        try {
            osw.close();
            fos.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getRSSItems: Exception writing " + e.getMessage());
        }

        return true;
    }

    /*
     * This is needed because the RSSItem.equals method does not seem to work.
     */
    public static boolean RSSEquals(RSSItem Item1, RSSItem Item2) {
        if (Item1==null || Item2==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSEquals: null parameter.");
            return false;
        }

        String ID1 = makeID(Item1);
        String ID2 = makeID(Item2);
        return ID1.equals(ID2);
    }

    /*
     * This is needed because .contains() method does not seem to work for List<RSSItem>.
     */
    public static boolean RSSListContains(List<RSSItem> Items, RSSItem Item) {
        if (Items==null || Item==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSListContains: null parameter.");
            return false;
        }

        for (RSSItem I : Items) {
            if (RSSEquals(I, Item)) {
                return true;
            }
        }

        return false;
    }
}
