/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class MultiAiring extends MultiObject {

    static final String AIRING_STORE        = "MultiUser.Airing";
    static final String MANUAL_IN_PROGRESS  = "ManualInProgress";
    static final String MANUAL              = "Manual";
    static final String WATCHEDTIME         = "WatchedTime";

    static final String[]   FLAGS = {MANUAL, MANUAL_IN_PROGRESS, INITIALIZED};
 
    private Object sageAiring       = null;


    public MultiAiring(String UserID, Object Airing) {
        
        super(UserID, AIRING_STORE, AiringAPI.GetAiringID(Airing), MediaFileAPI.GetMediaFileID(AiringAPI.GetMediaFileForAiring(Airing)), MultiMediaFile.MEDIAFILE_STORE);

        if (!isValid || Airing==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "MultiAiring: null parameter " + UserID);
            return;
        }

        if (!AiringAPI.IsAiringObject(Airing)) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiAiring: Object is not an Airing.");
            return;
        }

        sageAiring = Airing;

        if (!isInitialized) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiAiring: Initializing user " + userID + ":" + AiringAPI.GetAiringTitle(Airing));
            initializeUser();
        }
    }

    // Record related methods must be resolved to an Airing in the API because only
    // Airings can be recorded.

    boolean isManualRecord() {

        if (!isValid) {
            return AiringAPI.IsManualRecord(sageAiring);
        } else
            return containsFlag(MANUAL, userID);
    }

    // The API invokes the Sage core to start the recording.
    void setManualRecord() {
        if (!isValid)
            return;
        else {
            addFlag(MANUAL, userID);
            addFlag(MANUAL_IN_PROGRESS, userID);
        }
    }

    // The API invokes the Sage core to start the recording.
    void setRecordingTimes(long StartTime, long StopTime) {
        if (!isValid)
            return;
        else {
            addFlag(MANUAL, userID);
            addFlag(MANUAL_IN_PROGRESS, userID);
        }
    }

    // Cancels the recording in progress.
    void cancelManualRecord() {

        if (!isValid) {
            AiringAPI.CancelRecord(sageAiring);
            return;
        }

        removeFlag(MANUAL_IN_PROGRESS, userID);

        if (!containsFlagAnyData(MANUAL_IN_PROGRESS)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "cancelManualRecord: No users need this manual, removing.");
            AiringAPI.CancelRecord(sageAiring);
        }
    }


    // Favories methods must be resolved to an Airing in the API because only
    // Airings can have Favorite status.

    boolean isFavorite() {
        Object Favorite = FavoriteAPI.GetFavoriteForAiring(sageAiring);

        if (Favorite==null) {
            return false;
        }

        MultiFavorite MF = new MultiFavorite(userID, Favorite);
        return MF.isFavorite();
    }

    Object getFavoriteForAiring() {

        Object Favorite = FavoriteAPI.GetFavoriteForAiring(sageAiring);

        if (Favorite==null)
            return null;

        MultiFavorite MF = new MultiFavorite(userID, Favorite);
        return (MF.isFavorite() ? Favorite : null);
    }

    static Object[] getAllSageAirings() {
        Vector V = Database.SearchSelectedFields(" ", false, true, true, true, true, true, true, true, true, true);

        if (V==null || V.isEmpty())
            return null;
        else
            return V.toArray();
    }


    // Initialize this user for the Airing.
    final void initializeUser() {

        if (AiringAPI.IsDontLike(sageAiring))
            addFlag(DONTLIKE, userID);
        else
            removeFlag(DONTLIKE, userID);

        if (AiringAPI.IsManualRecord(sageAiring))
            addFlag(MANUAL, userID);
        else
            removeFlag(MANUAL, userID);

        removeFlag(MANUAL_IN_PROGRESS, userID);

        //setRealWatchedStartTime(AiringAPI.GetRealWatchedStartTime(sageAiring));
        //setRealWatchedEndTime(AiringAPI.GetRealWatchedEndTime(sageAiring));

        //setWatchedStartTime(AiringAPI.GetWatchedStartTime(sageAiring));
        //setWatchedEndTime(AiringAPI.GetWatchedEndTime(sageAiring));

        setRecordData(INITIALIZED, "true");
        isInitialized = true;
        return;
    }

    // Remove this user from the Airing.
    void clearUserFromFlags() {
        clearUser(userID, FLAGS);
    }

    // Wipes the entire database.
    static void WipeDatabase() {
        UserRecordAPI.DeleteAllUserRecords(AIRING_STORE);
    }

    // Gets the flags for the current user.
    List<String> getFlagsForUser() {

       List<String> theList = getObjectFlagsForUser();

       for (String flag : FLAGS)
           if (containsFlag(flag, userID))
               theList.add("Contains " + flag);
           else
               theList.add("!Contains " + flag);

       return theList;
   }
}
