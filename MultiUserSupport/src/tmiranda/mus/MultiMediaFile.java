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


public class MultiMediaFile extends MultiObject {

    // The DataStore for MediaFiles.
    static final String MEDIAFILE_STORE = "MultiUser.MediaFile";

    // These Flags will contain a comma delimited string of userIDs.
    static final String ARCHIVED        = "Archived";

    static final String[]   FLAGS = {ARCHIVED, INITIALIZED};

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
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiMediaFile: Object is not a MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            return;
        }

        sageMediaFile = MediaFile;

        if (!isInitialized) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiMediaFile: Initializing user " + userID + ":" + MediaFileAPI.GetMediaTitle(MediaFile));
            initializeUser();
            addFlag(INITIALIZED, userID);
        }
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
            removeFlag(ARCHIVED, userID);
    }

    boolean isArchived() {

        if (!isValid)
            return MediaFileAPI.IsLibraryFile(sageMediaFile);
        else
            return containsFlag(ARCHIVED, userID);
    }


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

        addFlag(INITIALIZED, userID);
        isInitialized = true;
        return;
    }

    // Remove this user from the MediaFile.
    void clearUserFromFlags() {
        clearUserFromFlagPrefix(FLAG_PREFIXES);
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
            if (userID.equalsIgnoreCase(Plugin.SUPER_USER)) {
               theList.add(flag + getRecordData(flag));
            } else {
               if (containsFlag(flag, userID))
                   theList.add("Contains " + flag);
               else
                   theList.add("!Contains " + flag);
            }

        for (String prefix : FLAG_PREFIXES)
            theList.add(prefix + "=" + getRecordData(prefix+userID));

       return theList;
   }
}
