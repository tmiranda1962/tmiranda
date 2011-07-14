
package tmiranda.navix;

import java.io.*;
import java.net.*;
import java.util.*;
import sage.media.rss.*;

/**
 * Photobucket RSS Images.
 *
 * Similar to an RssElement except the url points to a Podcast that contains images.
 *
 * @author Tom Miranda.
 */
public class RssImageElement extends PlaylistEntry implements Serializable {

    static final long serialVersionUID = NaviX.SERIAL_UID;

    public static final String DEFAULT_SAGE_ICON = "WiFiSignal4.png";

    private boolean     checkedChannel = false;
    private RSSChannel  channel = null;

    @Override
    public boolean isSupportedBySage() {
        return true;
    }

    @Override
    public String getSageIcon() {
        return icon==null ? DEFAULT_SAGE_ICON : icon;
    }

    /**
     * See if this feed has already been checked. When an RSS feed is checked we see
     * if it points to a valid RSS feed (some items on NaviX do not).
     *
     * @return
     */
    public boolean hasBeenChecked() {
        return checkedChannel;
    }

    /**
     * Check if this is a valid Podcast.  Valid means the url points to a valid RSS Channel (Podcast).
     * @return
     */
    public boolean isValidPodcast() {
        return channel != null;
    }

    /**
     * Read the RSS Channel and return the RSS Items in a class that can be easily handled
     * by the STV.  This method can take a long time to complete depending on the podcast.
     *
     * @return
     */
    public List<RssItemElement> getRssItemElements() {

        List<RssItemElement> rssItemElements = new LinkedList<RssItemElement>();

        LinkedList<RSSItem> rssItems = getRssItems();

        if (rssItems==null || rssItems.isEmpty()) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRssItemElements: No rssItems.");
            return rssItemElements;
        }

        for (RSSItem rssItem : rssItems) {
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "getRssItemElements: Added RSSItem " + rssItem.getTitle());
            rssItemElements.add(new RssItemElement(this, rssItem, channel, false));
        }

        return rssItemElements;
    }

    public RSSChannel getRssChannel() {

        if (checkedChannel)
            return channel;

        if (url==null || url.equalsIgnoreCase("http://")) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: null url " + url);
            return null;
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: url is " + url);
        checkedChannel = true;
        HttpURLConnection.setFollowRedirects(true);

        RSSHandler rssHandler = new RSSHandler();

        URL URL = null;

        try {
        URL = new URL(url);

        } catch (MalformedURLException e2) {
           Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: MalformedURLException " + e2.getMessage());
           return null;
        } catch (Exception e3) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: Exception creating URL " + e3.getMessage());
            return null;
        }

        try {
            RSSParser.parseXmlFile(URL, rssHandler, false);
        } catch (RSSException e1) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: RSSException " + e1.getMessage());
            return null;
        } catch (Exception e3) {
            Log.getInstance().write(Log.LOGLEVEL_WARN, "RssImageElement.getRssChannel: Exception parsing " + e3.getMessage());
            return null;
        }

        channel = rssHandler.getRSSChannel();
        return channel;
    }

    /**
     * Read the RSS Channel and return the RSS Items.
     *
     * @return
     */
    public LinkedList<RSSItem> getRssItems() {

        if (checkedChannel)
            if (channel==null)
                return null;
            else
                return channel.getItems();

        channel = getRssChannel();

        if (channel==null) {
            return null;
        }

        return channel.getItems();
    }

    public String getLink() {
        return url;
    }

    /**
     * Returns the image from the RSS Channel.
     *
     * @return
     */
    public String getRssThumb() {
        if (channel==null)
            return null;

        RSSImage image = channel.getRSSImage();

        return image==null ? null : image.getUrl();
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
               "Checked=" + checkedChannel + ", " +
               "RSSChannel=" + (channel==null ? "null" : channel.toDebugString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RssImageElement other = (RssImageElement) obj;
        if (this.checkedChannel != other.checkedChannel) {
            return false;
        }
        if (this.channel != other.channel && (this.channel == null || !this.channel.equals(other.channel))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.checkedChannel ? 1 : 0);
        hash = 17 * hash + (this.channel != null ? this.channel.hashCode() : 0);
        return hash;
    }

}
