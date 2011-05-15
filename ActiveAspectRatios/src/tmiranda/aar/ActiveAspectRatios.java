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

    public String ASPECTS_EXTENSION = "aspects";
    public String PROPERTY_PATHMAPS = "aar/path_maps";

    private boolean     isValid = true;

    // Key = A time in the MediaFile, relative to the start time.
    // Value = The aspect ratio.
    private Map<Long, Float> timeAspectRatioMap;
    private Long[]           times;

    public ActiveAspectRatios(Object MediaFile) {
        
        isValid = true;
        timeAspectRatioMap = new HashMap<Long, Float>();
        times = new Long[0];

        if (MediaFile==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ActiveAspectRatios: Null MediaFile.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_MAX, "ActiveAspectRatios: Constructor for " + MediaFileAPI.GetMediaTitle(MediaFile));

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

        String propertyLine = Configuration.GetProperty(PROPERTY_PATHMAPS, null);
        PathMapper pathMapper = new PathMapper(propertyLine);

        // Loop through all of the segments to build the complete aspect ratio map.
        for (int i=0; i<files.length; i++) {

            String path = files[i].getAbsolutePath();
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: MediaFile path " + path);

            String mappedPath = pathMapper.replacePath(path);
            Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "ActiveAspectRatios: MediaFile mapped path " + mappedPath);

            // Replace the extension with .aspects.
            String basePath = mappedPath.substring(0, mappedPath.lastIndexOf("."));
            String aspectsPath = basePath + "." + ASPECTS_EXTENSION;

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

    private String printTime(long time) {
        return Utility.PrintTimeFull(time);
    }
}
