/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class MultiFavorite extends MultiObject {

    static final String     FAVORITE_STORE  = "MultiUser.Favorite";
    static final String     FAVORITE_USERS  = "AllowedUsers";
    static final String[]   FLAGS = {FAVORITE_USERS};


    private Object          sageFavorite = null;
    private List<String>    allowedUsers = null;

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
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiFavorite: AllowedUsers " + allowedUsers);
        return;
    }

    synchronized void addFavorite() {
        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addFavories: !isValid.");
            return;
        }

        allowedUsers.add(userID);
        addFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " +  FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);
    }

    synchronized void removeFavorite() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFavories: !isValid.");
            return;
        }

        allowedUsers.remove(userID);
        DelimitedString DS = removeFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: Users for Favorite " + FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);

        if (DS==null || DS.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: No more users, deleting Favorite from sage database " + FavoriteAPI.GetFavoriteDescription(sageFavorite));
            FavoriteAPI.RemoveFavorite(sageFavorite);
        }
    }

    boolean isFavorite() {

        if (!isValid)
            return false;

        List<String> users = DelimitedString.delimitedStringToList(getRecordData(FAVORITE_USERS), Plugin.LIST_SEPARATOR);
        return users.contains(userID);
    }

    List<String> getAllowedUsers() {
        return allowedUsers;
    }

    static Object[] getFavorites() {

        Object[] sageFavorites = FavoriteAPI.GetFavorites();

        if (sageFavorites==null || sageFavorites.length==0)
            return sageFavorites;

        List<Object> Favorites = new ArrayList<Object>();

        for (Object Favorite : sageFavorites) {
            MultiFavorite F = new MultiFavorite(API.getLoggedinUser(), Favorite);
            if (F.isFavorite())
                Favorites.add(Favorite);
        }

        return Favorites.toArray(new Object[Favorites.size()]);
    }


    // Initialize this user for the Favorite.
    void initializeUser() {
        addFlag(FAVORITE_USERS, userID);
    }

    void clearUserFromFlags() {
        clearUser(userID, FLAGS);
    }

    // Wipes the entire database.
    static void WipeDatabase() {
        UserRecordAPI.DeleteAllUserRecords(FAVORITE_STORE);
    }
}
