
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;
import sagex.api.*;

/**
 * This class represents a Navi-X Playlist.  A Playlist usually contains one or more
 * "Elements", such as VideoElement, AudioElement, TextElement, etc.  All Elements are
 * a superclass of the PlaylistEntry class.
 *
 * Playlists are stored on the web in text format.  The Playlist url points to the web
 * address of the Playlist.
 *
 * @author Tom Miranda.
 */
public final class Playlist implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String COMMENT_CHARACTER = "#";
    public static final String SPAN_DELIMITER = "</span>";

    private static final String PROPERTY_STACK = "navix/url_stack";
    private static final String STACK_DELIMITER = ",";
    public static String        STACK_DEFAULT = "http://navix.turner3d.net/playlist/50242/navi-xtreme_nxportal_home.plx";

    private String                url;
    private PlaylistHeader        playlistHeader = new PlaylistHeader();
    private List<PlaylistEntry>   playlistEntries = new ArrayList<PlaylistEntry>();
    private List<String>          allLines = new ArrayList<String>();;
    private long                  cachedTime = 0;
    private boolean               hasLoaded = false;
    private long                  diskTime = 0;

    /**
     * Constructor.
     *
     * Creates a new Playlist and adds it to the cache.  If the Playlist is already in the
     * cache it will be removed and the newly created Playlist will replace it.
     *
     * @param HomeURL The root URL of the playlist.
     * @param putInCache true if the newly created Playlist should be placed in the PLaylist
     * cache, false if it should not.
     */
    public Playlist(String HomeURL, boolean putInCache) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist: Creating new Playlist for " + HomeURL);

        // Create the Header and the empty List.
        url = HomeURL;
        playlistHeader = new PlaylistHeader();
        playlistEntries = new ArrayList<PlaylistEntry>();
        allLines = new ArrayList<String>();
        hasLoaded = false;
        diskTime = 0;

        // Make sure the HomeURL is valid.
        if (HomeURL==null || HomeURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist: Empty HomeURL.");
            return;
        }

        if (url.startsWith(Search.SAVED_URL_PREFIX)) {
            url = Search.diskUrlToCacheUrl(HomeURL);
            String fileName = Search.diskUrlGetFileName(HomeURL);
            Playlist p = Search.getFromDiskCache(url, fileName);

            if (p==null) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist: Failed to fetch from disk cache " + url + " " + fileName);
                return;
            }

            this.playlistHeader = p.playlistHeader;
            this.playlistEntries = p.playlistEntries;
            this.allLines = p.allLines;
            this.hasLoaded = p.hasLoaded;
            return;
        }

        // Remove the Playlist from the memory Cache.
        PlaylistCache.getInstance().remove(url);

        // Create the file reader.
        BufferedReader br = read(HomeURL);

        if (br==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist: Failed to open URL. Adding to cache in unloaded state " + url);
            cachedTime = new Date().getTime();
            if (putInCache)
                PlaylistCache.getInstance().add(this);
            return;
        }

        // Read the entire playlist into memory. Skip any null, empty, or comment lines.

        //List<String> allLines = new ArrayList<String>();
        String line = null;

        try {
            while ((line=br.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_MAX, "Playlist: line = " + line);
                if (line!=null && !line.isEmpty() && !line.startsWith(COMMENT_CHARACTER))
                    allLines.add(line);
            }
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist: IO Exception " + e.getMessage());
        }

        int numberOfLines = allLines.size();
        allLines = combineSpannedLines(allLines);
        if (numberOfLines != allLines.size()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist: Total combined lines " + numberOfLines + ":" + allLines.size());
        }

        // Set the header information.
        scanForHeader(allLines);

        // Set the entries.
        scanForEntries(allLines);

        hasLoaded = true;

        // Add the new Playlist to the cache.
        cachedTime = new Date().getTime();
        if (putInCache) {
            PlaylistCache.getInstance().add(this);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist: Added to cache " + url);
        }

        return;
    }

    /**
     * Create a new Playlist but do not put it into the memory cache.
     * @param HomeURL
     */
    public Playlist(String HomeURL) {
        this(HomeURL, true);
    }

    /**
     * Creates a Playlist with the same url, header, entries and allLines.
     * 
     * @param playlist
     */
    public Playlist(Playlist playlist) {
        url = playlist.url;
        playlistHeader = playlist.playlistHeader;
        playlistEntries = playlist.playlistEntries;
        allLines = playlist.allLines;
        hasLoaded = playlist.hasLoaded;
        diskTime = playlist.diskTime;
    }

    /**
     * Creates an empty Playlist.
     */
    public Playlist() {}

    private List<String> combineSpannedLines(List<String> allLines) {

        if (allLines==null || allLines.isEmpty())
            return allLines;

        List<String> newLines = new ArrayList<String>();

        for (int lineNumber=0; lineNumber<allLines.size(); lineNumber++) {

            String line = allLines.get(lineNumber);

            line = line.replaceAll("\n", "");

            while (line.endsWith(SPAN_DELIMITER) && lineNumber<allLines.size()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "combineSpannedLines: Found delimiter in line " + line);
                line = line.replace(SPAN_DELIMITER, "");
                lineNumber++;
                if (lineNumber<allLines.size()) {
                    line = line + " " + allLines.get(lineNumber);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "combineSpannedLines: Combined line " + line);
                }
            }

            newLines.add(line);
        }

        return newLines;
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

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "scanForEntries: Found entries " + playlistEntries.size());
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

            case AUDIO:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found AUDIO element.");

                // Create the new element.
                entry = new AudioElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case VIDEO:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found VIDEO element.");
                
                // Create the new element.
                entry = new VideoElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case IMAGE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found IMAGE element.");

                // Create the new element.
                entry = new ImageElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case SCRIPT:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found SCRIPT element.");

                // Create the new element.
                entry = new ScriptElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case TEXT:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found TEXT element.");

                // Create the new element.
                entry = new TextElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case DOWNLOAD:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found DOWNLOAD element.");

                // Create the new element.
                entry = new DownloadElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case PLUGIN:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found PLUGIN element.");

                // Create the new element.
                entry = new PluginElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);
                playlistEntries.add(entry);

                break;

            case PLAYLIST:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found PLAYLIST element.");

                // Create the new element.
                entry = new PlaylistElement();
                
                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                // Look at the next lines for name, thumb, URL, player and rating.
                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS element.");

                // Create the new element.
                entry = new RssElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_RSS:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS:RSS element.");

                // Create the new element.
                entry = new RssRssElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_HTML:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS:HTML element.");

                // Create the new element.
                entry = new RssHtmlElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_IMAGE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS:IMAGE element.");

                // Create the new element.
                entry = new RssImageElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_VIDEO:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS:VIDEO element.");

                // Create the new element.
                entry = new RssVideoElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

            case ATOM:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found ATOM element.");

                // Create the new element.
                entry = new AtomElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case HTML_YOUTUBE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found HTML_YOUTUBE element.");

                // Create the new element.
                entry = new HtmlYouTubeElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case NAVIX:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found NAVIX element.");

                // Create the new element.
                entry = new NavixElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case XML_SHOUTCAST:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found XML_SHOUTCAST element.");

                // Create the new element.
                entry = new XmlShoutcastElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case XML_APPLEMOVIE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found XML_APPLEMOVIE element.");

                // Create the new element.
                entry = new XmlAppleMovieElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case RSS_FLICKR_DAILY:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found RSS_FLICKR_DAILY element.");
                
                // Create the new element.
                entry = new RssFlickrDailyElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case PLX:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found PLX element.");

                // Create the new element.
                entry = new PlxElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case HTML:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found HTML element.");

                // Create the new element.
                entry = new HtmlElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case LIST_NOTE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found LIST_NOTE element.");

                // Create the new element.
                entry = new ListNoteElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case OPML:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found OPML element.");

                // Create the new element.
                entry = new OpmlElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case PLAYLIST_YOUTUBE:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found PLAYLIST_YOUTUBE element.");

                // Create the new element.
                entry = new PlaylistYouTubeElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

                numberConsumed += entry.loadData(startLocation+1, 10, allLines);

                playlistEntries.add(entry);

                break;

            case SEARCH:
                Log.getInstance().write(Log.LOGLEVEL_MAX, "addEntryStartingAt: Found SEARCH element.");


                // Create the new element.
                entry = new SearchElement();

                // Set the type.
                entry.setType(parts.get(1));
                entry.setPlaylistUrl(this.url);

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

    /**
     * Get all of the raw data used to construct the Playlist.  This is included for debugging
     * purposes only.
     * @return
     */
    public List<String> getAllLines() {
        return allLines;
    }

    /**
     * Create a grouping depending on the Type of the PlaylistEntry.
     *
     * - For Type==Playlist (class PlaylistElement) the values will be children Playlists (class Playlist).
     * - For Type==RSS (class RssElement) the values will be RSSItems (class RssItemElement).
     * - For all other types the key and the value will be the same.
     *
     * @return
     */
    public Map<PlaylistEntry, List<Object>> group() {

        if (playlistEntries==null || playlistEntries.isEmpty()) {
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Starting.");

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

                    Playlist nextPlaylist;

                    if (isInCache(nextPlaylistAddress)) {
                        nextPlaylist = retrieveFromCache(nextPlaylistAddress);
                    } else {
                        nextPlaylist = new Playlist(nextPlaylistAddress);
                    }

                    for (PlaylistEntry nextPlaylistEntry : nextPlaylist.getElements()) {
                        String name = nextPlaylistEntry.getName();
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found name " + name);
                        groupItems.add(nextPlaylistEntry);
                    }

                    //Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found names " + groupItems);
                    groups.put(entry, groupItems);
                    break;

                case RSS:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS element " + entry.getName());

                    RssElement rssElement = (RssElement)entry;
                    
                    // If the RssElement channel has already been downloaded and validated
                    // the the group will contain RssItemElement, otherwise it will
                    // just contain a placeholder.
                    if (rssElement.hasBeenChecked() && rssElement.isValidPodcast()) {

                        List<RssItemElement> rssItemElements = rssElement.getRssItemElements();

                        if (rssItemElements==null || rssItemElements.isEmpty()) {
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "group: Podcast has no RSSItems.");
                            groupItems.add(new RssItemElement(entry, null, null));
                        } else {
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "group: Found RSS elements " + groupItems.size());
                            groupItems.addAll(rssItemElements);
                        }

                    } else {

                        // Create a placeholder RSSItemElement.
                        if (!rssElement.hasBeenChecked())
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "group: Unchecked Podcast.");
                        if (!rssElement.isValidPodcast())
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "group: Invalid Podcast.");
                        groupItems.add(new RssItemElement(entry, null, null));                      
                    }

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

                case RSS_VIDEO:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS:VIDEO element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case RSS_IMAGE:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS:IMAGE element.");

                    groupItems.add(entry);
                    groups.put(entry, groupItems);

                    break;

                case RSS_ITEM:
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "group: Found RSS:ITEM element.");

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

    /**
     * Get all of the PlaylistEntry elements for this Playlist.  Each PlaylistEntry will
     * actually be one of the various XXXElement classes.
     * @return
     */
    public List<PlaylistEntry> getElements() {
        return playlistEntries;
    }

    public void setElements(List<PlaylistEntry> playlistEntries) {
        this.playlistEntries = playlistEntries;
    }

    /*
     * Retrieve header information.
     */

    public String getTitle() {
        return playlistHeader.getTitle();
    }

    public void setTitle(String title) {
        playlistHeader.setTitle(title);
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

    public void setVersion(String version) {
        playlistHeader.setVersion(version);
    }

    public String getDescription() {
        return playlistHeader.getDescription();
    }

    public String getView() {
        return playlistHeader.getView();
    }

    public String getUrl() {
        return url;
    }

    public long getDiskTime() {
        return diskTime;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUrl() {
        url = UUID.randomUUID().toString();
    }

    public long getCachedTime() {
        return cachedTime;
    }

    public void setCachedTime(long time) {
        cachedTime = time;
    }

    public void setDiskTime(long time) {
        diskTime = time;
        return;
    }

    /**
     * Check to see if the Playlist was successfully loaded from the web.
     * @return
     */
    public boolean hasLoaded() {
        return hasLoaded;
    }

    void setHasLoaded(boolean value) {
        hasLoaded = value;
    }

    /*
     * Cache methods.
     */

    /**
     * Returns true of the Playlist is in the cache, false otherwise.
     * @param URL
     * @return
     */
    public static boolean isInCache(String URL) {
        return PlaylistCache.getInstance().contains(URL);
    }

    public static boolean shouldRetryNew(String URL) {
        return PlaylistCache.getInstance().shouldRetryNew(URL);
    }

    /**
     * Retrieve the specified Playlist from cache.  Returns null if the Playlist is not found.
     * @param URL
     * @return
     */
    public static Playlist retrieveFromCache(String URL) {
        return PlaylistCache.getInstance().get(URL);
    }

    /**
     * Add the Playlist to the cache.
     * @param playlist
     * @return
     */
    public static boolean addToCache(Playlist playlist) {
        return PlaylistCache.getInstance().add(playlist);
    }

    public static boolean removeFromCache(Playlist playlist) {
        return PlaylistCache.getInstance().remove(playlist.getUrl());
    }

    /**
     * Loads into cache all of the Playlists that are children and grandchildren
     * of the speficied Playlist. For each PlaylistElement or PlxElement that is found
     * a new thread will be spawned to do the actual retrieval.
     *
     * Enabling and disabling of caching of the grandchildren can be changed
     * by using PlaylistCache.setCacheSecondLevel()
     *
     * @param playlist
     */
    public static void cachePlaylistElements(Playlist playlist) {
        if (playlist==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.cachePlaylistElements: null Playlist.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist.cachePlaylistElements: Playlist elements " + playlist.getElements().size());
        
        for (PlaylistEntry p : playlist.getElements()) {
            if (p.isPlx() || p.isPlaylist()) {
                PlaylistCache.getInstance().fetch(p.getUrl());
            }
        }
    }

    /**
     * Add the Playlist URL onto the top of the virtual stack.  The stack is persistent between
     * menu loads and Sage sessions.
     * @param s
     */
    public static void pushUrl(String s){

        if (s==null || s.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.pushUrl: null url.");
            return;
        }

        if (s.contains(STACK_DELIMITER)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.pushUrl: Delimiter found in url.");
            s = s.replaceAll(STACK_DELIMITER, "");
        }

        String stack = Configuration.GetProperty(PROPERTY_STACK, STACK_DEFAULT);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.pushUrl: Raw stack " + stack);

        // This should not be needed but was getting null stacks during testing....
        if (stack==null || stack.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.pushUrl: null stack!");
            stack = STACK_DEFAULT;
            Configuration.SetProperty(PROPERTY_STACK, STACK_DEFAULT);
        }

        stack = s + STACK_DELIMITER + stack;
        Configuration.SetProperty(PROPERTY_STACK, stack);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.pushUrl: Updated stack " + stack);
        return;
    }

    /**
     * Remove the first Playlist URL from the top of the virtual stack.  The top of the
     * stack will always contain the URL of the Playlist currently being displayed.
     * @return
     */
    public static String popUrl() {

        String stack = Configuration.GetProperty(PROPERTY_STACK, STACK_DEFAULT);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.popUrl: Raw stack " + stack);

        // This should not be needed but was getting null stacks during testing....
        if (stack==null || stack.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.popUrl: null stack!");
            stack = STACK_DEFAULT;
            Configuration.SetProperty(PROPERTY_STACK, STACK_DEFAULT);
        }

        String[] elements = stack.split(STACK_DELIMITER);
        
        // If there is only 1 element leave it on the stack.
        if (elements.length==1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist.popUrl: Popped item " + elements[0]);
            return elements[0];
        }

        String newStack = null;

        for (int i=1; i<elements.length; i++) {
            String thisElement = elements[i];
            newStack = newStack==null ? thisElement : newStack + STACK_DELIMITER + thisElement;
        }

        Configuration.SetProperty(PROPERTY_STACK, newStack);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.popUrl: Updated stack and popped item " + stack + "  " + elements[0]);

        return elements[0];
    }

    /**
     * Gets the first Playlist URL from the top of the virtual stack without removing it.
     * The top of the stack will always contain the URL of the Playlist currently being displayed.
     * @return
     */
    public static String peekUrl() {
        String stack = Configuration.GetProperty(PROPERTY_STACK, STACK_DEFAULT);

        // This should not be needed but was getting null stacks during testing....
        if (stack==null || stack.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.peekUrl: null stack!");
            stack = STACK_DEFAULT;
            Configuration.SetProperty(PROPERTY_STACK, STACK_DEFAULT);
        }

        String[] elements = stack.split(STACK_DELIMITER, 2);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.peekUrl: Raw stack and peeked item " + stack + " " + elements[0]);
        return elements[0];
     }

    /**
     * Returns a List representing the URL stack.  The first element of he list is the top
     * of the stack and the last element is the bottom.  The top of the stack should always
     * be the current Playlist.
     *
     * @return
     */
    public static List<String> getUrlStack() {
        String stack = Configuration.GetProperty(PROPERTY_STACK, STACK_DEFAULT);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.getUrlStack: Raw stack " + stack);
        String[] elements = stack.split(STACK_DELIMITER);
        return Arrays.asList(elements);
    }

    /**
     * Return a formatted String showing the contents of the URL stack.  If the Playlist
     * for the URL is not in the cache then the URL will appear in the formatted String.
     * If the Playlist is in the cache then the Playlist name will appear.
     * @return
     */
    public static String getUrlStackString() {
        List<String> urls = getUrlStack();
        String s = null;

        for (String url : urls) {
            Playlist p = PlaylistCache.getInstance().get(url);
            String name = p==null ? url : p.getTitle();
            s = s==null ? name : s + "->" + name;
        }

        return s;
    }

    /**
     * Same as getUrlStackString() except if "fetch" is set to true it will fetch Playlists
     * from the web (to get the name) if they are not in the cache.  If "fetch" is set to
     * false this method will return exactly the same as getUrlStackString().
     * 
     * @param fetch
     * @return
     */
    public static String getUrlStackString(boolean fetch) {

        if (!fetch)
            return getUrlStackString();

        List<String> urls = getUrlStack();
        String s = null;

        for (String url : urls) {
            Playlist p = PlaylistCache.getInstance().get(url);

            if (p==null) {
                p = new Playlist(url);
            }

            String name = p==null ? url : p.getTitle();
            s = s==null ? name : s + "->" + name;
        }

        return s;
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
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if (this.playlistHeader != other.playlistHeader && (this.playlistHeader == null || !this.playlistHeader.equals(other.playlistHeader))) {
            return false;
        }
        if (this.playlistEntries != other.playlistEntries && (this.playlistEntries == null || !this.playlistEntries.equals(other.playlistEntries))) {
            return false;
        }
        if (this.allLines != other.allLines && (this.allLines == null || !this.allLines.equals(other.allLines))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 71 * hash + (this.playlistHeader != null ? this.playlistHeader.hashCode() : 0);
        return hash;
    }
}
