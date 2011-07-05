
package tmiranda.navix;

import java.util.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class CacherOLD {

    static List<String> playlistsProcessed = new ArrayList<String>();

    private boolean         isValid         = false;
    private String          urlString       = null;
    private String          fileString      = null;
    private List<String>    allLines        = null;
    private boolean         isLoading       = false;
    private boolean         hasLines        = false;
    private boolean         wroteLines      = false;
    private Date            loadStart       = null;
    private Date            loadEnd         = null;
    private long            totalPlaylists  = 0;
    private long            totalElements   = 0;
    private long            totalLines      = 0;
    private CacherReaderOLD    cacherReader    = null;

    public CacherOLD(String URLString, String FileString) {

        if (URLString==null || URLString.isEmpty() || FileString==null || FileString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Cacher: null parameter " + URLString + ":" + FileString);
            isValid = false;
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Created for " + URLString + ":" + FileString);
        urlString = URLString;
        fileString = FileString;
        isValid = true;
        allLines = new ArrayList<String>();
        isLoading = false;
        loadStart = new Date(0);
        loadEnd = new Date(0);
        totalPlaylists = 0;
        totalElements = 0;
        totalLines = 0;
        hasLines = false;
        wroteLines = false;
    }

    public void startLoad() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: Invalid instance.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher.loadAll: Starting reader thread.");
        cacherReader = new CacherReaderOLD(this);
        cacherReader.start();

        isLoading = true;
        loadStart = new Date();
        loadEnd = new Date(0);

        return;

        //allLines = recursivelyReadLines(urlString);

        //if (allLines.isEmpty()) {
            //Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: No lines read.");
            //return false;
        //}

        //hasLines = true;
        //wroteLines = false;

        //File targetFile = getTargetFile(fileString);

        //if (targetFile==null) {
            //Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: Error creating target file.");
            //return false;
        //}
        
        //wroteLines = writeTextFile(allLines, targetFile);

        //loadEnd = new Date();
        //isLoading = false;

        //return wroteLines;
    }

    public void stopReader() {
        cacherReader.setStop();
    }

    void setAllLines(List<String> lines) {
        allLines = lines;
    }

    public boolean isDirty() {
        return hasLines && !wroteLines;
    }

    public boolean writeLines() {

        // Can't do this if we are currently loading the lines.
        if (isLoading) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.writeLines: Load in progress, aborting.");
            return false;
        }

        if (allLines.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.writeLines: No lines to write, aborting.");
            return false;
        }

        if (wroteLines) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.writeLines: Cache is not dirty, aborting.");
            return false;
        }

        File targetFile = CacherReaderOLD.getTargetFile(fileString);

        if (targetFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.writeLines: Error creating target file.");
            return false;
        }

        wroteLines = CacherReaderOLD.writeTextFile(allLines, targetFile);

        return wroteLines;
    }

    public boolean isLoading() {
        return isLoading;
    }
    
    public Date getLoadStart() {
        return loadStart;
    }

    public Date getLoadEnd() {
        return loadEnd;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public boolean isHasLines() {
        return hasLines;
    }

    public void setHasLines(boolean hasLines) {
        this.hasLines = hasLines;
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
    }

    public boolean isValid() {
        return isValid;
    }

    public long getTotalPlaylists() {
        return totalPlaylists;
    }

    public void setTotalPlaylists(long totalPlaylists) {
        this.totalPlaylists = totalPlaylists;
    }

    public boolean isWroteLines() {
        return wroteLines;
    }

    public void setWroteLines(boolean wroteLines) {
        this.wroteLines = wroteLines;
    }

    public void setLoadEnd(Date loadEnd) {
        this.loadEnd = loadEnd;
    }

    public void setLoadStart(Date loadStart) {
        this.loadStart = loadStart;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public List<String> getAllLines() {
        return allLines;
    }

    public CacherReaderOLD getCacherReader() {
        return cacherReader;
    }

    public String getFileString() {
        return fileString;
    }

    public String getUrlString() {
        return urlString;
    }

    public long getHasBeenLoadingTime() {
        if (!isValid || !isLoading)
            return 0;

        Date now = new Date();
        return now.getTime() - loadStart.getTime();
    }

    public static List<String> getPlaylistsProcessed() {
        return playlistsProcessed;
    }

    public static void setPlaylistsProcessed(List<String> playlistsProcessed) {
        CacherOLD.playlistsProcessed = playlistsProcessed;
    }

    public static void clearPlaylistsProcessed() {
        CacherOLD.playlistsProcessed = new ArrayList<String>();
    }

}
