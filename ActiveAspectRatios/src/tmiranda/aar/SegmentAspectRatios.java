
package tmiranda.aar;

import java.util.*;
import java.io.*;

/**
 *
 * @author Tom Miranda.
 */
public class SegmentAspectRatios {

    public static final String TIMESTAMP_DELIMITER = ":";
    public static final String MILLI_DELIMITER = "\\.";

    /*
     * Key is timestamp in milliseconds relative to the start of the file.
     * comskipAspectInformation is the data corresponding to the line in the .aspects file.
     */
    private Map<Long, comskipAspectInformation> infoMap;

    public SegmentAspectRatios(String FilePath) {

        infoMap = new HashMap<Long, comskipAspectInformation>();

        if (FilePath==null || FilePath.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SegmentAspectRatios: null or empty FilePath.");
            return;
        }

        File aspectsFile = new File(FilePath);
        if (!aspectsFile.exists()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "SegmentAspectRatios: No .aspects file " + FilePath);
            return;
        }

        List<String> fileLines = readAspectsFile(FilePath);

        // Process every line in the file.
        for (String line : fileLines) {

            comskipAspectInformation comskipAspectInfo = new comskipAspectInformation(line);

            String timestamp = comskipAspectInfo.getTimestamp();

            // Now split into Hours:Minutes:Seconds.Milliseconds
            String parts[] = timestamp.split(TIMESTAMP_DELIMITER);

            if (parts.length == 3) {

                long hours = stringToLong(parts[0]) * 60L * 60L * 1000L;
                long minutes = stringToLong(parts[1]) * 60L * 1000L;

                String secAndMsec[] = parts[2].split(MILLI_DELIMITER);

                if (secAndMsec.length == 2) {
                    long seconds = stringToLong(secAndMsec[0]) * 1000L;
                    long startTime = hours + minutes + seconds + stringToLong(secAndMsec[1]);

                    infoMap.put(startTime, comskipAspectInfo);

                } else {
                    Log.getInstance().write(Log.LOGLEVEL_WARN, "SegmentAspectRatios: Malformed millisecond timestamp <" + timestamp + ">");
                }

            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "SegmentAspectRatios: Malformed timestamp <" + timestamp + ">");
            }
        }
    }

    /**
     * Reads the entire text file into an array.
     * @param FilePath
     * @return Each element in the List corresponds to a line in the text file.
     */
    private List<String> readAspectsFile(String FilePath) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "readAspectsFile: Reading from " + FilePath);

        List<String> textList = new ArrayList<String>();

        Scanner scanner;

        try {
            scanner = new Scanner(new File(FilePath));
        } catch (FileNotFoundException e) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "readAspectsFile: File not found " + FilePath);
            return textList;
        }

        while (scanner.hasNextLine()) {
            textList.add(scanner.nextLine());
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "readAspectsFile: Read lines " + textList.size());
        return textList;
    }

    private List<String> OLDreadAspectsFile(String FilePath) {

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "readAspectsFile: Reading from " + FilePath);

        List<String> textList = new ArrayList<String>();

        try {

            FileInputStream fstream = new FileInputStream(FilePath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;

            while ((strLine = br.readLine()) != null)   {
                textList.add(strLine);
            }

            in.close();

        } catch (Exception e){
          Log.getInstance().write(Log.LOGLEVEL_ERROR, "readAspectsFile: Exception " + e.getMessage());
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "readAspectsFile: Read lines " + textList.size());
        return textList;
        
    }

    private long stringToLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "stringToLong: Malformed number " + str);
            return 0L;
        }
    }

    public Set<Long> getTimestamps() {
        return infoMap.keySet();
    }

    public comskipAspectInformation getInfoForTime(Long time) {
        return infoMap.get(time);
    }

    public float getAspectForTime(Long time) {
        return infoMap.get(time).getAspect();
    }
}


/**
 * Class to parse the raw line found in the .aspects file.
 *
 * A sample line:
 *  0:54:25.16  720x 480 1.51 minX=   1, minY=   1, maxX= 720, maxY= 480
 *
 * @author Tom Miranda.
 */
class comskipAspectInformation {

    String timestamp    = null;
    String XandY        = null;
    float  aspect       = 0;
    String rawLine      = null;

    comskipAspectInformation(String line) {

        rawLine = line;

        // Break down into components;
        String parts[] = line.replaceAll("  ", " ").split(" ");

        if (parts==null || parts.length<4) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "comskipAspectInformation: Malformed line " + line);
            return;
        }

        // Timestamp is the first compenent.
        timestamp = parts[0];

        String floatStr = null;

        if (parts[1].endsWith("x")) {
            XandY = parts[1]+parts[2];
            floatStr = parts[3];
        }  else {
            XandY = parts[1];
            floatStr = parts[2];
        }

        try {
            aspect = Float.parseFloat(floatStr);
        } catch (NumberFormatException e) {
            aspect = 0;
            Log.getInstance().write(Log.LOGLEVEL_WARN, "comskipAspectInformation: Malformed aspect " + floatStr);
        }

        Log.getInstance().write(Log.LOGLEVEL_VERBOSE, "comskipAspectInformation: Parsed <" + line + ">" + ":" + timestamp + ":" + XandY + ":" + aspect);
    }

    String getTimestamp() {
        return timestamp;
    }

    String getXandY() {
        return XandY;
    }

    float getAspect() {
        return aspect;
    }

    String getRawLine() {
        return rawLine;
    }
}
