
package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import sagex.api.*;


/**
 * This class controls access to the Podcast database. It is created on the server and on each
 * client.
 *
 * @author Tom Miranda.
 */
public class DataStore {
    
    // This is the name of the file that will store the serialized Podcast objects that are Favorites.
    public final static String FavoriteDB          = "PodcastRecorderFavoritePodcasts.DB";
    public final static String FavoriteDBBackup    = "PodcastRecorderFavoritePodcasts.bak";

    // Cache the Podcasts in memory for faster access. Date is the last time the cache was updated. It is used
    // to determine if the cache (in class API) on SageClients needs to be updated.
    //private static boolean                  remoteCacheIsDirty = true;
    //private static List<Podcast>            PodcastCache = new ArrayList<Podcast>();
    private static Map<PodcastKey, Podcast> podcastMap = new HashMap<PodcastKey, Podcast>();
    private static Map<PodcastKey, Boolean> podcastLock = new HashMap<PodcastKey, Boolean>();
    private static Map<PodcastKey, Long>    podcastLockTime = new HashMap<PodcastKey, Long>();
    private static Date                     cacheDate = new Date();

    /**
     * Constructor.  Loads all of the Favorite Podcasts into memory.
     */
    DataStore() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "DataStore: Starting.");
        //PodcastCache = new ArrayList<Podcast>();
        podcastMap = new HashMap<PodcastKey, Podcast>();
        podcastLock = new HashMap<PodcastKey, Boolean>();
        podcastLockTime = new HashMap<PodcastKey, Long>();
        //remoteCacheIsDirty = true;
        cacheDate = new Date();
        readPodcastsFromDisk();
    }

    static void stop() {
        podcastMap.clear();
        podcastMap = null;
        podcastLock.clear();
        podcastLock = null;
        podcastLockTime.clear();
        podcastLockTime = null;
        cacheDate = new Date();
        //PodcastCache.clear();
        //PodcastCache = null;
    }

    /**
     * This method should be used by the API class to get a List of the Favorite Podcasts. The API
     * class passes in the Date if its Podcast cache so it can be compared to the Daet of the
     * cache stored in this class.  If the cache that the API class already has is up to date
     * this method will return null.  If it's not up to date this method will return a List
     * of the Podcasts.
     *
     * @param clientCacheDate The Date the the API class cache was known to be good.
     * @return null if the API class has the latest Podcast List, a List of the latest Podcasts if not.
     */
    public synchronized static List<Podcast> readFavoritePodcasts(Date clientCacheDate) {
        if (clientCacheDate.compareTo(cacheDate) < 0) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DataStore.readFavoritePodcasts: Date cache needs to be updated.");
            List<Podcast> podcastList = new ArrayList<Podcast>();
            podcastList.addAll(podcastMap.values());
            //remoteCacheIsDirty = false;
            return podcastList;
        }

        return null;
    }

    /**
     * Reads the Favorite Podcasts from the database on disk into local memory. This method
     * should never be invoked from the API class, it should only be invoked from the instance
     * running on the Sage server.
     *
     * @return A List of the Favorite Podcasts. It also updates the local cache and resets all
     * Podcast locks.
     */
    public synchronized static List<Podcast> readPodcastsFromDisk() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DataStore.readFavoritePodcasts: Reading Podcasts.");

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.readFavoritePodcasts: Error creating new FavoriteDB file.");
                return null;
            }
        }

        // Clear the cache to reclaim the memory.
        //PodcastCache.clear();
        //PodcastCache = null;
        podcastMap.clear();
        podcastLock.clear();
        //remoteCacheIsDirty = true;
        cacheDate = new Date();

        // Create the List to hold the elements.
        List<Podcast> favoritePodcasts = new ArrayList<Podcast>();

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(FavoriteDB);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.readFavoritePodcasts: Error opening FileInputStream.");
            //PodcastCache = favoritePodcasts;
            //remoteCacheIsDirty = true;
            cacheDate = new Date();
            return null;
        }

        ObjectInputStream objectStream;

        try {
            objectStream = new ObjectInputStream(fileStream);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DataStore.readFavoritePodcasts: No Podcasts to read.");
            try {fileStream.close();} catch (Exception ex) {}
            //remoteCacheIsDirty = false;
            //PodcastCache = favoritePodcasts;
            cacheDate = new Date();
            return favoritePodcasts;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.readFavoritePodcasts: Exception " + e.getMessage());
            try {fileStream.close();} catch (Exception ex) {}
            //remoteCacheIsDirty = true;
            //PodcastCache = favoritePodcasts;
            cacheDate = new Date();
            return favoritePodcasts;
        }

        Object p = null;
        Podcast podcast = null;
        boolean needsInitialization = false;

        try {
            while ((p=objectStream.readObject()) != null) {
                podcast = new Podcast((PodcastData)p);
                if (!favoritePodcasts.add(podcast))
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "DataStore.readFavoritePodcasts: Element already in set.");
        }

        } catch(EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "DataStore.readFavoritePodcasts: Complete.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "DataStore.readFavoritePodcasts: Objects in DB are invalid.");
            needsInitialization = true;
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.readFavoritePodcasts: Exception " + e.getMessage());
        }

        try {
            objectStream.close();
            fileStream.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.readFavoritePodcasts: Exception closing. " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "DataStore.readFavoritePodcasts: found " + favoritePodcasts.size());

        if (needsInitialization) {
            initializeDataBase();
        } else {

            // Update the local cache.
            for (Podcast pcast : favoritePodcasts) {
                PodcastKey key = pcast.getKey();
                podcastMap.put(key, pcast);
                podcastLock.put(key, Boolean.FALSE);
            }
            cacheDate = new Date();
            //remoteCacheIsDirty = true;
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DataStore.readFavoritePodcasts: Updated Podcast cache.");
        }

        return favoritePodcasts;
    }

    private static void initializeDataBase() {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "inializeDataBase: Initializing " + FavoriteDB);

        // Create the database file if it does not exist.
        File file = new File(FavoriteDB);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.inializeDataBase: Error creating new FavoriteDB file.");
            }

            return;
        }

        // It exists, but it has invalid Objects in it, so delete it.
        if (!file.delete()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.inializeDataBase: Error deleting file.");
            return;
        }

    }

    /**
     * Writes the Podcast cache to the disk
     * @return
     */
    private static boolean writePodcastsToDisk() {

        // Backup the current database file and delete the original.
        SageUtil.RenameFile(FavoriteDB, FavoriteDBBackup);

        File file = new File(FavoriteDB);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writeFavoritePodcasts: Error creating new FavoriteDB file.");
                return false;
            }
        }

        // Write the new database file.
        try {
            FileOutputStream fileStream = new FileOutputStream(FavoriteDB);
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);

            // Write all Podcasts to disk.
            for (Podcast p : podcastMap.values()) {
                PodcastData pData = new PodcastData(p);
                objectStream.writeObject(pData);
                //podcastLock.put(p.getKey(), Boolean.FALSE);
            }

            objectStream.close();
            fileStream.close();

        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Podcast.writeFavoritePodcasts: Exception " + e.getMessage());
            return false;
        }

        return true;
    }

    public synchronized static List<PodcastKey> getAllPodcastKeys() {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Podcast.getAllPodcastKeys: Looking.");

        List<Podcast> allPodcasts = getAllPodcasts();

        List<PodcastKey> keyList = new ArrayList<PodcastKey>();

        if (allPodcasts.isEmpty())
            return keyList;

        for (Podcast p : allPodcasts) {
            PodcastKey key = new PodcastKey(p.getOnlineVideoType(), p.getOnlineVideoItem(), p.getFeedContext());
            if (!keyList.add(key))
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.getAllPodcastKeys: Failed to add key.");
        }

        Log.getInstance().write(Log.LOGLEVEL_ALL, "DataStore.getAllPodcastKeys: Found " + keyList.size());
        return keyList;
    }

    /**
     * Get a List of all the Podcasts in the local cache.
     *
     * @return A List of Podcasts in the local cache.
     */
    public synchronized static List<Podcast> getAllPodcasts() {
        List<Podcast> podcasts = new ArrayList<Podcast>();
        podcasts.addAll(podcastMap.values());
        return podcasts;
    }

    public static Podcast getPodcastForUpdate(Podcast podcast) {
        if (podcast==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.getPodcastForUpdate: null podcast.");
            return null;
        }

        return getPodcastForUpdate(podcast.getKey());
    }

    public synchronized static Podcast getPodcastForUpdate(PodcastKey Key) {
        if (Key==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.getPodcastForUpdate: null Key.");
            return null;
        }

        Podcast podcast = podcastMap.get(Key);

        if (podcast == null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.getPodcastForUpdate: Podcast is not in Map");
            return null;
        }

        if (isPodcastLocked(Key)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "DataStore.getPodcastForUpdate: Podcast is already locked " + podcast.getShowTitle());
            return null;
        }

        podcastLock.put(Key, Boolean.TRUE);
        podcastLockTime.put(Key, Utility.Time());
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DataStore.getPodcastForUpdate: Locked Podcast " + podcast.getShowTitle());
        return podcast;
    }

    public static Podcast getPodcastForUpdate(String OVT, String OVI) {

        if (SageUtil.isNull(OVI, OVI))
            return null;

        for (Podcast podcast : getAllPodcasts()) {
            if (podcast.getOnlineVideoType().equals(OVT) && podcast.getOnlineVideoItem().equals(OVI)) {
                return getPodcastForUpdate(podcast);
            }
        }

        return null;
    }

    public static Podcast getPodcastForUpdate(String OVT, String OVI, long waitTime) {

        if (SageUtil.isNull(OVI, OVI))
            return null;

        for (Podcast podcast : getAllPodcasts()) {
            if (podcast.getOnlineVideoType().equals(OVT) && podcast.getOnlineVideoItem().equals(OVI)) {
                return getPodcastForUpdate(podcast, waitTime);
            }
        }

        return null;
    }

    public static Podcast getPodcastForUpdate(Podcast podcast, long waitTime) {
        if (podcast==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "DataStore.getPodcastForUpdate: null podcast.");
            return null;
        }

        for (long i=0; i<waitTime; i++) {
            Podcast newPodcast = getPodcastForUpdate(podcast);
            if (newPodcast!=null)
                return newPodcast;
            else
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    return null;
                }
        }

        return null;
    }

    public static Podcast getPodcast(PodcastKey Key) {
        if (Key==null)
            return null;
        else
            return podcastMap.get(Key);
    }

    public static Podcast getPodcast(Podcast podcast) {
        if (podcast==null)
            return null;
        else
            return getPodcast(podcast.getKey());
    }

    public static Podcast getPodcast(String OVT, String OVI) {
        if (SageUtil.isNull(OVI, OVI))
            return null;

        for (Podcast podcast : getAllPodcasts()) {
            if (podcast.getOnlineVideoType().equals(OVT) && podcast.getOnlineVideoItem().equals(OVI)) {
                return getPodcast(podcast);
            }
        }

        return null;
    }

    public synchronized static boolean addPodcast(Podcast podcast) {

        PodcastKey key = podcast.getKey();

        podcastMap.put(key, podcast);
        podcastLock.put(key, Boolean.FALSE);
        cacheDate = new Date();
        return writePodcastsToDisk();
    }

    public synchronized static boolean updatePodcast(Podcast podcast) {

        PodcastKey key = podcast.getKey();

        podcastMap.put(key, podcast);
        podcastLock.put(key, Boolean.FALSE);
        cacheDate = new Date();
        return writePodcastsToDisk();
    }

    public synchronized static boolean removePodcast(Podcast podcast) {

        PodcastKey key = podcast.getKey();

        podcastMap.remove(key);
        podcastLock.remove(key);
        cacheDate = new Date();
        return writePodcastsToDisk();
    }

    /**
     * Checks to see if a Podcast has been locked.
     * @param Key
     * @return true if the Podcast is locked, false otherwise.
     */
    public static boolean isPodcastLocked(PodcastKey Key) {

        if (Key==null)
            return true;

        Boolean locked = podcastLock.get(Key);
        return (locked==null ? false : locked);
    }

    public static boolean isPodcastLocked(Podcast podcast) {
        if (podcast==null)
            return true;
        else
            return isPodcastLocked(podcast.getKey());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
