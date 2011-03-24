

package tmiranda.mus;

import java.util.*;
import java.io.*;
import sagex.api.*;
import sagex.UIContext;

/**
 * This class contains the methods needed to change a "standard" STV to a "MultiUser" STV.
 *
 * Put MultiUser.properties in the Sage install directory.
 *
 * Format of the file should be:
 *
 *  OldMethodName = NewMethodName[, options]
 *
 * Examples:
 *  GetScheduledRecordings = tmiranda_mus_API_getScheduledRecordings
 *  SomeMethod = SomeOtherMethod, !UID XXX
 *  OneMethod = AnotherMethod, !UID XXX YYY ZZZ
 *  Method2 = Method2Replacement, !UISTARTSWITH YYY
 *
 * @author Tom Miranda.
 */
public class MultiSTV {

    private final static String    PROPERTIES_FILENAME = "MultiUser.properties";

    private final static String WIDGET_TYPE_ATTRIBUTE  = "Attribute";
    private final static String WIDGET_PROPERTY_VALUE  = "Value";

    public final static String OPTION_NOT_UID =  "!UID";
    public final static String OPTION_NOT_UID_STARTS_WITH = "!UISTARTSWITH";

    private String          contextName;
    private UIContext       context;
    private MultiProperties properties;
    private boolean         isValid;
    private Set<String>     keys;
    private Object[]        allWidgets;
    private int             numChanges;
    private long            numProcessed;

