
package tmiranda.navix;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class RTMP {

    Map<String, String> vars = null;
    Process process = null;
    String targetFile = null;
    long lastLength = 0;

    public RTMP(HashMap<String, String>vars) {
        this.vars = vars==null || vars.isEmpty() ? new HashMap<String, String>() : vars;
    }

    public String createUrl() {
        String rUrl = vars.get("url");

        if (rUrl==null || rUrl.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "createUrl: Missing url parameter.");
            return null;
        }

        if (!isRtmpStream(rUrl)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "createUrl: Is not an rtmp stream " + rUrl);
            return rUrl;
        }

        rUrl = "-r=" + rUrl;

        String v = vars.get("playpath");
        if (v!=null && !v.isEmpty()) {
            //v = v.replace("mp4:", "");
            rUrl = rUrl + " -y " + v;
        }

        /*
        String v = vars.get("playpath");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&-y=" + v;

        live = vars.get("live");
        if (live!=null && !live.isEmpty())
            rUrl = rUrl + "&-v";

        v = vars.get("playpath");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&-y=" + v;

        v = vars.get("swfVfy");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&--swfVfy=" + v;

        v = vars.get("swfplayer");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&-s=" + v;

        v = vars.get("app");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&-a=" + v;

        v = vars.get("pageurl");
        if (v!=null && !v.isEmpty())
            rUrl = rUrl + "&-p=" + v;
         * 
         */

       //rUrl = rUrl + "-V";

        //rUrl=append(rUrl, "&-y=", escape(vars.get("playpath")));
        //rUrl=append(rUrl, "&--swfVfy=", escape(vars.get("swfVfy")));
        //rUrl=append(rUrl, "&-s=", escape(vars.get("swfplayer")));
        //rUrl=append(rUrl, "&-a=", escape(vars.get("app")));
        //rUrl=append(rUrl, "&-p=", escape(vars.get("pageurl")));

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "createUrl: url " + rUrl);
        return rUrl;
    }

    public boolean startCapture(String url, String fileName) {

        targetFile = fileName==null || fileName.isEmpty() ? "rtmpdump.flv" : fileName;

        List<String> commandList = new ArrayList<String>();

        commandList.add("rtmpdump\\rtmpdump.exe");
        commandList.addAll(Arrays.asList(url.split(" ")));
        commandList.add("-V");
        //commandList.add("--live");
        commandList.add("-o");
        commandList.add(targetFile);
        //commandList.add("--debug");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "startCapture: " + commandList);

        String[] command = (String[])commandList.toArray(new String[commandList.size()]);

        // Start the rtmpdump program running.
        try {process = Runtime.getRuntime().exec(command); } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "startCapture: Exception starting rtmpdump " + e.getMessage());
            return false;
        }

        // Start the processes to read the streams.
        StreamGetter errorStream = new StreamGetter(process.getErrorStream(),"ERROR");
        StreamGetter outputStream = new StreamGetter(process.getInputStream(), "OUTPUT");
        errorStream.start();
        outputStream.start();

        //RTMPWatcher watcher = new RTMPWatcher(process, errorStream, outputStream);

        return true;
    }

    public static boolean isRtmpStream(String url) {
        return streamType(url,"rtmp");
    }

    public static boolean streamType(String url,String type) {
        try {
            URI u=new URI(url);
            return u.getScheme().startsWith(type);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "streamType: Exception " + url + " " + type + " " + e.getMessage());
            return false;
        }
    }

    private static String escape(String str) {
        try {
            return URLEncoder.encode(str,"UTF-8");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "escape: Escape error " + str + " " + e.getMessage());
        }

        return str;
    }

    private static String append(String res,String sep,String data) {
        if(empty(res))
            return data;
        if(empty(data))
            return res;
        if(empty(sep))
            return res+data;
        return res+sep+data;
    }

    public static boolean empty(String s) {
        return (s==null)||(s.length()==0);
    }

    public long getDownloadedSize() {
        if (targetFile==null || targetFile.isEmpty())
            return 0;

        File f = new File(targetFile);

        if (!f.exists() || f.isDirectory())
            return 0;

        lastLength = f.length();
        return lastLength;
    }

    public boolean isStillDownloading() {
        if (targetFile==null || targetFile.isEmpty())
            return false;

        File f = new File(targetFile);

        if (!f.exists() || f.isDirectory())
            return false;

        long prevLength = lastLength;
        lastLength = f.length();
        return lastLength > prevLength;
    }

    public void abortCapture() {
        process.destroy();
    }
}
