
package tmiranda.navix;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import sage.media.rss.*;

/**
 * This class represents an individual Podcast episode.
 *
 * Handles both video podcasts and photobucket Image Podcasts.
 *
 * @author Tom Miranda.
 */
public final class RssItemElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    private RSSItem             rssItem  = null;
    private RSSChannel          channel  = null;
    private boolean             isVideo;
    private Map<String, String> args = new HashMap<String, String>();

    public RssItemElement(PlaylistEntry entry, RSSItem RSSItem, RSSChannel Channel, boolean IsVideo) {
        rssItem      = RSSItem;
        channel      = Channel;
        isVideo      = IsVideo;
        version      = entry.version;
        title        = entry.title;
        background   = entry.background;
        type         = PlaylistType.RSS_ITEM.toString();
        name         = entry.name;
        thumb        = entry.thumb;
        url          = entry.url;
        player       = entry.player;
        rating       = entry.rating;
        description  = entry.description;
        processor    = entry.processor;
        icon         = entry.icon;
        date         = entry.date;
        view         = entry.view;
    }

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    public boolean isPlaceholder() {
        return channel==null && rssItem==null;
    }

    public boolean isValid() {
        return channel != null;
    }

    public boolean isVideoItem() {
        return isVideo;
    }

    public void setChannel(RSSChannel channel) {
        this.channel = channel;
    }

    public void setRssItem(RSSItem rssItem) {
        this.rssItem = rssItem;
    }

    public RSSItem getRssItem() {
        return rssItem;
    }

    /**
     * Gets the video link.  Invokes the processor as necessary.  Should only be invoked
     * if isVideoItem() returns true.
     *
     * @return
     */
    public String getVideoLink() {

        if (rssItem==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssItemElement.getVideoLink: Processor needed, will use " + processor);
            return null;
        }

        String rssVideoLink = rssItem.getLink();

        if (!hasProcessor() && !rssVideoLink.contains("http://wwww.youtube.com")) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssItemElement.getVideoLink: No processor needed for " + rssVideoLink);
            return rssVideoLink;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssItemElement.getVideoLink: Processor will use " + processor + " for " + rssVideoLink);

        List<String> answer = invokeProcessor(rssVideoLink, processor);

        if (answer==null || answer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssItemElement.getVideoLink: No translation from processor, returning original " + url);
            return url;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssItemElement.getVideoLink: Answer " + answer);

        for (String element : answer) {
            String[] parts = element.split("=", 2);

            if (parts.length==2) {
                args.put(parts[0], parts[1]);
                Log.getInstance().write(Log.LOGLEVEL_WARN, "RssItemElement.getVideoLink: Found arg " + parts[0] + ":" + parts[1]);
            } else {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "RssItemElement.getVideoLink: Unexpected response from processor " + element);
            }
        }

        return args.get("answer");
    }

    /**
     * Gets the link to the image.  Should only be invoked is isVideoItem() returns false.
     *
     * @return
     */
    public String getImageLink() {
        // Typical form of link is:
        // http://s1223.photobucket.com/albums/dd516/1jeanettebazan/?action=view&current=071311134616.jpg

        String link = rssItem.getLink();

        if (link==null || link.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssItemElement.getImageLink: null link.");
            return null;
        }

        String fileName = link.substring(link.lastIndexOf("=")+1, link.length());
        String u = link.substring(0, link.lastIndexOf("?"));
        Log.getInstance().write(Log.LOGLEVEL_TRACE, "RssItemElement.getImageLink: File " + u + fileName);

        return u + fileName;
    }

    public RSSChannel getChannel() {
        return channel;
    }

    public String getRssThumb() {
        if (rssItem==null)
            return null;

        RSSMediaGroup mediaGroup = rssItem.getMediaGroup();

        return mediaGroup==null ? null : mediaGroup.getThumbURL();
    }

    @Override
    public String toString() {
        return "Type=" + type + ", " +
               "Title=" + title + ", " +
               "Name=" + name + ", " +
               "Version=" + version + ", " +
               "Background=" + background + ", " +
               "Thumb=" + thumb + ", " +
               "URL=" + url + ", " +
               "Player=" + player + ", " +
               "Rating=" + rating + ", " +
               "Date=" + date + ", " +
               "Processor=" + processor + ", " +
               "Icon=" + icon + ", " +
               "View=" + view + ", " +
               "Description=" + description + ", " +
               "RSSItem=" + (rssItem==null ? "null" : rssItem.toDebugString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RssItemElement other = (RssItemElement) obj;
        if (this.rssItem != other.rssItem && (this.rssItem == null || !this.rssItem.equals(other.rssItem))) {
            return false;
        }
        if (this.channel != other.channel && (this.channel == null || !this.channel.equals(other.channel))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.rssItem != null ? this.rssItem.hashCode() : 0);
        hash = 67 * hash + (this.channel != null ? this.channel.hashCode() : 0);
        return hash;
    }
}
