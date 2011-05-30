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
    static final String PROPERTY_RECURRING_RECORDINGS = "ntr/RecurringRecordings";
    static final String DELIMITER = "-";

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
            System.out.println("NTR: getAiringTitle: null MediaFile");
            return title;
        }

        // If it's not a timed recording just return the original title.
        if (title==null || !title.startsWith(TIMED_RECORDING)) {
            return title;
        }

        System.out.println("NTR: getAiringTitle: Found a timed recording " + title);

        // We know it's a timed recording so try to find its name.
        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {

            String mediaFileName = getNameForMediaFile(MediaFile);
            System.out.println("NTR: getAiringTitle: Name for MediaFile " + mediaFileName);
            setMediaFileName(MediaFile, mediaFileName);
            return mediaFileName==null || mediaFileName.isEmpty() ? title : mediaFileName;

        } else if (AiringAPI.IsAiringObject(MediaFile)) {

            String airingName = getNameForAiring(MediaFile);
            System.out.println("NTR: getAiringTitle: Name for Airing " + airingName);
            setAiringMediaFileName(MediaFile, airingName);
            return airingName==null || airingName.isEmpty() ? title : airingName;

        } else {

            // If it's not a MediaFile or Airing return the original title.
            return title;
        }
    }

    public static void addRecurring(Object Airing) {

        if (Airing==null)
            return;

        String channel = AiringAPI.GetAiringChannelName(Airing);
        String recurrance = AiringAPI.GetScheduleRecordingRecurrence(Airing);

        String name = AiringAPI.GetManualRecordProperty(Airing, PROPERTY_NAME);

        if (name==null || name.isEmpty()) {
            System.out.println("NTR: addRecurring: Failed to get name for Airing.");
        } else {
            PropertyElement.addPropertyElement(channel+recurrance, name);
            System.out.println("NTR: addRecurring: Added " + channel+recurrance + DELIMITER + name);
        }
    }

    public static void cancelRecord(Object Airing) {
        
        if (Airing==null) {
            AiringAPI.CancelRecord(Airing);
            return;
        }
        
        Map<String, String> nameMap = PropertyElement.getNameMap();
        
        String channel = AiringAPI.GetAiringChannelName(Airing);
        String recurrance = AiringAPI.GetScheduleRecordingRecurrence(Airing);
        
        String name = nameMap.get(channel+recurrance);

        if (name!=null) {
            PropertyElement.removePropertyElement(channel+recurrance, name);
            System.out.println("NTR: cancelRecord: Removed " + channel+recurrance + DELIMITER + name);
        } else {
            System.out.println("NTR: cancelRecord: Failed to find " + channel+recurrance);
        }

        AiringAPI.CancelRecord(Airing);
    }

    public static String stripIllegalCharacters(String name) {
        return name==null || name.isEmpty() ? null : name.replaceAll(DELIMITER, "").replaceAll(";", "").replace(TIMED_RECORDING, "");
    }

    /*
     * Looks in MediaFileMetadata, then Airing ManualRecordProperty, then property map.
     */
    static String getNameForMediaFile(Object MediaFile) {

        // First see if the name has been set in the MediaFile metadata.
        String mediaFileName = MediaFileAPI.GetMediaFileMetadata(MediaFile, PROPERTY_NAME);

        // If the MediaFile had the name property return it.
        if (mediaFileName!=null && !mediaFileName.isEmpty())
            return mediaFileName;

        // The MediaFile did not have a name so see if the Airing has one.
        Object airing = MediaFileAPI.GetMediaFileAiring(MediaFile);

        if (airing==null)
            return null;

        // See if the Airing has a name.
        mediaFileName = AiringAPI.GetManualRecordProperty(MediaFile, PROPERTY_NAME);

        return mediaFileName==null || mediaFileName.isEmpty() ? getNameForRecurring(MediaFile) : mediaFileName;
    }

    /*
     * Looks in ManualRecordProperty, then property map, then MediaFileMetadata.
     */
    static String getNameForAiring(Object Airing) {

        // See if the Airing name has been set.
        String airingName = AiringAPI.GetManualRecordProperty(Airing, PROPERTY_NAME);

        if (airingName!=null && !airingName.isEmpty())
            return airingName;

        airingName = getNameForRecurring(Airing);

        if (airingName!=null && !airingName.isEmpty())
            return airingName;

        Object mediaFile = MediaFileAPI.GetMediaFileAiring(Airing);

        if (mediaFile==null)
            return null;
        else
            return MediaFileAPI.GetMediaFileMetadata(mediaFile, PROPERTY_NAME);
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
        Object channel = AiringAPI.GetChannel(Airing);

        String channelName = ChannelAPI.GetChannelName(channel);

        Map<String, String> nameMap = PropertyElement.getNameMap();

        String airingName = nameMap.get(channelName+recurrence);
        return airingName;
    }

    /*
     * Set MediaFileMetadata for a MediaFile.
     */
    static void setMediaFileName(Object MediaFile, String Name) {

        if (MediaFile==null || Name==null || Name.isEmpty())
            return;

        MediaFileAPI.SetMediaFileMetadata(MediaFile, PROPERTY_NAME, Name);
        return;
    }

    /*
     * Set MediaFileMetadata for an Airing.
     */
    static void setAiringMediaFileName(Object Airing, String Name) {

        Object MediaFile = AiringAPI.GetMediaFileForAiring(Airing);

        if (MediaFile!=null)
            setMediaFileName(MediaFile, Name);

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
                System.out.println("NTR: Changed property " + property + "=" + OldValue + " to " + NewValue);
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

        System.out.println("NTR: Number of properties with no branches " + propsWithNoBranches.length);

        // Get all of the branch roots.
        String[] branches = Configuration.GetSubpropertiesThatAreBranches("");

        if (branches==null || branches.length==0) {
            System.out.println("NTR: No branch roots found.");
            return allProperties;
        }

//for (String p : branches)
    //System.out.println("NTR: Branch roots = " + p);

        // Now get all of the leaves of the branch roots.
        for (String branch : branches)
            allProperties.addAll(getAllLeaves(branch));

        Collections.sort(allProperties);
        
        System.out.println("NTR: Total properties found = " + allProperties.size());
        
//for (String p : allProperties)
    //System.out.println("NTR: Property = " + p);

        return allProperties;
    }

    private static List<String> getAllLeaves(String root) {

//System.out.println("NTR: Processing root " + root);
        List<String> allLeaves = new ArrayList<String>();

        // Get the leaves of this root.
        String[] leaves = Configuration.GetSubpropertiesThatAreLeaves(root);

        // Add the leaves.
        if (leaves!=null && leaves.length>0) {
            for (String leaf : leaves) {
//System.out.println("NTR: Adding final leaf " + root + "/" + leaf);
                allLeaves.add(root + "/" + leaf);
            }
        }

        // Now recurse to get the leaves of the branches.
        String[] branches = Configuration.GetSubpropertiesThatAreBranches(root);

        if (branches!=null && branches.length>0)
            for (String branch : branches) {
//System.out.println("NTR: Recursing to find leafs of " + root + "/" + branch);
                allLeaves.addAll(getAllLeaves(root + "/" + branch));
            }
        
        return allLeaves;
    }

}
