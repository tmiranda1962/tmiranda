
package tmiranda.navix;

import java.util.regex.Pattern;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Utility methods.
 *
 * @author Tom Miranda.
 */
public class NaviX {

    public static final String VERSION = "0.01";

    /**
     * Get the version number of the JAR.
     *
     * @return
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     *
     * Some text has the embedded XML [COLOR=....] [/COLOR].  This method removes it.
     *
     * @param str
     * @return
     */
    public static String stripCOLOR(String str) {
        if (str==null)
            return str;

        str = str.replace("[/COLOR]", "");

        if (str != null) {
            str = str.replaceAll(Pattern.quote("[COLOR=") + "[a-fA-F0-9]{8}" + Pattern.quote("]"), "");
        }

        return str;
    }
    
    public static int numberCacherThreads() {
        int number = 0;
        
        for (Thread t : getAllThreads())
            if (t.getName().startsWith(Cacher.CACHER_THREAD_NAME))
                number++;
        
        return number;
    }

    private static Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup( );
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
        int nAlloc = thbean.getThreadCount( );
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[ nAlloc ];
            n = root.enumerate( threads, true );
        } while ( n == nAlloc );
        return java.util.Arrays.copyOf( threads, n );
    }

    private static ThreadGroup rootThreadGroup = null;

    private static ThreadGroup getRootThreadGroup( ) {
        if (rootThreadGroup != null )
            return rootThreadGroup;

        ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup ptg;

        while ((ptg = tg.getParent( )) != null )
            tg = ptg;

        rootThreadGroup = tg;
        return tg;
    }
}
