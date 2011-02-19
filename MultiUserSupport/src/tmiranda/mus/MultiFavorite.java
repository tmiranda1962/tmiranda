
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda
 */
public class MultiFavorite extends MultiObject {

    static final String     FAVORITE_STORE  = "MultiUser.Favorite";
    static final String     FAVORITE_USERS  = "AllowedUsers";
    static final String[]   FLAGS = {FAVORITE_USERS};


    private Object          sageFavorite = null;
    private List<String>    allowedUsers = null;

    /**
     * Constructor.  Creates a MultiFavorite Object for the specified User.
     * @param User
     * @param Favorite Must be a valid Favorite from the Sage core.
     */
    public MultiFavorite(String User, Object Favorite) {

        super(User, FAVORITE_STORE, FavoriteAPI.GetFavoriteID(Favorite), 0, null);

        if (!isValid || Favorite==null || !FavoriteAPI.IsFavoriteObject(Favorite)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiFavorite: Invalid Favorite Object. " + User + ":" + Favorite);
            isValid = false;
            return;
        }

        sageFavorite = Favorite;

        allowedUsers = new ArrayList<String>();

        String userString = getRecordData(FAVORITE_USERS);

        if (userString == null || userString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiFavorite: No AllowedUsers.");
            return;
        }

        allowedUsers = DelimitedString.delimitedStringToList(userString, Plugin.LIST_SEPARATOR);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiFavorite: AllowedUsers " + FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);
        return;
    }

    /**
     * Adds the current User to the Favorite.
     */
    synchronized void addFavorite() {
        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addFavories: !isValid.");
            return;
        }

        allowedUsers.add(userID);
        addFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " +  FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);
    }

    /**
     * Removes the current User from the Favorite.  IF no more Users are defined for the
     * Favorite it is removed from the Sage core.
     */
    synchronized void removeFavorite() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFavories: !isValid.");
            return;
        }

        if (allowedUsers.contains(userID))
            allowedUsers.remove(userID);

        DelimitedString DS = removeFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: Users for Favorite " + FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);

        if (DS==null || DS.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: No more users, deleting Favorite from sage database " + FavoriteAPI.GetFavoriteDescription(sageFavorite));
            removeRecord();
            FavoriteAPI.RemoveFavorite(sageFavorite);
        }
    }

    /**
     * Returns true if the current User has this Sage Favorite defined as a Favorite.
     * @return
     */
    boolean isFavorite() {

        if (!isValid)
            return false;

        List<String> users = DelimitedString.delimitedStringToList(getRecordData(FAVORITE_USERS), Plugin.LIST_SEPARATOR);
        return users.contains(userID);
    }

    /**
     * Return a list of Users that have the current Sage Favorite defined as  Favorite for them.
     * @return
     */
    List<String> getAllowedUsers() {
        return allowedUsers;
    }

    /**
     * Returns the Favorites for the current User.
     * @return The returned array is not mutable.
     */
    static Object[] getFavorites() {

        // Get all of the Sage Favorites.
        Object[] sageFavorites = FavoriteAPI.GetFavorites();

        if (sageFavorites==null || sageFavorites.length==0)
            return sageFavorites;

        List<Object> Favorites = new ArrayList<Object>();

        // Check each sage Favorite to see if the current user can access it.
        for (Object Favorite : sageFavorites) {
            MultiFavorite F = new MultiFavorite(API.getLoggedinUser(), Favorite);
            if (F.isFavorite())
                Favorites.add(Favorite);
        }

        return Favorites.toArray(new Object[Favorites.size()]);
    }


    /**
     * Adds the current User to the Favorite database. This method is depreciated, addFavorite
     * should be used instead.
     */
    @Deprecated
    void initializeUser() {
        addFlag(FAVORITE_USERS, userID);
    }

    /**
     * Removes the current User from the Favorites database.  Does NOT change remove the
     * underlying Sage Favorite even if the User being removed is the last User. This method is
     * depreciated, removeFavorite should be used instead.
     */
    @Deprecated
    void clearUserFromFlags() {
        clearUser(userID, FLAGS);
    }

    /**
     * Removes all Favorite related data.
     */
    static void WipeDatabase() {
        UserRecordAPI.DeleteAllUserRecords(FAVORITE_STORE);
    }
}
