/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import sage.media.rss.*;

/**
 *
 * @author Default
 */
public class UnrecordedEpisodeData implements Serializable {
    RSSItem ChanItem;
    String SPRRequestID;

    public UnrecordedEpisodeData(UnrecordedEpisode e) {
        ChanItem = e.ChanItem;
        SPRRequestID = e.SPRRequestID;
    }
}
