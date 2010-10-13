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
public class JobProfiler extends TimerTask {

    private ComskipJob Job = null;
    
    public JobProfiler(ComskipJob CallingJob) {
        Job = CallingJob;
    }

    /*
     * When called this process counts how many comskip jobs are running, how many recordings are in
     * progress, and reports back to the calling ComskipJob.
     */
    public void run() {
        int NumberRunning = ComskipManager.getInstance().getNumberRunning();

        int NumberRecording = 0;

        Object[] MediaFilesRecording = Global.GetCurrentlyRecordingMediaFiles();

        if (MediaFilesRecording!=null || MediaFilesRecording.length>0) {
            NumberRecording = MediaFilesRecording.length;
        }

        JobSnapshot Snapshot = new JobSnapshot(NumberRunning, NumberRecording);

        Job.addSnapshot(Snapshot);
    }

}