    /**
     * Constructor.  Supply the name of the context that will be modified.  Use GetUIContextName()
     * to get the name of the context.
     *
     * @param ContextName
     */
    public MultiSTV(String ContextName) {
        contextName = ContextName;
        context = new UIContext(contextName);
        isValid = true;
        numChanges = 0;
        numProcessed = 0;

        properties = new MultiProperties();
        FileInputStream fis;

        // Open the InputStream.
        try {
            fis = new FileInputStream(PROPERTIES_FILENAME);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiSTV: Exception opening properties file. "  + e.getMessage());
            isValid = false;
            return;
        }

        // Load the properties.
        try {
            properties.load(fis);
        } catch (Exception e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiSTV: Exception loading properties file. "  + e.getMessage());
            isValid = false;
            return;
        }

        keys = properties.stringPropertyNames();
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiSTV: Found properties.");

        allWidgets = WidgetAPI.GetAllWidgets(context);

        if (allWidgets==null || allWidgets.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "MultiSTV: No Widgets found for context " + contextName);
            isValid = false;
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "MultiSTV: Found widgets for context " + allWidgets.length);
    }

    /**
     * Get the number of Widgets that will be scanned.
     * @return The number of Widgets in the specified Context.
     */
    public long numberWidgets() {
        return allWidgets.length;
    }

    /**
     * Get the number of keys in the properties file.  The number of keys usually corresponds
     * to the number of methods that will be replaced.
     * @return
     */
    public int numberKeys() {
        return keys.size();
    }

    /**
     * Get the total number of items that will be processed.  This method can be used to implement
     * a progress indicator.
     *
     * @return The total number of items to process.  This is actually the number of Widgets
     * in the STV * the number of keys in the properties file.
     */
    public long numberToProcess() {
        return numberWidgets() * numberKeys();
    }

    /**
     * Get the total number of items that have been processed.  This method can be used to
     * implement a progress indicator.
     *
     * @return
     */
    public long numberProcessed() {
        return numProcessed;
    }

    /**
     * Get the number of Widgets that have been modified.
     *
     * @return The number of Widgets in the STV that have been modified.
     */
    public int numberChanged() {
        return numChanges;
    }

    /**
     * Scans all Widgets in the STV replacing method names as specified in the MultiUser.properties
     * file.
     *
     * @param ShowOnly If set to true no changes will actually be made.  If set to false the
     * STV will be modified.
     * @return The number of Widgets that were modified.  If ShowOnly is set to true it will return
     * the number of Widgets that would have been modified.
     */
    public int modifyWidgets(boolean ShowOnly) {

        if (!isValid) {
            System.out.println("modifyWidgets: Invalid object.");
            return 0;
        }

        // Scan for the keys (which are methods to replace) in the Widgets.
        for (String key : keys) {

            // The first element of the property value is the name of the method to use in place of
            // the method specified by 'key'.
            List<String> newMethodAndParams = properties.getPropertyAndParams(key);

            if (newMethodAndParams==null || newMethodAndParams.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "modifyWidgets: null newMethodAndParms.");
                continue;
            }

            String newMethod = newMethodAndParams.get(0);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Processing " + key + ":" + newMethod + ":" + newMethodAndParams);

            Map<String, String> optionMap = properties.parseOptions(key);

            // Scan all Widgets for this key to see if we need to replace anything.
            for (Object widget : allWidgets) {

                numProcessed++;

                String widgetName = WidgetAPI.GetWidgetName(context, widget);
                String widgetType = WidgetAPI.GetWidgetType(context, widget);
                String widgetSymbol = WidgetAPI.GetWidgetSymbol(context, widget);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "modifyWidgets: Widget name, type and symbol " + widgetName + ":" + widgetType + ":" + widgetSymbol);

                if (optionMap.keySet().contains(OPTION_NOT_UID_STARTS_WITH) && widgetSymbol.startsWith(optionMap.get(OPTION_NOT_UID_STARTS_WITH))) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Skipping UID starting with " + optionMap.get(OPTION_NOT_UID_STARTS_WITH));
                    continue;
                }


                if (optionMap.keySet().contains(OPTION_NOT_UID)) {

                    // Get the raw String.
                    String UIDString = optionMap.get(OPTION_NOT_UID);

                    // Get a List of all the UIDs.
                    List<String> UIDs = MultiProperties.parseOptionParameters(UIDString);

                    if (UIDs.contains(widgetSymbol)) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Skipping UID " + widgetSymbol);
                        continue;
                    }
                }

               if (widgetType.equalsIgnoreCase(WIDGET_TYPE_ATTRIBUTE)) {

                    // If it's an attribute widget, see if the "value" (initialization) contains
                    // a method that needs to be relaced.
                    String widgetValue = WidgetAPI.GetWidgetProperty(widget, WIDGET_PROPERTY_VALUE);

                    if (widgetValue!=null && widgetValue.contains(key)) {

                        // Replace only if we have an exact match.
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Found possible Attribute Value match " + widgetValue);
                        ReplaceString RS = new ReplaceString(widgetValue);
                        String newValue = RS.replaceExactMatch(key, newMethod);

                        if (!widgetValue.equals(newValue)) {
                            Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Replacing with " + newValue);
                            if (!ShowOnly)
                                WidgetAPI.SetWidgetProperty(widget, WIDGET_PROPERTY_VALUE, newValue);
                            numChanges++;
                        }

                    }
                } else if(widgetName.contains(key)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Found possible Widget name match " + widgetName);
                    ReplaceString RS = new ReplaceString(widgetName);
                    String newName = RS.replaceExactMatch(key, newMethod);

                    if (!widgetName.equals(newName)) {
                        Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Replacing with " + newName);
                        if (!ShowOnly)
                            WidgetAPI.SetWidgetName(widget, newName);
                        numChanges++;
                    }
                }
            }

        }

        return numChanges;
    }

}

class ReplaceString {
    private String originalString;

    ReplaceString(String OriginalString) {
        originalString = OriginalString;
        return;
    }

    String replaceExactMatch(String ExactMatchString, String ReplaceWithString) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: originalString:ExactMatch:ReplaceWith " + originalString + ":" + ExactMatchString + ":" + ReplaceWithString);

        int startIndex = originalString.indexOf(ExactMatchString);

        // Check if the ExactMatchString appears anywhere in the originalString.
        if (startIndex==-1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: Not in string " + ExactMatchString + ":" + originalString);
            return originalString;
        }

        // See if there are characters before the ExactMatchString.
        if (startIndex > 0) {
            String prefix = originalString.substring(0, startIndex);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "replaceExactMatch: prefix <" + prefix + ">");
            String lastChar = prefix.substring(prefix.length()-1, prefix.length());
//System.out.println("LAST CHAR <" + lastChar + ">");
            lastChar = lastChar.replaceAll("[A-Za-z0-9]", "");

            // If it's empty it means the prefix is actually part of the name, so we do
            // not have an exact match.
            if (lastChar.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: prefix is part of the name.");
                return originalString;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: prefix is not part of the name.");
        }

