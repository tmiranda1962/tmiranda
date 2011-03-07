
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 * Class that implements the SageTV FavoriteAPI for multiple users. Unless noted otherwise the
 * methods behave in the same way as the Sage core APIs.
 * @author Tom Miranda
 */
public class MultiFavorite extends MultiObject {

    /**
     * The UserRecordAPI store used to keep the data used by this class.
     */
    public static final String     FAVORITE_STORE  = "MultiUser.Favorite";

    /**
     * The UserRecordAPI store "name" used to keep a delimited String of users that have the
     * Favorite defined.
     */
    public static final String     FAVORITE_USERS  = "AllowedUsers";
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
    public void addFavorite() {
        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "addFavories: !isValid.");
            return;
        }

        allowedUsers.add(userID);
        addDataToFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "addFavorite: Users for Favorite " +  FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);
    }

    /**
     * Removes the current User from the Favorite.  IF no more Users are defined for the
     * Favorite it is removed from the Sage core.
     */
    public void removeFavorite() {

        if (!isValid) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeFavories: !isValid.");
            return;
        }

        if (allowedUsers.contains(userID))
            allowedUsers.remove(userID);

        DelimitedString DS = removeDataFromFlag(FAVORITE_USERS, userID);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: Users for Favorite " + FavoriteAPI.GetFavoriteDescription(sageFavorite) + ":" + allowedUsers);

        if (DS==null || DS.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "removeFavorite: No more users, deleting Favorite from sage database " + FavoriteAPI.GetFavoriteDescription(sageFavorite));
            removeRecord();
            FavoriteAPI.RemoveFavorite(sageFavorite);
        }
    }

    /**
     * Removes all users from the Favorite record.  Should be called when the Favorite
     * is removed by Admin or the null user.
     * @param Favorite
     */
    static void removeAllUsers(Object Favorite) {
        List<String> allUsers = User.getAllUsers(false);

        for (String thisUser : allUsers) {
            MultiFavorite MF = new MultiFavorite(thisUser, Favorite);
            MF.removeUser();
        }
    }

    /**
     * Removes the user from the list of allowed users without ever removing the core Favorite.
     */
    void removeUser() {
        if (allowedUsers.contains(userID))
            allowedUsers.remove(userID);

        removeDataFromFlag(FAVORITE_USERS, userID);
    }

    /**
     * Returns true if the current User has this Sage Favorite defined as a Favorite.
     * @return
     */
    public boolean isFavorite() {

        if (!isValid)
            return false;

        List<String> users = DelimitedString.delimitedStringToList(getRecordData(FAVORITE_USERS), Plugin.LIST_SEPARATOR);
        return users.contains(userID);
    }

    /**
     * Return a list of Users that have the current Sage Favorite defined as  Favorite for them.
     * @return
     */
    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    /**
     * Returns the Favorites for the current User.
     * @return The returned array is not mutable.
     */
    public static Object[] getFavorites() {

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
    public void initializeUser() {
        addDataToFlag(FAVORITE_USERS, userID);
    }

    /**
     * Removes the current User from the Favorites database.  Does NOT change remove the
     * underlying Sage Favorite even if the User being removed is the last User. This method is
     * depreciated, removeFavorite should be used instead.
     */
    @Deprecated
    public void clearUserFromFlags() {
        clearUser(userID, FLAGS);
    }

    /**
     * Removes all Favorite related data.
     */
    public static void WipeDatabase() {
        UserRecordAPI.DeleteAllUserRecords(FAVORITE_STORE);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MultiFavorite other = (MultiFavorite) obj;
        if (this.sageFavorite != other.sageFavorite && (this.sageFavorite == null || !this.sageFavorite.equals(other.sageFavorite))) {
            return false;
        }
        if (!this.userID.equals(other.userID) || (FavoriteAPI.GetFavoriteID(this.sageFavorite) != FavoriteAPI.GetFavoriteID(other.sageFavorite))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.sageFavorite != null ? this.sageFavorite.hashCode() : 0);
        hash = 17 * hash + (this.allowedUsers != null ? this.allowedUsers.hashCode() : 0);
        return hash;
    }

}
