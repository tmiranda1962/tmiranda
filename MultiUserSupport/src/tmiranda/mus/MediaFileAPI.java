package tmiranda.mus;

import java.io.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class MediaFileAPI {
    public static Object getMediaFilesWithImportPrefix(Object Mask, String Prefix, boolean b1, boolean b2, boolean b3) {
        String user = UserAPI.getLoggedinUser();

        if (user==null || user.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.Database.GetMediaFilesWithImportPrefix(Mask, Prefix, b1, b2, b3);
        }

        List<Object> userMediaFiles = new ArrayList<Object>();

        Object mediaFiles = sagex.api.Database.GetMediaFilesWithImportPrefix(Mask, Prefix, b1, b2, b3);

        if (mediaFiles==null)
            return null;

        for (Object mediaFile : (Object[]) mediaFiles) {
            if (isMediaFileForLoggedOnUser(mediaFile))
                userMediaFiles.add(mediaFile);
        }

        return userMediaFiles.toArray();
    }

    public static Object[] getMediaFiles(String Mask) {
        String user = UserAPI.getLoggedinUser();

        if (user==null || user.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.MediaFileAPI.GetMediaFiles(Mask);
        }

        List<Object> userMediaFiles = new ArrayList<Object>();

        Object[] mediaFiles = sagex.api.MediaFileAPI.GetMediaFiles(Mask);

        if (mediaFiles==null)
            return null;

        for (Object mediaFile : mediaFiles) {
            if (isMediaFileForLoggedOnUser(mediaFile))
                userMediaFiles.add(mediaFile);
        }

        return userMediaFiles.toArray();
    }

    public static Object[] getMediaFiles() {
        String user = UserAPI.getLoggedinUser();

        if (user==null || user.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.MediaFileAPI.GetMediaFiles();
        }

        List<Object> userMediaFiles = new ArrayList<Object>();

        Object[] mediaFiles = sagex.api.MediaFileAPI.GetMediaFiles();

        if (mediaFiles==null)
            return null;

        for (Object mediaFile : mediaFiles) {
            if (isMediaFileForLoggedOnUser(mediaFile))
                userMediaFiles.add(mediaFile);
        }

        return userMediaFiles.toArray();
    }

    /**
     * Checks to see if the specified MediaFile or Airing should be displayed for the currently logged
     * on user.  This method should be used in conjunction with the FilterByBoolMethod()
     * method to filter out those MediaFiles and Airings that should not be displayed.
     * @param MediaFile
     * @return true if the MediaFile or Airing should be displayed for the currently logged on
     * user, false otherwise.
     */
    public static boolean isMediaFileForLoggedOnUser(Object MediaFile) {
        String UserID = UserAPI.getLoggedinUser();

        if (UserID == null || UserID.equalsIgnoreCase(Plugin.SUPER_USER) || MediaFile==null)
            return true;

        MultiMediaFile MMF = new MultiMediaFile(UserID, API.ensureIsMediaFile(MediaFile));
        return !MMF.isDeleted();
    }

    public static Object[] filterMediaFilesNotForLoggedOnUser(Object[] MediaFiles) {
        List<Object> theList = new ArrayList<Object>();

        if (MediaFiles == null)
            return null;

        for (Object MediaFile : MediaFiles)
            if (isMediaFileForLoggedOnUser(MediaFile))
                theList.add(MediaFile);

        return theList.toArray();
    }

    public static Object[] filterMediaFilesNotForLoggedOnUser(List<Object> MediaFiles) {
        return filterMediaFilesNotForLoggedOnUser(MediaFiles.toArray());
    }

    /**
     * Replaces the core API.
     * @param file
     * @param Prefix
     * @return The added MediaFile.
     */
    public static Object addMediaFile(File file, String Prefix) {

        String User = UserAPI.getLoggedinUser();

        Object MediaFile = sagex.api.MediaFileAPI.AddMediaFile(file, Prefix);

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER) || file==null || Prefix==null || MediaFile==null)
            return MediaFile;

        User user = new User(User);
        user.addToMediaFile(MediaFile);
        return MediaFile;
    }

    /**
     * Replaces the core API IsLibraryFile().  (isArchived is a more intuitive name.)
     * @param MediaFile
     * @return true if archived, false otherwise.
     */
    public static boolean isArchived(Object MediaFile) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.MediaFileAPI.IsLibraryFile(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(MediaFile));
        return MMF.isArchived();
    }

    /**
     * Replaces the core API MoveTVFileOutOfLibrary().  (clearArchived is a more intuitive name.)
     * @param MediaFile
     */
    public static void clearArchived(Object MediaFile) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.MediaFileAPI.MoveTVFileOutOfLibrary(MediaFile);
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(MediaFile));
        MMF.clearArchived();
        return;
    }

    /**
     * Replaces the core API MoveFileToLibrary().  (setArchived is a more intuitive name.)
     * @param MediaFile
     */
    public static void setArchived(Object MediaFile) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            sagex.api.MediaFileAPI.MoveFileToLibrary(MediaFile);
            return;
        }

        MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(MediaFile));
        MMF.setArchived();
        return;
    }

    /**
     * Replaces the core API.
     * @param MediaFile
     * @return true if the MediaFile was deleted, false otherwise.
     */
    public static boolean deleteMediaFile(Object MediaFile) {

        String User = UserAPI.getLoggedinUser();

        // If the MediaFile is removed make sure we remove the corresponding record.
        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            MultiMediaFile MMF = new MultiMediaFile(Plugin.SUPER_USER, MediaFile);
            MMF.removeRecord();
            return sagex.api.MediaFileAPI.DeleteFile(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(UserAPI.getLoggedinUser(), API.ensureIsMediaFile(MediaFile));
        return MMF.delete(false);
    }

    /**
     * Undeletes the MediaFile for the specified user.
     * @param MediaFile
     */
    public static void undeleteMediaFile(String User, Object MediaFile) {

        MultiMediaFile MMF = null;

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            MMF = new MultiMediaFile(Plugin.SUPER_USER, MediaFile);
        } else {
            MMF = new MultiMediaFile(User, MediaFile);
        }

        MMF.unhide();
        return;

    }

    /**
     * Replaces the core API.
     * @param MediaFile
     * @return true if the MediaFile was deleted, false otherwise.
     */
    public static boolean deleteMediaFileWithoutPrejudice(Object MediaFile) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.MediaFileAPI.DeleteFileWithoutPrejudice(MediaFile);
        }

        MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(MediaFile));
        return MMF.delete(true);
    }

    /**
     * Replaces the core API. (NOT IMPLEMENTED.)
     * @param ID
     * @return
     */
    private static Object getMediaFileForID(int ID) {
        return null;
    }
}
