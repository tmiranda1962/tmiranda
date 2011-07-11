
package tmiranda.navix;

import java.util.*;
import java.io.*;
import sagex.api.*;

/**
 * Search for elements and keywords.
 *
 * @author Tom Miranda.
 */
public class Search {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String  CACHE_ENDING            = ".navix.cache";
    public static final String  PLAYLIST_ENDING         = ".navix.playlist";
    public static final String  SAVED_URL_PREFIX        = "disk:";
    public static final String  PROPERTY_CACHE_MAX_AGE  = "navix/disk_cache_max_age";

    private boolean             stop                = false;
    private boolean             cacheToMemory       = false;
    private String[]            cacheFiles          = null;
    private long                maxAge              = -1;
    private int                 totalPlaylists      = 0;
    private int                 totalElements       = 0;
    private List<String>        processedPlaylists  = new ArrayList<String>();
    private List<PlaylistEntry> resultsSoFar        = new ArrayList<PlaylistEntry>();

    /**
     * Constructor.
     *
     * @param cacheToMemory If set to true the Playlists processed will be added to the memory
     * cache.  This should only be used if the search is relatively limited.
     * @param cacheFiles The Playlists that are processed during the search will be saved in
     * the first file in the array. All cacheFiles will be considered when looking for
     * results.
     * @param maxAge The maximum age (in milliseconds) of cached Playlists cached in
     * the cacheFile to consider. If set to 0 all Playlists will be considered.
     */
    public Search(boolean cacheToMemory, String[] cacheFiles, long maxAge) {
        this.cacheToMemory = cacheToMemory;
        if (cacheFiles==null) {
            this.cacheFiles = new String[0];
        } else {
            this.cacheFiles = new String[cacheFiles.length];
            for (int i=0; i<cacheFiles.length; i++)
                this.cacheFiles[i] = convertToNavixCacheName(cacheFiles[i]);
        }
        this.cacheFiles = cacheFiles==null ? new String[0] : cacheFiles;
        this.maxAge = maxAge < 0 ? 0 : maxAge;
    }

    public Search(boolean cacheToMemory, List<String> cacheFiles, long maxAge) {
        this(cacheToMemory, cacheFiles.toArray(new String[cacheFiles.size()]), maxAge);
    }

    /**
     * Constructor.
     *
     * @param cacheToMemory If set to true the Playlists processed will be added to the memory
     * cache.  This should only be used if the search is relatively limited.
     * @param cacheFile The Playlists that are processed during the search will be saved
     * to this disk file.  The cacheFile can then be used in subsequent searches which should
     * result in improved performance. The cacheFile will also be updated with new Playlists
     * that are processed.
     * @param maxAge The maximum age (in milliseconds) of cached Playlists cached in
     * the cacheFile to consider. If set to 0 all Playlists will be considered.
     */
    public Search(boolean cacheToMemory, String cacheFile, long maxAge) {
        this.cacheToMemory = cacheToMemory;
        this.maxAge = maxAge < 0 ? 0 : maxAge;
        if (cacheFile==null)
            cacheFiles = new String[0];
        else {
            cacheFiles = new String[1];
            cacheFiles[0] = convertToNavixCacheName(cacheFile);
        }
    }

    /**
     * Constructor.
     *
     * @param cacheToMemory If set to true the Playlists processed will be added to the memory
     * cache.  This should only be used if the search is relatively limited.
     */
    public Search(boolean cacheToMemory) {
        this.cacheToMemory = cacheToMemory;
        this.cacheFiles = new String[0];
        this.maxAge = 0;
    }

    /**
     * Constructor.
     */
    public Search() {
        this.cacheToMemory = false;
        this.cacheFiles = new String[0];
        this.maxAge = 0;
    }

    /**
     * Adds the proper extension to files that will be used as cache files.  This is done
     * as a convenience so the STV does not have to worry about it.
     * @param fileName
     * @return
     */
    private static String convertToNavixCacheName(String fileName) {
        if (fileName==null || fileName.isEmpty())
            return "NavixDefault" + CACHE_ENDING;
        else
            return fileName.endsWith(CACHE_ENDING) ? fileName : fileName + CACHE_ENDING;
    }

