/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.util.*;
import java.io.*;
import java.net.*;
import sage.media.rss.*;
import sage.SageTV.*;
import sagex.api.*;



/**
 *
 * @author Tom Miranda
 */
// public class RecordingEpisode {
public class RecordingEpisode {

    public static final int BLOCK_SIZE = 2048;
    private static final String WINDOWS_SEPARATOR = "\\";
    private static final String LINUX_SEPARATOR = "/";

    private String FeedContext;
    private RSSItem ChanItem;
    private RSSItem OrigChanItem;
    private List<String> VideoURLs;
    private String FileExt;
    private File tempFile;
    private String DownloadStatus;
    private String RecDir;
    private String RecSubdir;
    private String FileName;
    private File NewFile;
    private String EpisodeID;
    private String ShowTitle;
    private String EpisodeTitle;
    private String RequestID;
    private long BlocksRecorded;
    private String Separator;
    private boolean Abort;

    /**
     * Make a new RecordingEpisode object. Takes the Feed Context as a parameter.
     */
    public RecordingEpisode(    String ReqID,
                                String Context,
                                String EpID,
                                RSSItem xOrigChanItem,
                                String RecDirectory,
                                String SubDir,
                                String Title,
                                String EpTitle,
                                String FName,
                                List<String> DefURLs) {

        VideoURLs = new ArrayList<String>();
        BlocksRecorded = 0;
        RequestID = ReqID;
        FeedContext = Context;
        EpisodeID = EpID;
        RecDir = fixPath(RecDirectory);
        RecSubdir = fixPath(SubDir);
        ShowTitle = Title;
        EpisodeTitle = EpTitle;
        FileName = FName;
        VideoURLs = DefURLs;
        Abort = false;
        OrigChanItem = xOrigChanItem;
    }

    /*
     * This is needed because when a SageClient running Windows creates a path it always assumes the server
     * is running Windows and uses the wrong separator character.
     */
    private String fixPath(String Path) {

        // Nothing to do if we're running the plugin on a Windows machine.
        if (Global.IsWindowsOS()) {
            return Path;
        }

        // Replace all of the Windows separator characters with linux separator characters.
        String NewPath = Path.replaceAll("\\\\", File.separator);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "fixPath " + Path + "->" + NewPath);
        
