/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 * Class that implements the SageTV AiringAPI for multiple users.
 * Unless noted otherwise the methods behave in the same way as the Sage core APIs.
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
        
        super(UserID, AIRING_STORE, sagex.api.AiringAPI.GetAiringID(Airing), sagex.api.MediaFileAPI.GetMediaFileID(sagex.api.AiringAPI.GetMediaFileForAiring(Airing)), MultiMediaFile.MEDIAFILE_STORE);

        if (!isValid || Airing==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "MultiAiring: null parameter " + UserID);
            return;
        }

        if (!sagex.api.AiringAPI.IsAiringObject(Airing)) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiAiring: Object is not an Airing " + sagex.api.MediaFileAPI.GetMediaTitle(Airing));
            return;
        }

        sageAiring = Airing;

        if (!isInitialized) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiAiring: Initializing user " + userID + ":" + sagex.api.AiringAPI.GetAiringTitle(Airing));
            initializeUser();
            addDataToFlag(INITIALIZED, userID);
        }
    }

    // Record related methods must be resolved to an Airing in the API because only
    // Airings can be recorded.

    boolean isManualRecord() {

        if (!isValid) {
            return sagex.api.AiringAPI.IsManualRecord(sageAiring);
        } else
            return containsFlag(MANUAL, userID);
    }

    // The API invokes the Sage core to start the recording.
    void setManualRecord() {
        if (!isValid)
            return;
        else {
            addDataToFlag(MANUAL, userID);
            addDataToFlag(MANUAL_IN_PROGRESS, userID);
        }
    }

    // The API invokes the Sage core to start the recording.
    void setRecordingTimes(long StartTime, long StopTime) {
        if (!isValid)
            return;
        else {
            addDataToFlag(MANUAL, userID);
            addDataToFlag(MANUAL_IN_PROGRESS, userID);
        }
    }

    // Cancels a manual recording for the logged on user.
    void cancelManualRecord() {

        if (!isValid) {
            sagex.api.AiringAPI.CancelRecord(sageAiring);
            return;
        }

        removeDataFromFlag(MANUAL, userID);
        removeDataFromFlag(MANUAL_IN_PROGRESS, userID);

        if (!containsFlagAnyData(MANUAL_IN_PROGRESS)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "cancelManualRecord: No users need this manual, removing.");
            sagex.api.AiringAPI.CancelRecord(sageAiring);
        }
    }

    // Cancels the manual record for all users.  Should be used to clear the flags after the
    // super user or null user cancels a manual recording.
    void cancelManualRecordForAllUsers() {

        if (!isValid) {
            return;
        }

        List<String> users = User.getAllUsers();

        for (String user : users) {
            removeDataFromFlag(MANUAL, user);
            removeDataFromFlag(MANUAL_IN_PROGRESS, user);
        }

    }


    // Favories methods must be resolved to an Airing in the API because only
    // Airings can have Favorite status.

    boolean isFavorite() {
        Object Favorite = sagex.api.FavoriteAPI.GetFavoriteForAiring(sageAiring);

        if (Favorite==null) {
            return false;
        }

        MultiFavorite MF = new MultiFavorite(userID, Favorite);
        return MF.isFavorite();
    }

    Object getFavoriteForAiring() {

        Object Favorite = sagex.api.FavoriteAPI.GetFavoriteForAiring(sageAiring);

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

        if (sagex.api.AiringAPI.IsDontLike(sageAiring))
            addDataToFlag(DONTLIKE, userID);
        else
            removeDataFromFlag(DONTLIKE, userID);

        if (sagex.api.AiringAPI.IsManualRecord(sageAiring))
            addDataToFlag(MANUAL, userID);
        else
            removeDataFromFlag(MANUAL, userID);

        removeDataFromFlag(MANUAL_IN_PROGRESS, userID);
        removeDataFromFlag(DELETED, userID);

        //setRealWatchedStartTime(AiringAPI.GetRealWatchedStartTime(sageAiring));
        //setRealWatchedEndTime(AiringAPI.GetRealWatchedEndTime(sageAiring));

        //setWatchedStartTime(AiringAPI.GetWatchedStartTime(sageAiring));
        //setWatchedEndTime(AiringAPI.GetWatchedEndTime(sageAiring));

        addDataToFlag(INITIALIZED, userID);
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
            if (userID.equalsIgnoreCase(Plugin.SUPER_USER)) {
               theList.add(flag + getRecordData(flag));
            } else {   
               if (containsFlag(flag, userID))
                   theList.add("Contains " + flag);
               else
                   theList.add("!Contains " + flag);
           }

       return theList;
   }
}
