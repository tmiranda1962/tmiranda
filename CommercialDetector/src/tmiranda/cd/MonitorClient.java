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
public class MonitorClient extends TimerTask {

    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MonitorClient: Looking for work.");

        // Read the first item off the queue.
        String IDString = CSC.getInstance().getFirstStatus(CSC.STATUS_QUEUE);
        
        // null or empty means there are no more items in the queue.
        while (IDString!=null && !IDString.isEmpty()) {

            // Convert the String to an int.
            int ID = 0;
            try {ID = Integer.parseInt(IDString);}
            catch (NumberFormatException e) {
                ID=0;
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MonitorClient: Malformed ID " + IDString);
            }

            // Get the corresponding MediaFile.
            Object MediaFile = MediaFileAPI.GetMediaFileForID(ID);
            if (MediaFile==null) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "MonitorClient: null MediaFile.");
            } else {
                CommercialDetectorMediaFile CDMediaFile = new CommercialDetectorMediaFile(MediaFile);
                CDMediaFile.queue();
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "MonitorClient: Queued " + MediaFileAPI.GetMediaTitle(MediaFile));
            }

            // Get the next item off the queue.
            IDString = CSC.getInstance().getFirstStatus(CSC.STATUS_QUEUE);
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MonitorClient: Finished.");
    }
}
