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
public class MultiFavorite {

    private boolean         isValid = true;
    private String          userID;
    private Object          sageFavorite = null;
    private List<String>    allowedUsers = null;

    private static final String FAVORITE_USERS  = "AllowedUsers";

    public MultiFavorite(String User, Object Favorite) {

        allowedUsers = new ArrayList<String>();

        if (Favorite==null || !FavoriteAPI.IsFavoriteObject(Favorite) || User==null || User.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiFavorite: Invalid Favorite Object. " + User + ":" + Favorite);
            isValid = false;
            return;
        }

        sageFavorite = Favorite;
        userID = User;

        String userString = FavoriteAPI.GetFavoriteProperty(Favorite, FAVORITE_USERS);

        if (userString == null || userString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "MultiFavorite: No AllowedUsers.");
            return;
        }

        String[] userArray = userString.split(",");

        if (userArray == null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "MultiFavorite: null userArray.");
            return;
        }

        allowedUsers = Arrays.asList(userArray);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiFavorite: AllowedUsers " + allowedUsers);
    }

    synchronized void addFavorite() {
        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addFavories: !isValid.");
            return;
        }

        DelimitedString DS = new DelimitedString(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
        DS.addUniqueElement(userID);
        FavoriteAPI.SetFavoriteProperty(sageFavorite, FAVORITE_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " + DS.toString());
    }

    synchronized void removeFavorite() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFavories: !isValid.");
            return;
        }

        DelimitedString DS = new DelimitedString(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
        DS.removeElement(userID);
        FavoriteAPI.SetFavoriteProperty(sageFavorite, FAVORITE_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: Users for Favorite " + DS.toString());

        if (DS.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: No more users, deleting from sage database.");
            FavoriteAPI.RemoveFavorite(sageFavorite);
        }
    }

    boolean isFavorite() {

        if (!isValid)
            return false;

        List<String> users = DelimitedString.delimitedStringToList(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
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
}
