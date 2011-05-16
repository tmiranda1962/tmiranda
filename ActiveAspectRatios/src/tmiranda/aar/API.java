
package tmiranda.aar;

import java.util.*;
import java.io.*;
import sagex.api.*;

/**
 * ActiveAspectRatios support jar.
 *
 * @author Tom Miranda.
 */
public class API {

    public static final String VERSION                      = "0.01";
    public static final String PROPERTY_MODE_MAP            = "aar/mode_map";
    public static final String PROPERTY_EXCLUDED_CHANNELS   = "aar/excluded_channels";
    public static final String PROPERTY_EXCLUDED_SHOWS      = "aar/exluded_shows";
    public static final String PROPERTY_RATIO_TOLERANCE     = "aar/ratio_tolerance";
    public static final String ASPECTS_EXTENSION            = "aspects";
    public static final String PROPERTY_PATHMAPS            = "aar/path_maps";

    public static String getVersion() {
        return VERSION;
    }

    public static boolean hasAspectsFile(Object MediaFile) {
        if (MediaFile==null)
            return false;

        File files[] = MediaFileAPI.GetSegmentFiles(MediaFile);

        String propertyLine = Configuration.GetProperty(API.PROPERTY_PATHMAPS, null);
        PathMapper pathMapper = new PathMapper(propertyLine);

        for (File segmentFile : files) {
            String path = segmentFile.getAbsolutePath();
            String mappedPath = pathMapper.replacePath(path);
            String basePath = mappedPath.substring(0, mappedPath.lastIndexOf("."));
            String aspectsPath = basePath + "." + API.ASPECTS_EXTENSION;
            File aspectsFile = new File(aspectsPath);
            if (aspectsFile.exists())
                return true;
        }

        return false;
    }

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

    public static String convertMillisToTimeString(long time) {

        String s = Utility.PrintDurationWithSeconds(time);

        if (s==null || s.isEmpty())
            return s;

        s = s.replace("hour", "hr");
        s = s.replace("minute", "mn");
        s = s.replace("second", "sec");
        return s;
    }
}
