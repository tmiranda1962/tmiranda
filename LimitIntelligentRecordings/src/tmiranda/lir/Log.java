/*
 * Logging.
 */

package tmiranda.lir;

import sagex.api.*;

/**
 *
 * @author Tom Miranda.
 */
public class Log {

    /**
     * Possible values for the LogLevel.
     */

    public static final String  PROPERTY_LOGLEVEL = "lir/loglevel";
    public static final int     LOGLEVEL_NONE       = 100;
    public static final int     LOGLEVEL_ERROR      = 75;
    public static final int     LOGLEVEL_WARN       = 50;
    public static final int     LOGLEVEL_TRACE      = 25;
    public static final int     LOGLEVEL_VERBOSE    = 10;
    public static final int     LOGLEVEL_ALL        = 0;

    private static int CurrentLogLevel = LOGLEVEL_WARN;

    private static Log instance = new Log();

    /*
     * Private constructor.  Only let getInstance return a valid instance.
     */
    private Log() {};

    /**
     * Gets the one and only instance for the Log class.
     * <p>
     * @return  The instance for the Logger.
     */
    public static Log getInstance() {
        return instance;
    }

    /**
     * Destroy the Log class.
     */
    public void destroy() {
        instance = null;
    }

    /**
     * Writes a string to the logfile if the level indicated is at least at the current LogLevel.
     * <p>
     * @param level The LogLevel at wich this message should be written.
     * @param s     The String to write.
     */
    public void write(int level, String s) {
        if (level >= CurrentLogLevel)
            System.out.println("LIR: " + s);

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

    public void Write(int level, String s) {
        if (level >= CurrentLogLevel)
            System.out.println("LIR STV:"+s);
    }

}

