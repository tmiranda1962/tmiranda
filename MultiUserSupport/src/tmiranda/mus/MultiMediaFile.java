/*
 * Multi-user MediaFile Object.
 */
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */


// Thoughts:
// - Do I really need "AllowedUsers"?
// - Split  MultiAiring and MultiMediaFile.
// - In API check to make sure Object are correct Airing/MediaFile and do conversion as necessary.
public class MultiMediaFile extends MultiObject {

    // The DataStore for MediaFiles.
    static final String MEDIAFILE_STORE = "MultiUser.MediaFile";

    // These Flags will contain a comma delimited string of userIDs.
    static final String ARCHIVED        = "Archived";
    static final String DELETED         = "Deleted";

    static final String[]   FLAGS = {ARCHIVED, DELETED, INITIALIZED};

    // These Flags+userId will contain the user watched times for the MediaFile.
    static final String DURATION_PREFIX     = "Duration_";
    static final String MEDIATIME_PREFIX    = "MediaTime_";
    static final String CHAPTERNUM_PREFIX   = "ChapterNum_";
    static final String TITLENUM_PREFIX     = "TitleNum_";

    private String userID           = null;
    private Object sageMediaFile    = null;

    public MultiMediaFile(String UserID, Object MediaFile) {
        super(MEDIAFILE_STORE, MediaFileAPI.GetMediaFileID(MediaFile));

        if (!isValid || UserID==null || UserID.isEmpty() || MediaFile==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiMediaFile: null parameter " + UserID);
            return;
        }

        if (!MediaFileAPI.IsMediaFileObject(MediaFile)) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiMediaFile: Object is not a MediaFile.");
            return;
        }

        userID = UserID;
        sageMediaFile = MediaFile;

        if (!isInitialized) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiMediaFile: Initializing user " + userID);
            initializeUser();
        }
    }

    boolean isDeleted() {
        return (isValid ? containsFlag(DELETED, userID) : false);
    }

    boolean delete(boolean WithoutPrejudice) {

        // If we have an invalid MMF, just return error.
        if (!isValid) {
System.out.println("DELETE: IS NOT VALID");
            return false;
        }

        // Mark the MediaFile as deleted.
        addFlag(DELETED, userID);
System.out.println("DELETE: AFTER ADDFLAG " + getRecordData(DELETED));

        // If all users have it marked as deleted, delete it for real.
        if (containsFlagAllUsers(DELETED)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Deleting physical file for user " + userID);
            return (WithoutPrejudice ? MediaFileAPI.DeleteFileWithoutPrejudice(sageMediaFile) : MediaFileAPI.DeleteFile(sageMediaFile));
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Leaving physical file intact.");
        return true;
    }

    void setArchived() {
        if (!isValid)
            MediaFileAPI.MoveFileToLibrary(sageMediaFile);
        else
            addFlag(ARCHIVED, userID);
    }
    
    void clearArchived() {
        if (!isValid)
            MediaFileAPI.MoveTVFileOutOfLibrary(sageMediaFile);
        else
            addFlag(ARCHIVED, userID);
    }

    boolean isArchived() {

        if (!isValid)
            return MediaFileAPI.IsLibraryFile(sageMediaFile);
        else
            return containsFlag(ARCHIVED, userID);
    }

    long getDuration() {
        String D = getRecordData(DURATION_PREFIX + userID);

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getDuration: Bad number " + D);
            return 0;
        }
    }

    void setDuration(String Duration) {
        setRecordData(DURATION_PREFIX + userID, Duration);
    }

    void setDuration(long Duration) {
        Long D = Duration;
        setDuration(D.toString());
    }

    long getMediaTime() {
        String D = getRecordData(MEDIATIME_PREFIX + userID);

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getMediaTime: Bad number " + D);
            return 0;
        }
    }

    void setMediaTime(String Duration) {
        setRecordData(MEDIATIME_PREFIX + userID, Duration);
    }

    void setMediaTime(long Duration) {
        Long D = Duration;
        setMediaTime(D.toString());
    }

    int getChapterNum() {
        String D = getRecordData(CHAPTERNUM_PREFIX + userID);

        try {
            int duration = Integer.parseInt(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getChapterNum: Bad number " + D);
            return 0;
        }
    }

    void setChapterNum(String Num) {
        setRecordData(CHAPTERNUM_PREFIX + userID, Num);
    }

    void setChapterNum(int Num) {
        Integer N = Num;
        setChapterNum(N.toString());
    }

    int getTitleNum() {
        String D = getRecordData(TITLENUM_PREFIX + userID);

        try {
            int duration = Integer.parseInt(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "getTitleNum: Bad number " + D);
            return 0;
        }
    }

    void setTitleNum(String Num) {
        setRecordData(TITLENUM_PREFIX + userID, Num);
    }

    void setTitleNum(int Num) {
        Integer N = Num;
        setTitleNum(N.toString());
    }

    // Initialize MediaFile for this user.
    void initializeUser() {

        if (MediaFileAPI.IsLibraryFile(sageMediaFile))
            addFlag(ARCHIVED, userID);
        else
            removeFlag(ARCHIVED, userID);

        removeFlag(DELETED, userID);
        addFlag(INITIALIZED, "true");
        setDuration(null);
        setMediaTime(null);
        setChapterNum(null);
        setTitleNum(null);

        isInitialized = true;
        return;
    }

    // Remove this user from the MediaFile.
    void clearUserFromFlags() {
        clearUser(userID, FLAGS);
    }

    // Wipes the entire database.
    static void WipeDatabase() {
        UserRecordAPI.DeleteAllUserRecords(MEDIAFILE_STORE);
    }

    static List<String> GetFlagsStartingWith(String Prefix) {
        List<String> TheList = new ArrayList<String>();

        Object[] AllRecords = UserRecordAPI.GetAllUserRecords(MEDIAFILE_STORE);

        if (AllRecords==null || AllRecords.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "GetFlagsStartingWith: No records.");
            return TheList;
        }

        for (Object record : AllRecords) {

            String[] names = UserRecordAPI.GetUserRecordNames(record);

            for (String name : names)
                if (name.startsWith(Prefix))
                    TheList.add(name);
        }

        return TheList;
    }
}
