/*
 * Multi-user MediaFile Object.
 */
package tmiranda.mus;

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

    public MultiMediaFile(String userID, Object MediaFile) {
        super(MediaFile);
        this.userID = (userID==null ? "null" : userID);
    }

    boolean isDeleted() {
        String deleted = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+DELETED);
        return (deleted == null ? false : deleted.equalsIgnoreCase("true"));
    }

    boolean isDontLike() {

        if (useSageDataBase(userID)) {
            return AiringAPI.IsDontLike(sageMediaFile);
        }

        String DontLike = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+DONTLIKE);

        return (DontLike==null ? false : DontLike.equalsIgnoreCase("true"));
    }
    
    void setDontLike(String value) {
        MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+DONTLIKE, checkBooleanString(value));
    }

    boolean isArchived() {

        if (useSageDataBase(userID)) {
            return MediaFileAPI.IsLibraryFile(sageMediaFile);
        }

        String Archived = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+ARCHIVED);

        return (Archived==null ? false : Archived.equalsIgnoreCase("true"));
    }

    void setArchived(String value) {
        MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+ARCHIVED, checkBooleanString(value));
    }

    boolean isFavorite() {

        if (useSageDataBase(userID)) {
            return AiringAPI.IsFavorite(sageMediaFile);
        }

        String Favorite = MediaFileAPI.GetMediaFileMetadata(sageMediaFile, userID+FAVORITE);

        return (Favorite==null ? false : Favorite.equalsIgnoreCase("true"));
    }

    void setFavorite(String value) {
        MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+FAVORITE, checkBooleanString(value));
    }

    boolean delete() {

        if (useSageDataBase(userID)) {
            return MediaFileAPI.DeleteFile(sageMediaFile);
        }

        MediaFileAPI.SetMediaFileMetadata(sageMediaFile, userID+DELETED, "true");
        return true;
    }

    private String checkBooleanString(String value) {

        if (value == null) {
            return "false";
        }

        return (value.equalsIgnoreCase("true") ? "true" : "false");
    }
}
