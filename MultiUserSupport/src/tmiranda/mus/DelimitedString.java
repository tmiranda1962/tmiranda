
package tmiranda.mus;

import java.util.*;
import sagex.api.*;

/**
 * DelimitedString is a class that allows manipulation of Strings containing data separated
 * by a delimiter.
 * @author Tom Miranda
 */
public class DelimitedString {

    private String Delimiter = null;
    private String TheString = null;

    /**
     * Constructor.
     * @param Str The String which contains data separated by delimiters.
     * @param TheDelimiter The delimiter separating the data.
     */
    public DelimitedString(String Str, String TheDelimiter) {
        Delimiter = TheDelimiter;
        TheString = (Str==null || Str.isEmpty() ? null : Str);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DelimitedString: TheString " + TheString);
    }

    /**
     * Constructor. Concatenates two Strings.
     * @param S1 A String which contains data separated by delimiters.
     * @param S2 A String which contains data separated by delimiters.
     * @param TheDelimiter TheDelimiter The delimiter separating the data.
     */
    public DelimitedString (String S1, String S2, String TheDelimiter) {
        Delimiter = TheDelimiter;

        String Str1 = (S1==null || S1.isEmpty() ? null : S1);
        String Str2 = (S2==null || S2.isEmpty() ? null : S2);

        if (Str1==null && Str2==null)
            TheString = null;
        else if (Str1==null)
            TheString = Str2;
        else if (Str2==null)
            TheString = Str1;
        else
            TheString = Str1 + Delimiter + Str2;

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DelimitedString: TheString " + TheString);
    }

    /**
     * Check if the delimited String contains the specified Element.
     * @param Element The element to check for.
     * @return true if it's in the delimited String, false otherwise.
     */
    public boolean contains(String Element) {
        return delimitedStringToList(TheString, Delimiter).contains(Element);
    }

    /**
     * Check if the delimited String contains all of the specified Elements.
     * @param Elements The elements to check for.
     * @return true if all of the elements are in the delimited String, false otherwise.
     */
    public boolean containsAll(List<String> Elements) {
        return delimitedStringToList(TheString, Delimiter).containsAll(Elements);
    }

    /**
     * Adds an element IN PLACE.
     * @param Element The element to add.
     */
    public void addUniqueElement(String Element) {

        if (contains(Element)) {
            Log.getInstance().write(Log.LOGLEVEL_ALL, "addUniqueElement: Element already exists " + Element);
            return;
        }

        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUniqueElement: TheString before " + TheString + ":" + Element);
        if (TheString == null || TheString.isEmpty())
            TheString = Element;
        else
            TheString = TheString + Delimiter + Element;
        //Log.getInstance().write(Log.LOGLEVEL_TRACE, "addUniqueElement: TheString after " + TheString);
    }

    /**
     * Removes an element IN PLACE.
     * @param Element The element to remove.
     */
    public void removeElement(String Element) {

        if (Element == null || Element.isEmpty() || !contains(Element))
            return;

        List<String> CurrentList = delimitedStringToList(TheString, Delimiter);

        if (CurrentList == null || CurrentList.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "removeElement: Unexpected CurrentList.");
            return;
        }

        // Manually construct a new List.  For some reason .remove does not work?
        List<String> UpdatedList = new ArrayList<String>();

        for (String S : CurrentList) {
            if (!S.equals(Element)) {
                UpdatedList.add(S);
            }
        }

        TheString = listToString(UpdatedList);

