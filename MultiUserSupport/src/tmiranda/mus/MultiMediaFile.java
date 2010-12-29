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
public class MultiMediaFile extends MediaFileControl {

    static final String DONTLIKE = "_DontLike";
    static final String ARCHIVED = "_Archived";
    static final String FAVORITE = "_Favorite";
    static final String DELETED = "_Deleted";
    static final String WATCHEDTIME = "_WatchedTime";

    private String  userID = null;

    public MultiMediaFile(String UserID, Object MediaFile) {
        super(MediaFile);
        userID = (UserID==null ? "null" : UserID);
    }

    boolean isDeleted() {
        if (!isValid())
            return true;

        String deleted = null;
          
        if (isMediaFile()) 
            deleted = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+DELETED);
        else
            deleted = UserRecordAPI.GetUserRecordData(userRecord, userID+DELETED);

        return (deleted == null ? false : deleted.equalsIgnoreCase("true"));
    }

    boolean isDontLike() {

        if (!isValid()) {
            return false;
        }

        String DontLike = null;
         
        if (isMediaFile()) 
            DontLike = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+DONTLIKE);
        else
            DontLike = UserRecordAPI.GetUserRecordData(userRecord, userID+DONTLIKE);

        return (DontLike==null || DontLike.isEmpty() ? AiringAPI.IsDontLike(sageMediaFile) : DontLike.equalsIgnoreCase("true"));
    }
    
    void setDontLike(String value) {
        if (!isValid())
            return;

        // Set in both Airing DB and MediaFile!

        MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+DONTLIKE, checkBooleanString(value));
        UserRecordAPI.SetUserRecordData(userRecord, userID+DONTLIKE, checkBooleanString(value));
    }

    boolean isArchived() {

        if (!isValid() || useSageDataBase(userID)) {
            return MediaFileAPI.IsLibraryFile(sageMediaFile);
        }

        String Archived = null;
                
        if (isMediaFile()) 
            Archived = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+ARCHIVED);
        else
            Archived = UserRecordAPI.GetUserRecordData(userRecord, userID+ARCHIVED);

        return (Archived==null || Archived.isEmpty() ? MediaFileAPI.IsLibraryFile(sageMediaFile) : Archived.equalsIgnoreCase("true"));
    }

    void setArchived(String value) {
        if (!isValid())
            return;

        if (isMediaFile())
            MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+ARCHIVED, checkBooleanString(value));
        else
            UserRecordAPI.SetUserRecordData(userRecord, userID+ARCHIVED, checkBooleanString(value));
    }

    boolean delete(boolean WithoutPrejudice) {

        // If we have an invalid MMF, just return error.
        if (!isValid()) {
            return false;
        }

        // Mark the MediaFile as deleted.
        if (isMediaFile())
            MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+DELETED, "true");
        else
            UserRecordAPI.SetUserRecordData(userRecord, userID+DELETED, "true");

        // If the user has access to the file (Admin, explicitly allowed, globally all allowed, global allow this user)
        // delete it if this is the last user.
        if (isUserAllowed(userID)) {

            // Remove this user.
            removeUser(userID);

            // See who else can access it.
            List<String> allowedUsers = getUserList();

            // If nobody, then delete it for real.
            if (allowedUsers == null || allowedUsers.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Deleting physical file for user " + userID);
                return (WithoutPrejudice ? MediaFileAPI.DeleteFileWithoutPrejudice(sageMediaFile) : MediaFileAPI.DeleteFile(sageMediaFile));
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "delete: Leaving physical file intact.");
        return true;
    }

    void setWatched(String value) {
        
    }

    private String checkBooleanString(String value) {

        if (value == null) {
            return "false";
        }

        return (value.equalsIgnoreCase("true") ? "true" : "false");
    }
}
