
package tmiranda.navix;

import java.util.*;
import java.io.*;

/**
 * Manages Favorites (which are really more like Bookmarks.)
 * @author Tom Miranda.
 */
public class Favorite {
    
    public static final String FAVORITE_PROPERTY_FILE    = "Favorites.navix.properties";

    /**
     * Return a Set of urls that point to favorite (bookmarked) Playlists.
     * @return
     */
    public static Set<String> getAllFavorites() {
        Properties props = getProperties();

        return props==null ? new HashSet<String>() : props.stringPropertyNames();
    }

    /**
     * Add the url as a Favorite with the specified name.  Normally name will be the results
     * of the Playlist.getTitle() method.  The name is stored so we can display the name
     * without physically loading the Playlist.
     * @param url
     * @param name
     * @return
     */
    public static boolean addFavorite(String url, String name) {
        Properties props = getProperties();
        if (props==null)
            return false;
        props.setProperty(url, name);
        return saveProperties(props);
    }

    /**
     * Get the name of the Favorite.
     * @param url
     * @return
     */
    public static String getName(String url) {
        if (url==null || url.isEmpty())
            return null;
        Properties props = getProperties();
        if (props==null)
            return null;
        return props.getProperty(url);
    }

    /**
     * Remove the specified Favorite.
     * @param url
     * @return
     */
    public static boolean removeFavorite(String url) {
        Properties props = getProperties();
        if (props==null || url==null || url.isEmpty())
            return false;
        props.remove(url);
        return saveProperties(props);
    }

    /**
     * Check if the url points to a Favorite.
     * @param url
     * @return
     */
    public static boolean isFavorite(String url) {
        return getAllFavorites().contains(url);
    }

    /**
     * Check if any Favorites have been defined.
     * @return
     */
    public static boolean hasAnyFavorites() {
        return !getAllFavorites().isEmpty();
    }

    private static Properties getProperties() {

        Properties props = new Properties();

        try {
            File f = new File(FAVORITE_PROPERTY_FILE);
            if (!f.exists())
                f.createNewFile();

            InputStream in = new FileInputStream(FAVORITE_PROPERTY_FILE);
            props.load(in);
            //in.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Favorite.getProperties: Exception loading " + e.getMessage());
            return null;
        }

        return props;
    }

    private static boolean saveProperties(Properties props) {
        try {
            OutputStream out = new FileOutputStream(FAVORITE_PROPERTY_FILE);
            props.store(out, "Navix Properties - Used to store Favorites.");
            //out.close();
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "Favorite.saveProperties: Exception saving " + e.getMessage());
            return false;
        }

        return true;
    }

}
