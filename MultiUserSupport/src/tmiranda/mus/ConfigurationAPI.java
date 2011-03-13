package tmiranda.mus;

import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class ConfigurationAPI {
    /**
     * Replaces the core API.
     * @return
     */
    public static boolean isIntelligentRecordingDisabled() {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return Configuration.IsIntelligentRecordingDisabled();
        }

        User user = new User(User);
        return user.isIntelligentRecordingDisabled();
    }

    /**
     * Convenience method to avoid the confusion of double negatives.
     * @return
     */
    public static boolean isIntelligentRecordingEnabled() {
        return !isIntelligentRecordingDisabled();
    }

    /**
     * Invoke IN PLACE OF core API.
     *
     * If enabling IR, it will be enabled in the Core as well as for the user requesting it
     * to be enabled.
     *
     * If disabling IR, it will be disabled in the Core if no users have IR enabled.
     *
     * If the null user or Admin requests the action the behavior is different.  If IR is
     * being disabled we disable it for all users.  If it's being enabled we do not do anything
     * for individual users.  This implies that if Admin turns IR off all of the users must
     * individually turn it back on.
     *
     * @param disabling
     */
    public static void setIntelligentRecordingDisabled(boolean disabling) {

        String user = UserAPI.getLoggedinUser();

        if (user==null || user.equalsIgnoreCase(Plugin.SUPER_USER)) {

            // Set the Core to the appropriate state.
            Configuration.SetIntelligentRecordingDisabled(disabling);

            // If we just turned IR off, turn it off for all users.
            if (disabling)
                User.disableIntelligentRecordingForAllUsers();

            return;
        }

        // Change the state for the user.
        User u = new User(user);
        u.setIntelligentRecordingDisabled(disabling);

        // If we just enabled IR for the user, make sure it's enabled in the Core.
        // If we just disabled IR for the user and no users have IR enabled, disable it in the Core.
        if (!disabling) {
            Configuration.SetIntelligentRecordingDisabled(disabling);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setIntelligentRecordingDisabled: Enabling IR in the core.");
        } else if (!User.isIntelligentRecordingEnabledForAnyUsers()) {
            Configuration.SetIntelligentRecordingDisabled(disabling);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "setIntelligentRecordingDisabled: No users are using IR, disabling it in the core.");
        }

        return;
    }
}
