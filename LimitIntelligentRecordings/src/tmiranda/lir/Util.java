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
     * Gets all completed recordings.
     * @return Return as Object because in most cases the results of this method will be
     * passed to other Sage methods that require Object parameters.
     */
    public static Object getAllCompleteRecordings() {
        return Database.FilterByBoolMethod(MediaFileAPI.GetMediaFiles("T"), "IsCompleteRecording", true);
    }

    /**
     * Return the number of completed recordings with the same Airing title as MediaFile.
     * @param MediaFile
     * @return
     */
    public static int getNumberRecorded(Object MediaFile) {
        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getNumberRecorded: null MediaFile.");
            return 0;
        }  else
            return getAllRecorded(MediaFile).size();
    }

    /**
     * Return a List of MediaFiles with the same Airing Title as the specified MediaFile.
     * @param MediaFile
     * @return
     */
    public static List<Object> getAllRecorded(Object MediaFile) {

        List<Object> recordings = new ArrayList<Object>();

        if (MediaFile == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllRecorded: null MediaFile.");
            return recordings;
        }

        // The key will by the AiringTitle and the value will be a List of episodes.
        Map<String, List> airingMap = Database.GroupByMethod(getAllCompleteRecordings(), "GetAiringTitle");
        return airingMap.get(AiringAPI.GetAiringTitle(MediaFile));
    }

    /**
     * Get a List of MediaFiles with the same title as the specified MediaFile and
     * sorted by the specified sortMethod.
     * @param MediaFile
     * @param sortMethod
     * @param descending
     * @return
     */
    public static List<Object> getAllRecorded(Object MediaFile, String sortMethod, boolean descending) {
        return Arrays.asList((Object[])Database.Sort(getAllRecorded(MediaFile), descending, sortMethod));
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

    /**
     * Gets the Airing Titles of all completed Intelligent Recordings sorted alphabetically
     * by the title with the leading articles stripped.
     * @return
     */
    public static String[] getAllIntelligentRecordingTitles() {

        // Filter out any recorded TV files that are Favorites or Manuals.
        Object allIntelligentRecordings = Database.FilterByBoolMethod(Database.FilterByBoolMethod(getAllCompleteRecordings(), "IsFavorite", false), "IsManualRecord", false);

        if (allIntelligentRecordings==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllIntelligentRecordingTitles: null allIntelligentRecordings.");
            return new String[0];
        }

        // Group them according to title.
        Map<String, List> airingMap = Database.GroupByMethod(allIntelligentRecordings, "GetAiringTitle");

        if (airingMap==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAllIntelligentRecordingTitles: null airingMap.");
            return new String[0];
        }

        // The keys are the titles that we need.
        Set<String> unsortedKeys = airingMap.keySet();

        // Ignore leading articles and sort them alphabetically.
        String[] sortedKeys = (String[])unsortedKeys.toArray(new String[unsortedKeys.size()]);
        Arrays.sort(sortedKeys, new strippedTitleComparator());
        return sortedKeys;
    }

    /**
     * Gets the Airing Titles of all completed Intelligent Recordings sorted alphabetically
     * by the title with the leading articles stripped. It also adds a string on the end
     * that contains the custom maximum (if any) for the title.
     * @return
     */
    public static String[] getAllIntelligentRecordingTitlesAndMax() {
        String[] titles = getAllIntelligentRecordingTitles();

        for (int i=0; i<titles.length; i++) {
            String title = titles[i];

            DataStore store = new DataStore(title);
            String titleAndMax = title + " <" + store.getMaxString() + ">";
            if (store.isMonitored())
                titles[i] = titleAndMax;
        }

        return titles;
    }

    public static String[] OLDgetAllIntelligentRecordingTitles() {

        Object allMediaFiles = MediaFileAPI.GetMediaFiles("T");
        Object allNonFavorites = Database.FilterByBoolMethod(allMediaFiles, "IsFavorite", false);
        Object allIntelligentRecordings = Database.FilterByBoolMethod(allNonFavorites, "IsManualRecord", false);
        Map<String, List> airingMap = Database.GroupByMethod(allIntelligentRecordings, "GetAiringTitle");
        Set<String> unsortedKeys = airingMap.keySet();

        // Is there a better way to strip the leading articles and sort?

        // Key will be stripped title, value will be actual title.
        Map<String, String> strippedToUnstrippedMap = new HashMap<String, String>();

        // Place to hold titles once the articles are stripped.
        List<String> strippedKeys = new ArrayList<String>();

        // Finally, a place to keep the sorted keys, but they will contain the articles.
        List<String> sortedKeys = new ArrayList<String>();

        // Go through the raw titles and create the List of stripped titles and the Map
        // to convert stripped titles back to unstripped titles.
        for (String key : unsortedKeys) {
            String strippedKey = Database.StripLeadingArticles(key);
            strippedKeys.add(strippedKey);
            strippedToUnstrippedMap.put(strippedKey, key);
        }

        // Sort the titles that have had their articles stripped.
        Collections.sort(strippedKeys);

        // Create the list of unstripped titles in the order or the sorted stripped titles.
        for (String key : strippedKeys) {
            sortedKeys.add(strippedToUnstrippedMap.get(key));
        }

        return (String[])sortedKeys.toArray(new String[sortedKeys.size()]);
    }

    /**
     * Strips the custom maximum String from the AIring Title.
     * @param title
     * @return
     */
    public static String removeNumberMax(String title) {
        if (title==null || !title.contains(" <") || !title.contains(">"))
            return title;
        else
            return title.substring(0, title.indexOf(" <"));
    }

    /**
     * Comparator used to sort Strings alphabetically ignoring the leading articles.
     */
    static class strippedTitleComparator implements Comparator<Object> {

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        public int compare(Object o1, Object o2) {
            return Database.StripLeadingArticles((String)o1).compareTo(Database.StripLeadingArticles((String)o2));
        }
    }
}
