package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class GlobalAPI {
    public static long getUsedVideoDiskspace() {
        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return Global.GetUsedVideoDiskspace();
        }

        Object[] allMediaFiles = sagex.api.MediaFileAPI.GetMediaFiles("T");
        if (allMediaFiles==null || allMediaFiles.length==0)
            return 0;

        long bytes = 0;

        for (Object mediaFile : allMediaFiles) {
            MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(mediaFile));
            if (!MMF.isDeleted())
                bytes += sagex.api.MediaFileAPI.GetSize(mediaFile);
        }

        return bytes;
    }

    /**
     * Returns the number of bytes used by TV recordings belonging to User.
     * @param User
     * @return The number of bytes of space used by the User.
     */
    public static long getUsedVideoDiskspace(String User) {

        if (User==null)
            return 0;

        if (User.equalsIgnoreCase(Plugin.SUPER_USER))
            return Global.GetUsedVideoDiskspace();

        Object[] allMediaFiles = sagex.api.MediaFileAPI.GetMediaFiles("T");
        if (allMediaFiles==null || allMediaFiles.length==0)
            return 0;

        long bytes = 0;

        for (Object mediaFile : allMediaFiles) {
            MultiMediaFile MMF = new MultiMediaFile(User, API.ensureIsMediaFile(mediaFile));
            if (!MMF.isDeleted())
                bytes += sagex.api.MediaFileAPI.GetSize(mediaFile);
        }

        return bytes;
    }

    public static Object[] getScheduledRecordings() {
        String user = UserAPI.getLoggedinUser();

        if (user==null || user.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return Global.GetScheduledRecordings();
        }

        List<Object> userScheduledRecordings = new ArrayList<Object>();

        Object[] scheduledRecordings = Global.GetScheduledRecordings();

        if (scheduledRecordings==null)
            return null;

        for (Object recording : scheduledRecordings) {
            if (API.isMediaFileForLoggedOnUser(recording))
                userScheduledRecordings.add(recording);
        }

        return userScheduledRecordings.toArray();
    }
}