        return NewPath;
    }

    public void abortCurrentDownload() {
        Abort = true;
    }

    public String getRequestID() {
        return RequestID;
    }

    public String getShowTitle() {
        return ShowTitle;
    }

    public String getEpisodeID() {
        return EpisodeID;
    }

    public void setChanItem(RSSItem item) {
        ChanItem = item;
    }

    public RSSItem getChanItem() {
        return ChanItem;
    }

    public RSSItem getOrigChanItem() {
        return OrigChanItem;
    }

    public String getDownloadStatus() {
        return DownloadStatus;
    }

    private boolean isRecDirWindows() {

        // Create a new File object.
        File testfile = new File(RecDir + LINUX_SEPARATOR + "PodcastRecorder.tst");

        // If it failed assume it's a Windows filesystem.  Linux filesystems will happily accept the Windows separator.
        if (testfile==null || !(testfile instanceof File)) {
            return true;
        }

        // If the testfile exists it must be a Linux filesystem.
        if (testfile.exists()) {
            testfile.delete();
            return false;
        }

        // Try to create a new file.
        try {

            // If it worked it must be a Linux filesystem.
            if (testfile.createNewFile()) {
                testfile.delete();
                return false;
            }
        } catch (Exception e) {
            return true;
        }

        return false;
    }

    public long getBlocksRecorded() {
        return BlocksRecorded;
    }

    public boolean isComplete() {
        return  FeedContext != null &&
                EpisodeID != null &&
                RecDir != null &&
                RecSubdir != null &&
                ShowTitle != null &&
                EpisodeTitle != null &&
                FileName != null &&
                RequestID != null &&
                VideoURLs != null && !VideoURLs.isEmpty();
    }

    public String getRSSItemTitle() {
        if (ChanItem==null) {
            return null;
        }
        return ChanItem.getTitle();
    }


    /**
     * Gets the RSSItem for the given ID.
     * <p>
     * @param RSSItems A list of the RSSItems to search.
     * @param ID The RSSItem ID
     * @return The RSSItem with the given ID.
     */
    public RSSItem getItemForID(List<RSSItem> RSSItems, String ID) {

        if (RSSItems == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP getItemForID: null RSSItems.");
            return null;
        }

        for (RSSItem item : RSSItems) {
            String tID = RSSHelper.makeID(item);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "REEP getItemForID: tID = " + tID);
            if (tID.equals(ID)) {
                return item;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP getItemForID: No matches found for ID = " + ID);
        return null;
    }

    private File getUniqueFile() {

        String fullpath = RecDir + File.separator + RecSubdir + File.separator;
        String filetotry = FileName;

        File name = new File(fullpath+filetotry+FileExt);

        for (Integer i=0; name.exists() && i<1000; i++) {
            filetotry = FileName + "-" + i.toString();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP getUniqueFileName: Trying alternate filename = " + filetotry);
            name = new File(fullpath+filetotry+FileExt);
        }

        if (name.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP getUniqueFileName: Error - Can't find unique name.");
            return null;
        }

        return name;
    }


    /*
     * ******************************************************
     * Methods below here should be called in order listed. *
     * ******************************************************
     */


    /**
     * Gets the RSSItems for the RecordingEpisode Object.
     * <p>
     * @return
     */
    public List<RSSItem> getRSSItems() {
        String SearchURL = FeedContext;
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "REEP getRSSItems FeedContext = " + FeedContext);
        return RSSHelper.getRSSItems(SearchURL);
    }

    /**
     * Sets the file extension based on the URL.
     * <p>
     * @param URL The URL for the episode that is being recorded.
     * @return true if success, false otherwise.
     */
    public boolean setFileExt() {

        String url = VideoURLs.get(0).toLowerCase();

        if (url.endsWith(".mpg")) {
            //isVideo = true;
            FileExt = ".mpg";
        } else if (url.endsWith(".mp3")) {
            //isVideo = false;
            FileExt = ".mp3";
        } else if (url.endsWith(".m4a")) {
            //isVideo = false;
            FileExt = ".m4a";
        } else {
            //isVideo = true;
            FileExt = ".flv";
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP setFileExt: FileExt = " + FileExt);

        return true;
    }

    /**
     * Sets the tempfile that will be used to download the episode.
     * <p>
     * @return true if success, false otherwise.
     */
    public boolean setTempFile() {
        try {
            tempFile = File.createTempFile("MaloreOnlineVideo", FileExt);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP setTempFile: " + tempFile.getAbsolutePath());
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP setTempFile: Exception creating tempfile. " + e.getMessage());
            return false;
        }

        if (tempFile==null || !(tempFile instanceof File)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP setTempFile: Failed to create valid tempFile.");
            return false;
        }

        return true;
    }



    /**
     * Move the tempfile to the final location and reanme it to the final name.  Deletes the tempFile.
     * <p>
     * @return true if success, false otherwise. Always deletes the tempFile before returning.
     */
    public boolean moveToFinalLocation() {

        if (RecDir==null || RecSubdir==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP moveToFinalLocation: null Dir or SubDir = " + RecDir + ":" + RecSubdir);

            if (!tempFile.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP moveToFinalLocation: Failed to delete tempFile.");
            }

            return false;
        }

        File DestPath = new File(RecDir + File.separator + RecSubdir);

        if (DestPath==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP moveToFinalLocation: null DestPath = " + RecDir + ":" + RecSubdir);

            if (!tempFile.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP moveToFinalLocation: Failed to delete tempFile.");
            }

            return false;
        }

        if (!DestPath.isDirectory()) {
            if (!DestPath.mkdir()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP moveToFinalLocation: Could not create directory " + DestPath);

                if (!tempFile.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP moveToFinalLocation: Failed to delete tempFile.");
                }
                return false;
            }
        }

        NewFile = this.getUniqueFile();

        if (NewFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP importAsMediaFile: Could not create unique file.");

            if (!tempFile.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP moveToFinalLocation: Failed to delete tempFile.");
            }

            return false;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP moveToFinalLocation: Moving = " + tempFile.getAbsolutePath() + "->" + NewFile.getAbsolutePath());

        if (!tempFile.renameTo(NewFile)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP moveToFinalLocation: Moving failed.");
        }

        if (!tempFile.delete()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP moveToFinalLocation: Failed to delete tempFile.");
        }

        return true;
    }

    /**
     * Imports NewFile into the Sage database.
     * <p>
     * @return true if success, false otherwise.
     */
    public boolean importAsMediaFile() {


        String MovedFileString = NewFile.getAbsolutePath();

        File MovedFile = new File(MovedFileString);

        if (!MovedFile.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP importAsMediaFile: Error. MovedFile does not exist.");
        }

        Object MF = MediaFileAPI.AddMediaFile(MovedFile, RecSubdir);
        if (MF == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP importAsMediaFile: AddMediaFile failed for " + MovedFile.getAbsolutePath());
        }


        String Title = ShowTitle;
        boolean IsFirstRun = true;
        String Episode = EpisodeTitle;
        String Description = ChanItem.getCleanDescription();
        long Duration = ChanItem.getDuration();
        String Category = "Podcast";
        String SubCategory = "MaloreOnlineBrowser";
        String Author = ChanItem.getAuthor();
        String PeopleList[] = {"Author"};
        String RolesList[] = {Author};
        String Rated = null;
        String ExpandedRatedList[] = null;
        String Year = Utility.DateFormat("yyyy", Utility.Time());
        String ParentalRating = null;
        String MiscList[] = {RSSHelper.makeID(ChanItem)};
        String ExternalID = "ONL" + Utility.Time();
        String Language = "http";
        long OriginalAirDate = Utility.Time();

        Object Show = ShowAPI.AddShow(
                Title,
                IsFirstRun,
                Episode,
                Description,
                Duration,
                Category,
                SubCategory,
                PeopleList,
                RolesList,
                Rated,
                ExpandedRatedList,
                Year,
                ParentalRating,
                MiscList,
                ExternalID,
                Language,
                OriginalAirDate);

        if (Show == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP importAsMediaFile: AddShow failed.");
            return false;
        }

        if (!MediaFileAPI.SetMediaFileShow(MF, Show)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP importAsMediaFile: SetMediaFileShow failed.");
            return false;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP importAsMediaFile succeeded.");
        return true;
    }

    public boolean download() {

        HttpURLConnection.setFollowRedirects(true);
        if (!HttpURLConnection.getFollowRedirects()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP download: Warning - Redirects are NOT set.");
        }

        BufferedInputStream in = null;
        FileOutputStream fout = null;

        for (String url : VideoURLs) {

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP download: Trying URL =" + url);

            BlocksRecorded = 0;

            try {
                in = new BufferedInputStream(new URL(url).openStream());
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP download: Failed to open " + url);
                break;
            }

            try {
                fout = new FileOutputStream(tempFile);
            } catch (FileNotFoundException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP download: Failed to open tempFile. " + e.getMessage());
                try {in.close();} catch (Exception e1) {}
                break;
            }

            byte data[] = new byte[BLOCK_SIZE];
            int count = 0;

            try {
                while (((count = in.read(data, 0, BLOCK_SIZE)) != -1) && !Abort)
                {
                    fout.write(data, 0, count);
                    BlocksRecorded++;
                }
            } catch (IOException e2) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP download: Exception during transfer " + e2.getMessage());
                try {
                    in.close();
                    fout.close();
                } catch (IOException e3) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP download: Exception closing files " + e3.getMessage());
                }
                return false;
            }

            if (Abort) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "RecordingEpisode was aborted.");
            }

            try {
                in.close();
                fout.close();
            } catch (IOException e2) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP download: Error closing files. " + e2.getMessage());
            }

            if (count==-1) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP download: Completed successfully.");
                return true;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "REEP download: Failed for all URLs.");
        return false;

    }

    public void completed() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "REEP completed: Successfully recorded " + this.getShowTitle());
        DownloadManager.getInstance().setCurrentlyRecordingID(null);
        DownloadManager.getInstance().addCompletedDownloads(RequestID);
        DownloadManager.getInstance().removeActiveDownloads(RequestID);
        return;
    }

    public void failed() {
        Log.getInstance().write(Log.LOGLEVEL_ERROR, "REEP failed: Recording failed.");
        DownloadManager.getInstance().setCurrentlyRecordingID(null);
        DownloadManager.getInstance().addFailedDownloads(RequestID);
        DownloadManager.getInstance().removeActiveDownloads(RequestID);
        return;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RecordingEpisode other = (RecordingEpisode) obj;
        if ((this.FeedContext == null) ? (other.FeedContext != null) : !this.FeedContext.equals(other.FeedContext)) {
            return false;
        }
        if (this.ChanItem != other.ChanItem && (this.ChanItem == null || !this.ChanItem.equals(other.ChanItem))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.FeedContext != null ? this.FeedContext.hashCode() : 0);
        hash = 37 * hash + (this.ChanItem != null ? this.ChanItem.hashCode() : 0);
        return hash;
    }


}