        return;
    }

    /**
     * Converts a delimited string to a List.
     * @param S The delimited String.
     * @param D The delimiter.
     * @return A List containing the elements in the delimited String.
     */
    public static List<String> delimitedStringToList(String S, String D) {
        List<String> TheList = new ArrayList<String>();

        if (S == null || D == null || S.isEmpty()) {
            return TheList;
        }

        String[] StringArray = S.split(D);

        if (StringArray == null || StringArray.length==0) {
            return TheList;
        }

        return new ArrayList<String>(Arrays.asList(StringArray));
    }

    /**
     * Converts a set of data to a delimited String.
     * @param TheSet The set of data.
     * @param D The delimiter.
     * @return A delimited String that contains the data from the set. The ordering remains unchanged.
     */
    public static String setToDelimitedString(Set<String> TheSet, String D) {

        String TheString = null;

        for (String Element : TheSet)
            if (TheString == null)
                TheString = Element;
            else
                TheString = TheString + D + Element;

        return TheString;
    }

    /**
     * Converts a List of elements to a delimited String.
     * @param TheList The List of data.
     * @param D The delimiter.
     * @return A delimited String that contains the data from the List. The ordering remains unchanged.
     */
    public static String listToDelimitedString(List<String> TheList, String D) {

        String TheString = null;

        for (String Element : TheList)
            if (TheString == null)
                TheString = Element;
            else
                TheString = TheString + D + Element;

        return TheString;
    }

    /**
     * Convert a delimited String to a Set.
     * @param S The delimited String.
     * @param D The delimiter.
     * @return A Set containing the elements in the delimited String.
     */
    public static Set<String> delimitedStringToSet(String S, String D) {
        Set<String> TheSet = new HashSet<String>();
        List<String> TheList = delimitedStringToList(S, D);

        for (String E : TheList)
            TheSet.add(E);

        return TheSet;
    }

    /**
     * Convert a List to a delimited String.
     * @param TheList The List containing the data.
     * @return A delimited String containing the data.
     */
    public String listToString(List<String> TheList) {

        if (TheList == null || TheList.isEmpty())
            return null;

        String NewString = null;

        for (String S : TheList) {
            if (NewString == null)
                NewString = S;
            else
                NewString = NewString + Delimiter + S;
        }

        return NewString;
    }

    /**
     * Adds User to the delimited String contained in the User Record.
     * @param Record A User Record from the UserRecordAPI.
     * @param Flag The name of the Flag to store the data.
     * @param User The User name to add to the Flag in the Record. The User name does
     * not have to be a valid User so this method can really be used to add any data to a Flag.
     */
    public static synchronized void addToUserRecord(Object Record, String Flag, String User) {
        if (Record != null) {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(Record, Flag), Plugin.LIST_SEPARATOR);
            DS.addUniqueElement(User);
            UserRecordAPI.SetUserRecordData(Record, Flag, DS.toString());
        }
    }

    /**
     * Removes a User from the delimited String contained in the User Record.
     * @param Record A User Record from the UserRecordAPI.
     * @param Flag The name of the Flag to store the data.
     * @param User The User name to add to the Flag in the Record.
     */
    public static synchronized void removeFromUserRecord(Object Record, String Flag, String User) {
        if (Record != null) {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(Record, Flag), Plugin.LIST_SEPARATOR);
            DS.removeElement(User);
            UserRecordAPI.SetUserRecordData(Record, Flag, DS.toString());
        }
    }

    @Deprecated
    public static synchronized void addToMediaFile(Object MediaFile, String Flag, String User) {
        if (MediaFile != null) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(MediaFile, Flag), Plugin.LIST_SEPARATOR);
            DS.addUniqueElement(User);
            MediaFileAPI.SetMediaFileMetadata(MediaFile, Flag, DS.toString());
        }
    }

    @Deprecated
    public static synchronized void removeFromMediaFile(Object MediaFile, String Flag, String User) {
        if (MediaFile != null) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(MediaFile, Flag), Plugin.LIST_SEPARATOR);
            DS.removeElement(User);
            MediaFileAPI.SetMediaFileMetadata(MediaFile, Flag, DS.toString());
        }
    }

    /**
     * Returns a delimited String.
     * @return The delimited String.
     */
    @Override
    public String toString() {
        return TheString;
    }

    /**
     * Checks to see if the DelimitedString is empty.
     * @return true if it contains no elements, false otherwise.
     */
    public boolean isEmpty() {
        return (TheString==null || TheString.isEmpty());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DelimitedString other = (DelimitedString) obj;
        if ((this.Delimiter == null) ? (other.Delimiter != null) : !this.Delimiter.equals(other.Delimiter)) {
            return false;
        }
        if ((this.TheString == null) ? (other.TheString != null) : !this.TheString.equals(other.TheString)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.Delimiter != null ? this.Delimiter.hashCode() : 0);
        hash = 37 * hash + (this.TheString != null ? this.TheString.hashCode() : 0);
        return hash;
    }

}
