
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class Playlist {

    private PlaylistHeader      playlistHeader;
    private List<PlaylistEntry> playlistEntries;

    /**
     * Constructor.
     *
     * @param HomeURL The root URL of the playlist.
     */
    public Playlist(String HomeURL) {

        // Create the Header and the empty List.
        playlistHeader = new PlaylistHeader();
        playlistEntries = new ArrayList<PlaylistEntry>();

        // Make sure the HomeURL is valid.
        if (HomeURL==null || HomeURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist: Empty HomeURL.");
            return;
        }

        // Create the file reader.
        BufferedReader br = read(HomeURL);

        // Read the entire playlist into memory.

        List<String> allLines = new ArrayList<String>();
        String line = null;

        try {
            while ((line=br.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_MAX, "Playlist: line = " + line);
                allLines.add(line);
            }
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist: IO Exception " + e.getMessage());
        }

        // Set the header information.
        scanForHeader(allLines);

        // Set the entries.
        scanForEntries(allLines);

        // Free up the memory.
        allLines.clear();
        allLines = null;
    }

    private void scanForHeader(List<String> allLines) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "scanForHeader: Looking for playlist header.");

        for (int i=0; i<allLines.size(); i++) {

            String line = allLines.get(i);

            String lcLine = line.toLowerCase();

            if (lcLine.startsWith("version")) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found header start.");

                // Get the version number.
                List<String> parts = parseParts(line);

                if (parts!=null && parts.size()==2) {
                    playlistHeader.setVersion(parts.get(1));
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found version " + parts.get(1));
                }

                // Look at the next 4 lines.
                for (int j=++i; j<allLines.size() && j<i+5; j++) {
                    line = allLines.get(j);
                    lcLine = line.toLowerCase();

                    parts = parseParts(line);

                    if (parts!=null && parts.size()==2) {

                        String value = parts.get(1);

                        if (lcLine.startsWith("background")) {
                            playlistHeader.setBackground(value);
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found background " + value);
                        } else if (lcLine.startsWith("title")) {
                            playlistHeader.setTitle(value);
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found title " + value);
                        } else if (lcLine.startsWith("logo")) {
                            playlistHeader.setLogo(value);
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found logo " + value);
                        } else if (lcLine.startsWith("description")) {
                            playlistHeader.setDescription(value.substring(0, value.lastIndexOf("/")));
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found description " + playlistHeader.getDescription());
                        } else {
                            Log.getInstance().write(Log.LOGLEVEL_WARN, "scanForHeader: Unknown element " + lcLine);
                        }
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Unknown element " + line);
                    }
                }
            }
        }
    }

    private void scanForEntries(List<String> allLines) {

    }

    private int findNextToken(String token, int start, List<String> allLines) {

        for (int i=start; i<allLines.size(); i++) {
            String lcLine = allLines.get(i).toLowerCase();
            if (lcLine.startsWith(token)) {
                Log.getInstance().write(Log.LOGLEVEL_MAX, "findNextToken: Found token at location " + i);
                return i;
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_MAX, "findNextToken: Failed to find token.");
        return -1;
    }

    private static BufferedReader read(String urlString) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "read: Creating reader for " + urlString);

        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "read: Malformed URL " + urlString);
            return null;
        }

        InputStream is = null;

        try {
            is = url.openStream();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "read: IO Exception " + e.getMessage());
            return null;
        }

        InputStreamReader isr = new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr);

        return br;
    }

    private static List<String> parseParts(String line) {
        List<String> partsList = new ArrayList<String>();

        if (!line.contains("=")) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "parseParts: Not a valid entry " + line);
            return partsList;
        }

        String[] parts = line.split("=");

        if (parts.length != 2) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "parseParts: Not a valid entry " + line);
            return partsList;
        }

        partsList.add(parts[0]);
        partsList.add(parts[1]);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "parseParts: Parsed " + partsList);
        return partsList;
    }
}
