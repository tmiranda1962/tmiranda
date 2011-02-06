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
    //static final String MEDIATIME_PREFIX    = "MediaTime_";
    static final String CHAPTERNUM_PREFIX   = "ChapterNum_";
    static final String TITLENUM_PREFIX     = "TitleNum_";

    static final String[] FLAG_PREFIXES = {CHAPTERNUM_PREFIX, TITLENUM_PREFIX};

    private Object sageMediaFile    = null;

    public MultiMediaFile(String UserID, Object MediaFile) {
        super(UserID, MEDIAFILE_STORE, MediaFileAPI.GetMediaFileID(MediaFile), AiringAPI.GetAiringID(MediaFileAPI.GetMediaFileAiring(MediaFile)), MultiAiring.AIRING_STORE);

        if (!isValid || MediaFile==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiMediaFile: null parameter " + UserID);
            return;
        }

        if (!MediaFileAPI.IsMediaFileObject(MediaFile)) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiMediaFile: Object is not a MediaFile.");
            return;
        }

        sageMediaFile = MediaFile;

        if (!isInitialized) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiMediaFile: Initializing user " + userID + ":" + MediaFileAPI.GetMediaTitle(MediaFile));
            initializeUser();
        }
    }

    // Delete and Archive must be resolved to a MediaFile in the API because the methods
    // only pertain to physical files.

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

/*
    long getMediaTime() {
        String D = getRecordData(MEDIATIME_PREFIX + userID);

        try {
            long duration = Long.parseLong(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getMediaTime: Bad number " + D);
            return -1;
        }
    }

    void setMediaTime(String Duration) {
        setRecordData(MEDIATIME_PREFIX + userID, Duration);
    }

    void setMediaTime(long Duration) {
        Long D = Duration;
        setMediaTime(D.toString());
    }
 
 */


    int getChapterNum() {
        String D = getRecordData(CHAPTERNUM_PREFIX + userID);

        try {
            int duration = Integer.parseInt(D);
            return duration;
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getChapterNum: Bad number " + D);
            return -1;
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
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getTitleNum: Bad number " + D);
            return -1;
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
    final void initializeUser() {

        if (MediaFileAPI.IsLibraryFile(sageMediaFile))
            addFlag(ARCHIVED, userID);
        else
            removeFlag(ARCHIVED, userID);

        removeFlag(DELETED, userID);

        setWatchedDuration(null);
        //setMediaTime(null);
        setChapterNum(null);
        setTitleNum(null);

        //setRealWatchedStartTime(AiringAPI.GetRealWatchedStartTime(sageMediaFile));
        //setRealWatchedEndTime(AiringAPI.GetRealWatchedEndTime(sageMediaFile));
        
        //setWatchedStartTime(AiringAPI.GetWatchedStartTime(sageMediaFile));
        //setWatchedEndTime(AiringAPI.GetWatchedEndTime(sageMediaFile));

        setRecordData(INITIALIZED, "true");
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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "GetFlagsStartingWith: Found records " + AllRecords.length);

        for (Object record : AllRecords) {

            String[] names = UserRecordAPI.GetUserRecordNames(record);

            for (String name : names)
                if (name.startsWith(Prefix)) {
                    String Data = UserRecordAPI.GetUserRecordData(record, name);
                    TheList.add(name + "=" + Data);
                }
        }

        return TheList;
    }

    List<String> getFlagsForUser() {

       List<String> theList = getObjectFlagsForUser();

       for (String flag : FLAGS)
           if (containsFlag(flag, userID))
               theList.add("Contains " + flag);
           else
               theList.add("!Contains " + flag);

        for (String prefix : FLAG_PREFIXES)
            theList.add(prefix + "=" + getRecordData(prefix+userID));

       return theList;
   }
}
