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
        else
            return getAllRecorded(MediaFile).size();
    }

    public static List<Object> getAllRecorded(Object MediaFile) {

        List<Object> recordings = new ArrayList<Object>();

        if (MediaFile == null)
            return recordings;

        // The key will by the AiringTitle and the value will be a List of episodes.
        Map<String, List> airingMap = Database.GroupByMethod(MediaFileAPI.GetMediaFiles("T"), "GetAiringTitle");
        return airingMap.get(AiringAPI.GetAiringTitle(MediaFile));
    }

    public static int GetIntProperty(String Property, String Value) {

        String prop = Configuration.GetServerProperty(Property, Value);

        int p = 0;

        try {
            p = Integer.parseInt(prop);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "GetIntProperty: Invalid integer for GetIntProperty " + Property + " " + prop);
            p = 0;
        }

        return p;
    }

    public static int GetIntProperty(String Property, Integer Value) {
        return GetIntProperty(Property, Value.toString());
    }
}
