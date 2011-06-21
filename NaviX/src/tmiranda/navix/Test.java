/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.navix;


/**
 *
 * @author Default
 */
public class Test {
    public Test(String HomeURL) {

        if (HomeURL==null || HomeURL.isEmpty()) {
            System.out.println("TEST: Empty HomeURL");
            return;
        }

        Log.getInstance().SetLogLevel(Log.LOGLEVEL_VERBOSE);

        Playlist playlist = new Playlist(HomeURL);

        for (PlaylistEntry entry : playlist.getElements()) {
            if (entry.isPlaylist()) {
                PlaylistElement playlistElement = (PlaylistElement)entry;
                System.out.println("TEST: Found a playlist, url = " + playlistElement.getNextPlaylist());
                //Playlist newPlaylist = new Playlist(((PlaylistElement)entry).getNextPlaylist());
                Test t = new Test(playlistElement.getNextPlaylist());
            }
        }
    }
}
