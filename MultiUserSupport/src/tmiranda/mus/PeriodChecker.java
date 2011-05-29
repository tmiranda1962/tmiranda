/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;

/**
 *
 * @author Default
 */
public class PeriodChecker extends TimerTask {

    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PeriodChecker: Start.");

        for (String u : API.getAllDefinedUsers(false)) {

            User user = new User(u);

            if (user.hasWatchLimit() && user.isPeriodOver()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "PeriodChecker: User period is over " + u);

                // Reset the period to now.
                user.setStartPeriod();

                // Clear the watched minutes counter.
                user.setWatchTime(0L);

                Log.getInstance().write(Log.LOGLEVEL_TRACE, "PeriodChecker: Period now ends at " + user.printEndPeriod());
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "PeriodChecker: End.");
    }

}
