
package tmiranda.navix;

import java.util.*;
import java.io.*;
import org.python.util.*;

/**
 *
 * @author Tom Miranda.
 */
public class PlaylistEntry {

    String version      = null;
    String title        = null;     // Web page title NOT content title.
    String background   = null;
    String type         = null;
    String name         = null;
    String thumb        = null;
    String url          = null;
    String player       = null;
    String rating       = null;
    String description  = null;     // Ends with /description
    String[] descriptionParts = null;
    String processor    = null;
    String icon         = null;
    String date         = null;
    String view         = null;

    public static final String COMPONENT_VERSION        = "version";
    public static final String COMPONENT_TITLE          = "title";
    public static final String COMPONENT_BACKGROUND     = "background";
    public static final String COMPONENT_TYPE           = "type";
    public static final String COMPONENT_NAME           = "name";
    public static final String COMPONENT_THUMB          = "thumb";
    public static final String COMPONENT_URL            = "url";
    public static final String COMPONENT_PLAYER         = "player";
    public static final String COMPONENT_RATING         = "rating";
    public static final String COMPONENT_DESCRIPTION    = "description";
    public static final String COMPONENT_PROCESSOR      = "processor";
    public static final String COMPONENT_ICON           = "icon";
    public static final String COMPONENT_DATE           = "date";
    public static final String COMPONENT_VIEW           = "view";

    private static final String DEFAULT_SAGE_ICON       = "SageIcon62.png";

    // FIXME
    public static final String PYTHON_PATH = "";
    public static final String SAGE_PROCESSOR = "";
    public static final String SCRIPT_DONE = "script done";
    public static final String SCRIPT_ANSWER = "answer=";

