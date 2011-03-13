package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class FavoriteAPI {
    /**
     * Invoke IN PLACE OF core API.
     * @return
     */
    public static Object[] getFavorites() {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.FavoriteAPI.GetFavorites();
        } else {
            return MultiFavorite.getFavorites();
        }
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Favorite
     */
    public static void removeFavorite(Object Favorite) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            MultiFavorite.removeAllUsers(Favorite);
            sagex.api.FavoriteAPI.RemoveFavorite(Favorite);
            return;
        }

        MultiFavorite MF = new MultiFavorite(User, Favorite);
        MF.removeFavorite();
        return;
    }

    /**
     * Invoke right after Core API.
     * @param Favorite
     */
    public static void addFavorite(Object Favorite) {

        String U = UserAPI.getLoggedinUser();

        if (U==null || U.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Adding Favorite for all Users.");

            List<String> allUsers = User.getAllUsers(false);

            for (String thisUser : allUsers) {
                MultiFavorite MF = new MultiFavorite(thisUser, Favorite);
                MF.addFavorite();
            }

            return;
        }

        MultiFavorite MF = new MultiFavorite(U, Favorite);
        MF.addFavorite();
        return;
    }

    /**
     * Add the Favorite for the specified UserID.
     * @param UserID
     * @param Favorite
     */
    public static void addFavorite(String UserID, Object Favorite) {

        if (UserID==null || UserID.equalsIgnoreCase(Plugin.SUPER_USER)) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Adding Favorite for all Users.");

            List<String> allUsers = User.getAllUsers(false);

            for (String thisUser : allUsers) {
                MultiFavorite MF = new MultiFavorite(thisUser, Favorite);
                MF.addFavorite();
            }

            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Adding Favorite for User " + UserID);
        MultiFavorite MF = new MultiFavorite(UserID, Favorite);
        MF.addFavorite();
        return;
    }

    /**
     * Invoke IN PLACE OF core API.
     * @param Airing
     * @return
     */
    public static Object getFavoriteForAiring(Object Airing) {

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return sagex.api.FavoriteAPI.GetFavoriteForAiring(Airing);
        }

        MultiAiring MA = new MultiAiring(User, API.ensureIsAiring(Airing));
        return MA.getFavoriteForAiring();
    }

    // Not in the default STV, but put here as a placeholder.
    public static int getFavoriteID(Object Favorite) {
        return sagex.api.FavoriteAPI.GetFavoriteID(Favorite);
    }

    /**
     * Returns a List of users that have the Favorite defined.
     * @param Favorite
     * @return
     */
    public static List<String> GetUsersForFavorite(Object Favorite) {

        List<String> TheList = new ArrayList<String>();

        if (Favorite==null || !sagex.api.FavoriteAPI.IsFavoriteObject(Favorite)) {
            return TheList;
        }

        String User = UserAPI.getLoggedinUser();

        if (User==null || User.equalsIgnoreCase(Plugin.SUPER_USER)) {
            return TheList;
        }

        MultiFavorite MF = new MultiFavorite(User, Favorite);
        return MF.getAllowedUsers();
    }
}
