
package tmiranda.navix;

import java.util.*;

// FIXME - Will not work properly because circular references are allowed in the playlists.

/**
 *
 * @author Tom Miranda.
 */
public class Search {

    private List<PlaylistEntry> allElements;

    private static int totalPlaylists= 0;
    private static int totalElements = 0;
    private static List<String> processedPlaylists = new ArrayList<String>();

    public Search(String RootURL) {
        allElements = new ArrayList<PlaylistEntry>();
    }

    public List<PlaylistEntry> getAllElements(String RootURL) {
        return allElements;
    }

    public List<PlaylistEntry> dynamicSearchForType(String RootURL, String Type) {

        List<PlaylistEntry> tree = new ArrayList<PlaylistEntry>();

        if (RootURL==null || RootURL.isEmpty() || Type==null || Type.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "searchForType: null RootURL.");
            return tree;
        }

        if (processedPlaylists.contains(RootURL)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "searchForType: Pkaylist already loaded " + RootURL);
            return tree;
        }

        processedPlaylists.add(RootURL);
        totalPlaylists++;

        Playlist playlist = new Playlist(RootURL);

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            // Add to the List if this is the correct type.
            if (entry.getType().equalsIgnoreCase(Type)) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "searchForType: Adding " + entry.getName());
                tree.add(entry);
            }

            // Recurse if this is a playlist.
            if (entry.isPlaylist() || entry.isPlx()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "searchForType: Recursing. Totals " + totalPlaylists + ":" + totalElements);
                tree.addAll(dynamicSearchForType(entry.getUrl(), Type));
            }
        }

        return tree;
    }

    public List<PlaylistEntry> dynamicSearchForWords(String RootURL, List<String> Words) {

        List<PlaylistEntry> tree = new ArrayList<PlaylistEntry>();

        if (RootURL==null || RootURL.isEmpty() || Words==null || Words.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "searchForWords: null RootURL.");
            return tree;
        }

        Playlist playlist = new Playlist(RootURL);

        for (PlaylistEntry entry : playlist.getElements()) {

            // Build a List of words in this entry.
            List<String> wordList = new ArrayList<String>();
            wordList.addAll(stringToWords(entry.getDescription()));
            wordList.addAll(stringToWords(entry.getName()));
            wordList.addAll(stringToWords(entry.getTitle()));

            // See if any of these words appear in Words.
            for (String word : wordList) {
                if (Words.contains(word)) {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "searchForWords: Adding " + entry.getName());
                    tree.add(entry);
                }
            }

            // Recurse if this is a playlist.
            if (entry.isPlaylist() || entry.isPlx()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "searchForWords: Recursing.");
                tree.addAll(dynamicSearchForWords(entry.getUrl(), Words));
            }
        }

        return tree;
    }

    private static List<String> stringToWords(String s) {

        List<String> wordList = new ArrayList<String>();

        if (s==null || s.isEmpty())
            return wordList;

        String[] wordArray = s.split(" ");
        return Arrays.asList(wordArray);
    }
}
