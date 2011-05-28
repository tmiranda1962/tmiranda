package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class UserAPI {

    public static String getDefaultNullUser(String User) {

        // If it's not null just return the current user.
        if (User != null)
            return User;

        return Configuration.GetProperty(Plugin.PROPERTY_DEFAULT_NULL_USER, null);
    }

    public static void setPassword(String User, String Password) {

        if (User==null || User.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setPassword: null user.");
            return;
        }

        if (Password==null || Password.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setPassword: null password.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setPassword: Setting Password to " + Password + ":" + User);
        User u = new User(User);
        u.setPassword(Password);
        return;
    }

    public static void setShowImports(String User, Boolean setting) {

        if (User==null || User.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setShowImports: null user.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "setShowImports: Setting Show Imports to " + setting + ":" + User);
        User u = new User(User);
        u.setShowImports(setting);
        return;
    }

    public static boolean isShowImports(String U) {

        if (U==null || U.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return true;
        }

        User user = new User(U);
        return user.isShowImports();
    }

    /**
     * Logs on the specified secondary user.
     * @param UserID
     */
    public static void loginSecondaryUser(String UserID) {

        if (UserID == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "loginSecondaryUser: null UserID.");
            return;
        }

        String userString = SageUtil.getUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);
        DelimitedString DS = new DelimitedString(userString, Plugin.LIST_SEPARATOR);
        DS.addUniqueElement(UserID);

        SageUtil.setUIProperty(Plugin.PROPERTY_SECONDARY_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "loginSecondaryUser: Logged on user " + UserID + ":" + DS.toString());
    }

    /**
     * Logs out a particular user.
     */
    public static void logoutSecondaryUser(String UserID) {

        String userString = SageUtil.getUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);

        if (userString==null || userString.isEmpty() || UserID==null || UserID.isEmpty())
            return;

        DelimitedString DS = new DelimitedString(userString, Plugin.LIST_SEPARATOR);
        DS.removeElement(UserID);
        SageUtil.setUIProperty(Plugin.PROPERTY_SECONDARY_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logoutSecondaryUser: Logged out user " + UserID + ":" + DS.toString());
    }

    /**
     * Logs out all secondary users.
     */
    public static void logoutAllSecondaryUsers() {
        SageUtil.setUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "logoutAllSecondaryUsers: Logged out all users.");
    }

    /**
     * Get a List containing all currently logged on secondary users. Will never return a null but it
     * will return an empty List if no users are logged on.
     * @return A List of users that are logged on.
     */
    public static List<String> getLoggedinSecondaryUsers() {
        String userString = SageUtil.getUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "getLoggedinSecondaryUsers: Users " + userString);
        return (userString==null || userString.isEmpty()) ? new ArrayList<String>() : DelimitedString.delimitedStringToList(userString, Plugin.LIST_SEPARATOR);
    }

    public static String getRawLoggedinSecondaryUsers() {
        return SageUtil.getUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);
    }

    public static boolean areSecondaryUsersLoggedIn() {
        return !getLoggedinSecondaryUsers().isEmpty();
    }

    public static boolean isSecondaryUserLoggedOn(String User) {
        return getLoggedinSecondaryUsers().contains(User);
    }

    /**
     * Checks to see if Admin is one of the logged in secondary users.
     * @return true if Admin is one of the logged in secondary users, false otherwise.
     */
    public static boolean isAdminLoggedInAsSecondary() {
        return getLoggedinSecondaryUsers().contains(Plugin.SUPER_USER);
    }

    /**
     * Returns a List of users that should be logged on after Sage is rebooted.  It may be null
     * or empty indicating that no user should be logged on.
     * @return The users that should be logged in after the UI is reloaded.
     */
    public static List<String> getSecondaryUsersAfterReboot() {

        List<String> userList = new ArrayList<String>();

        if (SageUtil.GetLocalBoolProperty(Plugin.PROPERTY_LOGIN_SECONDARY_USERS, "true")) {
            String userString = SageUtil.getUIProperty(Plugin.PROPERTY_SECONDARY_USERS, null);

            if (userString==null || userString.isEmpty())
                return userList;

            // Make sure the user still exists in the database.
            for (String thisUser : userList) {
                User user = new User(thisUser);
                if (user.exists())
                    userList.add(thisUser);
                else
                    logoutSecondaryUser(thisUser);
            }
        }
            
        return userList;
    }

    /*
     * Watch time.
     */

    public static String getWatchPeriod() {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return null;
        }

        return getWatchPeriod(User);
    }

    public static String getWatchPeriod(String User) {
        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return null;
        }

        User user = new User(User);
        String period = user.getWatchPeriod();
        return period==null || period.isEmpty() ? "DAILY" : period;
    }

    public static void setWatchPeriod(String Period) {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        if (Period==null || Period.isEmpty()) {
            return;
        }

        setWatchPeriod(User, Period);
    }

    public static void setWatchPeriod(String User, String Period) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        if (Period==null || Period.isEmpty()) {
            return;
        }

        User user = new User(User);
        user.setWatchPeriod(Period);
    }


    public static long getWatchTime() {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return 0;
        }

        return getWatchTime(User);
    }

    public static long getWatchTime(String User) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return 0;
        }

        User user = new User(User);
        return user.getWatchTime();
    }

    public static void setWatchTime(Long watchTime) {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        setWatchTime(User, watchTime);
        return;
    }

    public static void setWatchTime(String User, Long watchTime) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        User user = new User(User);
        user.setWatchTime(watchTime);
        return;
    }

    public static void setWatchTime(String User, Integer watchTime) {
        setWatchTime(User, watchTime.longValue());
    }

    public static void addWatchTime(Long watchTime) {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        addWatchTime(User, watchTime);
        return;
    }

    public static void addWatchTime(String User, Long watchTime) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        User u = new User(User);
        u.addWatchTime(watchTime);
        return;
    }

    public static void addWatchTime(String User, Integer watchTime) {
        addWatchTime(User, watchTime.longValue());
    }
    

    public static long getWatchLimit() {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return 0;
        }

        return getWatchLimit(User);
    }

    public static long getWatchLimit(String User) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return 0;
        }

        User u = new User(User);
        return u.getWatchLimit();
    }

    public static void setWatchLimit(Long watchLimit) {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        setWatchLimit(User, watchLimit);
        return;
    }

    public static void setWatchLimit(String User, Long watchLimit) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return;
        }

        User user = new User(User);
        user.setWatchLimit(watchLimit);
        return;
    }

    public static void setWatchLimit(String User, Integer watchLimit) {
        setWatchLimit(User, watchLimit.longValue());
    }

    public static boolean isOverWatchLimit() {
        String User = API.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return false;
        }

        return isOverWatchLimit(User);
    }

    public static boolean isOverWatchLimit(String User) {

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return false;
        }

        User u = new User(User);
        return u.isOverWatchLimit();
    }

}
