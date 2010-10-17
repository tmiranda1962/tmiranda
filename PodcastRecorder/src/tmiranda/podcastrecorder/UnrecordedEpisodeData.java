/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;
import java.util.*;
import sage.media.rss.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class UnrecordedEpisodeData implements Serializable {
    private RSSItem ChanItem;           // The RSSItem for this particular Episode.
    private String SPRRequestID;        // A unique ID supplied by the SagePodcastRecorder.
}
