/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.podcastrecorder;

import java.io.*;

/**
 *
 * @author Default
 */
public class PodcastKey implements Serializable {

    private static final long serialVersionUID = 1L;

    String OVT;
    String OVI;
    String FeedContext;

    public PodcastKey(String OnlineVideoType, String OnlineVideoItem, String FeedContext) {
        OVT = OnlineVideoType;
        OVI = OnlineVideoItem;
        this.FeedContext = FeedContext;
    }

    public String getOVT() {
        return OVT;
    }

    public String getOVI() {
        return OVI;
    }

    public String getFeedContext() {
        return FeedContext;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PodcastKey other = (PodcastKey) obj;
        if ((this.OVT == null) ? (other.OVT != null) : !this.OVT.equals(other.OVT)) {
            return false;
        }
        if ((this.OVI == null) ? (other.OVI != null) : !this.OVI.equals(other.OVI)) {
            return false;
        }
        if ((this.FeedContext == null) ? (other.FeedContext != null) : !this.FeedContext.equals(other.FeedContext)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.OVT != null ? this.OVT.hashCode() : 0);
        hash = 53 * hash + (this.OVI != null ? this.OVI.hashCode() : 0);
        hash = 53 * hash + (this.FeedContext != null ? this.FeedContext.hashCode() : 0);
        return hash;
    }

}
