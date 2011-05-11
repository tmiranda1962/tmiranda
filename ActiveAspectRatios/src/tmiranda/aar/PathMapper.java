
package tmiranda.aar;

import java.util.*;
import java.io.File;
import sagex.api.*;

/**
 * Maps local paths to UNC paths.
 *
 * @author Tom Miranda.
 */
public class PathMapper {

    public String DELIMITER = ",";  // Delimits multiple entries.
    public String CONNECTOR = ">";  // Connects path to UNC.
    public String DRIVE_SEPARATOR = ":";

    private Map<String, String> pathMap;

    public PathMapper(String propertyLine) {
        pathMap = new HashMap<String, String>();

        if (propertyLine==null || propertyLine.isEmpty() || !Global.IsWindowsOS()) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "PathMapper: Null or empty propertyLine, or not Windows OS. " + propertyLine);
            return;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PathMapper: propertyLine " + propertyLine);

        String pairs[] = propertyLine.split(DELIMITER);
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "PathMapper: Found pairs " + pairs.length);

        for (String pair : pairs) {
            String parts[] = pair.split(CONNECTOR);

            if (parts.length != 2) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "PathMapper: Invalid syntax " + pair);
            } else {
                pathMap.put(parts[0], parts[1]);
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "PathMapper: Mapping " + parts[0] + "->" + parts[1]);
            }
        }
    }

    public String getPath(String localPath) {
        return pathMap.get(localPath);
    }

    public Map<String, String> getPathMap() {
        return pathMap;
    }

    public String replacePath(String unmappedPath) {

        //String unmappedLC = unmappedPath.toLowerCase();

        for (String key : pathMap.keySet()) {

            String prefix = key + DRIVE_SEPARATOR;
            if (unmappedPath.startsWith(prefix)) {
                Log.getInstance().write(Log.LOGLEVEL_TRACE, "replacePath: Found match " + key);
                return unmappedPath.replace(key+DRIVE_SEPARATOR, pathMap.get(key));
            }
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "replacePath: No match found for " + unmappedPath);
        return unmappedPath;
    }
}
