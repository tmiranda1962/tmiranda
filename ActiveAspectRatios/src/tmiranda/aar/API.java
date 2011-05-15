
package tmiranda.aar;

import java.util.*;
import sagex.api.*;

/**
 * ActiveAspectRatios support jar.
 *
 * @author Tom Miranda.
 */
public class API {

    public static String PROPERTY_MODE_MAP          = "aar/mode_map";
    public static String PROPERTY_EXCLUDED_CHANNELS = "aar/excluded_channels";
    public static String PROPERTY_EXCLUDED_SHOWS    = "aar/exluded_shows";

    public static Set<Float> getAllRatiosWithoutModes() {
        Set<Float> ratios = new HashSet<Float>();

        Object[] allMediaFiles = MediaFileAPI.GetMediaFiles("TV");

        if (allMediaFiles==null || allMediaFiles.length==0)
            return ratios;

        RatioModeMapper RMM = new RatioModeMapper(PROPERTY_MODE_MAP);

        for (Object mediaFile : allMediaFiles) {
            ActiveAspectRatios AAR = new ActiveAspectRatios(mediaFile);

            Set<Float> ratiosForMediaFile = AAR.getRatios();

            for (Float thisRatio : ratiosForMediaFile) {
                String thisMode = RMM.getMode(thisRatio);
                if (thisMode==null || thisMode.isEmpty())
                    ratios.add(thisRatio);
            }
        }

        return ratios;
    }

    public static String getLogLevelName() {
        switch (Log.getInstance().GetLogLevel()) {
            case Log.LOGLEVEL_ERROR:    return "Error";
            case Log.LOGLEVEL_TRACE:    return "Trace";
            case Log.LOGLEVEL_VERBOSE:  return "Verbose";
            case Log.LOGLEVEL_MAX:      return "Maximum";
            case Log.LOGLEVEL_WARN:     return "Warn";
            case Log.LOGLEVEL_NONE:     return "None";
            default:                    return "Unknown";
        }
    }

    public static int getLogLevel() {
        return Log.getInstance().GetLogLevel();
    }

    public static void setLogLevel(int newLevel) {
        Log.getInstance().SetLogLevel(newLevel);
    }

    public static boolean isChannelExcluded(String Channel) {
        ChannelExcluder excluder = new ChannelExcluder(PROPERTY_EXCLUDED_CHANNELS);
        return excluder.isChannelExcluded(Channel);
    }

    public static void addExcludedChannel(String Channel) {
        ChannelExcluder excluder = new ChannelExcluder(PROPERTY_EXCLUDED_CHANNELS);
        excluder.addExcludedChannel(Channel);
        return;
    }

    public static void removeExcludedChannel(String Channel) {
        ChannelExcluder excluder = new ChannelExcluder(PROPERTY_EXCLUDED_CHANNELS);
        excluder.removeExcludedChannel(Channel);
        return;
    }

    public static boolean isShowExcluded(String Show) {
        ShowExcluder excluder = new ShowExcluder(PROPERTY_EXCLUDED_SHOWS);
        return excluder.isShowExcluded(Show);
    }

    public static void addExcludedShow(String Show) {
        ShowExcluder excluder = new ShowExcluder(PROPERTY_EXCLUDED_SHOWS);
        excluder.addExcludedShow(Show);
        return;
    }

    public static void removeExcludedShow(String Show) {
        ShowExcluder excluder = new ShowExcluder(PROPERTY_EXCLUDED_SHOWS);
        excluder.removeExcludedShow(Show);
        return;
    }
}
