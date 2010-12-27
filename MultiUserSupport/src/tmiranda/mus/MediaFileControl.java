/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class MediaFileControl {

    public static final String  SUPER_USER = "Admin";
    public static final String  USER_RECORD_KEY = "MultiUser";
    public static final String  KEY_PREFIX = "MUSXXX";

    static final String ALLOWED_USERS = "AllowedUsers";

    private List<String>    allowedUsers = null;
    Object                  sageMediaFile = null;
    Integer                 airingID = 0;
    String                  airingKey = null;
    private boolean         isMediaFile = false;
    private boolean         isValid = true;
    Object                  userRecord = null;

    public MediaFileControl(Object MediaFile) {

        allowedUsers = new ArrayList<String>();

        if (MediaFile==null) {
            sageMediaFile = "null";
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "MediaFileControl: null MediaFile.");
            return;
        }

        sageMediaFile = MediaFile;

        if (MediaFileAPI.IsMediaFileObject(MediaFile)) {

            // If this is a MediaFile use MediaFileMetaData to store list of valid users.
            // MediaFiles are physical files.

            isMediaFile = true;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileContol: Found a MediaFile.");

            String userString = MediaFileAPI.GetMediaFileMetadata(MediaFile, ALLOWED_USERS);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileControl: AllowedUsers " + userString);

            if (userString == null || userString.isEmpty()) {
                return;
            }

            String[] userArray = userString.split(",");

            if (userArray == null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "MediaFileControl: null userArray.");
                return;
            }

            allowedUsers = Arrays.asList(userArray);

        } else if (AiringAPI.IsAiringObject(MediaFile) || ShowAPI.IsShowObject(MediaFile)) {

            // If this is an Airing, use the UserRecordAPI to store a list of valid users.
            // Show can have multiple Airings.

            isMediaFile = false;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileContol: Found an Airing or Show.");

            airingID = AiringAPI.GetAiringID(MediaFile);
            airingKey = airingID.toString();

            if (airingKey==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MediaFileContol: null airingKey for airingID " + airingID);
                isValid = false;
                return;
            }

            airingKey = KEY_PREFIX + airingKey;

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileContol: airingKey is " + airingKey);

            userRecord = UserRecordAPI.GetUserRecord(USER_RECORD_KEY, airingKey);

            // If there is no record, create it.
            if (userRecord==null) {

                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileContol: Creating new userRecord.");

                userRecord = UserRecordAPI.AddUserRecord(USER_RECORD_KEY, airingKey);

                if (userRecord==null) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "MediaFileContol: Error creating userRecord for airingKey " + airingKey);
                    isValid = false;
                }

                return;
            }

            String userString = UserRecordAPI.GetUserRecordData(userRecord, ALLOWED_USERS);

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MediaFileContol: AllowedUsers for this record " + userString);

            if (userString == null || userString.isEmpty()) {
                return;
            }

            String[] userArray = userString.split(",");

            if (userArray == null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "MediaFileControl: null userArray.");
            }

            allowedUsers = Arrays.asList(userArray);

        } else {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MediaFileContol: Found unknown Object.");
        }
    }

    boolean isValid() {
        return isValid;
    }

    boolean isMediaFile() {
        return isMediaFile && isValid;
    }

    boolean isAiring() {
        return !isMediaFile && isValid;
    }

    /**
     * Check if this user has permission to access this MediaFile either explicitly (because they have
     * permission) or implicitly (because the MediaFile is unassigned and the config options are set appropriately.)
     *
     * @param UserID
     * @return
     */
    boolean isUserAllowed(String UserID) {

        if (!isValid || UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "isUserAllowed: null UserID");
            return false;
        }

        if (UserID.equalsIgnoreCase(SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: Allowing Super User.");
            return true;
        }

        // If the MediaFile has not been assigned to any users decide what to so.  Options are dependent
        // on config settings, could be allow all, allow none, or allow certain users.
        if (!hasAnyUsers()) {

            Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: MediaFile is unassigned.");

            String Allow = Configuration.GetServerProperty(Plugin.PROPERTY_UNASSIGNEDMF, Plugin.UNASSIGNEDMF_ALLOW_ALL);

            if (Allow.equalsIgnoreCase(Plugin.UNASSIGNEDMF_ALLOW_ALL)) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: Allowing All.");
                return true;
            }

            if (Allow.equalsIgnoreCase(Plugin.UNASSIGNEDMF_ALLOW_NONE)) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: Disallowing.");
                return false;
            }

            String Users = Configuration.GetServerProperty(Plugin.PROPERTY_UNASSIGNEDMF_USERS_TO_ALLOW, null);

            if (Users == null) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: null Users.");
                return false;
            }

            String[] UserArray = Users.split(",");

            if (UserArray == null) {
                Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: null UserArray.");
                return false;
            }

            for (String ThisUser : UserArray) {
                if (ThisUser.equalsIgnoreCase(UserID)) {
                    Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: Allowing ThisUser " + ThisUser);
                    return true;
                }

                Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: User not allowed.");
                return false;
            }
        }

        boolean allowed =  allowedUsers.contains(UserID);
        Log.getInstance().write(Log.LOGLEVEL_ALL, "isUserAllowed: Allowed " + allowed);
        return allowed;
    }

    private boolean isUserExplicitlyAllowed(String UserID) {

        if (!isValid || UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "isUserExplicitlyAllowed: null UserID");
            return false;
        }

        return allowedUsers.contains(UserID);
    }

    boolean useSageDataBase(String UserID) {

        if (!isValid || UserID == null || UserID.equalsIgnoreCase(SUPER_USER) || !isUserExplicitlyAllowed(UserID)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if this MediaFile is associated with any users.
     *
     * @return
     */
    boolean hasAnyUsers() {
        return (allowedUsers == null || allowedUsers.isEmpty() || !isValid ? false : true);
    }

    void addUser(String UserID) {
        if (!isValid)
            return;

        if (isMediaFile) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(sageMediaFile, ALLOWED_USERS), ",");
            DS.addUniqueElement(UserID);
            MediaFileAPI.SetMediaFileMetadata(sageMediaFile, ALLOWED_USERS, DS.toString());
        } else {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(userRecord, ALLOWED_USERS), ",");
            DS.addUniqueElement(UserID);
            UserRecordAPI.SetUserRecordData(userRecord, ALLOWED_USERS, DS.toString());
        }

    }

    void removeUser(String UserID) {
        if (!isValid)
            return;

        if (isMediaFile) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(sageMediaFile, ALLOWED_USERS), ",");
            DS.removeElement(UserID);
            MediaFileAPI.SetMediaFileMetadata(sageMediaFile, ALLOWED_USERS, DS.toString());
        } else {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(userRecord, ALLOWED_USERS), ",");
            DS.removeElement(UserID);
            UserRecordAPI.SetUserRecordData(userRecord, ALLOWED_USERS, DS.toString());
        }
    }

    void removeAllUsers() {
        if (!isValid)
            return;

        if (isMediaFile)
            MediaFileAPI.SetMediaFileMetadata(sageMediaFile, ALLOWED_USERS, null);
        else
            UserRecordAPI.SetUserRecordData(userRecord, ALLOWED_USERS, null);
    }

    List<String> getUserList() {
        if (!isValid)
            return null;

        if (isMediaFile)
            return DelimitedString.delimitedStringToList(MediaFileAPI.GetMediaFileMetadata(sageMediaFile, ALLOWED_USERS), ",");
        else
            return DelimitedString.delimitedStringToList(UserRecordAPI.GetUserRecordData(userRecord, ALLOWED_USERS), ",");
    }
}
