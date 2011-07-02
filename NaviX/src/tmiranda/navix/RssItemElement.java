
package tmiranda.navix;

import java.util.*;
import sage.media.rss.*;

/**
 *
 * @author Tom Miranda.
 */
public final class RssItemElement extends PlaylistEntry {

    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    private RSSItem rssItem     = null;
    private RSSChannel channel  = null;

    public RssItemElement(PlaylistEntry entry, RSSItem RSSItem, RSSChannel Channel) {
        rssItem      = RSSItem;
        channel      = Channel;
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

    public void setChannel(RSSChannel channel) {
        this.channel = channel;
    }

    public void setRssItem(RSSItem rssItem) {
        this.rssItem = rssItem;
    }

    public RSSItem getRssItem() {
        return rssItem;
    }

    public String getVideoLink() {

        if (rssItem==null) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Processor needed, will use " + processor);
            return null;
        }

        String rssVideoLink = rssItem.getLink();

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Processor will use " + processor + " for " + rssVideoLink);

        List<String> answer = invokeProcessor(rssVideoLink, processor);

        if (answer==null || answer.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "getVideoLink: No tarnslation from processor, returning original " + url);
            return url;
        }

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Answer " + answer);

        if (answer.size() > 1) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getVideoLink: Concatenating.");

            String a = null;

            for (String s : answer)
                a = (a==null ? s : a + s);

            return a;
        }

        return answer.get(0).replace(PlaylistEntry.SCRIPT_ANSWER, "");
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
