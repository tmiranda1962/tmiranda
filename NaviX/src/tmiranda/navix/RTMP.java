
package tmiranda.navix;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class RTMP implements Serializable {

    static List<RTMP>   activeList = new ArrayList<RTMP>();

    UUID                id          = null;
    Map<String, String> vars        = null;
    Process             process     = null;
    File                tempFile    = null;
    long                lastLength  = 0;

    public RTMP(HashMap<String, String>vars) {
        this.vars = vars==null || vars.isEmpty() ? new HashMap<String, String>() : vars;
        id = UUID.randomUUID();
    }

    /**
     * Get the ID for this RTMP.  Each RTMP has a unique ID.
     * @return
     */
    public UUID getId() {
        return id;
    }

    /**
     * Create a URL string that can be used by startCapture() to capture this rtmp stream.
     * @return
     */
    public String createUrl() {
        String rUrl = vars.get("url");

        if (rUrl==null || rUrl.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RTMP.createUrl: Missing url parameter.");
            return null;
        }

        if (!isRtmpStream(rUrl)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMP.createUrl: Is not an rtmp stream " + rUrl);
            return rUrl;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMP.createUrl: All keys " + vars.keySet());

        rUrl = "-r " + rUrl;

        String v = vars.get("playpath");
        if (v!=null && !v.isEmpty()) {
            //v = v.replace("mp4:", "");
            rUrl = rUrl + " -y " + v;
        }

        v = vars.get("live");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " -v ";

        v = vars.get("swfvfy");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " --swfVfy " + v;

        v = vars.get("swfplayer");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " -s " + v;

        v = vars.get("app");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " -a " + v;

        v = vars.get("pageurl");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " -p " + v;

        v = vars.get("swfurl");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + " -s " + v;

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMP.createUrl: url " + rUrl);
        return rUrl;
    }

    /**
     * Starts the rtmp capture for the specified url.  In most cases url should be the
     * return value from createUrl().
     *
     * Known limitations:
     *   Doesn't work on some server because they expect a different version of Adobe?
     *   See this thread: http://stream-recorder.com/forum/rtmpdump-does-not-start-error-handshake-type-t8603.html
     *     WARNING: HandShake: Server not genuine Adobe!
     *     ERROR: RTMP_Connect1, handshake failed.
     *     DEBUG: Closing connection.
     *
     * @param url
     * @return The File that will contain the downloaded video.
     */
    public File startCapture(String url) {

        try {
            tempFile = File.createTempFile("navix", ".flv");
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RTMP.startCapture: Error creating target file.");
            return null;
        }

        tempFile.deleteOnExit();

        String targetFile = tempFile.getAbsolutePath();

        //targetFile = fileName==null || fileName.isEmpty() ? "rtmpdump.flv" : fileName;

        List<String> commandList = new ArrayList<String>();

        commandList.add("rtmpdump\\rtmpdump.exe");
        commandList.addAll(Arrays.asList(url.split(" ")));
        commandList.add("-V");
        //commandList.add("--live");
        commandList.add("-o");
        commandList.add(targetFile);
        //commandList.add("--debug");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMP.startCapture: " + commandList);

        String[] command = (String[])commandList.toArray(new String[commandList.size()]);

        // Start the rtmpdump program running.
        try {process = Runtime.getRuntime().exec(command); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RTMP.startCapture: Exception starting rtmpdump " + e.getMessage());
            tempFile.delete();
            return null;
        }

        // Start the processes to read the streams.
        StreamGetter errorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter outputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        errorStream.start();
        outputStream.start();

        activeList.add(this);
        return tempFile;
    }

    /**
     * Checks to see if the specified URL is an rtmp stream.
     * @param url
     * @return
     */
    public static boolean isRtmpStream(String url) {
        return streamType(url,"rtmp");
    }

    private static boolean streamType(String url, String type) {

        if (url==null || url.isEmpty() || type==null || type.isEmpty())
            return false;

        String newUrl = url.contains(" ") ? url.split(" ")[0] : url;

        try {
            URI u=new URI(newUrl);
            return u.getScheme().startsWith(type);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "RTMP.streamType: Exception " + newUrl + " " + type + " " + e.getMessage());
            return false;
        }
    }

    /**
     * Return the size of the downloaded file corresponding to this capture.
     * @return
     */
    public long getDownloadedSize() {

        if (!tempFile.exists())
            return 0;

        lastLength = tempFile.length();
        return lastLength;
    }

    /**
     * Checks to see if the capture download is still progressing.  This will return true
     * if the downloaded file has grown in size since the last time this method or the
     * getDownloadedSize() method has been invoked.
     * @return
     */
    public boolean isStillDownloading() {

        if (!tempFile.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RTMP.isStillDownloading: tempfile does not exist.");
            return false;
        }

        long prevLength = lastLength;
        lastLength = tempFile.length();
        return lastLength > prevLength;
    }

    /**
     * Check to see if the current capture has started.  "Started" means that the size of the
     * downloaded file > 0.
     * @return
     */
    public boolean downloadStarted() {
        return getDownloadedSize() > 0;
    }

    /**
     * Cancels the current capture and removes it from the Active List.
     */
    public void abortCapture() {
        process.destroy();
        activeList.remove(this);
    }

    /**
     * Return the list of active RTMPs.  In most cases "active" means the rtmp stream
     * is currently being downloaded.
     * @return
     */
    public static List<RTMP> getActiveList() {
        return activeList;
    }

    /**
     * Returns true if the specified ID is in the Active List, false otherwise.
     * @param activeId
     * @return
     */
    public static boolean isIdActive(UUID activeId) {
        for (RTMP rtmp : activeList) {
            if (rtmp.getId().equals(activeId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the RTMP for the specified activeID.
     * @param activeId
     * @return
     */
    public static RTMP getRtmpForId(UUID activeId) {
        for (RTMP rtmp : activeList) {
            if (rtmp.getId().equals(activeId)) {
                return rtmp;
            }
        }

        return null;
    }

    /**
     * Used to manually add the specified RTMP to the Active List.  Note that RTMPs will
     * automatically be added to the Active List by startCapture() if the method was successful.
     * @param rtmp
     * @return
     */
    public static boolean addActiveList(RTMP rtmp) {
        return activeList.add(rtmp);
    }

    /**
     * Used to manually remove the RTMP from the Active List.  Note that abortCapture() will
     * automatically remove the RTMP from the Active List.
     * @param rtmp
     * @return
     */
    public static boolean removeActiveList(RTMP rtmp) {
        return activeList.remove(rtmp);
    }

    /**
     * Stops the capture of the specified RTMP and removes it from the Active List.
     * @param rtmp
     * @return
     */
    public static void abortCapture(RTMP rtmp) {
        rtmp.abortCapture();
        return;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RTMP other = (RTMP) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

}
