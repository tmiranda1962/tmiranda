
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getFeedContext: Found null parameter.");
            return null;
        }

        String OVT = OnlineVideoType;
        String OVI = OnlineVideoItem;
        String FeedContext = null;

        if (OVI.startsWith("xBack")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found xBack.");
            return null;
        }

        String IsCat = Props.getProperty(OVI+"/IsCategory");
        if (IsCat!=null && IsCat.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found Subcategory.");
            return null;
        }

        String TrackingCategory = Props.getProperty(OVI+"/TrackingCat");
        if (TrackingCategory !=null && !TrackingCategory.isEmpty()) {
            OVI = Configuration.GetProperty("online_video/last_sub_category/" + TrackingCategory, TrackingCategory);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found TrackingCategory " + TrackingCategory + ":" + OVI);
        }

        String IsSearch = Props.getProperty(OVI+"/IsSearch");
        if (IsSearch!=null && IsSearch.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found Search.");
            return null;
        }

        // ActionAfterRSSDownload = "xJumpToResultsScreen"

        String IsSingleSourceSubCat = Props.getProperty(OVT + "/IsSingleSourceSubCat");
        if (IsSingleSourceSubCat!=null && IsSingleSourceSubCat.equalsIgnoreCase("true")) {
            FeedContext = OVI;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found FeedContext from OVI " + FeedContext);
        } else {
            if (OVT.startsWith("xPodcast")) {
                FeedContext = Props.getProperty(OVI + "/URLContext");
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found FeedContext from /URLContext " + FeedContext);

                if (FeedContext==null || FeedContext.isEmpty()) {

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: 0 size FeedContext.");

                    String FeedPropValue = Props.getProperty("xFeedPodcast/" + OVI);
                    if (FeedPropValue==null || FeedPropValue.isEmpty()) {
                        FeedPropValue = Props.getProperty("xFeedPodcastCustom/" + OVI);
                        if (FeedPropValue==null || FeedPropValue.isEmpty()) {
                            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getFeedContext: Can't find FeedContext. Aborting.");
                            return null;
                        }
                    }

                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RSSHelper.getFeedContext: FeedPropValue " + FeedPropValue);

                    String[] s = FeedPropValue.split(";");

                    if (s.length < 2) {
                        Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getFeedContext: Bad FeedPropValue. Aborting.");
                        return null;
                    }

                    FeedContext = s[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: found FeedContext from FeedPropValue " + FeedContext);
                }

            } else if (OVT.startsWith("xChannelsDotCom")) {
                // LangList = GetProperty("ui/ChannelsDotCom/Langs","en")
                String LangList = Configuration.GetProperty("ui/ChannelsDotCom/Langs","en");

                // FeedContext = FeedContext + java_util_Properties_getProperty( gOnlineVideoListProps, OnlineVideoType + "/URLLangPrefix", "" ) + LangList

                FeedContext = Props.getProperty(OnlineVideoType + "/URLLangPrefix");

                if (SageUtil.GetBoolProperty("ui/ChannelsDotCom/AllowAdultContent",false)) {
                    // FeedContext = FeedContext + java_util_Properties_getProperty( gOnlineVideoListProps, OnlineVideoType + "/URLAdultPrefix", "" )
                    FeedContext = FeedContext + Props.getProperty(OnlineVideoType + "/URLAdultPrefix");

                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found FeedContext from xChannelsDotCom " + FeedContext);
                }

            } else {
                FeedContext = Props.getProperty(OVI + "/URLContext");
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getFeedContext: Found FeedContext from non-xPodcast " + FeedContext);
            }
        }

        return FeedContext;
    }


    /*
     * Duplicate what happens when item is selected in the Online Video Menu. Have FeedContext, get the full SearchURL.
     */
    public static String getSearchURL(Properties Props, String OVT, String OVI, String FeedContext) {

        if (Props==null || OVT==null || OVI==null || FeedContext==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getSearchURL: Found null parameter.");
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
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: NewResultsIndex " + NewResultsIndex);

        String URLPrefix = Props.getProperty(OVT + "/URLPrefix");
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: URLPrefix " + URLPrefix);

        String s = Props.getProperty(OVT + "/NeedsPageNumPostfix");

        boolean NeedsPageNumPostfix = (s!=null && s.equalsIgnoreCase("true"));
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: NeedsPageNumPostfix " + NeedsPageNumPostfix);

        s = Props.getProperty(OVT + "/ItemsPerPage", "20");
        int NumItemsPerPage = Integer.parseInt(s);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: NumItemsPerPage " + NumItemsPerPage);

        s = Props.getProperty(OVT + "/PageNumPostfixBase", "0");
        Integer PageIndexBase = Integer.parseInt(s);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: pageIndexBase " + PageIndexBase);

        // Don't put the start number at the end if there is max-results specified.  This
        // makes it easier for gteRSSItems later on.
        String URLPostfix = Props.getProperty(OVT + "/URLPostfix", "");
        if (NeedsPageNumPostfix && !URLPostfix.contains("max-results")) {
            Integer I = ((NewResultsIndex-1) * NumItemsPerPage + PageIndexBase);
            URLPostfix = URLPostfix + I.toString();
        }
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: URLPostfix " + URLPostfix);

        String SearchURL = (URLPrefix==null ? FeedContext : URLPrefix + FeedContext);

        if (URLPostfix!=null) {
            SearchURL = SearchURL + URLPostfix;
        }
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getSearchURL: SearchURL " + SearchURL);

        return SearchURL;
    }

    private static String zapMaxResults(String SearchURL) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.zapMaxResults: Getting rid of max-results.");

        if (!SearchURL.contains("&max-results=")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.zapMaxResults: max-results not present.");
            return(SearchURL);
        }

        // Most common.
        if (SearchURL.contains("&max-results=20")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.zapMaxResults: Eliminating max-results=20.");
            return(SearchURL.replace("&max-results=20", ""));
        }

        // It must contain max-results=nn& so we need to replace nn with something big.
        String[] parts = SearchURL.split("&");

        // Should have at least 2 parts.
        if (parts.length < 2) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RSSHelper.zapMaxResults: Unexpected max-results postfix " + SearchURL);
            return SearchURL;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.zapMaxResults: Found parts " + parts.length);

        // Search for string ending with "max-results" because the string after that is the number.
        for (int i=0; i<(parts.length-1); i++) {
            if (parts[i].endsWith("max-results")) {

                // The next string contains the stuff after max-results= so it should start with
                // a number.
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.zapMaxResults: Did not find max-results.");

        return SearchURL;

    }

    /*
     * Duplicate what happens when item is selected in the Online Video Menu. Have FeedContext and SearchURL, download.
     */
    public static List<RSSItem> getRSSItems(String SearchURL) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: SearchURL = " + SearchURL);

        if (SearchURL.contains("max-results")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Need to get RSSItems for MultiPage.");
            return getRSSItemsMultiPage(SearchURL);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Need to get RSSItems for normally.");
            return getRSSItemsNormal(SearchURL);
        }


/**
        List<RSSItem> RSSItems = new ArrayList<RSSItem>();

        if (SearchURL==null || SearchURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null or empty SearchURL.");
            return RSSItems;
        }

        //String SearchURL = zapMaxResults(origSearchURL);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: SearchURL after zapping max-results = " + SearchURL);

        RSSHandler hand = new RSSHandler();
        if (hand==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null handler.");
            return RSSItems;
        }

        String SearchURLlc = SearchURL.toLowerCase();

        if (SearchURLlc.startsWith("xurlnone")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Found xurlnone");
            return RSSItems;
        }

        if (SearchURLlc.startsWith("external")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found external feed " + SearchURL);

            String FeedParts[] = SearchURL.split(",",3);
            String FeedEXE = null;
            String FeedParamList[] = null;

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RSSHelper.getRSSItems: FeedParts = " + FeedParts);

            // Parse the various parts.
            switch (FeedParts.length) {
                case 2:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedEXE = " + FeedEXE);
                    break;
                case 3:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedEXE = " + FeedEXE);

                    String FeedParam = FeedParts[2];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedParam = " + FeedParam);

                    if (FeedParam.length() > 0) {
                        FeedParamList = FeedParam.split("\\|\\|");
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found parameters " + FeedParamList.length);

                        // "REM Walk through parameter list to check for any special cases."
                        for (int i=0; i<FeedParamList.length; i++) {
                            String Param = FeedParamList[i];
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Parameters " + Param);

                            if (Param!=null && Param.startsWith("%%") && Param.endsWith("%%")) {
                                // ThisParam = Substring(ThisParam, 2, Size(ThisParam) - 2)
                                String ThisParam = Param.substring(2, Param.length()-2);
                                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found special parameter " + ThisParam);

                                String ThisParamLC = ThisParam.toLowerCase();

                                if (ThisParamLC.startsWith("property=")) {

                                    // ThisParam = Substring(ThisParam, Size("property="), -1)
                                    String S = "property=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Property " + ThisParam);

                                    String NewVal = Configuration.GetProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("serverproperty=")) {

                                    // ThisParam = Substring(ThisParam, Size("serverproperty="), -1)
                                    String S = "serverproperty=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: ServerProperty " + ThisParam);

                                    String NewVal = Configuration.GetServerProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("getuserinput=")) {
                                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Parameter requires user input.");
                                }
                            }
                        }
                    }
                    break;
                default:
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Bad SearchURL = " + SearchURL);
                    return RSSItems;

            }

            // Execute the command.  If we are not running on Windows we need to execute the command remotely.

            String feedText = null;

            if (Global.IsWindowsOS()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Execute " + FeedEXE + " " + StringArrayToString(FeedParamList));
                feedText = Utility.ExecuteProcessReturnOutput(FeedEXE, FeedParamList, null, true, false);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: RemoteExecute " + FeedEXE + " " + FeedParamList);
                //feedText = SageUtil.ExecuteUPnPBrowser(FeedEXE, FeedParamList);
                feedText = Utility.ExecuteProcessReturnOutput("/opt/sagetv/server/SageOnlineServicesEXEs/UPnPBrowser.out", FeedParamList, null, true, false);
            }

            if (feedText==null || feedText.isEmpty() || feedText.length() == 0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: No results from ExecuteProcess.");
                return RSSItems;
            }

           Log.getInstance().write(Log.LOGLEVEL_ALL, "RSSHelper.getRSSItems: feedtext " + feedText);

            String RSSWriteFilePath = GetWriteFilePath();

            if (RSSWriteFilePath==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Failed to get RSSWriteFilePath.");
                return RSSItems;
            }

            //String RSSReadFilePath = "file:" + RSSWriteFilePath;
            String RSSReadFilePath = RSSWriteFilePath;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: RSSReadFilePath = " + RSSReadFilePath);

            // Write the text to a file.
            if (!WriteText(RSSWriteFilePath, feedText)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Failed to write text.");
                return RSSItems;
            }
            
            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(RSSReadFilePath, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }

        } else {

            // Create the new URL from the String and check for errors.
            URL url;
            try {
                url = new URL(SearchURL);
            } catch (MalformedURLException urle) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: malformed URL. " + SearchURL + " - "+ urle.getMessage());
                return RSSItems;
            }

            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(url, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }
        }

        // Create the new Channel and check for errors.
        RSSChannel rsschan = new RSSChannel();

        // Get the Channel for this handle.
        rsschan = hand.getRSSChannel();
        if (rsschan == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null chan.");
            return null;
        }

        // Get the RSSItems in a LinkedList.
        @SuppressWarnings("unchecked")
        LinkedList<RSSItem> ChanItems = rsschan.getItems();

        // Loop through all the ChanItems and convert to a List.
        for (RSSItem item : ChanItems) {
            if (!RSSItems.add(item))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Error adding.");
        }

        // Done at last.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Returning ChanItems = " + RSSItems.size());
        return RSSItems;
 *
 */
    }


    private static List<RSSItem> getRSSItemsNormal(String SearchURL) {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: SearchURL = " + SearchURL);

        List<RSSItem> RSSItems = new ArrayList<RSSItem>();

        if (SearchURL==null || SearchURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null or empty SearchURL.");
            return RSSItems;
        }

        //String SearchURL = zapMaxResults(origSearchURL);
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: SearchURL after zapping max-results = " + SearchURL);

        RSSHandler hand = new RSSHandler();
        if (hand==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null handler.");
            return RSSItems;
        }

        String SearchURLlc = SearchURL.toLowerCase();

        if (SearchURLlc.startsWith("xurlnone")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Found xurlnone");
            return RSSItems;
        }

        if (SearchURLlc.startsWith("external")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found external feed " + SearchURL);

            String FeedParts[] = SearchURL.split(",",3);
            String FeedEXE = null;
            String FeedParamList[] = null;

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RSSHelper.getRSSItems: FeedParts = " + FeedParts);

            // Parse the various parts.
            switch (FeedParts.length) {
                case 2:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedEXE = " + FeedEXE);
                    break;
                case 3:
                    FeedEXE = FeedParts[1];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedEXE = " + FeedEXE);

                    String FeedParam = FeedParts[2];
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: FeedParam = " + FeedParam);

                    if (FeedParam.length() > 0) {
                        FeedParamList = FeedParam.split("\\|\\|");
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found parameters " + FeedParamList.length);

                        // "REM Walk through parameter list to check for any special cases."
                        for (int i=0; i<FeedParamList.length; i++) {
                            String Param = FeedParamList[i];
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Parameters " + Param);

                            if (Param!=null && Param.startsWith("%%") && Param.endsWith("%%")) {
                                // ThisParam = Substring(ThisParam, 2, Size(ThisParam) - 2)
                                String ThisParam = Param.substring(2, Param.length()-2);
                                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Found special parameter " + ThisParam);

                                String ThisParamLC = ThisParam.toLowerCase();

                                if (ThisParamLC.startsWith("property=")) {

                                    // ThisParam = Substring(ThisParam, Size("property="), -1)
                                    String S = "property=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Property " + ThisParam);

                                    String NewVal = Configuration.GetProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("serverproperty=")) {

                                    // ThisParam = Substring(ThisParam, Size("serverproperty="), -1)
                                    String S = "serverproperty=";
                                    ThisParam = ThisParam.substring(S.length());
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: ServerProperty " + ThisParam);

                                    String NewVal = Configuration.GetServerProperty(ThisParam, null);
                                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Value " + NewVal);

                                    FeedParamList[i] = NewVal;

                                } else if (ThisParamLC.startsWith("getuserinput=")) {
                                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Parameter requires user input.");
                                }
                            }
                        }
                    }
                    break;
                default:
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Bad SearchURL = " + SearchURL);
                    return RSSItems;

            }

            // Execute the command.  If we are not running on Windows we need to execute the command remotely.

            String feedText = null;

            if (Global.IsWindowsOS()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Execute " + FeedEXE + " " + StringArrayToString(FeedParamList));
                feedText = Utility.ExecuteProcessReturnOutput(FeedEXE, FeedParamList, null, true, false);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: RemoteExecute " + FeedEXE + " " + FeedParamList);
                //feedText = SageUtil.ExecuteUPnPBrowser(FeedEXE, FeedParamList);
                feedText = Utility.ExecuteProcessReturnOutput("/opt/sagetv/server/SageOnlineServicesEXEs/UPnPBrowser.out", FeedParamList, null, true, false);
            }

            if (feedText==null || feedText.isEmpty() || feedText.length() == 0) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: No results from ExecuteProcess.");
                return RSSItems;
            }

           Log.getInstance().write(Log.LOGLEVEL_ALL, "RSSHelper.getRSSItems: feedtext " + feedText);

            String RSSWriteFilePath = GetWriteFilePath();

            if (RSSWriteFilePath==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Failed to get RSSWriteFilePath.");
                return RSSItems;
            }

            //String RSSReadFilePath = "file:" + RSSWriteFilePath;
            String RSSReadFilePath = RSSWriteFilePath;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: RSSReadFilePath = " + RSSReadFilePath);

            // Write the text to a file.
            if (!WriteText(RSSWriteFilePath, feedText)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Failed to write text.");
                return RSSItems;
            }

            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(RSSReadFilePath, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }

        } else {

            // Create the new URL from the String and check for errors.
            URL url;
            try {
                url = new URL(SearchURL);
            } catch (MalformedURLException urle) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: malformed URL. " + SearchURL + " - "+ urle.getMessage());
                return RSSItems;
            }

            // Create the new Parser and check for errors.
            RSSParser parser = new RSSParser();

            // Parse the XML file pointed to be the URL.
            try {
                parser.parseXmlFile(url, hand, false);
            } catch (RSSException rsse) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception parsing URL. " + rsse.getMessage());
                return RSSItems;
            }
        }

        // Create the new Channel and check for errors.
        RSSChannel rsschan = new RSSChannel();

        // Get the Channel for this handle.
        rsschan = hand.getRSSChannel();
        if (rsschan == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: null chan.");
            return null;
        }

        // Get the RSSItems in a LinkedList.
        @SuppressWarnings("unchecked")
        LinkedList<RSSItem> ChanItems = rsschan.getItems();

        // Loop through all the ChanItems and convert to a List.
        for (RSSItem item : ChanItems) {
            if (!RSSItems.add(item))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Error adding.");
        }

        // Done at last.
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: Returning ChanItems = " + RSSItems.size());
        return RSSItems;
    }

    private static List<RSSItem> getRSSItemsMultiPage(String SearchURL) {

        List<RSSItem> RSSItems = new ArrayList<RSSItem>();
        int page = 0;
        int itemsPerPage = 0;
        boolean done = false;

        do {
            Integer startingAt = (page * itemsPerPage) + 1;
            String newSearchURL = SearchURL + startingAt.toString();

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItemsMultiPage: newSearchURL = " + newSearchURL);

            List<RSSItem> newList = getRSSItemsNormal(newSearchURL);

            if (newList.isEmpty()) {
                done = true;
            } else {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItemsMultiPage: Fetched RSSItems " + newList.size());
                
                // Use the number of items we fetch the first time as the number we expect
                // to fetch each time, i.e. max-results=xx.
                if (itemsPerPage == 0)
                    itemsPerPage = newList.size();
                
                RSSItems.addAll(newList);
                page++;
            }

        } while (!done);

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
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RSSHelper.getRSSItems: RSSWriteFilePath = " + RSSWriteFilePath.toString());

            return RSSWriteFilePath.toString();
    }

    private static boolean WriteText(String WriteFilePath, String text) {

        File f = new File(WriteFilePath);

        FileOutputStream fos;

        try {
            fos = new FileOutputStream(f, false);
        } catch (FileNotFoundException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception opening fos " + WriteFilePath);
            return false;
        }

        OutputStreamWriter osw;

        try {
            osw = new OutputStreamWriter(fos, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception opening osw " + e.getMessage());
            try {fos.close();} catch (IOException ioe) {}
            return false;
        }

        try {
            osw.write(text);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception writing " + e.getMessage());
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.getRSSItems: Exception writing " + e.getMessage());
        }

        return true;
    }

    /*
     * This is needed because the RSSItem.equals method does not seem to work.
     */
    public static boolean RSSEquals(RSSItem Item1, RSSItem Item2) {
        if (Item1==null || Item2==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.RSSEquals: null parameter.");
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
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RSSHelper.RSSListContains: null parameter.");
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
