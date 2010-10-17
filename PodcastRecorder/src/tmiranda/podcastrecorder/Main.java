/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

/**
 *
 * @author Default
 */
public class Main {

    public static void main(String[] args) {
        int currentLevel = Log.getInstance().GetLogLevel();
        Log.getInstance().SetLogLevel(Log.LOGLEVEL_TRACE);
        Podcast dummy = new Podcast();
        dummy.dumpFavorites();
        Log.getInstance().SetLogLevel(currentLevel);
    }

}
