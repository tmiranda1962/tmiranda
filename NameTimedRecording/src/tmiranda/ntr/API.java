/*
 * JAR file for NameTimedRecordings.
 */

package tmiranda.ntr;

import java.util.*;
import sagex.api.*;

/**
 * This JAR file contains a method that replaces the GetAiringTitle method from the Sage core.
 * The purpose is to return the proper Airing title if the Airing is a manual recording that
 * has been given a name.
 *
 * @author Tom Miranda.
 */
public class API {

    // This is the default title that GetAiringTitle() will return for timed recordings.
    static final String TIMED_RECORDING = "Timed Record";

    // This is the ManualRecordProperty or MediaFileProperty that will be used to store the name.
    static final String PROPERTY_NAME = "TimedRecordingName";

    // This is the property that is used to keep track of the names of recurring timed recordings.
    static final String PROPERTY_RECURRING_RECORDINGS = "ntr/RecurringRecordingsNew";
    static final String PROPERTY_RECURRING_RECORDINGS_OLD = "ntr/RecurringRecordings";
    
    static final String KEY_NAME_DELIMITER = "-";

    static final String[] FORBIDDEN_NAME_ELEMENTS = {KEY_NAME_DELIMITER, Key.ELEMENT_DELIMITER, TIMED_RECORDING, PropertyElement.ELEMENT_DELIMITER};

    /**
     * This method should be used in place of the core GetAiringTitle() method.
     *
     * @param MediaFile A Sage Object that may be an Airing, MediaFile or other type of Object.
     * @return The correct title.  If MediaFile is not a MediaFile or Airing the default title
     * will be returned.  If MediaFile is not a timed recording the default title will be
     * returned.  If MediaFile is a timed recording the timed recording name will be returned.
     */
    public static String getAiringTitle(Object MediaFile) {

        String title = AiringAPI.GetAiringTitle(MediaFile);

        // If it's null return whatever the core would have returned.
        if (MediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAiringTitle: null MediaFile");
            return title;
        }

        // If it's not a timed recording just return the original title.
        if (title==null || !title.startsWith(TIMED_RECORDING)) {
            return title;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getAiringTitle: Found a timed recording " + title);

        // We know it's a timed recording so try to find its name.
        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {

            String mediaFileName = getNameForMediaFile(MediaFile);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getAiringTitle: Name for MediaFile " + mediaFileName);
            setMediaFileName(MediaFile, mediaFileName);
            return mediaFileName==null || mediaFileName.isEmpty() ? title : mediaFileName;

        } else if (AiringAPI.IsAiringObject(MediaFile)) {

            String airingName = getNameForAiring(MediaFile);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getAiringTitle: Name for Airing " + airingName);
            setAiringMediaFileName(MediaFile, airingName);
            return airingName==null || airingName.isEmpty() ? title : airingName;

        } else {

            // If it's not a MediaFile or Airing return the original title.
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getAiringTitle: Is not an Airing or MediaFile.");
            return title;
        }
    }

    /**
     * Add a timed recording for an Airing that already has a ManualRecordProperty.
     * @param Airing
     */
    public static void addRecurring(Object Airing) {

        if (Airing==null)
            return;

        String name = AiringAPI.GetManualRecordProperty(Airing, PROPERTY_NAME);

        // Make sure it's got a ManualRecordProperty name.
        if (name==null || name.isEmpty()|| name.startsWith(API.TIMED_RECORDING)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addRecurring: Failed to get name for Airing " + name);
            return;
        }
            
        // If it's not a recurring recording there is no need to store the name in
        // the property map.
        String recurrence = AiringAPI.GetScheduleRecordingRecurrence(Airing);

        if (recurrence!=null && !recurrence.equalsIgnoreCase("once")) {
            PropertyElement.addPropertyElement(Key.makeRawMapKey(Airing), name);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addRecurring: Added recurring " + Key.makeRawMapKey(Airing) + KEY_NAME_DELIMITER + name);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addRecurring: Recurrence is once.");
        }
    }

    /**
     * Add a timed recording.
     *
     * @param Channel
     * @param StartTime
     * @param EndTime
     * @param Recurrence
     * @param Name
     */
    public static void addRecurring(Object Channel, Long StartTime, Long EndTime, String Recurrence, String Name) {

        if (Channel==null || StartTime==null || EndTime==null || Recurrence==null || Recurrence.isEmpty() || Name==null || Name.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addRecurring: null parameter.");
            return;
        }

        if (Recurrence.equalsIgnoreCase("once")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addRecurring: Recurrence is once.");
            return;
        }

        Key key = new Key(ChannelAPI.GetChannelName(Channel), Recurrence, StartTime, EndTime);
        PropertyElement.addPropertyElement(key.getRawMapKey(), Name);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addRecurring: Added recurring " + key.getRawMapKey() + KEY_NAME_DELIMITER + Name);
    }

