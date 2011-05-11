/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.aar;

import java.io.File;
import java.util.*;
import sagex.api.*;

/**
 *
 * @author Default
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

        if (MediaFile==null) {
            isValid = false;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "ActiveAspectRatios: Null MediaFile.");
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Constructor for " + MediaFileAPI.GetMediaTitle(MediaFile));
Log.getInstance().SetLogLevel(Log.LOGLEVEL_VERBOSE);
        int numberOfSegments = MediaFileAPI.GetNumberOfSegments(MediaFile);

        long segmentStartTimes[] = new long[numberOfSegments];
        for (int segment=0; segment < numberOfSegments; segment++) {
            segmentStartTimes[segment] = MediaFileAPI.GetStartForSegment(MediaFile, segment);
        }

        // This throws an exception.  sagex bug?
        //segmentStartTimes = MediaFileAPI.GetStartTimesForSegments(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Number of MediaFile start times " + segmentStartTimes.length);

        File files[] = MediaFileAPI.GetSegmentFiles(MediaFile);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Number of MediaFile segments " + files.length);

        if (files==null || files.length==0) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "ActiveAspectRatios: Null or empty files.");
            isValid = false;
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: MediaFile start time " + MediaFileAPI.GetFileStartTime(MediaFile));

        String propertyLine = Configuration.GetProperty(PROPERTY_PATHMAPS, null);
        PathMapper pathMapper = new PathMapper(propertyLine);

        // Loop through all of the segments to build the complete aspect ratio map.
        for (int i=0; i<files.length; i++) {

            String path = files[i].getAbsolutePath();
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: MediaFile path " + path);

            String mappedPath = pathMapper.replacePath(path);
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: MediaFile mapped path " + mappedPath);

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
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Segment start time " + segmentStartTime);

            // Loop through all of the timestamps in the segment.
            for (int j=0; j<timestamps.length; j++) {

                // Get a timestamp and its corresponding aspect ratio.
                Long startTime = timestamps[j];
                Float ratio = segmentInfo.getAspectForTime(startTime);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Timestamp and ratio " + startTime + ":" + ratio);

                // Calculate the start time relative to the beginning of the show.
                Long realStartTime = segmentStartTime + startTime;
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "ActiveAspectRatios: Adjusted timestamp " + realStartTime);

                // Put it into the Map.
                timeAspectRatioMap.put(realStartTime, ratio);
            }
        }

        times = timeAspectRatioMap.keySet().toArray(new Long[timeAspectRatioMap.keySet().size()]);
    }

    public Float getRatioForExactTime(Long Time) {
        return isValid ? timeAspectRatioMap.get(Time) : 0F;
    }

    public Float getRatioForTime(Long Time) {
System.out.println("getRatioForTime checking " + Time);
        if (times.length==0)
            return 0F;

        Long previousTimestamp = times[0];

        for (Long thisTimestamp : times) {
System.out.println("getRatioForTime this and previous " + thisTimestamp + ":" + previousTimestamp);
            if (thisTimestamp > Time) {
System.out.println("getRatioForTime FOUND IT");
                return timeAspectRatioMap.get(previousTimestamp);
            } else {
                previousTimestamp = thisTimestamp;
            }
        }
System.out.println("getRatioForTime LAST TIMESTAMP.");
        return timeAspectRatioMap.get(previousTimestamp);
    }
}
