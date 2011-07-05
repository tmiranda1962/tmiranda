
package tmiranda.navix;

import java.util.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class CacherReaderOLD extends Thread {

    private CacherOLD cacher   = null;
    private boolean stop    = false;

    public CacherReaderOLD(CacherOLD cacher) {
        this.cacher = cacher;
        stop = false;
    }

    @Override
    public void run() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CacherReader: Starting.");

        //cacher.setIsLoading(true);
        //cacher.setLoadStart(new Date());
        //cacher.setLoadEnd(new Date(0));

        cacher.setAllLines(recursivelyReadLines(cacher.getUrlString()));

        if (cacher.getAllLines().isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "CacherReader: No lines read.");
            return;
        }

        cacher.setHasLines(true);
        cacher.setWroteLines(false);

        File targetFile = getTargetFile(cacher.getFileString());

        if (targetFile==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "CacherReader: Error creating target file.");
        } else {
            cacher.setWroteLines(writeTextFile(cacher.getAllLines(), targetFile));
        }

        cacher.setLoadEnd(new Date());
        cacher.setIsLoading(false);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "CacherReader: Completed.");
        return;
    }

    public void setStop() {
        stop = true;
    }

    private List<String> recursivelyReadLines(String RootURL) {

        List<String> lines = new ArrayList<String>();

        if (RootURL==null || RootURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "recursivelyReadLines: null RootURL.");
            return lines;
        }

        if (cacher.playlistsProcessed.contains(RootURL)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "recursivelyReadLines: Playlist has already been processed " + RootURL);
            return lines;
        }

        cacher.playlistsProcessed.add(RootURL);
        cacher.setTotalPlaylists(cacher.getTotalPlaylists()+1);

        Playlist playlist = new Playlist(RootURL);

        lines.add(Playlist.COMMENT_CHARACTER + " RootURL=" + RootURL);
        lines.addAll(playlist.getAllLines());

        for (PlaylistEntry entry : playlist.getElements()) {

            cacher.setTotalElements(cacher.getTotalElements()+1);

            // Recurse if this is a playlist.
            if (entry.isPlaylist() || entry.isPlx()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "recursivelyReadLines: Recursing. Totals " + cacher.getTotalPlaylists() + ":" + cacher.getTotalElements());
                lines.addAll(recursivelyReadLines(entry.getUrl()));
            }
        }

        return lines;
    }

    static File getTargetFile(String fileName) {

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

    static boolean writeTextFile(List<String> lines, File targetFile) {

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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher.writeTextFile: Wrote lines.");
        out.close();
        return false;
    }
}
