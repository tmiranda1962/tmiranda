
package tmiranda.otf;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class API {

    public static boolean isOneTimeFavorite(Object Airing) {

        if (Airing==null)
            return false;

        Object Favorite = FavoriteAPI.GetFavoriteForAiring(Airing);

        if (Favorite == null) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "isOneTimeFavorite: Is not a favorite.");
            return false;
        }

        String oneTime = FavoriteAPI.GetFavoriteProperty(Favorite, Plugin.FAVORITE_PROPERTY);

        if (oneTime==null || oneTime.isEmpty() || !oneTime.equalsIgnoreCase("true")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "isOneTimeFavorite: Is not a one time favorite.");
            return false;
        }

        return true;
    }
}