        // See if there are character after the ExactMatchString.
        int endIndex = startIndex + ExactMatchString.length();

        if (endIndex < originalString.length()) {
            String postfix = originalString.substring(endIndex, originalString.length());
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "replaceExactMatch: postfix <" + postfix + ">");
            String firstChar = postfix.substring(0, 1);
//System.out.println("FIRST CHAR <" + firstChar + ">");
            firstChar = firstChar.replaceAll("[A-Za-z0-9]", "");

            // If it's empty it means the postfix is actually part of the name, so we do
            // not have an exact match.
            if (firstChar.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "replaceExactMatch: postfix is part of the name.");
                return originalString;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: postfix is not part of the name.");
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: Replacing " + ExactMatchString + "<->" + ReplaceWithString);
        return originalString.replace(ExactMatchString, ReplaceWithString);
    }
}

class MultiProperties extends Properties {

    public final static String ELEMENT_SEPARATOR = ",";
    public final static String PARAMETER_SEPARATOR = " ";

    MultiProperties() {
        super();
    }

    /**
     * Gets the raw property string for the specified key.
     * @param key
     * @return
     */
    String getRawProperty(String key) {
        return getProperty(key);
    }

    /**
     * Gets a List of all the elements in the property value string.
     * @param key
     * @return
     */
    List<String> getPropertyAndParams(String key) {
        List<String> propertyList = new ArrayList<String>();

        // Get the raw property, which may or may not contain extra parameters.
        String rawProperty = getProperty(key);

        // Make sure it's not null.
        if (rawProperty==null || rawProperty.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPropertyAndParams: null rawProperty.");
            return propertyList;
        }

        // Split the String to get the elements.
        String[] elements = rawProperty.split(ELEMENT_SEPARATOR);

        // Error?
        if (elements==null) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getPropertyAndParams: null elements.");
            return propertyList;
        }

        // Copy the elements to the List removing any leading or trailing spaces.
        for (int i=0; i<elements.length; i++) {
            String element = elements[i];
            propertyList.add(element.trim());
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getPropertyAndParams: propertyList " + propertyList);
        return propertyList;
    }

    /**
     * Parses the options returning a Map whose keys are the first argument of the option
     * (!UID for example) and the value is whatever follows.  Use parseOptionParameters to
     * further parse the line.
     * @param key
     * @return
     */
    Map<String, String> parseOptions(String key) {

        Map<String, String> parsedMap = new HashMap<String, String>();

        List<String> elements = getPropertyAndParams(key);

        // If there was an error, or there are no parameters in the property, just return an
        // empty Map.
        if (elements==null || elements.isEmpty() || elements.size()==1) {
            return parsedMap;
        }

        // Skip the first element because that is the name of the method we will use as a
        // replacement.
        for (int i=1; i<elements.size(); i++) {

            // Get the element which will be of the format "XX YY".
            String element = elements.get(i);

            if (element==null || element.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "parseOptions: null element.");
                continue;
            }

            String[] optionAndParm = element.split(PARAMETER_SEPARATOR);

            if (optionAndParm==null || optionAndParm.length < 2) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "parseOptions: Malformed parameters.");
                if (optionAndParm != null)
                    for (String e : optionAndParm)
                        Log.getInstance().write(Log.LOGLEVEL_WARN, "parseOptions: optionAndParm " + e);
                continue;
            }

            // The first element is the key and what follows are the values.
            String UIDs = null;
            for (i=1; i<optionAndParm.length; i++)
                if (i==1)
                    UIDs = optionAndParm[1];
                else
                    UIDs = UIDs + PARAMETER_SEPARATOR + optionAndParm[i];

            Log.getInstance().write(Log.LOGLEVEL_WARN, "parseOptions: Putting " + optionAndParm[0] + ":" + UIDs);
            parsedMap.put(optionAndParm[0].trim(), UIDs);
        }

        return parsedMap;
    }

    static List<String> parseOptionParameters(String S) {

        if (S==null || S.isEmpty())
            return new ArrayList<String>();

        String[] elements = S.split(PARAMETER_SEPARATOR);

        if (elements==null || elements.length==0)
            return new ArrayList<String>();

        return Arrays.asList(elements);
    }
}
