
package tmiranda.navix;

import sagex.api.*;

/**
 * Load a Playlist into the memory cache.
 *
 * @author Tom Miranda.
 */
public class Cacher extends Thread {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    final static String CACHER_THREAD_NAME = "NavixCacher";
    String url;

    /**
     * Constructor.  The url points to a Playlist that will be loaded into the memory cache.
     * Looks at the Sage property specified by PlaylistCache.PROPERTY_CACHE_SECOND_LEVEL to
     * determine if the "children" to this Playlist will also be cached.
     *
     * Note that if the children are cached the thread priority will be lowered considerably.
     * 
     * @param url
     */
    public Cacher(String url) {
        this.url = url;
    }

    /**
     *
     */
    @Override
    public void run() {
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Starting for " + url);

        Thread t = Thread.currentThread();
        t.setName(CACHER_THREAD_NAME);

        if (url==null || url.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Cacher: null url.");
            return;
        }

        // Creating the Playlist will automatically load it into the Cache.
        Playlist p = new Playlist(url);

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: First level done for " + url);

        // Now cache the next level down the tree.

        String doSecondLevel = Configuration.GetProperty(PlaylistCache.PROPERTY_CACHE_SECOND_LEVEL, "true").toLowerCase();
        if (doSecondLevel.startsWith("false")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Second level cache is disabled.");
            PlaylistCache.alreadyRunning.remove(url);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Second level starting for " + url);
     
        t.setPriority(MIN_PRIORITY+1);

        for (PlaylistEntry p2 : p.getElements()) {
            if (p2.isPlaylist()) {
                PlaylistElement element = (PlaylistElement)p2;
                String nextPlaylistName = element.getNextPlaylist();
                if (!PlaylistCache.getInstance().contains(nextPlaylistName)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Caching second level PlaylistElement " + nextPlaylistName);
                    p = new Playlist(nextPlaylistName);
                }
            } else if (p2.isPlaylist()) {
                PlxElement element = (PlxElement)p2;
                String nextPlaylistName = element.getNextPlaylist();
                if (!PlaylistCache.getInstance().contains(nextPlaylistName)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Caching second level PlxElement " + nextPlaylistName);
                    p = new Playlist(nextPlaylistName);
                }
            }
        }

        PlaylistCache.alreadyRunning.remove(url);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "Cacher: Ending for " + url);
    }
}
