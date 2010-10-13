/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class TuneIntelligentScheduling {

    private List<Object> SampleMediaFiles = null;
    private boolean isRunning = false;
    private boolean recScheduleHasChanged = false;
    private InitISTuner initISTuner = null;

    public static final String THIRTY_MINUTE_FILE = "CD-30.mpg";
    public static final String SIXTY_MINUTE_FILE = "CD-60.mpg";
    
    public TuneIntelligentScheduling() {
        SampleMediaFiles = new ArrayList<Object>();
        isRunning = false;
        recScheduleHasChanged = false;
        initISTuner = null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isInitialized() {
        return (SampleMediaFiles==null ? false : true);
    }

    public boolean isInitializerRunning() {
        return (initISTuner == null ? false : true);
    }

    public boolean isSageRecording() {
        Object[] MediaFiles = Global.GetCurrentlyRecordingMediaFiles();
        return (MediaFiles==null || MediaFiles.length==0 ? false : true);
    }

    public boolean willSageBeRecording(long stopTime) {
        Object[] MediaFiles = Global.GetScheduledRecordingsForTime(Utility.Time(), stopTime);
        return (MediaFiles==null || MediaFiles.length==0 ? false : true);
    }

    public boolean isComskipping() {
        return (ComskipManager.getInstance().getNumberRunning()==0 ? false : true);
    }

    public boolean isAnythingQueued() {
        return (ComskipManager.getInstance().getQueueSize(false)==0 ? false : true);
    }

    public void recScheduleHasChanged() {
        recScheduleHasChanged = true;
    }

    /**
     * Starts the initialiation process.  We need a separate process for this because initialization can take
     * a lot of time due to the fact that we need to copy files.
     */
    public void startInitialize() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "TIS: Starting initialization process.");
        initISTuner = new InitISTuner();
        new Thread(initISTuner).start();
        return;
    }
}

class InitISTuner implements Runnable {
    @Override
    public void run() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "TIS: InitISTuner begin.");

        if (!createThirtyMinuteFile()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "TIS: InitISTuner failed to make 30 minute test file.");
            return;
        }

        if (!createSixtyMinuteFile()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "TIS: InitISTuner failed to make 60 minute test file.");
            return;
        }
    }

    private boolean createThirtyMinuteFile() {
        return true;
    }

    private boolean createSixtyMinuteFile() {
        return true;
    }
}
