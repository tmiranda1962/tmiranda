/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.mus;

import java.util.*;

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
System.out.println("THESTRING = <" + TheString + ">");
    }

    boolean contains(String Element) {
        return delimitedStringToList(TheString, Delimiter).contains(Element);
    }

    // Adds in place.
    void addUniqueElement(String Element) {

        if (contains(Element)) {
            return;
        }

        if (TheString == null || TheString.isEmpty())
            TheString = Element;
        else
            TheString = TheString + Delimiter + Element;
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
System.out.println("S AND TheString <" + S + "><" + Element + ">");
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

    @Override
    public String toString() {
        return TheString;
    }
}
