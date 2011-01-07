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
public class DelimitedString {

    private String Delimiter = null;
    private String TheString = null;

    DelimitedString(String Str, String TheDelimiter) {
        Delimiter = TheDelimiter;
        TheString = (Str==null || Str.isEmpty() ? null : Str);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "DelimitedString: TheString " + TheString);
    }

    DelimitedString (String S1, String S2, String TheDelimiter) {
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

    boolean contains(String Element) {
        return delimitedStringToList(TheString, Delimiter).contains(Element);
    }

    boolean containsAll(List<String> Elements) {
        return delimitedStringToList(TheString, Delimiter).containsAll(Elements);
    }

    // Adds in place.
    void addUniqueElement(String Element) {

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

    // Removes in place.
    void removeElement(String Element) {

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

    static List<String> delimitedStringToList(String S, String D) {
        List<String> TheList = new ArrayList<String>();

        if (S == null || D == null || S.isEmpty()) {
            return TheList;
        }

        String[] StringArray = S.split(D);

        if (StringArray == null || StringArray.length==0) {
            return TheList;
        }

        return Arrays.asList(StringArray);
    }

    static String setToDelimitedString(Set<String> TheSet, String D) {

        String TheString = null;

        for (String Element : TheSet)
            if (TheString == null)
                TheString = Element;
            else
                TheString = TheString + D + Element;

        return TheString;
    }

    static String listToDelimitedString(List<String> TheSet, String D) {

        String TheString = null;

        for (String Element : TheSet)
            if (TheString == null)
                TheString = Element;
            else
                TheString = TheString + D + Element;

        return TheString;
    }

    static Set<String> delimitedStringToSet(String S, String D) {
        Set<String> TheSet = new HashSet<String>();
        List<String> TheList = delimitedStringToList(S, D);

        for (String E : TheList)
            TheSet.add(E);

        return TheSet;
    }

    private String listToString(List<String> TheList) {

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

    static synchronized void addToUserRecord(Object Record, String Flag, String User) {
        if (Record != null) {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(Record, Flag), Plugin.LIST_SEPARATOR);
            DS.addUniqueElement(User);
            UserRecordAPI.SetUserRecordData(Record, Flag, DS.toString());
        }
    }

    static synchronized void removeFromUserRecord(Object Record, String Flag, String User) {
        if (Record != null) {
            DelimitedString DS = new DelimitedString(UserRecordAPI.GetUserRecordData(Record, Flag), Plugin.LIST_SEPARATOR);
            DS.removeElement(User);
            UserRecordAPI.SetUserRecordData(Record, Flag, DS.toString());
        }
    }

    static synchronized void addToMediaFile(Object MediaFile, String Flag, String User) {
        if (MediaFile != null) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(MediaFile, Flag), Plugin.LIST_SEPARATOR);
            DS.addUniqueElement(User);
            MediaFileAPI.SetMediaFileMetadata(MediaFile, Flag, DS.toString());
        }
    }

    static synchronized void removeFromMediaFile(Object MediaFile, String Flag, String User) {
        if (MediaFile != null) {
            DelimitedString DS = new DelimitedString(MediaFileAPI.GetMediaFileMetadata(MediaFile, Flag), Plugin.LIST_SEPARATOR);
            DS.removeElement(User);
            MediaFileAPI.SetMediaFileMetadata(MediaFile, Flag, DS.toString());
        }
    }

    @Override
    public String toString() {
        return TheString;
    }

    public boolean isEmpty() {
        return (TheString==null || TheString.isEmpty());
    }
}
