
package tmiranda.aar;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class PropertyElement {

    public static boolean HasPropertyElement(String PropertyName, String DefaultValue, String Element) {

        if (PropertyName==null || Element==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN,"HasPropertyElement: null parameter. " + PropertyName + ":" + Element);
            return false;
        }

        String contents = Configuration.GetProperty(PropertyName, DefaultValue);

        if (contents==null || contents.isEmpty())
            return false;

        return contents.contains(";"+ Element + ";");
    }

    public static boolean SetPropertyElement(String PropertyName, String DefaultValue, String Element) {

        if (PropertyName==null || Element==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN,"SetPropertyElement: null parameter. " + PropertyName + ":" + Element);
            return false;
        }

        String CurrentElements = Configuration.GetProperty(PropertyName, DefaultValue);

        if (CurrentElements==null || CurrentElements.isEmpty()) {
            Configuration.SetProperty(PropertyName, ";" + Element + ";");
            return true;
        }

        if (CurrentElements.contains(";" + Element + ";"))
            return false;

        String NewElements = CurrentElements + ";" + Element + ";";
        Configuration.SetProperty(PropertyName, NewElements);
        return true;
    }

    public static boolean RemovePropertyElement(String PropertyName, String DefaultValue, String Element) {

        if (PropertyName==null || Element==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN,"RemovePropertyElement: null parameter. " + PropertyName + ":" + Element);
            return false;
        }

        String CurrElements = Configuration.GetProperty(PropertyName, DefaultValue);

        if (CurrElements==null || CurrElements.isEmpty() || !CurrElements.contains(";" + Element + ";"))
            return false;

        String ElementRemoved = CurrElements.replaceAll(";"+Element+";","");
        Configuration.SetProperty(PropertyName, ElementRemoved);
        return true;
    }

    public static boolean HasPropertyElement(String PropertyName, String DefaultValue, int Element) {
        Integer E = Element;
        String S = E.toString();
        return HasPropertyElement(PropertyName, DefaultValue, S);
    }

    public static boolean SetPropertyElement(String PropertyName, String DefaultValue, int Element) {
        Integer E = Element;
        String S = E.toString();
        return SetPropertyElement(PropertyName, DefaultValue, S);
    }

    public static boolean RemovePropertyElement(String PropertyName, String DefaultValue, int Element) {
        Integer E = Element;
        String S = E.toString();
        return RemovePropertyElement(PropertyName, DefaultValue, S);
    }

    public static String[] GetPropertyArray(String PropertyName, String DefaultValue) {

        String[] ElementArray = null;
        String mediaElements = Configuration.GetProperty(PropertyName, DefaultValue);

        if (mediaElements != null){
            ElementArray = mediaElements.split(";*;");
        } else {
            ElementArray = null;
        }

        return ElementArray;
    }

    public static int[] GetPropertyArray(String PropertyName, int DefaultValue){

        String[] orarr=null;
        orarr = GetPropertyArray(PropertyName, java.lang.Integer.toString(DefaultValue));
        int [] newarr = null;
        for(int i=0;i<orarr.length;i++){
            newarr[i]=java.lang.Integer.parseInt(orarr[i]);
        }
        return newarr;
    }

    public static boolean InsertElementAtIndex(String PropertyName, String NewElement, int Index) {

        String[] CurrentProperties = GetPropertyArray(PropertyName, "");
        int i = 0;

        if (Index < 0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR,"InsertElementAtIndex: Negative integer passed to InsertElementAtIndex.");
            return false;
        }

        // Clear the existing properties.
        Configuration.SetProperty(PropertyName, null);

        // Add the properties that come before the new property.
        // Start at 1 because GetPropertyArray always returns a bogus first element.
        for (i=1; i<Index && i<CurrentProperties.length; i++) {
            if ((CurrentProperties[i] != null) && (!CurrentProperties[i].equals(""))) {
                SetPropertyElement(PropertyName, "", CurrentProperties[i]);
            }
        }

        // Add in the new Element.
        SetPropertyElement(PropertyName, "", NewElement);

        // Now add the rest of the Elements.
        for (int j=i; j<CurrentProperties.length; j++) {
            if ((CurrentProperties[j] != null) && (!CurrentProperties[j].equals(""))) {
                SetPropertyElement(PropertyName, "", CurrentProperties[j]);
            }
        }

        return true;
    }

    public static int GetElementIndex(String PropertyName, String Element) {
        if (!HasPropertyElement(PropertyName, "", Element)) {
            return -1;
        }

        String[] PropArray = GetPropertyArray(PropertyName, "");

        for (int i=0; i < PropArray.length; i++) {
            if (Element.compareTo(PropArray[i]) == 0) {
                return i;
            }
        }

        return -1;
    }

    public static boolean MoveElementToIndex(String PropertyName, String Element, int Index) {

        if (!HasPropertyElement(PropertyName, "", Element)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MoveElementToIndex: Could not find Element: " + PropertyName + ":" + Element);
            return false;
        }

        if (!RemovePropertyElement(PropertyName, "", Element)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MoveElementToIndex: Could not remove Element: " + PropertyName + ":" + Element);
            return false;
        }

        if (!InsertElementAtIndex(PropertyName, Element, Index)) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MoveElementToIndex: Could not insert Element: " + PropertyName + ":" + Element);
            return false;
        }

        return true;
    }
}
