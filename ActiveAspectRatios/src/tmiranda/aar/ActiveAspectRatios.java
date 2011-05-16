/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.aar;

import java.io.File;
import java.util.*;
import sagex.api.*;

/**
 * Terminology:
 * - Ratio: A Float representation of the aspect ratio.
 * - Time: Time in milliseconds into the recording.  The start time of the recording is
 *         actually the time the recording was started.
 *
 * @author Tom Miranda.
 */
public class ActiveAspectRatios {

    private boolean     isValid = true;

    // Key = A time in the MediaFile, relative to the start time.
    // Value = The aspect ratio.
    private Map<Long, Float> timeAspectRatioMap;
    private Long[]           times;
    private long             mediaFileStartTime;
    private long             mediaFileEndTime;
    private float            tolerance;

    public ActiveAspectRatios(Object MediaFile) {
        
        isValid = true;
        timeAspectRatioMap = new HashMap<Long, Float>();
        times = new Long[0];
        tolerance = 0F;

        if (MediaFile==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ActiveAspectRatios: Null MediaFile.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_MAX, "ActiveAspectRatios: Constructor for " + MediaFileAPI.GetMediaTitle(MediaFile));

        mediaFileStartTime = MediaFileAPI.GetStartForSegment(MediaFile, 0);
        mediaFileEndTime = MediaFileAPI.GetFileEndTime(MediaFile);

        int numberOfSegments = MediaFileAPI.GetNumberOfSegments(MediaFile);

        long segmentStartTimes[] = new long[numberOfSegments];
        for (int segment=0; segment < numberOfSegments; segment++) {
            segmentStartTimes[segment] = MediaFileAPI.GetStartForSegment(MediaFile, segment);
        }

        // This throws an exception.  sagex bug?
        //segmentStartTimes = MediaFileAPI.GetStartTimesForSegments(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Number of MediaFile start times " + segmentStartTimes.length);

        File files[] = MediaFileAPI.GetSegmentFiles(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Number of MediaFile segments " + files.length);

        if (files==null || files.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ActiveAspectRatios: Null or empty files.");
            isValid = false;
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: MediaFile start time " + MediaFileAPI.GetFileStartTime(MediaFile));

        String propertyLine = Configuration.GetProperty(API.PROPERTY_PATHMAPS, null);
        PathMapper pathMapper = new PathMapper(propertyLine);

        // Loop through all of the segments to build the complete aspect ratio map.
        for (int i=0; i<files.length; i++) {

            String path = files[i].getAbsolutePath();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: MediaFile path " + path);

            String mappedPath = pathMapper.replacePath(path);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: MediaFile mapped path " + mappedPath);

            // Replace the extension with .aspects.
            String basePath = mappedPath.substring(0, mappedPath.lastIndexOf("."));
            String aspectsPath = basePath + "." + API.ASPECTS_EXTENSION;

            // Get all of the information for this segment.  All times are relative to the
            // beginning of the segment.
            SegmentAspectRatios segmentInfo = new SegmentAspectRatios(aspectsPath);

            // Get all of the timestamps in the segment.
            Long timestamps[] = segmentInfo.getTimestamps().toArray(new Long[segmentInfo.getTimestamps().size()]);

            // Get the start time for this segment.
            Long segmentStartTime = segmentStartTimes[i];
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Segment start time " + segmentStartTime);

            // Loop through all of the timestamps in the segment.
            for (int j=0; j<timestamps.length; j++) {

                // Get a timestamp and its corresponding aspect ratio.
                Long startTime = timestamps[j];
                Float ratio = segmentInfo.getAspectForTime(startTime);
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Timestamp and ratio " + startTime + ":" + ratio);

                // Calculate the start time relative to the beginning of the show.
                Long realStartTime = segmentStartTime + startTime;
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Adjusted timestamp " + realStartTime);

                // Put it into the Map.
                timeAspectRatioMap.put(realStartTime, ratio);
            }
        }

        times = timeAspectRatioMap.keySet().toArray(new Long[timeAspectRatioMap.keySet().size()]);
        Arrays.sort(times);

        if (Log.getInstance().GetLogLevel() >= Log.LOGLEVEL_MAX) {

            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Times:");
            for (long t : times) {
                Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "  " + printTime(t));
            }
        }

        String toleranceStr = Configuration.GetProperty(API.PROPERTY_RATIO_TOLERANCE, "0.05");

        if (toleranceStr!=null && !toleranceStr.isEmpty()) {
            try {
                tolerance = Float.parseFloat(toleranceStr);
            } catch (NumberFormatException e) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "ActiveAspectRatios: Malformed tolerance " + toleranceStr);
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: Tolerance " + tolerance);
    }

    public Float getRatioForExactTime(Long Time) {
        return isValid ? timeAspectRatioMap.get(Time) : 0F;
    }

    public Float getRatioForTime(Long Time) {

        if (times.length==0)
            return 0F;

        Long previousTimestamp = times[0];

        for (Long thisTimestamp : times) {

            if (thisTimestamp > Time) {
                return timeAspectRatioMap.get(previousTimestamp);
            } else {
                previousTimestamp = thisTimestamp;
            }
        }

        return timeAspectRatioMap.get(previousTimestamp);
    }

    public Long[] getTimes() {
        return times;
    }

    public Set<Float> getRatios() {
        Set<Float> ratios = new HashSet<Float>();

        for (Long time : timeAspectRatioMap.keySet()) {
            ratios.add(timeAspectRatioMap.get(time));
        }

        return ratios;
    }

    public boolean hasExactRatio(Float Ratio) {

        for (Long time : times)
            if (timeAspectRatioMap.get(time) == Ratio)
                return true;

        return false;
    }

    public long getExactDurationOfRatio(Float Ratio) {

        Map<Float, Long> ratioTimeMap = new HashMap<Float, Long>();

        for (int i=0; i<times.length-1; i++) {
            Long thisTime = times[i];
            Long nextTime = i+1 < times.length ? times[i+1] : mediaFileEndTime;

            Float thisRatio = timeAspectRatioMap.get(thisTime);
            Long thisRatioCumulativeTime = ratioTimeMap.get(thisRatio);

            if (thisRatioCumulativeTime == null)
                thisRatioCumulativeTime = 0L;

            Long thisRatioNewTime = thisRatioCumulativeTime + (nextTime - thisTime);

            ratioTimeMap.put(thisRatio, thisRatioNewTime);
        }

        return ratioTimeMap.get(Ratio);
    }

    public long getDurationOfRatio(Float Ratio) {
        return getExactDurationOfRatio(getClosestRatioWithinTolerance(Ratio));
    }

    private List<Float> getRatiosWithinTolerance(Float Ratio) {

        List<Float> ratiosWithinTolerance = new ArrayList<Float>();

        // If there is no tolerance look for an exact match.
        if (tolerance==0F) {
            for (Long time : times) {
                Float thisRatio = timeAspectRatioMap.get(time);
                if (thisRatio==Ratio) {
                    ratiosWithinTolerance.add(Ratio);
                    return ratiosWithinTolerance;
                }
            }
            
            return ratiosWithinTolerance;
        }

        for (Long time : times) {
            Float thisRatio = timeAspectRatioMap.get(time);

            Float high = thisRatio * (1.0F + tolerance);
            Float low = thisRatio * (1.0F - tolerance);

            if (thisRatio >= low && thisRatio <= high) {
                ratiosWithinTolerance.add(thisRatio);
            }     
        }

        return ratiosWithinTolerance;
    }

    /**
     * Get the ratio closest to the ratio specified.
     * @param Ratio
     * @return -1 if none within tolerance;
     */
    public Float getClosestRatioWithinTolerance(Float Ratio) {

        List<Float> ratiosWithinTolerance = getRatiosWithinTolerance(Ratio);

        if (ratiosWithinTolerance==null || ratiosWithinTolerance.isEmpty())
            return -1F;

        Float closestRatio = Float.MAX_VALUE;

        for (Float thisRatio : ratiosWithinTolerance) {
            Float d1 = Math.abs(thisRatio - Ratio);
            Float d2 = Math.abs(closestRatio - Ratio);
            closestRatio = d1 < d2 ? thisRatio : closestRatio;
        }

        return closestRatio;
    }

    private String printTime(long time) {
        return Utility.PrintTimeFull(time);
    }
}