    public List<String> invokeProcessor(String rawURL, String rawProcessor) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "invokeProcessor: Invoking processor for " + rawURL);
        List<String> answer = new ArrayList<String>();

        if (rawURL==null || rawURL.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "invokeProcessor: null processor.");
            return answer;
        }

        // Create the pipe that will be used to connect the python script to this method.
        PipedOutputStream output = new PipedOutputStream();
        InputStream input = null;

        try {
            input = new PipedInputStream(output);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "invokeProcessor: Exception opening PipedInputStream " + e.getMessage());
            try {output.close();} catch (IOException e1) {}
            return answer;
        }

        // Create the pipe reader.
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(isr);

        // Create the properties that will be needed to setup the python path.
        Properties prop = new Properties();
        prop.setProperty("python.path",".;.\\NaviX\\scripts;.\\NaviX\\python\\jython2.5.2\\lib");

        // This is the data we will pass to the python script.
        String[] argv = {"SageProcessor.py", rawURL, rawProcessor==null ? "" : rawProcessor};

        // Initialize the python runtime environment.
        PythonInterpreter.initialize(System.getProperties(), prop, argv);

        // Start the python script.
        PythonInterpreter interp = new PythonInterpreter();
        interp.setOut(output);
        interp.setErr(output);
        interp.execfile(".\\NaviX\\scripts\\SageProcessor.py");

        // Read the results.
        String line = null;
        boolean done = false;

        try {
            while (!done && (line = br.readLine()) != null) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "invokeProcessor: Read line " + line);
                if (line.equalsIgnoreCase(SCRIPT_DONE)) {
                    done = true;
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "invokeProcessor: Script has completed.");
                } else if (line.startsWith(SCRIPT_ANSWER)) {
                    answer.add(line);
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "invokeProcessor: Script answer received " + line);
                }
            }
        } catch (IOException e ) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "invokeProcessor: Exception reading " + e.getMessage());
        }

        interp.cleanup();

        try {
            br.close();
            isr.close();
            input.close();
            output.close();
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "invokeProcessor: Exception cleaning up " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "invokeProcessor: Answer " + answer);
        return answer;
    }

    public boolean isSupportedBySage() {
        return false;
    }

    public boolean isAtom() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.ATOM;
    }

    public boolean isAudio() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.AUDIO;
    }

    public boolean isDownload() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.DOWNLOAD;
    }

    public boolean isHtml() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.HTML;
    }

    public boolean isHtmlYouTube() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.HTML_YOUTUBE;
    }

    public boolean isImage() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.IMAGE;
    }

    public boolean isListNote() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.LIST_NOTE;
    }

    public boolean isNaviX() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.NAVIX;
    }

    public boolean isPlaylist() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.PLAYLIST;
    }

    public boolean isPlaylistYouTube() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.PLAYLIST_YOUTUBE;
    }

    public boolean isPlugin() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.PLUGIN;
    }

    public boolean isPlx() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.PLX;
    }

    public boolean isRss() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.RSS;
    }

    public boolean isRssItem() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.RSS_ITEM;
    }

    public boolean isRssFlickrDaily() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.RSS_FLICKR_DAILY;
    }

    public boolean isScript() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.SCRIPT;
    }

    public boolean isSearch() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.SEARCH;
    }

    public boolean isText() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.TEXT;
    }

    public boolean isVideo() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.VIDEO;
    }

    public boolean isXmlAppleMovie() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.XML_APPLEMOVIE;
    }

    public boolean isXmlShoutcast() {
        return type==null ? false : PlaylistType.toEnum(type) == PlaylistType.XML_SHOUTCAST;
    }

    /**
     * Loads the data into the Object.  Stops when it finds the next "type" or loads the "number"
     * of components.
     *
     * @param startLocation
     * @param number
     * @param allLines
     * @return The number of lines consumed.
     */
    int loadData(int startLocation, int number, List<String> allLines) {

        int numberConsumed = 0;

        for (int i=startLocation; i<allLines.size() && i<=startLocation+number; i++) {

            numberConsumed++;

            // Get the line.
            String line = allLines.get(i);

            // Make sure it's not empty.
            if (line==null || line.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "loadData: null line.");
                return numberConsumed;
            }

            // Remove leading and trailing spaces.
            line = line.trim();

            List<String> parts = Playlist.parseParts(line);

            if (parts.size() != 2) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "loadData: Malformed line, skipping " + line);
                return numberConsumed;
            }

            String component = parts.get(0).toLowerCase();
            String value = parts.get(1);
            Log.getInstance().write(Log.LOGLEVEL_MAX, "loadData: component and value " + component + ":" + value);

            if (component.startsWith(COMPONENT_VERSION))
                setVersion(value);
            else if (component.startsWith(COMPONENT_TITLE))
                setTitle(value);
            else if (component.startsWith(COMPONENT_BACKGROUND))
                setBackground(value);
            else if (component.startsWith(COMPONENT_NAME))
                setName(value);
            else if (component.startsWith(COMPONENT_THUMB))
                setThumb(value);
            else if (component.startsWith(COMPONENT_URL))
                setUrl(value);
            else if (component.startsWith(COMPONENT_PLAYER))
                setPlayer(value);
            else if (component.startsWith(COMPONENT_RATING))
                setRating(value);
            else if (component.startsWith(COMPONENT_DESCRIPTION))
                i += loadDescription(i+1, value, allLines);
            else if (component.startsWith(COMPONENT_PROCESSOR))
                setProcessor(value);
            else if (component.startsWith(COMPONENT_ICON))
                setIcon(value);
            else if (component.startsWith(COMPONENT_DATE))
                setDate(value);
            else if (component.startsWith(COMPONENT_VIEW))
                setView(value);
            else if (component.startsWith(COMPONENT_TYPE)) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "loadData: Found type component, finished.");
                return numberConsumed-1;
            } else {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "loadData: Found unknown component " + component);
            }
        }

        return numberConsumed;
    }

    int loadDescription(int startLocation, String beginning, List<String> allLines) {

        int numberConsumed = 0;

        String delimiter = "/" + COMPONENT_DESCRIPTION;

        // Check for special case of a single line description.
        if (beginning.lastIndexOf(delimiter)!=-1) {
            setDescription(beginning.substring(0, beginning.lastIndexOf(delimiter)));
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadDescription: " + numberConsumed + ": <" + description + ">");
            return numberConsumed;
        }

        String desc = beginning;
        boolean found = false;

        for (int i=startLocation; i<allLines.size() && !found; i++) {
            String nextLine = allLines.get(i);

            numberConsumed++;

            if (nextLine==null || nextLine.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "loadDescription: null line.");
                continue;
            }

            nextLine = nextLine.trim();
            nextLine = nextLine.replace("\n", "");

            if (nextLine.lastIndexOf(delimiter)!=-1) {
                desc = desc + "" + nextLine.substring(0, nextLine.lastIndexOf(delimiter));
                found = true;
            } else {
                desc = desc + " " + nextLine;
            }
        }

        setDescription(desc);

        parseDescription();

        if (!found)
            Log.getInstance().write(Log.LOGLEVEL_WARN, "loadDescription: Did not find delimiter.");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadDescription: " + numberConsumed + ": <" + description + ">");

        return numberConsumed;
    }

    private void parseDescription() {
        if (description==null)
            return;

        descriptionParts = description.split("-=");

        if (descriptionParts==null || descriptionParts.length==0)
            return;

        for (int i=0; i<descriptionParts.length; i++) {
            descriptionParts[i] = descriptionParts[i].replace("-=", "");
            descriptionParts[i] = descriptionParts[i].replace("=-", "");
            descriptionParts[i] = descriptionParts[i].trim();
        }
    }

    /*
     * Getters and Setters.
     */
    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getDescriptionParts() {
        return descriptionParts;
    }

    public String getIcon() {
        return icon;
    }

    public String getSageIcon() {
        return icon == null ? DEFAULT_SAGE_ICON : icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getProcessor() {
        return processor;
    }

    public boolean hasProcessor() {

        if (url!=null && url.contains("youtube.com"))
            return true;
        else
            return processor != null && !processor.isEmpty();
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getThumb() {
// FIXME - Need to handle having "?...." after .jpg
// FIXME - Some playlists have "default" for this value.
        return thumb==null || thumb.equalsIgnoreCase("default") ? null : thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    @Override
    public String toString() {
        return "Type=" + type + ", " +
               "Title=" + title + ", " +
               "Name=" + name + ", " +
               "Version=" + version + ", " +
               "Background=" + background + ", " +
               "Thumb=" + thumb + ", " +
               "URL=" + url + ", " +
               "Player=" + player + ", " +
               "Rating=" + rating + ", " +
               "Date=" + date + ", " +
               "Processor=" + processor + ", " +
               "Icon=" + icon + ", " +
               "View=" + view + ", " +
               "Description=" + description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlaylistEntry other = (PlaylistEntry) obj;
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
            return false;
        }
        if ((this.background == null) ? (other.background != null) : !this.background.equals(other.background)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.thumb == null) ? (other.thumb != null) : !this.thumb.equals(other.thumb)) {
            return false;
        }
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if ((this.player == null) ? (other.player != null) : !this.player.equals(other.player)) {
            return false;
        }
        if ((this.rating == null) ? (other.rating != null) : !this.rating.equals(other.rating)) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        if ((this.processor == null) ? (other.processor != null) : !this.processor.equals(other.processor)) {
            return false;
        }
        if ((this.icon == null) ? (other.icon != null) : !this.icon.equals(other.icon)) {
            return false;
        }
        if ((this.date == null) ? (other.date != null) : !this.date.equals(other.date)) {
            return false;
        }
        if ((this.view == null) ? (other.view != null) : !this.view.equals(other.view)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 79 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 79 * hash + (this.background != null ? this.background.hashCode() : 0);
        hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.thumb != null ? this.thumb.hashCode() : 0);
        hash = 79 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 79 * hash + (this.player != null ? this.player.hashCode() : 0);
        hash = 79 * hash + (this.rating != null ? this.rating.hashCode() : 0);
        hash = 79 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 79 * hash + (this.processor != null ? this.processor.hashCode() : 0);
        hash = 79 * hash + (this.date != null ? this.date.hashCode() : 0);
        hash = 79 * hash + (this.view != null ? this.view.hashCode() : 0);
        return hash;
    }

}
