
package tmiranda.navix;

import java.util.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class Cacher {

    private static final String CACHE_FILE      = "NaviX-Cache.txt";
    private static final String CACHE_FILE_OLD  = "NaviX-Cache.old";

    private boolean         isValid     = false;
    private String          urlString   = null;
    private String          fileString  = null;
    private List<String>    allLines    = null;
    private boolean         isLoading   = false;
    private boolean         hasLines    = false;
    private boolean         wroteLines  = false;
    private Date            loadStart   = null;
    private Date            loadEnd     = null;
    private long            totalPlaylists;
    private long            totalElements;
    private long            totalLines;

    public Cacher(String URLString, String FileString) {

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

    public boolean loadAll() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: Invalid instance.");
            return false;
        }

        isLoading = true;
        loadStart = new Date();
        loadEnd = new Date(0);

        allLines = recursivelyReadLines(urlString);

        if (allLines.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: No lines read.");
            return false;
        }

        hasLines = true;
        wroteLines = false;

        File targetFile = getTargetFile(fileString);

        if (targetFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.loadAll: Error creating target file.");
            return false;
        }
        
        wroteLines = writeTextFile(allLines, targetFile);

        loadEnd = new Date();
        isLoading = false;

        return wroteLines;
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

        File targetFile = getTargetFile(fileString);

        if (targetFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher.writeLines: Error creating target file.");
            return false;
        }

        wroteLines = writeTextFile(allLines, targetFile);

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

    public long getHasBeenLoadingTime() {
        if (!isValid || !isLoading || loadEnd.before(loadStart))
            return 0;

        Date now = new Date();
        return now.getTime() - loadStart.getTime();
    }

    private List<String> recursivelyReadLines(String RootURL) {

        List<String> lines = new ArrayList<String>();

        if (RootURL==null || RootURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "recursivelyReadLines: null RootURL.");
            return lines;
        }

        totalPlaylists++;

        Playlist playlist = new Playlist(RootURL);

        lines.addAll(playlist.getAllLines());

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            // Recurse if this is a playlist.
            if (entry.isPlaylist() || entry.isPlx()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "recursivelyReadLines: Recursing. Totals " + totalPlaylists + ":" + totalElements);
                lines.addAll(recursivelyReadLines(entry.getUrl()));
            }
        }

        return lines;
    }

    private boolean writeTextFile(List<String> lines, File targetFile) {

        FileWriter outFile = null;

        try {
            outFile = new FileWriter(targetFile);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Cacher.writeTextFile: Exception creating FileWriter " + e.getMessage());
            return false;
        }

        PrintWriter out = new PrintWriter(outFile);

        for (String line : lines) {
            out.println(line);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Cacher.writeTextFile: Wrote " + line);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher.writeTextFile: Wrote lines " + allLines.size());
        out.close();
        return false;
    }

    private File getTargetFile(String fileName) {

        // First backup the existing file if it exists.
        File target = new File(fileName);

        if (target.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher.getTargetFile: Backing up existing file");

            File backupTarget = new File(fileName + ".old");

            // If the backup file exists, delete it.
            if (backupTarget.exists()) {
                if (!backupTarget.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "Cacher.getTargetFile: Failed to delete old backup file " + fileName + ".old");
                    return null;
                }
            }

            // Rename the file.
            if (!target.renameTo(backupTarget)) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Cacher.getTargetFile: Failed to rename.");
                return null;
            }

        }

        return new File(fileName);
    }
}
