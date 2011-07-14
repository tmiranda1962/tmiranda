
package tmiranda.navix;

import java.util.*;
import java.io.*;

/**
 * Save searches so they can be reused (by the Search class) without the user having
 * to retype everything.
 *
 * @author Tom Miranda.
 */
public class SavedSearch {

    public static final String SEARCH_PROPERTY_FILE    = "Searches.navix.properties";
    
    String          name;
    String          typeOrWords;
    Boolean         cacheToMemory;
    String[]        cacheFiles;
    Long            maxAge;
    String          url;
    List<String>    searchItems;
    Integer         limit;
    Integer         depth;
    Integer         maxPlaylists;

    boolean         initialized = false;
    Search          search = null;

    /**
     * Constructor to create a new SavedSearch.
     *
     * @param Name
     * @param TypeOrWords
     * @param CacheToMemory
     * @param CacheFiles
     * @param MaxAge
     * @param Url
     * @param SearchItems
     * @param Limit
     * @param Depth
     * @param MaxPlaylists
     */
    public SavedSearch( String Name,
                        String TypeOrWords,
                        boolean CacheToMemory,
                        String[] CacheFiles,
                        long MaxAge,
                        String Url,
                        List<String>SearchItems,
                        int Limit,
                        int Depth,
                        int MaxPlaylists) {

        name = Name;
        typeOrWords = TypeOrWords;
        cacheToMemory = CacheToMemory;
        cacheFiles = CacheFiles;
        maxAge = MaxAge;
        url = Url;
        searchItems = SearchItems;
        limit = Limit;
        depth = Depth;
        maxPlaylists = MaxPlaylists;

        initialized = false;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SavedSearch: Created " + this.toString());
    }

    public SavedSearch( String Name,
                        String TypeOrWords,
                        boolean CacheToMemory,
                        String CacheFile,
                        long MaxAge,
                        String Url,
                        List<String>SearchItems,
                        int Limit,
                        int Depth,
                        int MaxPlaylists) {
        name = Name;
        typeOrWords = TypeOrWords;
        cacheToMemory = CacheToMemory;
        cacheFiles = new String[1];
        cacheFiles[0] = CacheFile;
        maxAge = MaxAge;
        url = Url;
        searchItems = SearchItems;
        limit = Limit;
        depth = Depth;
        maxPlaylists = MaxPlaylists;

        initialized = false;
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SavedSearch: Created " + this.toString());
    }
    /**
     * Constructor to restore a SavedSearch.
     *
     * @param Name
     */
    public SavedSearch(String Name) {

        if (Name==null || Name.isEmpty())
            return;

        Properties props = getProperties();

        name = Name;
        typeOrWords = props.getProperty(Name+"/typeOrWords");
        cacheToMemory = stringToBoolean(props.getProperty(Name+"/cacheToMemory"));
        maxAge = stringToLong(props.getProperty(Name+"/maxAge"));
        url = props.getProperty(Name+"/url");
        limit = stringToInt(props.getProperty(Name+"/limit"));
        depth = stringToInt(props.getProperty(Name+"/depth"));
        maxPlaylists = stringToInt(props.getProperty(Name+"/maxPlaylists"));

        int number = stringToInt(props.getProperty(Name+"/cacheFiles"));
        cacheFiles = new String[number];
        for (Integer i=0; i<number; i++) {
            cacheFiles[i] = props.getProperty(Name+"/cachefile/"+i.toString());
        }

        number = stringToInt(props.getProperty(Name+"/searchItems"));
        searchItems = new ArrayList<String>();
        for (Integer i=0; i<number; i++) {
            searchItems.add(props.getProperty(Name+"/searchItem/"+i.toString()));
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "SavedSearch: Restored " + this.toString());
    }

    /**
     * Saves the Search so it can be restored later.
     */
    public boolean save() {

        if (name==null || typeOrWords==null || url==null || searchItems==null)
            return false;

        Properties props = getProperties();

        props.setProperty("SavedSearch/"+name, name);
        props.setProperty(name+"/typeOrWords", typeOrWords);
        props.setProperty(name+"/cacheToMemory", cacheToMemory.toString());
        props.setProperty(name+"/maxAge", maxAge.toString());
        props.setProperty(name+"/url", url.toString());
        props.setProperty(name+"/limit", limit.toString());
        props.setProperty(name+"/depth", depth.toString());
        props.setProperty(name+"/maxPlaylists", maxPlaylists.toString());

        Integer number = cacheFiles==null ? 0 : cacheFiles.length;
        props.setProperty(name+"/cachefiles", number.toString());

        for (Integer i=0; i<number; i++) {
            props.setProperty(name+"/cachefile/"+i.toString(), cacheFiles[i]);
        }

        number = searchItems.size();
        props.setProperty(name+"/searchItems", number.toString());

        for (Integer i=0; i<number; i++) {
            props.setProperty(name+"/searchItem/"+i.toString(), searchItems.get(i));
        }

        return saveProperties(props);
    }

