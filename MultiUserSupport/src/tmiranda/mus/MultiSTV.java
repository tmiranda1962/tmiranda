

package tmiranda.mus;

import java.util.*;
import java.io.*;
import sagex.api.*;
import sagex.UIContext;

/**
 * This class contains the methods needed to change a "standard" STV to a "MultiUser" STV.
 * @author Tom Miranda.
 */
public class MultiSTV {

    private final String    PROPERTIES_FILENAME = "MultiUser.properties";

    private final String WIDGET_TYPE_ATTRIBUTE  = "Attribute";
    private final String WIDGET_PROPERTY_VALUE  = "Value";

    private String      contextName;
    private UIContext   context;
    private Properties  properties;
    private boolean     isValid;
    private Set<String> keys;
    private Object[]    allWidgets;
    private Object[]    existingWidgets;
    private Object[]    importedWidgets;

    public MultiSTV(String ContextName, Object[] ExistingWidgets, Object[] ImportedWidgets) {
        contextName = ContextName;
        context = new UIContext(contextName);
        existingWidgets = ExistingWidgets;
        importedWidgets = ImportedWidgets;
        isValid = true;

        properties = new Properties();
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

    public int showChanges() {
        int numChanges = 0;

        if (!isValid) {
            return 0;
        }

        // Scan for the keys in the Widgets.
        for (String key : keys) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "showChanges: Processing " + key);

            for (Object widget : allWidgets) {
                String name = WidgetAPI.GetWidgetName(widget);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "showChanges: Widget name " + name);

                if (name.contains(key)) {
                    Log.getInstance().write(Log.LOGLEVEL_TRACE, "showChanges: Found Widget " + name);
                    numChanges++;
                }
            }
        }

        return numChanges;
    }

    // How to change Attibute Value?
    // Needs to be an exact match
    public int modifyWidgets(boolean ShowOnly) {
        int numChanges = 0;

        if (!isValid) {
            return 0;
        }

        // Scan for the keys (which are methods to replace) in the Widgets.
        for (String key : keys) {

            // The property value is the name of the method to use in place of the method
            // specified by 'key'.
            String newMethod = properties.getProperty(key);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "modifyWidgets: Processing " + key + ":" + newMethod);

            if (newMethod==null || newMethod.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "modifyWidgets: null value for " + key);
            } else {

                // Scan all Widgets for this key to see if we need to relace anything.
                for (Object widget : allWidgets) {
                    String widgetName = WidgetAPI.GetWidgetName(widget);
                    String widgetType = WidgetAPI.GetWidgetType(widget);
                    Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "modifyWidgets: Widget name and type " + widgetName + ":" + widgetType);

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
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: prefix <" + prefix + ">");
            String lastChar = prefix.substring(prefix.length()-1, prefix.length());
System.out.println("LAST CHAR <" + lastChar + ">");
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
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: postfix <" + postfix + ">");
            String firstChar = postfix.substring(0, 1);
System.out.println("FIRST CHAR <" + firstChar + ">");
            firstChar = firstChar.replaceAll("[A-Za-z0-9]", "");

            // If it's empty it means the postfix is actually part of the name, so we do
            // not have an exact match.
            if (firstChar.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: postfix is part of the name.");
                return originalString;
            }

            Log.getInstance().write(Log.LOGLEVEL_TRACE, "replaceExactMatch: postfix is not part of the name.");
        }

        return originalString.replace(ExactMatchString, ReplaceWithString);
    }
}
