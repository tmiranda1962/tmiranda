
package tmiranda.ntr;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class DeadManSwitch extends TimerTask {

    private static String property = "ntr/DeadManSwitch";

    DeadManSwitch() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "NameTimedRecording: DeadManSwitch created for " + property);
    }

    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_MAX, "NameTimedRecording: DeadManSwitch created for " + property);
        Configuration.SetServerProperty(property, "true");
    }

    public static boolean isAlive() {
        return Configuration.GetServerProperty(property, "false").equalsIgnoreCase("true");
    }

    public static void resetSwitch() {
        Configuration.SetServerProperty(property, "false");
    }

    public static long getResetTime() {
        return Plugin.deadManResetTime;
    }

}
