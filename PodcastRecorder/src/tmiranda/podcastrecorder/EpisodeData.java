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
public class EpisodeData implements Serializable {
    Podcast podcast;
    String  ID;

    public EpisodeData(Episode e) {
        podcast = e.podcast;
        ID = e.ID;
    }
}
