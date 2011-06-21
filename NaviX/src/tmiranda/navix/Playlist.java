
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class Playlist {

    public static String COMMENT_CHARACTER = "#";
    
    private PlaylistHeader      playlistHeader;
    private List<PlaylistEntry> playlistEntries;

    private Playlist            parentPlaylist;
    private List<Playlist>      childPlaylists;

    private List<String>        allLines;

    /**
     * Constructor.
     *
     * @param HomeURL The root URL of the playlist.
     */
    public Playlist(String HomeURL) {

        // Create the Header and the empty List.
        playlistHeader = new PlaylistHeader();
        playlistEntries = new ArrayList<PlaylistEntry>();
        parentPlaylist = null;
        childPlaylists = new ArrayList<Playlist>();
        allLines = new ArrayList<String>();

        // Make sure the HomeURL is valid.
        if (HomeURL==null || HomeURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist: Empty HomeURL.");
            return;
        }

        // Create the file reader.
        BufferedReader br = read(HomeURL);

        if (br==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist: Failed to open URL.");
            return;
        }

        // Read the entire playlist into memory. Skip any null, empty, or comment lines.

        //List<String> allLines = new ArrayList<String>();
        String line = null;

        try {
            while ((line=br.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist: line = " + line);
                if (line!=null && !line.isEmpty() && !line.startsWith(COMMENT_CHARACTER))
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
        //allLines.clear();
        //allLines = null;
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

                boolean complete = false;

                // Look at the next 4 lines.
                for (int j=++i; j<allLines.size() && j<i+5 && !complete; j++) {
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
                            j += playlistHeader.loadDescription(j, line, allLines);
                            //playlistHeader.setDescription(value.substring(0, value.lastIndexOf("/")));
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found description " + playlistHeader.getDescription());
                        } else if (lcLine.startsWith("view")) {
                            playlistHeader.setView(value);
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Found view " + value);
                        } else {
                            Log.getInstance().write(Log.LOGLEVEL_WARN, "scanForHeader: Unknown element, assuming end " + lcLine);
                            complete = true;
                        }
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForHeader: Unknown element " + line);
                    }
                }
            }
        }
    }

    private void scanForEntries(List<String> allLines) {

        int startLocation = findNextToken("type", 0, allLines);

        while (startLocation != -1) {

            // Populate the entry.
            int numberConsumed = addEntryStartingAt(startLocation, allLines);

            // Get the position of the next Entry.
            startLocation = findNextToken("type", startLocation+numberConsumed, allLines);
        }
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

    private int addEntryStartingAt(int startLocation, List<String> allLines) {

        int numberConsumed = 1;

        // Get the line starting with "type".
        String line = allLines.get(startLocation);
        List<String> parts = parseParts(line);

        if (parts==null || parts.size()!=2) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "addEntryStartingAt: Malformed line " + line);
            return numberConsumed;
        }

        PlaylistEntry entry = null;

        // Create the correct Playlist Object.

        PlaylistType t = PlaylistType.toEnum(parts.get(1));

        if (t==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addEntryStartingAt: Found unknown type, skipping " + parts.get(1));
            return numberConsumed;
        }

        switch (t) {
            //case HEAD:
                //Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found the header, skipping.");
                //break;

            case AUDIO:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found AUDIO element.");

                // Create the new element.
                entry = new AudioElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case VIDEO:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found VIDEO element.");
                
                // Create the new element.
                entry = new VideoElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case IMAGE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found IMAGE element.");

                // Create the new element.
                entry = new ImageElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case SCRIPT:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found SCRIPT element.");

                // Create the new element.
                entry = new ScriptElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case TEXT:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found TEXT element.");

                // Create the new element.
                entry = new TextElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case DOWNLOAD:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found DOWNLOAD element.");

                // Create the new element.
                entry = new DownloadElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case PLUGIN:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found PLUGIN element.");

                // Create the new element.
                entry = new PluginElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case PLAYLIST:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found PLAYLIST element.");

                // Create the new element.
                entry = new PlaylistElement();
                
                // Set the type.
                entry.setType(parts.get(1));

                // Look at the next lines for name, thumb, URL, player and rating.
                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found RSS element.");

                // Create the new element.
                entry = new RssElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_RSS:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found RSS:RSS element.");

                // Create the new element.
                entry = new RssRssElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_HTML:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found RSS:HTML element.");

                // Create the new element.
                entry = new RssHtmlElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_IMAGE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found RSS:IMAGE element.");

                // Create the new element.
                entry = new RssImageElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case ATOM:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found ATOM element.");

                // Create the new element.
                entry = new AtomElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case HTML_YOUTUBE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found HTML_YOUTUBE element.");

                // Create the new element.
                entry = new HtmlYouTubeElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case NAVIX:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found NAVIX element.");

                // Create the new element.
                entry = new NavixElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case XML_SHOUTCAST:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found XML_SHOUTCAST element.");

                // Create the new element.
                entry = new XmlShoutcastElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case XML_APPLEMOVIE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found XML_APPLEMOVIE element.");

                // Create the new element.
                entry = new XmlAppleMovieElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_FLICKR_DAILY:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found RSS_FLICKR_DAILY element.");
                
                // Create the new element.
                entry = new RssFlickrDailyElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case PLX:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found PLX element.");

                // Create the new element.
                entry = new PlxElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case HTML:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found HTML element.");

                // Create the new element.
                entry = new HtmlElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case LIST_NOTE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found LIST_NOTE element.");

                // Create the new element.
                entry = new ListNoteElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case OPML:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found OPML element.");

                // Create the new element.
                entry = new OpmlElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case PLAYLIST_YOUTUBE:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found PLAYLIST_YOUTUBE element.");

                // Create the new element.
                entry = new PlaylistYouTubeElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case SEARCH:
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addEntryStartingAt: Found SEARCH element.");


                // Create the new element.
                entry = new SearchElement();

                // Set the type.
                entry.setType(parts.get(1));

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            default:
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "addEntryStartingAt: Unknown type " + parts.get(1));
                return numberConsumed;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addEntryStartingAt: Added " + entry.toString());

        return numberConsumed;
    }

    static BufferedReader read(String urlString) {

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

    static List<String> parseParts(String line) {
        List<String> partsList = new ArrayList<String>();

        if (line==null || line.isEmpty() || !line.contains("=")) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "parseParts: Missing '=', not a valid entry " + line);
            return partsList;
        }

        String[] parts = line.split("=", 2);

        if (parts.length != 2) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "parseParts: Length != 2, not a valid entry " + line);
            return partsList;
        }

        partsList.add(parts[0]);
        partsList.add(parts[1]);
        //Log.getInstance().write(Log.LOGLEVEL_MAX, "parseParts: Parsed " + partsList);
        return partsList;
    }

    public List<String> getAllLines() {
        return allLines;
    }

    public Map<PlaylistEntry, List<Object>> group() {

        if (playlistEntries==null || playlistEntries.isEmpty()) {
            return null;
        }

        Map<PlaylistEntry, List<Object>> groups = new HashMap<PlaylistEntry, List<Object>>();

        for (PlaylistEntry entry : playlistEntries) {

            String t = entry.getType();

            if (t==null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "group: null type.");
                continue;
            }

            List<Object> groupItems = new ArrayList<Object>();

            switch (PlaylistType.toEnum(t)) {
                case AUDIO:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found AUDIO element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case VIDEO:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found VIDEO element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case IMAGE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found IMAGE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case SCRIPT:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found SCRIPT element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case TEXT:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found TEXT element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case DOWNLOAD:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found DOWNLOAD element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case PLUGIN:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found PLUGIN element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case PLAYLIST:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found PLAYLIST element.");

                    PlaylistElement playlistElement = (PlaylistElement)entry;
                    String nextPlaylistAddress = playlistElement.getNextPlaylist();
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "group: Next playlist " + nextPlaylistAddress);

                    Playlist nextPlaylist = new Playlist(nextPlaylistAddress);

                    for (PlaylistEntry nextPlaylistEntry : nextPlaylist.getElements()) {
                        String name = nextPlaylistEntry.getName();
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found name " + name);
                        groupItems.add(nextPlaylistEntry);
                    }

                    //Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found names " + groupItems);
                    groups.put(entry, groupItems);

                    break;

                case RSS:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case RSS_RSS:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS:RSS element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case RSS_HTML:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS:HTML element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case ATOM:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found ATOM element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case HTML_YOUTUBE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found HTML_YOUTUBE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case NAVIX:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found NAVIX element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case XML_SHOUTCAST:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found XML_SHOUTCAST element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case XML_APPLEMOVIE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found XML_APPLEMOVIE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case RSS_FLICKR_DAILY:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS_FLICKR_DAILY element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case PLX:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found PLX element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case HTML:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found HTML element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case LIST_NOTE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found LIST_NOTE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case OPML:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found OPML element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case PLAYLIST_YOUTUBE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found PLAYLIST_YOUTUBE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case SEARCH:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found SEARCH element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                default:
                    Log.getInstance().write(Log.LOGLEVEL_ERROR, "group: Unknown type " + t);

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;
            }

        }

        return groups;
    }


    public List<PlaylistEntry> getElements() {
        return playlistEntries;
    }

    /*
     * Parent - Child methods.
     */
    public Playlist getParent() {
        return parentPlaylist;
    }

    private void setParent(Playlist parentPlaylist) {
        
        if (parentPlaylist != null)
            this.parentPlaylist = parentPlaylist;
        else
            Log.getInstance().write(Log.LOGLEVEL_WARN, "setParent: null parentPlaylist.");
    }

    public boolean addChild(Playlist child) {

        if (child == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addChild: null child.");
            return false;
        }

        child.setParent(this);
        return childPlaylists.add(child);
    }

    public boolean removeChild(Playlist child) {

        if (child == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeChild: null child.");
            return false;
        }

        if (child.hasChildren()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeChild: Attempt to remove child with children.");
            return false;
        }

        child.setParent(null);
        return childPlaylists.remove(child);
    }

    public boolean hasChildren() {
        return childPlaylists.size() > 0;
    }

    public boolean isRoot() {
        return parentPlaylist == null;
    }

    /*
     * Retrieve header information.
     */

    public String getTitle() {
        return playlistHeader.getTitle();
    }

    public String getBackground() {
        return playlistHeader.getBackground();
    }

    public String getLogo() {
        return playlistHeader.getLogo();
    }

    public String getVersion() {
        return playlistHeader.getVersion();
    }

    public String getDescription() {
        return playlistHeader.getDescription();
    }

    public String getView() {
        return playlistHeader.getView();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Playlist other = (Playlist) obj;
        if (this.playlistHeader != other.playlistHeader && (this.playlistHeader == null || !this.playlistHeader.equals(other.playlistHeader))) {
            return false;
        }
        if (this.playlistEntries != other.playlistEntries && (this.playlistEntries == null || !this.playlistEntries.equals(other.playlistEntries))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.playlistHeader != null ? this.playlistHeader.hashCode() : 0);
        hash = 13 * hash + (this.playlistEntries != null ? this.playlistEntries.hashCode() : 0);
        return hash;
    }

}
