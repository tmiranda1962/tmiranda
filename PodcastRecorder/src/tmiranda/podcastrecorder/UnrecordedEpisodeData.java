
package tmiranda.podcastrecorder;

import java.io.*;
import sage.media.rss.*;

/**
 *
 * @author Tom Miranda.
 */
public class UnrecordedEpisodeData implements Serializable {
    RSSItem ChanItem;
    String SPRRequestID;

    public UnrecordedEpisodeData(UnrecordedEpisode e) {
        ChanItem = e.ChanItem;
        SPRRequestID = e.SPRRequestID;
    }
}
