
package tmiranda.rpad;

import sagex.api.*;

/**
 *
 * @author Default
 */
public class Log {

    /**
     * Possible values for the LogLevel.
     */
    public static final int LOGLEVEL_NONE       = 100;
    public static final int LOGLEVEL_ERROR      = 75;
    public static final int LOGLEVEL_WARN       = 50;
    public static final int LOGLEVEL_TRACE      = 25;
    public static final int LOGLEVEL_VERBOSE    = 10;
    public static final int LOGLEVEL_ALL        = 0;

    private static int CurrentLogLevel = LOGLEVEL_TRACE;

    private static final Log instance = new Log();

    public static final String PROPERTY_LOGLEVEL = "rpad/loglevel";

    /*
     * Private constructor.  Only let getInstance return a valid instance.
     */
    private Log() {};

    /**
     * Gets the one and only instance for the Log class.
     * <p>
     * @return  The instance for hte Logger.
     */
    public static Log getInstance() {
        return instance;
    }

    /**
     * Writes a string to the logfile if the level indicated is at least at the current LogLevel.
     * <p>
     * @param level The LogLevel at wich this message should be written.
     * @param s     The String to write.
     */
    public void write(int level, String s) {
        if (level >= CurrentLogLevel)
            System.out.println("PR: " + s);
        if (level == Log.LOGLEVEL_ERROR)
            printStackTrace();
    }

    /**
     * Set the current LogLevel.
     * <p>
     * @param NewLevel  The new Loglevel.
     */
    public void SetLogLevel(Integer NewLevel) {
        CurrentLogLevel = NewLevel;
        Configuration.SetServerProperty(PROPERTY_LOGLEVEL, NewLevel.toString());
        Configuration.SaveProperties();
        System.out.println("RPAD: Setting loglevel to " + NewLevel.toString());
    }

    /**
     * Gets the current LogLevel.
     * <p>
     * @return  The current LogLevel.
     */
    public int GetLogLevel() {
        return CurrentLogLevel;
    }

    public static void Write(int level, String s) {
        if (level >= CurrentLogLevel)
            System.out.println("RPAD STV: " + s);
        if (level == Log.LOGLEVEL_ERROR)
            printStackTrace();
    }

    public static void printStackTrace() {
        Exception e = new Exception();
        e.printStackTrace();
    }

}