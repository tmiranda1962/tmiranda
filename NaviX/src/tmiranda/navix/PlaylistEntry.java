
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */
public class PlaylistEntry {

    private String version      = null;
    private String title        = null;     // Web page title NOT content title.
    private String background   = null;
    private String type         = null;
    private String name         = null;
    private String thumb        = null;
    private String url          = null;
    private String player       = null;
    private String rating       = null;
    private String description  = null;     // Ends with /description
    private String processor    = null;
    private String icon         = null;
    private String date         = null;

    public enum PlaylistType {
        HEAD,       // The page heading.
        AUDIO,
        VIDEO,
        IMAGE,
        SCRIPT,
        TEXT,
        DOWNLOAD,
        PLUGIN,
        PLAYLIST,
        RSS,
        ATOM,
        HTML_YOUTUBE,
        NAVIX,
        XML_SHOUTCAST,
        XML_APPLEMOVIE,
        RSS_FLICKR_DAILY,
        PLX,
        HTML,
        LIST_NOTE,
        PLAYLIST_YOUTUBE
    }

    /*
     * Getters and Setters.
     */
    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlaylistEntry other = (PlaylistEntry) obj;
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
            return false;
        }
        if ((this.background == null) ? (other.background != null) : !this.background.equals(other.background)) {
            return false;
        }
        if ((this.thumb == null) ? (other.thumb != null) : !this.thumb.equals(other.thumb)) {
            return false;
        }
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if ((this.player == null) ? (other.player != null) : !this.player.equals(other.player)) {
            return false;
        }
        if ((this.rating == null) ? (other.rating != null) : !this.rating.equals(other.rating)) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        if ((this.processor == null) ? (other.processor != null) : !this.processor.equals(other.processor)) {
            return false;
        }
        if ((this.icon == null) ? (other.icon != null) : !this.icon.equals(other.icon)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 59 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 59 * hash + (this.background != null ? this.background.hashCode() : 0);
        hash = 59 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.thumb != null ? this.thumb.hashCode() : 0);
        hash = 59 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 59 * hash + (this.player != null ? this.player.hashCode() : 0);
        hash = 59 * hash + (this.rating != null ? this.rating.hashCode() : 0);
        hash = 59 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 59 * hash + (this.processor != null ? this.processor.hashCode() : 0);
        hash = 59 * hash + (this.icon != null ? this.icon.hashCode() : 0);
        hash = 59 * hash + (this.date != null ? this.date.hashCode() : 0);
        return hash;
    }

}
