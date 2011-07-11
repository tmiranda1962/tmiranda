
package tmiranda.navix;

import java.util.*;
import sagex.api.*;

/**
 * Singleton class used to manage the Playlist memory cache.  Playlists typically have to be read
 * from the web and this takes time.  The methods in this class can be used to access and
 * populate a local cache which drastically improves performance.
 *
 * @author Tom Miranda.
 */
public class PlaylistCache {

    static final long serialVersionUID = NaviX.SERIAL_UID;

// FIXME - Check for maximum number of items in cache.

    /**
     * If true Playlist children and grandchildren will be cached.  If false only children
     * will be cached.
     */
    public static final String PROPERTY_CACHE_SECOND_LEVEL  = "navix/cache_second_level";
    
    /**
     * The minimum number of milliseconds between retries if the Playlist can't be loaded from
     * the web.  This is used to prevent constantly retrying dead links.
     */
    public static final String PROPERTY_MIN_RETRY_TIME      = "navix/cache_min_retry_time";

    /**
     * The maximum number of background threads that will be started to cache playlists.
     */
    public static final String PROPERTY_MAX_THREADS = "navix/max_cacher_threads";

    private static PlaylistCache instance = new PlaylistCache();

    // Key is url.
    private static Map<String, Playlist> cache = new HashMap<String, Playlist>();

    static List<String> alreadyRunning = new ArrayList<String>();

    // Private constructor for singleton.
    private PlaylistCache() {}

    /**
     * Get an instance of this class.
     * @return
     */
    public static PlaylistCache getInstance() {
        return instance;
    }

    /**
     * Return the number of entries in the cache.
     * @return
     */
    public int size() {
        return cache.size();
    }

    /**
     * Checks if the Playlist at the specified URL is already in the cache.
     * @param url
     * @return
     */
    public boolean contains(String url) {
        return cache.containsKey(url);
    }

    /**
     * Get the Playlist from the cache.
     * @param url
     * @return The Playlist or null if the Playlist is not in the cache.
     */
    public Playlist get(String url) {
        Playlist cachedPlaylist = cache.get(url);
        if (cachedPlaylist != null)
            cachedPlaylist.setCachedTime(new Date().getTime());
        return cachedPlaylist;
    }
    
    /**
     * Remove the Playlist from the cache.
     * @param url
     * @return true if the Playlist was removed, false otherwise.  Will return false if the
     * Playlist was not in the cache.
     */
    public boolean remove(String url) {
        return cache.remove(url) != null;
    }
    
    /**
     * Adds the specified Playlist to the cache.  If the cache already contains the Playlist
     * the old Playlist will be removed and the new Playlist added.
     * @param url
     * @param playlist
     * @return true if success, false otherwise.
     */
    public boolean add(Playlist playlist) {
        if (playlist==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.add: null Playlist.");
            return false;
        }
        
        String url = playlist.getUrl();
        
        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.add: null url.");
            return false;
        }
  
