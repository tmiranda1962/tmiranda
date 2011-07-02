
package tmiranda.navix;

/**
 *
 * @author Tom Miranda
 */
public class RTMPWatcher extends Thread {

    Process process;
    StreamGetter t1;
    StreamGetter t2;

    RTMPWatcher(Process pRunning, StreamGetter t1ToKill, StreamGetter t2ToKill) {
        process = pRunning;
        t1 = t1ToKill;
        t2 = t2ToKill;
    }

    @Override
    public void run() {
        int status = 0;
        try {status=process.waitFor(); } catch (InterruptedException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RTMPWatcher: Process was interrupted " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMPWatcher: Process completed with status " + status);
        t1.interrupt();
        t2.interrupt();
    }

}
