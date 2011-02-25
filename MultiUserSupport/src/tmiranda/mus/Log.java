
package tmiranda.mus;

import sagex.api.*;

/**
 * Logging methods. Singleton class.
 * @author Tom Miranda
 */
public class Log {

    /**
     * Possible values for the LogLevel.
     */

    /**
     * Property used to store the current log level.
     */
    public static final String  PROPERTY_LOGLEVEL = "mus/loglevel";

    public static final int     LOGLEVEL_NONE       = 100;
    public static final int     LOGLEVEL_ERROR      = 75;
    public static final int     LOGLEVEL_WARN       = 50;
    public static final int     LOGLEVEL_TRACE      = 25;
    public static final int     LOGLEVEL_VERBOSE    = 10;
    public static final int     LOGLEVEL_ALL        = 0;

    private static int CurrentLogLevel = LOGLEVEL_WARN;

    private static final String     DEFAULT_LOGLEVEL = "50";

    private static Log instance = null;

    /*
     * Private constructor.  Only let getInstance return a valid instance.
     */
    private Log() {
        String LoglevelString = Configuration.GetServerProperty(PROPERTY_LOGLEVEL, DEFAULT_LOGLEVEL);

        try {
            CurrentLogLevel = Integer.parseInt(LoglevelString);
            System.out.println("MUS: Log - Setting CurrentLoglevel to " + LoglevelString);
        } catch (NumberFormatException e) {
            CurrentLogLevel = LOGLEVEL_WARN;
            System.out.println("MUS: Log - Malformed loglevel, setting to WARN. " + e.getMessage());
        }
    }

    /**
     * Gets the one and only instance for the Log class.
     * <p>
     * @return  The instance for the Logger.
     */
    public static Log getInstance() {
        return instance;
    }

    /**
     * Destroy the logger.
     */
    public static void destroy() {
        instance = null;
    }

    /**
     * Restart the logger.
     */
    public static void start() {
        instance = new Log();
    }

    /**
     * Writes a string to the logfile if the level indicated is at least at the current LogLevel.
     * <p>
     * @param level The LogLevel at which this message should be written.
     * @param s     The String to write.
     */
    public void write(int level, String s) {
        if (level >= CurrentLogLevel)
            System.out.println("MUS: " + s);

        if (level == Log.LOGLEVEL_ERROR) {
            Exception e = new Exception();
            e.printStackTrace();
        }
    }

    /**
     * Set the current LogLevel.
     * <p>
     * @param NewLevel  The new Loglevel.
     */
    public void SetLogLevel(Integer NewLevel) {
        CurrentLogLevel = NewLevel;
        Configuration.SetServerProperty(PROPERTY_LOGLEVEL, NewLevel.toString());
    }

    /**
     * Gets the current LogLevel.
     * <p>
     * @return  The current LogLevel.
     */
    public int GetLogLevel() {
        return CurrentLogLevel;
    }
}
