
package tmiranda.navix;

import java.util.*;
import java.io.*;

// FIXME - Will not work properly because circular references are allowed in the playlists.

/**
 * Search for elements and keywords.
 *
 * @author Tom Miranda.
 */
public class Search {

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
     * the first file in the array. All cacheFiles will be consididered when looking for
     * results.
     * @param maxAge The maximum age (in milliseconds) of cached Playlists cached in
     * the cacheFile to consider. If set to 0 all Playlists will be considered.
     */
    public Search(boolean cacheToMemory, String[] cacheFiles, long maxAge) {
        this.cacheToMemory = cacheToMemory;
        this.cacheFiles = cacheFiles==null ? new String[0] : cacheFiles;
        this.maxAge = maxAge < 0 ? 0 : maxAge;
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
            cacheFiles[0] = cacheFile;
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

    public List<PlaylistEntry> getType(String url, PlaylistType type, int limit, int depth, int maxPlaylists) {
        return getType(url, type.toString(), limit, depth, maxPlaylists);
    }

    /**
     * Search for Playlists of the specified type.
     *
     * @param url The URL of the root Playlist.
     * @param type The type of Playlist to search for.
     * @param limit The maximum number of PlaylistEntry to return.
     * @param depth The maximum depth to travel down any one branch of the Playlist tree.
     * @param maxPlaylists The maximum number of Playlists to inspect. Playlists that fail
     * to load (because their host is unreachable) are counted.
     * @return
     */
    public List<PlaylistEntry> getType(String url, String type, int limit, int depth, int maxPlaylists) {

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: url = " + url);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: type = " + type);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: limit = " + limit);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: depth = " + depth);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Playlists searched = " + totalPlaylists);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Elements found = " + resultsSoFar.size());

        List<PlaylistEntry> results = new ArrayList<PlaylistEntry>();

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: null URL.");
            return results;
        }

        if (type==null || type.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: null type.");
            return results;
        }

        if (resultsSoFar.size() >= limit) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Results limit reached.");
            return results;
        }

        if (depth<=0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Depth reached.");
            return results;
        }

        if (totalPlaylists >= maxPlaylists) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Playlist limit reached.");
            return results;
        }

        if (processedPlaylists.contains(url)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getType: Playlist already searched " + url);
            return results;
        }

        processedPlaylists.add(url);
        totalPlaylists++;
        boolean retrievedFromDisk = false;

        Playlist playlist = null;

        // Try to retrieve from memory cache or one of the specified disk cache files.
        if (Playlist.isInCache(url) && !Playlist.shouldRetryNew(url)) {
            playlist = Playlist.retrieveFromCache(url);
        } else for (String cacheFile : cacheFiles) {
            if (getUrlsInDiskCache(cacheFile).contains(url)) {
                Playlist p = getFromDiskCache(url, cacheFile);
                if (p!=null && (maxAge==0 || ((new Date().getTime()-p.getDiskTime())<maxAge))) {
                    playlist = p;
                    retrievedFromDisk = true;
                    break;
                }
            }
        }

        // Handle the case of having the cache deleted after checking.
        if (playlist == null)
            playlist = new Playlist(url, cacheToMemory);

        // Save it to disk cache if we didn't get it from there in the first place.
        if (!retrievedFromDisk && cacheFiles.length >0 && !addToDiskCache(playlist, cacheFiles[0])) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getType: Failed to save to disk cache.");
        }

        // FIXME - Sanity check
        //if (!getUrlsInDiskCache("NavixSearchCache.db").contains(playlist.getUrl()))
            //Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.getType: Failed to retrieve URL " + playlist.getUrl());

        // Make sure the url was reachable.
        if (!playlist.hasLoaded()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getType: Playlist failed to load, skipping " + url);
            return results;
        }

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            // Add to the List if this is the correct type and we are below the limit.
            if (entry.getType().equalsIgnoreCase(type) && resultsSoFar.size() < limit) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "Search.getType: Adding " + entry.getName());
                results.add(entry);
                resultsSoFar.add(entry);
            }

            // Recurse if this is a playlist and we're not over the limit.
            if ((entry.isPlaylist() || entry.isPlx()) && resultsSoFar.size() < limit) {
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

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: null URL.");
            return results;
        }

        if (words==null || words.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: null words.");
            return results;
        }

        if (resultsSoFar.size() >= limit) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Results limit reached.");
            return results;
        }

        if (depth<=0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Depth reached.");
            return results;
        }

        if (totalPlaylists >= maxPlaylists) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Playlist limit reached.");
            return results;
        }

        if (processedPlaylists.contains(url)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getWords: Playlist already searched " + url);
            return results;
        }

        processedPlaylists.add(url);
        totalPlaylists++;
        boolean retrievedFromDisk = false;

        Playlist playlist = null;

        // Try to retrieve from memory cache or one of the specified disk cache files.
        if (Playlist.isInCache(url) && !Playlist.shouldRetryNew(url)) {
            playlist = Playlist.retrieveFromCache(url);
        } else for (String cacheFile : cacheFiles) {
            if (getUrlsInDiskCache(cacheFile).contains(url)) {
                Playlist p = getFromDiskCache(url, cacheFile);
                if (p!=null && (maxAge==0 || ((new Date().getTime()-p.getDiskTime())<maxAge))) {
                    playlist = p;
                    retrievedFromDisk = true;
                    break;
                }
            }
        }

        // Handle the case of having the cache deleted after checking.
        if (playlist == null)
            playlist = new Playlist(url, cacheToMemory);

        // Save it to disk cache if we didn't get it from there in the first place.
        if (!retrievedFromDisk && cacheFiles.length >0 && !addToDiskCache(playlist, cacheFiles[0])) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Search.getWords: Failed to save to disk cache.");
        }

        // Make sure the url was reachable.
        if (!playlist.hasLoaded()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.getWords: Playlist failed to load, skipping " + url);
            return results;
        }

        for (PlaylistEntry entry : playlist.getElements()) {

            totalElements++;

            // Create a list of all the words found in the various fields.
            List<String> foundWords = phraseToWords(entry.getName());
            foundWords.addAll(phraseToWords(entry.getTitle()));
            foundWords.addAll(phraseToWords(entry.getDescription()));

            // See if any match the words we are looking for.
            for (String word : words) {
                if (foundWords.contains(word) && resultsSoFar.size() < limit) {
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
    public static synchronized boolean addToDiskCache(Playlist playlist, String fileName) {

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

        // Read through the file and replace the Playlist if we find it or add it to the end.
        boolean inserted = false;
        Playlist p;
System.out.println("NAVIX:: ABOUT TO START " + fileName);
        try {
            while ((p = (Playlist)ois.readObject()) != null) {
System.out.println("NAVIX:: READ");
                if (p.getUrl().equals(playlist.getUrl())) {
System.out.println("NAVIX:: MATCH");
                    playlist.setDiskTime(new Date().getTime());
                    oos.writeObject(playlist);
                    inserted = true;
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.addToDiskCache: Playlist updated " + playlist.getUrl());
                } else {
System.out.println("NAVIX:: NO MATCH");
                    oos.writeObject(p);
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
     * FIXME - NOT YET IMPLEMENTED.
     * @param playlist
     * @param fileName
     * @return
     */
    public static synchronized boolean removeFromDiskCache(Playlist playlist, String fileName) {
        return false;
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
                if (p.getUrl().equals(url)) {
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
     * the Playlist will be empty.  The caller can use the various Playlist setter methods
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
     * @return A URL that can be used to retrieve the Playlist from the Search cache.  This URL
     * will not be suitable for retrieving the Playlist from the web.
     */
    public String saveResultsAsPlaylist(String fileName) {

        Playlist playlist = new Playlist();

        playlist.setUrl(UUID.randomUUID().toString());
        playlist.setElements(resultsSoFar);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Search.saveResultsAsPlaylist: Saving " + fileName + " url " + playlist.getUrl());
        if (!addToDiskCache(playlist, fileName)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Search.saveResultsAsPlaylist: Failed to save file.");
        }

        return playlist.getUrl();
    }

    public static boolean savePlaylist(Playlist playlist, String fileName) {
        return addToDiskCache(playlist, fileName);
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
}
