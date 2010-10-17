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
    private Podcast podcast;             // The Podcast that contains this Episode.
    private String  ID;                  // A unique String that identifies each Episode.
}
