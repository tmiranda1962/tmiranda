
package tmiranda.ntr;

import java.util.*;
import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class Key {
    
    static final String ELEMENT_DELIMITER = "~";
    //static final String[] WEEKDAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

    private String channel;                 // The name of the channel.
    private String recurrence;              // The recurrence string, as used in the STV.
    private Long startTime;                 // The start time of this Airing.
    private Long endTime;                   // The end time of thie Airing.
    private Calendar startTimeCalendar;     // Calendar representation of the startTime.
    private Calendar endTimeCalendar;       // Calendar representation of the endTime.

    /**
     * Constructor.
     *
     * @param Channel       The Channel name.
     * @param Recurrence    The Recurrence String, as used in the STV.
     * @param StartTime     The start time of the Airing.
     * @param EndTime       The end time of the Airing.
     */
    public Key(String Channel, String Recurrence, long StartTime, long EndTime) {
        channel = Channel;
        recurrence = normalizeRecurrence(Recurrence);
        startTime = StartTime;
        endTime = EndTime;

        startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.setTimeInMillis(startTime);

        endTimeCalendar = Calendar.getInstance();
        endTimeCalendar.setTimeInMillis(endTime);

        Log.getInstance().write(Log.LOGLEVEL_MAX, "Key: Created " + channel + ELEMENT_DELIMITER + recurrence + ELEMENT_DELIMITER + startTime.toString() + ELEMENT_DELIMITER + endTime.toString());
    }

    /**
     * Make sure the Recurrence string uses three letter abbreviations for days and not two.
     *
     * @param Recurrence
     * @return
     */
    private static String normalizeRecurrence(String Recurrence) {

        if (Recurrence==null || Recurrence.isEmpty())
            return Recurrence;

        if (Recurrence.contains("Sun") || Recurrence.contains("Mon") || Recurrence.contains("Tue") || Recurrence.contains("Wed") || Recurrence.contains("Thu") || Recurrence.contains("Fri") || Recurrence.contains("Sat"))
            return Recurrence;

        String recurrence = Recurrence.toLowerCase();

        if (recurrence.startsWith("once") || recurrence.startsWith("daily") || recurrence.startsWith("weekly") || recurrence.startsWith("monthly") || recurrence.startsWith("continuous"))
            return Recurrence;

        recurrence = recurrence.replace("su", "Sun");
        recurrence = recurrence.replace("mo", "Mon");
        recurrence = recurrence.replace("tu", "Tue");
        recurrence = recurrence.replace("we", "Wed");
        recurrence = recurrence.replace("th", "Thu");
        recurrence = recurrence.replace("fr", "Fri");
        recurrence = recurrence.replace("sa", "Sat");

        return recurrence;
    }

    static String makeRawMapKey(Object Airing) {
        String channel = AiringAPI.GetAiringChannelName(Airing);
        String recurrence = AiringAPI.GetScheduleRecordingRecurrence(Airing);
        Long startTime = AiringAPI.GetAiringStartTime(Airing);
        Long endTime = AiringAPI.GetAiringEndTime(Airing);
        Key key = new Key(channel, recurrence, startTime, endTime);
        return key.getRawMapKey();
    }

    /**
     * Make a key for the Airing.  "Raw" means the start time and end time are specific
     * to this Airing so they will probably not match what's stored in the name mapping property.
     *
     * @param Airing
     * @return
     */
    static Key makeRawKey(Object Airing) {
        return Key.toKey(makeRawMapKey(Airing));
    }


    String getRawMapKey() {
        return channel + ELEMENT_DELIMITER + recurrence + ELEMENT_DELIMITER + startTime.toString() + ELEMENT_DELIMITER + endTime.toString();
    }

    @Override
    public String toString() {
        return getRawMapKey();
    }

    /**
     * Convert a Map key into a Key Object.
     * @param keyString
     * @return
     */
    static Key toKey(String keyString) {
        if (keyString==null || keyString.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Key.toKey: null keyString.");
            return new Key("","",0,0);
        }

        String[] parts = keyString.split(ELEMENT_DELIMITER);

        if (parts==null || parts.length!=4) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Key.toKey: Malformed keyString " + keyString);
            return new Key("","",0,0);
        }

        return new Key(parts[0], parts[1], stringToLong(parts[2]), stringToLong(parts[3]));

    }

    /**
     * Get the key in the property map that matches this key.
     *
     * @return
     */
    String getMatchingKey() {

        Map<String, String> nameMap = PropertyElement.getNameMap();
        Set<String> nameMapKeys = nameMap.keySet();

        // Search through the Set looking for a key that matches this one.
        for (String nameMapKey : nameMapKeys) {

            // Convert the Map key to a Key Object.
            Key key = Key.toKey(nameMapKey);

            if (key.equals(this)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.getMatchingKey: Found matching key " + nameMapKey);
                return nameMapKey;
            }
        }

        return null;
    }

    /**
     * Safely parse a String to a long.
     * @param S
     * @return
     */
    private static long stringToLong(String S) {
        if (S==null || S.isEmpty())
            return 0;

        long result;

        try {
            result = Long.parseLong(S);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "Key.stringToLong: Malformed long " + S);
            result = 0;
        }

        return result;
    }

    long duration () {
        return endTime - startTime;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            Log.getInstance().write(Log.LOGLEVEL_MAX, "Key.equals: null Object.");
            return false;
        }

        if (getClass() != obj.getClass()) {
            Log.getInstance().write(Log.LOGLEVEL_MAX, "Key.equals: Class does not match.");
            return false;
        }

        final Key other = (Key) obj;

        if ((this.channel == null) ? (other.channel != null) : !this.channel.equalsIgnoreCase(other.channel)) {
            return false;
        }

        if ((this.recurrence == null) ? (other.recurrence != null) : !this.recurrence.equalsIgnoreCase(other.recurrence)) {
            Log.getInstance().write(Log.LOGLEVEL_MAX, "Key.equals: Recurrence does not match " + this.recurrence + ":" + other.recurrence);
            return false;
        }

        if (this.duration() != other.duration()) {
            Log.getInstance().write(Log.LOGLEVEL_MAX, "Key.equals: Duration does not match " + this.duration() + ":" + other.duration());
            return false;
        }

        if (!isMultiple(other)) {
            Log.getInstance().write(Log.LOGLEVEL_MAX, "Key.equals: Is not a time multiple.");
            return false;
        }

        return true;
    }

    /**
     * Checks if the start time is a multiple of the otherKey's start time and the
     * end time is a multiple of the otherKey's end time.
     *
     * MUST first ensure the recurrences are the same.
     * 
     * @param otherCalendar
     * @return
     */
    private boolean isMultiple(Key otherKey) {

        if (recurrence.equalsIgnoreCase("once") || recurrence.equalsIgnoreCase("continuous")) {

            // If this is a one time shot or a continuous recording, "multiple" is meaninngless.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.isMultiple: Once or Continuous.");
            return true;

        } else if (recurrence.equalsIgnoreCase("daily")) {

            // If it's a daily recording make sure the hour fields match.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.isMultiple: Daily.");
            return hourOfDayMatches(otherKey);

        } else if (recurrence.equalsIgnoreCase("weekly")) {

            // If it's a weekly recording make sure the day of week fields match.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.isMultiple: Weekly.");
            return dayOfWeekMatches(otherKey);

        } else if (recurrence.equalsIgnoreCase("monthly")) {

            // If it's a monthly recording make sure the day of month fields match.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.isMultiple: Monthly.");
            return dayOfMonthMatches(otherKey);

        } else {

            // It's probably for specific days.
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "Key.isMultiple: Specific days " + recurrence);
            return hourOfDayMatches(otherKey);
        }
    }

    private boolean hourOfDayMatches(Key otherKey) {

        return startTimeCalendar.get(Calendar.HOUR_OF_DAY) == otherKey.startTimeCalendar.get(Calendar.HOUR_OF_DAY) &&
                endTimeCalendar.get(Calendar.HOUR_OF_DAY) == otherKey.endTimeCalendar.get(Calendar.HOUR_OF_DAY);
    }

    private boolean dayOfWeekMatches(Key otherKey) {
        return startTimeCalendar.get(Calendar.DAY_OF_WEEK) == otherKey.startTimeCalendar.get(Calendar.DAY_OF_WEEK) &&
                endTimeCalendar.get(Calendar.DAY_OF_WEEK) == otherKey.endTimeCalendar.get(Calendar.DAY_OF_WEEK);
    }

    private boolean dayOfMonthMatches(Key otherKey) {
        return startTimeCalendar.get(Calendar.DAY_OF_MONTH) == otherKey.startTimeCalendar.get(Calendar.DAY_OF_MONTH) &&
                endTimeCalendar.get(Calendar.DAY_OF_MONTH) == otherKey.endTimeCalendar.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.channel != null ? this.channel.hashCode() : 0);
        hash = 41 * hash + (this.recurrence != null ? this.recurrence.hashCode() : 0);
        hash = 41 * hash + (this.startTime != null ? this.startTime.hashCode() : 0);
        hash = 41 * hash + (this.endTime != null ? this.endTime.hashCode() : 0);
        return hash;
    }

}
