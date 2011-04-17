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
        if (MediaFile==null)
            return title;

        // If it's not a timed recording just return the original title.
        if (title==null || !title.equalsIgnoreCase(TIMED_RECORDING))
            return title;

        // We know it's a timed recording so try to find its name.
        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {

            // If it's a MediaFile we will try to find the name in the MediaFile properties.
            // If the MediaFile does not have a name we will try to find one from the
            // (Airing) ManualRecordProperties.

            // First see if the name has been set in the MediaFile metadata.
            String mediaFileName = MediaFileAPI.GetMediaFileMetadata(MediaFile, PROPERTY_NAME);

            // If the MediaFile had the name property return it.
            if (mediaFileName!=null && !mediaFileName.isEmpty())
                return mediaFileName;

            // The MediaFile did not have a name so see if the Airing has one.
            Object airing = MediaFileAPI.GetMediaFileAiring(MediaFile);

            // If there is no Airing return the original title.
            if (airing==null)
                return title;

            // See if the Airing has a name.
            String airingName = AiringAPI.GetManualRecordProperty(MediaFile, PROPERTY_NAME);

            // If it has a name return it, otherwise return the original name.
            if (airingName!=null && !airingName.isEmpty())
                return airingName;
            else
                return title;

        } else if (AiringAPI.IsAiringObject(MediaFile)) {

            // If it's an Airing we will try to find the name in the Airing or the
            // corresponding MediaFile.  If the Airing has a MediaFile we will also set
            // the name in the MediaFile because the Airing name will be lost as soon as the
            // Airing is no longer considered to be a manual record.

            // See if the Airing name has been set.
            String airingName = AiringAPI.GetManualRecordProperty(MediaFile, PROPERTY_NAME);

            // If the Airing does not have a name check if the MediaFile has one.
            if (airingName==null || airingName.isEmpty()) {

                // The Airing name has not been set, see if the MediaFile has a name.
                Object mediaFile = AiringAPI.GetMediaFileForAiring(MediaFile);

                // If there is no MediaFile return the original Airing name.
                if (mediaFile==null)
                    return title;

                // Get the MediaFileName.
                String mediaFileName = MediaFileAPI.GetMediaFileMetadata(MediaFile, PROPERTY_NAME);

                // If there is no name return the original Airing title, otherwise
                // return the MediaFile name.
                if (mediaFileName==null || mediaFileName.isEmpty())
                    return title;
                else
                    return mediaFileName;

            } else {

                // The Airing has a name.  Set the name in the MediaFile if there is one.
                Object mediaFile = MediaFileAPI.GetMediaFileAiring(MediaFile);

                if (mediaFile != null) {

                    // Get the existing name.
                    String mediaFileName = MediaFileAPI.GetMediaFileMetadata(mediaFile, PROPERTY_NAME);

                    // If it doesn't have one or it's different, set the name to the same
                    // name as the Airing.
                    if (mediaFileName==null || mediaFileName.isEmpty() || !mediaFileName.equals(airingName))
                        MediaFileAPI.SetMediaFileMetadata(mediaFile, PROPERTY_NAME, airingName);
                }

                return airingName;

            }

        } else {

            // If it's not a MediaFile or Airing return the original title.
            return title;
        }
    }

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
