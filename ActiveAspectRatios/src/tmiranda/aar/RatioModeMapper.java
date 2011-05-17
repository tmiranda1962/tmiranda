
package tmiranda.aar;

import java.util.*;
import sagex.api.*;

/**
 * Maps a specific aspect ratio to a Sage aspect mode.
 * @author Tom Miranda
 */
public class RatioModeMapper {

    private String PAIR_DELIMITER = "-";

    private Map<Float, String>  ratioModeMap;
    private String              rawProperty;
    private String              propertyName;

    public RatioModeMapper(String PropertyName) {

        propertyName = PropertyName;
        rawProperty = null;
        ratioModeMap = new HashMap<Float, String>();

        if (PropertyName==null || PropertyName.isEmpty())
            return;

        rawProperty = Configuration.GetProperty(PropertyName, null);

        if (rawProperty==null || rawProperty.isEmpty())
            return;

        String elements[] = rawProperty.split(";*;");

        if (elements==null || elements.length==0)
            return;

        for (String element : elements) {
            String ratioMode[] = element.split(PAIR_DELIMITER);

            if (ratioMode==null || ratioMode.length!=2) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RatioModeMapper: Invalid ratio-mode entry " + element);
            } else {

                Float ratio;

                try {
                    ratio = Float.parseFloat(ratioMode[0]);
                } catch (NumberFormatException e) {
                    ratio = 0F;
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "RatioModeMapper: Malformed aspect " + ratioMode[0]);
                }

                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "RatioModeMapper: Found " + ratio + ":" + ratioMode[1]);
                ratioModeMap.put(ratio, ratioMode[1]);
            }
        }
    }

    public String getExactMode(Float ratio) {
        return ratioModeMap.get(ratio);
    }

    public boolean hasExactMode(Float ratio) {
        return ratioModeMap.get(ratio) != null;
    }

    public String getMode(Float ratio) {
        String mode = ratioModeMap.get(ratio);

        if (mode!=null && !mode.isEmpty())
            return mode;
        else
            return getModeWithinTolerance(ratio);
    }

    public String getModeWithinTolerance(Float ratio) {

        /*******************
        String toleranceStr = Configuration.GetProperty(PROPERTY_RATIO_TOLERANCE, "0.05");

        if (toleranceStr==null || toleranceStr.isEmpty())
            return null;

        Float tolerance = 0F;

        try {
            tolerance = Float.parseFloat(toleranceStr);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getMode: Malformed tolerance " + toleranceStr);
            return null;
        }

        if (tolerance==0)
            return ratioModeMap.get(ratio);

        Set<Float> ratios = ratioModeMap.keySet();

        if (ratios==null || ratios.isEmpty())
            return null;

        Float closestRatio = Float.MAX_VALUE;

        for (Float thisRatio : ratios) {

            Float high = thisRatio * (1.0F + tolerance);
            Float low = thisRatio * (1.0F - tolerance);

            if (thisRatio >= low && thisRatio <= high) {
                Float d1 = Math.abs(thisRatio - ratio);
                Float d2 = Math.abs(closestRatio - ratio);
                closestRatio = d1 < d2 ? thisRatio : closestRatio;
            }
        }
         *****************/
        Float closestRatio = getClosestRatioWithMode(ratio);
        return closestRatio == 0F ? null : ratioModeMap.get(closestRatio);
    }

    public Float getClosestRatioWithMode(Float ratio) {

        String toleranceStr = Configuration.GetProperty(API.PROPERTY_RATIO_TOLERANCE, "0.05");

        if (toleranceStr==null || toleranceStr.isEmpty())
            return 0F;

        Float tolerance = 0.0F;

        try {
            tolerance = Float.parseFloat(toleranceStr);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getClosestRatioWithMode: Malformed tolerance " + toleranceStr);
            return 0F;
        }

        Set<Float> ratios = ratioModeMap.keySet();

        if (ratios==null || ratios.isEmpty())
            return 0F;

        Float closestRatio = Float.MAX_VALUE;
        Float smallestDifference = Float.MAX_VALUE;

        for (Float thisRatio : ratios) {

            // Calculate the highest and lowest values that the ratio can be and
            // still fall within the tolerance guidelines.
            Float high = thisRatio * (1.0F + tolerance);
            Float low = thisRatio * (1.0F - tolerance);

            // See if the ratio falls within the tolerance range.
            if (ratio >= low && ratio <= high) {

                // Calculate how far this is from the ratio.
                Float d1 = Math.abs(thisRatio - ratio);

                if (d1 < smallestDifference) {
                    closestRatio = thisRatio;
                    smallestDifference = d1;
                }
            }
        }

        return closestRatio==Float.MAX_VALUE ? 0F : closestRatio;
    }

    public void addRatio(Float ratio, String mode) {

        if (mode==null || mode.isEmpty()) {
            return;
        }

        if (ratioModeMap.get(ratio)!= null) {
            removeRatio(ratio, mode);
        }

        ratioModeMap.put(ratio, mode);
        addPropertyElement(ratio, mode);
    }

    public void removeRatio(Float ratio, String mode) {
        if (ratioModeMap.get(ratio)!= null) {
            ratioModeMap.remove(ratio);
            removePropertyElement(ratio, mode);
        }
    }

    private void removePropertyElement(Float ratio, String mode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty before " + rawProperty);
        String element = ratio.toString() + PAIR_DELIMITER + mode;
        rawProperty.replaceAll(";"+element+";", "");
        Configuration.SetProperty(propertyName, rawProperty);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "removePropertyElement: rawProperty after " + rawProperty);
    }

    private void addPropertyElement(Float ratio, String mode) {
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty before " + rawProperty);
        if (rawProperty==null || rawProperty.isEmpty())
            rawProperty = ";" + ratio.toString() + PAIR_DELIMITER + mode + ";";
        else
            rawProperty = rawProperty + ";" + ratio.toString() + PAIR_DELIMITER + mode + ";";
        Configuration.SetProperty(propertyName, rawProperty);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "addPropertyElement: rawProperty after " + rawProperty);
    }
}
