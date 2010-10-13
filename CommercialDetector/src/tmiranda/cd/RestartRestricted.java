/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class RestartRestricted extends TimerTask {

    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RestartRestricted: Checking.");

        if (!ComskipManager.getInstance().inRestrictedTime() && ComskipManager.getInstance().getNumberRunning()==0) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RestartRestricted: Restarting.");
            ComskipManager.getInstance().startMaxJobs();
        }
    }
}


