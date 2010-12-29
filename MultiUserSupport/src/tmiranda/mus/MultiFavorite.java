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

        String userString = FavoriteAPI.GetFavoriteProperty(Favorite, FAVORITE_USERS);

        if (userString == null || userString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiFavorite: No AllowedUsers.");
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

    void addFavorite() {
        if (!isValid)
            return;

        DelimitedString DS = new DelimitedString(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
        DS.addUniqueElement(userID);
        FavoriteAPI.SetFavoriteProperty(sageFavorite, FAVORITE_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " + DS.toString());
    }

    void removeFavorite() {

        if (!isValid)
            return;

        DelimitedString DS = new DelimitedString(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
        DS.removeElement(userID);
        FavoriteAPI.SetFavoriteProperty(sageFavorite, FAVORITE_USERS, DS.toString());
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " + DS.toString());

        if (DS.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Deleting from sage database.");
            FavoriteAPI.RemoveFavorite(sageFavorite);
        }
    }

    boolean isFavorite() {

        if (!isValid)
            return false;

        List<String> users = DelimitedString.delimitedStringToList(FavoriteAPI.GetFavoriteProperty(sageFavorite, FAVORITE_USERS), ",");
        return users.contains(userID);
    }
}