    /**
     * Initialize the SavedSearch so it can be run. The Search Object returned can be used
     * to track the progress of the Search and to save the results.
     *
     * @return
     */
    public Search init() {
        initialized = true;
        search = new Search(cacheToMemory, cacheFiles, maxAge);
        return search;
    }

    /**
     * Run the Search and return the results. In most cases init() will be invoked before
     * run() but that is not mandatory.  The primary reason for invoking init() is to get
     * the Search Object necessary for tracking the progress and then saving the results to
     * disk.
     *
     * @return
     */
    public List<PlaylistEntry> run() {

        if (!initialized)
            init();

        if (search==null)
            return new ArrayList<PlaylistEntry>();

        if (typeOrWords.equalsIgnoreCase("type"))
            return search.getType(url, searchItems, limit, depth, maxPlaylists);
        else
            return search.getWords(url, searchItems, limit, depth, maxPlaylists);
    }

    /**
     * Remove the SavedSearch.
     *
     * @param Name
     * @return
     */
    public static boolean remove(String Name) {

        if (Name==null || Name.isEmpty())
            return false;

        Properties props = getProperties();

        props.remove("SavedSearch/"+Name);
        props.remove(Name+"/typeOrWords");
        props.remove(Name+"/cacheToMemory");
        props.remove(Name+"/maxAge");
        props.remove(Name+"/url");
        props.remove(Name+"/limit");
        props.remove(Name+"/depth");
        props.remove(Name+"/maxPlaylists");

        int number = stringToInt(props.getProperty(Name+"/cacheFiles"));

        for (Integer i=0; i<number; i++) {
            props.remove(Name+"/cachefile/"+i.toString());
        }

        props.remove(Name+"/cacheFiles");

        number = stringToInt(props.getProperty(Name+"/searchItems"));

        for (Integer i=0; i<number; i++) {
            props.remove(Name+"/searchItem/"+i.toString());
        }

        props.getProperty(Name+"/searchItems");

        return saveProperties(props);
    }

    public static List<String> getAllSavedSearch() {

        List<String> names = new ArrayList<String>();

        Properties props = getProperties();
        Enumeration keys = props.keys();

        if (keys==null)
            return names;

        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();

            if (key.startsWith("SavedSearch")) {
                names.add(props.getProperty(key));
            }
        }

        return names;
    }

    private static Properties getProperties() {

        Properties props = new Properties();

        try {
            File f = new File(SEARCH_PROPERTY_FILE);
            if (!f.exists())
                f.createNewFile();

            InputStream in = new FileInputStream(SEARCH_PROPERTY_FILE);
            props.load(in);
            //in.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SavedSearch.getProperties: Exception loading " + e.getMessage());
            return null;
        }

        return props;
    }

    private static boolean saveProperties(Properties props) {
        try {
            OutputStream out = new FileOutputStream(SEARCH_PROPERTY_FILE);
            props.store(out, "Navix Properties - Used to store Saved Searches.");
            //out.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "SavedSearch.saveProperties: Exception saving " + e.getMessage());
            return false;
        }

        return true;
    }

    private static int stringToInt(String s) {

        if (s==null || s.isEmpty())
            return 0;

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long stringToLong(String s) {

        if (s==null || s.isEmpty())
            return 0;

        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean stringToBoolean(String s) {
        if (s==null || s.isEmpty())
            return false;
        else
            return s.equalsIgnoreCase("true");
    }

    @Override
    public final String toString() {
        return  "Name=" + name + ", " +
                "TypeOrWords=" + typeOrWords + ", " +
                "CacheToMemory=" + cacheToMemory + ", " +
                "CacheFile=" + (cacheFiles==null || cacheFiles.length==0 ? "None" : cacheFiles[0]) + ", " +
                "MaxAge=" + maxAge + ", " +
                "RootURL=" + url + ", " +
                "Limit=" + limit + ", " +
                "Depth=" + depth + ", " +
                "MaxPlaylists=" + maxPlaylists + "," +
                "SearchItems=" + searchItems;
    }
}
