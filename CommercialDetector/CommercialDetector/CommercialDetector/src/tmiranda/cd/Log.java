/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.cd;

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

    private static int CurrentLogLevel = LOGLEVEL_WARN;

    private static Log instance = new Log();

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
            System.out.println("CD: " + s);
    }

    /**
     * Set the current LogLevel.
     * <p>
     * @param NewLevel  The new Loglevel.
     */
    public void SetLogLevel(Integer NewLevel) {
        CurrentLogLevel = NewLevel;
        Configuration.SetServerProperty("cd/loglevel", NewLevel.toString());
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
            System.out.println("CD STV:"+s);
    }

}