    /**
     * Adds the proper extension to files that will be used as stored files.  This is done
     * as a convenience so the STV does not have to worry about it.
     * @param fileName
     * @return
     */
    public static String convertToNavixPlaylistName(String fileName) {
        if (fileName==null || fileName.isEmpty())
            return "NavixDefault" + PLAYLIST_ENDING;
        else
            return fileName.endsWith(PLAYLIST_ENDING) ? fileName : fileName + PLAYLIST_ENDING;
    }

    public List<PlaylistEntry> getType(String url, String type, int limit, int depth, int maxPlaylists) {
        List<String> theList = new ArrayList<String>();
        theList.add(type);
        return getType(url, theList, limit, depth, maxPlaylists);
    }

    /**
     * Search for Playlists of the specified type.
     *
     * @param url The URL of the root Playlist.
     * @param type A List containing the types of Playlist to search for.
     * @param limit The maximum number of PlaylistEntry to return. -1 for unlimited.
     * @param depth The maximum depth to travel down any one branch of the Playlist tree. -1 for unlimited.
     * @param maxPlaylists The maximum number of Playlists to inspect. Playlists that fail
     * to load (because their host is unreachable) are counted. -1 for unlimited.
     * @return
     */
    public List<PlaylistEntry> getType(String url, List<String> type, int limit, int depth, int maxPlaylists) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: url = " + url);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: type = " + type);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: limit = " + limit);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: depth = " + depth);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Playlists searched = " + totalPlaylists);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Elements found = " + resultsSoFar.size());

        List<PlaylistEntry> results = new ArrayList<PlaylistEntry>();

        if (stop) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Manually cancelled.");
            return results;
        }

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: null URL.");
            return results;
        }

        if (type==null || type.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: null type.");
            return results;
        }

        if (limit!=-1 && resultsSoFar.size() >= limit) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Results limit reached.");
            return results;
        }

        if (depth==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Depth reached.");
            return results;
        }

        if (maxPlaylists != -1 && totalPlaylists >= maxPlaylists) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Playlist limit reached.");
            return results;
        }

        if (processedPlaylists.contains(url)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getType: Playlist already searched " + url);
            return results;
        }

        processedPlaylists.add(url);
        totalPlaylists++;

        Playlist playlist = getFromMemoryFileOrWeb(url);

        // Make sure the url was reachable.
        if (playlist==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getType: Playlist failed to load, skipping " + url);
            return results;
        }

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            String thisType = entry.getType();

            // Add to the List if this is the correct type and we are below the limit.
            if (type.contains(thisType) && (limit==-1 || resultsSoFar.size() < limit)) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Adding " + entry.getName());
                results.add(entry);
                resultsSoFar.add(entry);
            }

            // Recurse if this is a playlist and we're not over the limit.
            if ((entry.isPlaylist() || entry.isPlx()) && (limit==-1 || resultsSoFar.size() < limit)) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Recursing. Totals " + totalPlaylists + ":" + totalElements);
                results.addAll(getType(entry.getUrl(), type, limit, depth-1, maxPlaylists));
            }

        }

        return results;
    }

    public List<PlaylistEntry> getWords(String url, List<String> words, int limit, int depth, int maxPlaylists) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: url = " + url);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: limit = " + limit);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: depth = " + depth);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: Playlists searched = " + totalPlaylists);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: Elements found = " + resultsSoFar.size());

        List<PlaylistEntry> results = new ArrayList<PlaylistEntry>();

        if (stop) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Manually cancelled.");
            return results;
        }

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: null URL.");
            return results;
        }

        if (words==null || words.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: null words.");
            return results;
        }

        if (limit!=-1 && resultsSoFar.size() >= limit) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Results limit reached.");
            return results;
        }

        if (depth==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Depth reached.");
            return results;
        }

        if (maxPlaylists != -1 && totalPlaylists >= maxPlaylists) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Playlist limit reached.");
            return results;
        }

        if (processedPlaylists.contains(url)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getWords: Playlist already searched " + url);
            return results;
        }

        processedPlaylists.add(url);
        totalPlaylists++;

        Playlist playlist = getFromMemoryFileOrWeb(url);

        // Make sure the url was reachable.
        if (playlist==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getWords: Playlist failed to load, skipping " + url);
            return results;
        }

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            // Create a list of all the words found in the various fields.
            List<String> foundWords = new ArrayList<String>();
            if (entry.getName() != null) foundWords.addAll(phraseToWords(entry.getName().toLowerCase()));
            if (entry.getTitle() != null) foundWords.addAll(phraseToWords(entry.getTitle().toLowerCase(Locale.FRENCH)));
            if (entry.getDescription() != null) foundWords.addAll(phraseToWords(entry.getDescription().toLowerCase()));

            // See if any match the words we are looking for.
            for (String word : words) {
                if (foundWords.contains(word.toLowerCase()) && resultsSoFar.size() < limit) {
                    if (!results.contains(entry)) {
                        results.add(entry);
                        resultsSoFar.add(entry);
                    }
                }
            }

            // Recurse if this is a playlist and we're not over the limit.
            if ((entry.isPlaylist() || entry.isPlx()) && resultsSoFar.size() < limit) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getWords: Recursing. Totals " + totalPlaylists + ":" + totalElements);
                results.addAll(getWords(entry.getUrl(), words, limit, depth-1, maxPlaylists));
            }

        }

        return results;
    }

    /**
     * Fetches the Playlist specified by the url from either the memory cache, the disk
     * cache or from the web, in that order.  We go through this grief to improve performance
     * and to make sure we don't keep trying to fetch dead links from the web.
     * @param url
     * @return
     */
    private Playlist getFromMemoryFileOrWeb(String url) {

        Playlist playlist;

        // Try to retrieve from memory cache.
        if (Playlist.isInCache(url)) {
            playlist = Playlist.retrieveFromCache(url);

            // If it was loaded (reachable), return it.
            if (playlist!=null && playlist.hasLoaded()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist retrieved from memory cache.");
                return playlist;
            }

            // It was unreachable.  If it's not time to try again return failure.
            if (!Playlist.shouldRetryNew(url)) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist is in memory but unreachable.");
                return null;
            }
        }

        // It's either in memory cache and unreachable (but time to try again) or it's
        // not in memory at all.

        playlist = null;

        // See if it's one of the cache files.
        for (String cacheFile : cacheFiles) {
            if (getUrlsInDiskCache(cacheFile).contains(url)) {
                Playlist p = getFromDiskCache(url, cacheFile);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist in disk cache.");
                if (p!=null && (maxAge==0 || ((new Date().getTime()-p.getDiskTime())<maxAge))) {

                    // We found it.  If it's loaded just return it. If it wasn't loaded
                    // continue on and try to fetch it from the web.
                    if (p.hasLoaded()) {
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist retrieved from disk cache.");
                        return p;
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist in disk cache but not loaded.");
                        break;
                    }
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist in disk cache is too old.");
                }
            }
        }

        // It's either not in memory or disk cache, or it is but it's invalid. In any case,
        // try to fetch it from the web.

        playlist = new Playlist(url, cacheToMemory);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getFromMemoryFileOrWeb: Playlist retrieved from web.");

        // Save it to disk cache.
        if (cacheFiles.length>0 && !addToDiskCache(playlist, cacheFiles[0])) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getFromMemoryFileOrWeb: Failed to save to disk cache.");
        }

        // Make sure the url was reachable.
        if (!playlist.hasLoaded()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getFromMemoryFileOrWeb: Playlist failed to load, skipping " + url);
            return null;
        }

        return playlist;
    }

    /**
     * Converts a phrase (words separated by spaces) into a List of individual words.
     * This needs to be refined because words with punctuation will not be matched.
     * @param phrase
     * @return
     */
    private static List<String> phraseToWords(String phrase) {
        List<String> words = new ArrayList<String>();

        if (phrase==null || phrase.isEmpty())
            return words;

        String[] parts = phrase.split(" ");
        return Arrays.asList(parts);
    }

    /**
     * Adds the specified Playlist to the file.  If the Playlist already exists in the
     * file it will be deleted and the new Playlist will replace it.
     *
     * @param playlist
     * @param fileName
     * @return true if success, false otherwise.
     */
    public synchronized boolean addToDiskCache(Playlist playlist, String fileName) {

        if (playlist==null || fileName==null || fileName.isEmpty())
            return false;

        // See if the cache file exists.
        File f = new File(fileName);

        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.addToDiskCache: Error creating input file " + e.getMessage());
                return false;
            }

        }

        f = null;

        // Create the input stream.
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error opening file input stream " + e.getMessage());
            return false;
        }

        try {
            ois = new ObjectInputStream(fis);
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist.addToDiskCache: File is empty.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error opening object input stream " + e.getMessage());
            try {fis.close();} catch (Exception e1) {}
            return false;
        }

        // Delete the .tmp file if it already exists.
        File tempFile = new File(fileName + ".tmp");
        if (tempFile.exists()) {
            if (!tempFile.delete()) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error deleting existing tempfile " + fileName + ".tmp");
                if (ois!=null)
                    try {ois.close();} catch (Exception e1) {}
                if (fis!=null)
                    try {fis.close();} catch (Exception e2) {}
                return false;
            }
        }

        tempFile = null;

        // Create the output stream.
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(fileName + ".tmp");
            oos = new ObjectOutputStream(fos);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error opening output file " + e.getMessage());
            if (ois!=null)
                try {ois.close();} catch (Exception e1) {}
            if (fis!=null)
                try {fis.close();} catch (Exception e2) {}
            if (oos!=null)
                try {oos.close();} catch (Exception e3) {}
            if (fos!=null)
                try {fos.close();} catch (Exception e4) {}
            return false;
        }

        // Calculate the maximum age.  Playlists older then this will be eliminated from
        // the cache.
        Long defaultMaxAge = 7 * 24 * 60 * 60 * 1000L;   // A week.
        String maxAgeString = Configuration.GetProperty(PROPERTY_CACHE_MAX_AGE, defaultMaxAge.toString());
        Long maxDiskAge = 0L;
        long now = new Date().getTime();
        try {
            maxDiskAge = Long.parseLong(maxAgeString);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.addToDiskCache: Invalid max age " + maxAgeString);
            maxDiskAge = 0L;
        }

        // Read through the file and replace the Playlist if we find it or add it to the end.
        boolean inserted = false;
        Playlist p;

        try {
            while ((p = (Playlist)ois.readObject()) != null) {

                if (p.getUrl().equals(playlist.getUrl())) {
                    playlist.setDiskTime(new Date().getTime());
                    oos.writeObject(playlist);
                    inserted = true;
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.addToDiskCache: Playlist updated " + playlist.getUrl());
                } else {
                    if ((p.getDiskTime() + maxDiskAge) > now) {
                        oos.writeObject(p);
                    } else {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.addToDiskCache: Old Playlist removed " + playlist.getUrl());
                    }
                }
            }

        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.addToDiskCache: End of DB reached.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Objects in DB are invalid " + ic.getMessage());
        } catch (NotSerializableException nse) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.addToDiskCache: NotSerializableException updating file " + nse.getMessage());
        } catch (ObjectStreamException ose) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.addToDiskCache: ObjectStreamException updating file " + ose.getMessage());
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.addToDiskCache: Exception updating file " + e.getMessage());
        }

        // If it wasn't already insert it, add it to the end.
        if (!inserted) {

            playlist.setDiskTime(new Date().getTime());
            try {
                oos.writeObject(playlist);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.addToDiskCache: Playlist added " + playlist.getUrl());
            } catch (IOException e) {
                Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error adding to end of file.");
            }
            
        }

        // Close all of the files.
        if (ois!=null)
            try {ois.close();} catch (Exception e1) {}
        if (fis!=null)
            try {fis.close();} catch (Exception e2) {}
        if (oos!=null)
            try {oos.close();} catch (Exception e3) {}
        if (fos!=null)
            try {fos.close();} catch (Exception e4) {}

        // Delete the original file and rename the new one.
        f = new File(fileName);

        if (!f.delete()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error deleting original file " + fileName);
            return false;
        }

        // Rename the .tmp file to the actual file name.
        tempFile = new File(fileName + ".tmp");
        f = new File(fileName);

        if (!tempFile.renameTo(f)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.addToDiskCache: Error renaming tempfile " + fileName);
            return false;
        }

        return true;
    }

    /**
     * Retrieve the Playlist with the specified URL from the file specified.
     *
     * @param url
     * @param fileName
     * @return true if success, false otherwise.
     */
    public static synchronized Playlist getFromDiskCache(String url, String fileName) {

        if (url==null || url.isEmpty()|| fileName==null || fileName.isEmpty())
            return null;

        // See if the cache file exists.
        File f = new File(fileName);

        if (!f.exists()) {
            return null;
        }

        // Create the input stream.
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(f);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.getFromDiskCache: Error opening file input stream " + e.getMessage());
            return null;
        }

        try {
            ois = new ObjectInputStream(fis);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.getFromDiskCache: Error opening object input stream " + e.getMessage());
            try {fis.close();} catch (Exception e1) {}
            return null;
        }

        Playlist p;

        try {
            while ((p = (Playlist)ois.readObject()) != null) {
                if (p.getUrl().equals(url) || url.equals("FIRST")) {
                    if (ois!=null)
                        try {ois.close();} catch (Exception e1) {}
                    if (fis!=null)
                        try {fis.close();} catch (Exception e2) {}
System.out.println(":::getFromDiskCache elements " + p.getElements().size());
                    return p;
                }
            }

        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.getFromDiskCache: End of file reached.");
        } catch (InvalidClassException ic) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.getFromDiskCache: Objects in file are invalid.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.getFromDiskCache: Error updating file " + e.getMessage());
            if (ois!=null)
                try {ois.close();} catch (Exception e1) {}
            if (fis!=null)
                try {fis.close();} catch (Exception e2) {}
            return null;
        }

        if (ois!=null)
            try {ois.close();} catch (Exception e1) {}
        if (fis!=null)
            try {fis.close();} catch (Exception e2) {}

        return null;
    }

    public static synchronized Playlist getFromDiskCache(String fileName) {
        return getFromDiskCache("FIRST", fileName);
    }

    public static synchronized List<String> getUrlsInDiskCache(String fileName) {
        List<String> urlList = new ArrayList<String>();

        // See if the cache file exists.
        File f = new File(fileName);

        if (!f.exists()) {
            return urlList;
        }

        // Create the input stream.
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(f);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.getUrlsInCache: Error opening file input stream " + e.getMessage());
            return urlList;
        }

        try {
            ois = new ObjectInputStream(fis);
        } catch (IOException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.getUrlsInCache: Error opening object input stream " + e.getMessage());
            try {fis.close();} catch (Exception e1) {}
            return urlList;
        }

        Playlist p;

        try {
            while ((p = (Playlist)ois.readObject()) != null) {
                urlList.add(p.getUrl());
            }
        } catch (EOFException eof) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Playlist.getUrlsInCache: End of file reached.");
        } catch (InvalidClassException ic) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Playlist.getUrlsInCache: Objects in file are invalid.");
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Playlist.getUrlsInCache: Error updating list " + e.getMessage());
            if (ois!=null)
                try {ois.close();} catch (Exception e1) {}
            if (fis!=null)
                try {fis.close();} catch (Exception e2) {}
            return null;
        }

        if (ois!=null)
            try {ois.close();} catch (Exception e1) {}
        if (fis!=null)
            try {fis.close();} catch (Exception e2) {}

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Playlist.getUrlsInCache: Found URLs " + urlList.size());
        return urlList;
    }

    /**
     * Saves the search results as a Playlist. Other than the url and PlaylistElements fields
     * the Playlist will contain default values.  The caller can use the various Playlist setter methods
     * to add data to the Playlist and then invoke the savePlaylist method to save the
     * updated Playlist to the Search cache.
     *
     * If this method is invoked before the search method has completed the partial results
     * will be written to the Search cache.
     *
     * @param fileName The name of the file to save the Playlist into.  if the file exists this
     * Playlist will be appended to it.  If the Playlist already exists in the file it will
     * be replaced.
     *
     * @param append If set to true the results will be appended to the file, if the file
     * exists.  Note that in most cases this parameter should be set to false.
     *
     * @return A URL that can be used to retrieve the Playlist from the Search cache.  This URL
     * will not be suitable for retrieving the Playlist from the web.
     */
    public String saveResultsAsPlaylist(String fileName, boolean append) {

        if (!append) {
            File f = new File(fileName);
            if (f.exists()) {
                if (!f.delete()) {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.saveResultsAsPlaylist: Failed to delete existing file " + fileName);
                } else {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.saveResultsAsPlaylist: Deleted existing file " + fileName);
                }
            }
        }

        fileName = convertToNavixPlaylistName(fileName);

        Playlist playlist = new Playlist();

        playlist.setHasLoaded(true);
        playlist.setUrl(SAVED_URL_PREFIX + fileName + ":" + UUID.randomUUID().toString());
        playlist.setElements(resultsSoFar);
        playlist.setVersion(resultsSoFar.size() + " items");
        playlist.setTitle(fileName);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.saveResultsAsPlaylist: Saving " + fileName + " url " + playlist.getUrl() + " items " + resultsSoFar.size());
        if (!addToDiskCache(playlist, fileName)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.saveResultsAsPlaylist: Failed to save file.");
        }

        return playlist.getUrl();
    }

    public List<PlaylistEntry> getResultsSoFar() {
        return resultsSoFar;
    }

    public List<String> getProcessedPlaylists() {
        return processedPlaylists;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public int getTotalPlaylists() {
        return totalPlaylists;
    }

    /**
     * Stops the current search after the current Playlist is processed.
     */
    public void cancel() {
        stop = true;
    }

    /**
     * getUrl() returns disk:fileName:URL.  The disk:fileName: must be stripped off to get the
     * actual URL that is used in the Disk cache file.
     *
     * @param diskURL
     * @return
     */
    public static String diskUrlToCacheUrl(String diskURL) {
        if (diskURL==null || diskURL.isEmpty() || !diskURL.startsWith(SAVED_URL_PREFIX)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.diskUrlToCacheUrl: Invalid URL " + diskURL);
            return diskURL;
        }

        String[] parts = diskURL.split(":", 3);
        if (parts==null || parts.length!=3) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.diskUrlToCacheUrl: Invalid URL " + diskURL);
            return diskURL;
        }

        return parts[2];
    }

    public static String diskUrlGetFileName(String diskURL) {
        if (diskURL==null || diskURL.isEmpty() || !diskURL.startsWith(SAVED_URL_PREFIX)) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.diskUrlGetFileName: Invalid URL " + diskURL);
            return diskURL;
        }

        String[] parts = diskURL.split(":", 3);
        if (parts==null || parts.length!=3) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.diskUrlGetFileName: Invalid URL " + diskURL);
            return diskURL;
        }

        return parts[1];
    }

    public static List<String> getAllCacheFiles() {
        List<String>fileNames = new ArrayList<String>();

        List<File>allFiles = getFilesEndingWith(CACHE_ENDING, ".");

        if (allFiles==null || allFiles.isEmpty()) {
            return fileNames;
        }

        for (File f : allFiles) {
            fileNames.add(f.getName());
        }

        return fileNames;
    }

    public static List<String> getAllPlaylistFiles() {
        List<String>fileNames = new ArrayList<String>();

        List<File>allFiles = getFilesEndingWith(PLAYLIST_ENDING, ".");
        
        if (allFiles==null || allFiles.isEmpty()) {
            return fileNames;
        }

        for (File f : allFiles) {
            fileNames.add(f.getName());
        }

        return fileNames;
    }

    static List<File> getFilesEndingWith(String ending, String directory) {

        List<File> files = new ArrayList<File>();

        if (ending==null || ending.isEmpty())
            return files;

        File[] allFiles = getFileListing(directory);

        if (allFiles==null || allFiles.length==0)
            return files;

        for (File f : allFiles) {
            if (f.getAbsolutePath().endsWith(ending))
                files.add(f);
        }

        return files;
    }

    static File[] getFileListing(String directory) {

        if (directory==null || directory.isEmpty())
            return null;

        File f = new File(directory);

        if (!f.isDirectory())
            return null;

        return f.listFiles();
    }

    public static boolean deletePlaylistFile(String filename) {
        String file = convertToNavixPlaylistName(filename);
        File f = new File(file);
        return f.delete();
    }
}