        if (cache.containsKey(url)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.add: Removing existing Playlist " + url);
            cache.remove(url);
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.add: Added Playlist " + url);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.add: Cache elements " + cache.size()+1);
        return cache.put(url, playlist) != null;          
    }

    /**
     * Load the Playlist specified by the url into the cache.  If the Playlist already exists
     * in the cache it will not be reloaded.
     * @param url
     */
    public void fetch(String url) {
        if (url==null || url.isEmpty() || cache.containsKey(url))
            return;

        if (alreadyRunning.contains(url)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.fetch: Thread already running for " + url);
            return;
        }

        String maxString = Configuration.GetProperty(PROPERTY_MAX_THREADS, "20");

        int maxThreads = 20;

        try {
            maxThreads = Integer.parseInt(maxString);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.fetch: Malformed number " + maxString);
            Configuration.SetProperty(PROPERTY_MAX_THREADS, "20");
            maxThreads = 20;
        }

        int numberRunning = NaviX.numberCacherThreads();

        if (numberRunning >= maxThreads) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.fetch: Maximum thread count reacher, aborting " + url);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.fetch: Fetching " + url);

        alreadyRunning.add(url);
        Cacher cacher = new Cacher(url);
        cacher.start();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.fetch: Number running " + numberRunning);

        return;
    }

    /**
     * Check if an attempt should be made to fetch the Playlist from the web.  We keep Playlists
     * that could not be fetched in the cache to avoid constantly trying to reach a web server
     * that is not responding.
     *
     * @param url
     * @return
     */
    public boolean shouldRetryNew(String url) {

        if (url==null || url.isEmpty())
            return true;

        Playlist playlist = cache.get(url);

        if (playlist==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.shouldRetryFetch: Playlist is not in cache " + url);
            return true;
        }

        // No need to retry if it's successfully loaded.
        if (playlist.hasLoaded())
            return false;

        Long timeDefault = 15 * 60 * 60 * 1000L;  // 15 minutes
        String timeString = Configuration.GetProperty(PROPERTY_MIN_RETRY_TIME, timeDefault.toString());

        try {
            timeDefault = Long.parseLong(timeString);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.shouldRetryFetch: Invalid default time " + timeString);
            Configuration.SetProperty(PROPERTY_MIN_RETRY_TIME, timeDefault.toString());
        }

        long now = new Date().getTime();

        return (now - playlist.getCachedTime()) > timeDefault;
    }

    /**
     * Remove the oldest items from the cache.
     *
     * @param numberToRemove The number to remove.
     * @return The number actually removed.
     */
    public int removeOldest(int numberToRemove) {

        if (numberToRemove <= 0)
            return 0;

        int numberRemoved = 0;

        for (Playlist p : sortByAge()) {
            if (remove(p.getUrl()))
                numberRemoved++;
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.removeOldest: Failed to remove " + p.getUrl());

            if (numberRemoved >= numberToRemove)
                return numberRemoved;
        }

        return numberRemoved;
    }

    /**
     * Shrink the cache to only contain "size" elements.  Playlists are removed oldest to newest.
     *
     * @param size
     * @return The number removed.
     */
    public int shrinkTo(int size) {
        if (size() <= size)
            return 0;

        int numberRemoved = 0;

        for (Playlist p : sortByAge()) {
            if (remove(p.getUrl()))
                numberRemoved++;
            else
                Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.shrinkTo: Failed to remove " + p.getUrl());

            if (size() <= size)
                return numberRemoved;
        }

        return numberRemoved;
    }

    /**
     * Remove elements from the cache that have not been recently accessed.
     *
     * @param age The amount of time since the last access.
     * @return The number removed.
     */
    public int removeOlderThan(long age) {
        if (age <= 0)
            return 0;

        int numberRemoved = 0;
        long now = new Date().getTime();

        for (Playlist p : cache.values()) {

            long playlistAge = now - p.getCachedTime();

            if (playlistAge > age) {
                if (remove(p.getUrl()))
                    numberRemoved++;
                else
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "PlaylistCache.removeOlderTan: Failed to remove " + p.getUrl());
            }
        }

        return numberRemoved;
    }

    /**
     * Returns a List of all the Playlists in the cache sorted by oldest accessed time to newest.
     * @return
     */
    public List<Playlist> sortByAge() {
        List<Playlist> allPlaylists = new ArrayList<Playlist>();
        allPlaylists.addAll(cache.values());
        Collections.sort(allPlaylists, new PlaylistCacheAge());
        Collections.reverse(allPlaylists);
        return allPlaylists;
    }

    /**
     * Controls if the grandchildren of the Playlist being processed will be cached or not.
     * If set to true the caching of the grandchildren will be run in a thread set to
     * minimum priority to help reduce the system load.
     *
     * @param value true to enable the caching of grandchildren, false to disable the
     * caching of grandchildren.
     */
    public static void setCacheSecondLevel(Boolean value) {
        Configuration.SetProperty(PROPERTY_CACHE_SECOND_LEVEL, value.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PlaylistCache.setCacheSecondLevel: Second level caching set to " + value.toString());
    }

    public static boolean isCacheSecondLevel() {
        String value = Configuration.GetProperty(PROPERTY_CACHE_SECOND_LEVEL, "true");
        return value.toLowerCase().equals("true");
    }

    /**
     * For debugging purposes.  Will print useful information about the cache to the logfile.
     *
     * @param verbose If set to true then the output will contain more detail.
     */
    private void dump(boolean verbose) {

        long now = new Date().getTime();

        System.out.println("PlaylistCache.dumpToLogfile: Start.");

        System.out.println("**Number of entries = "+size());

        for (Playlist p : cache.values()) {
            System.out.println("******************************************");
            System.out.println("**Title and url = " + p.getTitle() + " - " + p.getUrl());
            if (verbose) {
                System.out.println("**Description = " + p.getDescription());
                System.out.println("**Version = " + p.getVersion());
                System.out.println("**Age (minutes) = " + (now-p.getCachedTime())/1000/60);
            }
        }

        System.out.println("PlaylistCache.dumpToLogfile: End.");
    }

    /**
     * For debugging purposes.  Will print useful information about the cache to the logfile.
     *
     * @param verbose If set to true then the output will contain more detail.
     */
    public static void dumpToLogfile(boolean verbose) {
        PlaylistCache.getInstance().dump(verbose);
    }
}

/**
 * Compare the cached age of two Playlists.  The cached age is the last time the Playlist
 * was referenced.
 *
 * @author tom Miranda.
 */
class PlaylistCacheAge implements Comparator {
    public int compare(Object p1, Object p2) {

        if (p1==null || p2==null)
            return 0;

        if (!(p1 instanceof Playlist) || !(p2 instanceof Playlist))
            return 0;

        Playlist play1 = (Playlist)p1;
        Playlist play2 = (Playlist)p2;
        long t1 = play1.getCachedTime();
        long t2 = play2.getCachedTime();

        if (t1==t2)
            return 0;
        else if (t1<t2)
            return -1;
        else
            return 1;
    }
}
