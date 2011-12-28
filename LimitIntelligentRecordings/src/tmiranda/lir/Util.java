/*
 * Various utility methods.
 */

package tmiranda.lir;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class Util {

    /**
     * Return the number of recordings with the same Airing title as MediaFile.
     * @param MediaFile
     * @return
     */
    public static int getNumberRecorded(Object MediaFile) {
        if (MediaFile == null)
            return 0;

        Map airingMap = Database.GroupByMethod(MediaFileAPI.GetMediaFiles(), "GetAiringTitle");
        return airingMap.keySet().size();
    }
}