    public static void cancelRecord(Object Airing) {
        
        if (Airing==null) {
            AiringAPI.CancelRecord(Airing);
            return;
        }
        
        Map<String, String> nameMap = PropertyElement.getNameMap();

        String key = getKeyForAiring(Airing);
        
        String name = nameMap.get(key);

        if (name!=null) {
            PropertyElement.removePropertyElement(key, name);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "cancelRecord: Removed " + key + KEY_NAME_DELIMITER + name);
        } else {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "cancelRecord: Failed to find " + key);
        }

        AiringAPI.CancelRecord(Airing);
    }

    public static boolean changeNameOfManual(Object Airing, String NewName) {
    
        if (Airing==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "changeNameOfManual: null Airing.");
            return false;
        }
        
        if (!AiringAPI.IsManualRecord(Airing)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "changeNameOfManual: Airing is not a manual recording.");
            return false;            
        }
        
        // Change the ManualRecordProperty.
        AiringAPI.SetManualRecordProperty(Airing, PROPERTY_NAME, NewName.isEmpty() ? null : NewName);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "changeNameOfManual: Setting ManualRecordProperty " + (NewName.isEmpty() ? null : NewName));

        // Change the MediaFileName.
        if (AiringAPI.IsAiringObject(Airing)) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "changeNameOfManual: Setting Airing MediaFileName " + NewName);
            setAiringMediaFileName(Airing, NewName);
        }  else {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "changeNameOfManual: Setting MediaFileName " + NewName);
            setMediaFileName(Airing, NewName);
        }
        
        // If it's in the property map we need to remove it.
        String airingName = getNameForRecurring(Airing);
        
        if (airingName == null || airingName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "changeNameOfManual: Not in property map.");
            return true;
        }
        
        String keyForManual = getKeyForAiring(Airing);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "changeNameOfManual: Looking for " + keyForManual);        
        
        PropertyElement.removePropertyElement(keyForManual, airingName);
        PropertyElement.addPropertyElement(keyForManual, NewName);
        
        return true;
    }

    public static String stripIllegalCharacters(String name) {

        if (name==null || name.isEmpty())
            return name;

        String n = name;

        for (String e : FORBIDDEN_NAME_ELEMENTS)
            if (n!=null)
                n = n.replaceAll(e, "");

        return n;
    }

    /*
     * Looks in MediaFileMetadata, then Airing ManualRecordProperty, then property map.
     */
    static String getNameForMediaFile(Object MediaFile) {

        // First see if the name has been set in the MediaFile metadata.
        String mediaFileName = MediaFileAPI.GetMediaFileMetadata(MediaFile, PROPERTY_NAME);

        // If the MediaFile had the name property return it.
        if (mediaFileName!=null && !mediaFileName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForMediaFile: Found name in Metadata " + mediaFileName);
            return mediaFileName;
        }

        // The MediaFile did not have a name so see if the Airing has one.
        Object airing = MediaFileAPI.GetMediaFileAiring(MediaFile);

        if (airing==null) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForMediaFile: No Airing, returning null.");
            return null;
        }

        // See if the Airing has a name.
        mediaFileName = AiringAPI.GetManualRecordProperty(MediaFile, PROPERTY_NAME);

        if (mediaFileName!=null || !mediaFileName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForMediaFile: Found name in ManualRecordProperty " + mediaFileName);
            return mediaFileName;
        }

        mediaFileName = getNameForRecurring(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForMediaFile: Found name in property map " + mediaFileName);
        return mediaFileName;
    }

    /*
     * Looks in ManualRecordProperty, then property map, then MediaFileMetadata.
     */
    static String getNameForAiring(Object Airing) {

        // First look in the property map.
        String airingName = getNameForRecurring(Airing);

        if (airingName!=null && !airingName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForAiring: Found name in property map " + airingName);
            return airingName;
        }

        // Look in the ManualRecordProperty.
        airingName = AiringAPI.GetManualRecordProperty(Airing, PROPERTY_NAME);

        if (airingName!=null && !airingName.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForAiring: Found name in ManualRecordProperty " + airingName);
            return airingName;
        }

        Object mediaFile = MediaFileAPI.GetMediaFileAiring(Airing);

        if (mediaFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForAiring: No MediaFile, returning null.");
            return null;
        }  else {
            airingName = MediaFileAPI.GetMediaFileMetadata(mediaFile, PROPERTY_NAME);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getNameForAiring: Found name in Metadata " + airingName);
            return airingName;
        }
    }

    /*
     * Looks in the property map.
     */
    static String getNameForRecurring(Object Airing) {
        if (Airing==null)
            return null;

        // Get the recurrence.
        String recurrence = AiringAPI.GetScheduleRecordingRecurrence(Airing);

        // If it has no recurrence, or is only a one-shot manual, see if the ManualRecordProperty
        // has been set.
        if (recurrence==null || recurrence.isEmpty() || recurrence.equalsIgnoreCase("Once")) {
            String airingName = AiringAPI.GetManualRecordProperty(Airing, PROPERTY_NAME);

            if (airingName!=null && !airingName.isEmpty())
                return airingName;
        }

        // It must be a recurring recording, so see if it's in the property file.
        String key = getKeyForAiring(Airing);

        Map<String, String> nameMap = PropertyElement.getNameMap();

        String airingName = nameMap.get(key);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getNameForRecurring: Name is " + airingName);
        return airingName;
    }

    /*
     * Set MediaFileMetadata for a MediaFile.
     */
    static void setMediaFileName(Object MediaFile, String Name) {

        if (MediaFile==null || Name==null || Name.isEmpty())
            return;

        MediaFileAPI.SetMediaFileMetadata(MediaFile, PROPERTY_NAME, Name);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "setMediaFileName: Setting Metadata name " + Name);
        return;
    }

    /*
     * Set MediaFileMetadata for an Airing.
     */
    static void setAiringMediaFileName(Object Airing, String Name) {

        Object MediaFile = AiringAPI.GetMediaFileForAiring(Airing);

        if (MediaFile!=null)
            setMediaFileName(MediaFile, Name);
        else
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "setAiringMediaFileName: null MediaFile.");

        return;
    }

    public static List<String> getSuggestedNames(Object Channel, long startTime, long endTime) {

        List<String> names = new ArrayList<String>();

        if (Channel==null)
            return names;

        Object[] airings = Database.GetAiringsOnChannelAtTime(Channel, startTime, endTime, false);

        if (airings==null || airings.length==0)
            return names;

        for (Object airing : airings) {
            String name = stripIllegalCharacters(AiringAPI.GetAiringTitle(airing));
            if (name!=null && !name.isEmpty())
                names.add(name);
        }

        return names;
    }

    public static String getVersion() {
        return Plugin.VERSION;
    }
    
    /**
     * The default STV stores the method names that are used as filters in the properties
     * file.  This method is used to change GetAiringTitle to getAiringTitle in the properties
     * file.
     * @param OldValue The original value of the property.
     * @param NewValue The new value for the property.
     * @return
     */
    public static int changePropertyValue(String OldValue, String NewValue) {

        if (OldValue==null || OldValue.isEmpty() || NewValue==null || NewValue.isEmpty())
            return 0;

        // Make sure all properties are flushed to disk.
        Configuration.SaveProperties();

        int numberChanged = 0;

        List<String> allProperties = getAllProperties();

        for (String property : allProperties) {
            String currentValue = Configuration.GetProperty(property, null);

            if (currentValue!=null && currentValue.equals(OldValue)) {
                Configuration.SetProperty(property, NewValue);
                numberChanged++;
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "changePropertyValue: Changed property " + property + "=" + OldValue + " to " + NewValue);
            }
        }

        Configuration.SaveProperties();
        return numberChanged;
    }

    private static List<String> getAllProperties() {

        List<String> allProperties = new ArrayList<String>();

        // Get properties that have no branches.
        String[] propsWithNoBranches = Configuration.GetSubpropertiesThatAreLeaves("");

        if (propsWithNoBranches!=null && propsWithNoBranches.length>0)
            allProperties.addAll(Arrays.asList(propsWithNoBranches));

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getAllProperties: Number of properties with no branches " + propsWithNoBranches.length);

        // Get all of the branch roots.
        String[] branches = Configuration.GetSubpropertiesThatAreBranches("");

        if (branches==null || branches.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getAllProperties: No branch roots found.");
            return allProperties;
        }

        if (Log.GetLogLevel() <= Log.LOGLEVEL_VERBOSE)
            for (String p : branches)
                System.out.println("getAllProperties: Branch roots = " + p);

        // Now get all of the leaves of the branch roots.
        for (String branch : branches)
            allProperties.addAll(getAllLeaves(branch));

        Collections.sort(allProperties);
        
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getAllProperties: Total properties found = " + allProperties.size());

        if (Log.GetLogLevel() <= Log.LOGLEVEL_VERBOSE)
            for (String p : allProperties)
                System.out.println("getAllProperties: Property = " + p);

        return allProperties;
    }

    private static List<String> getAllLeaves(String root) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getAllLeaves: Processing root " + root);
        List<String> allLeaves = new ArrayList<String>();

        // Get the leaves of this root.
        String[] leaves = Configuration.GetSubpropertiesThatAreLeaves(root);

        // Add the leaves.
        if (leaves!=null && leaves.length>0) {
            for (String leaf : leaves) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getAllLeaves: Adding final leaf " + root + "/" + leaf);
                allLeaves.add(root + "/" + leaf);
            }
        }

        // Now recurse to get the leaves of the branches.
        String[] branches = Configuration.GetSubpropertiesThatAreBranches(root);

        if (branches!=null && branches.length>0)
            for (String branch : branches) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getAllLeaves: Recursing to find leafs of " + root + "/" + branch);
                allLeaves.addAll(getAllLeaves(root + "/" + branch));
            }
        
        return allLeaves;
    }

    private static String getKeyForAiring(Object Airing) {

        Key airingKey = Key.makeRawKey(Airing);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getKeyForAiring: Raw key " + airingKey);
        String keyForAiring = airingKey.getMatchingKey();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getKeyForAiring: Found key " + keyForAiring);
        return keyForAiring;
    }
}
