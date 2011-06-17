
package tmiranda.navix;

/**
 *
 * @author Tom Miranda
 */
public class PlaylistHeader {

    private String version      = null;
    private String title        = null;
    private String background   = null;
    private String description  = null;
    private String logo         = null;

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlaylistHeader other = (PlaylistHeader) obj;
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
            return false;
        }
        if ((this.background == null) ? (other.background != null) : !this.background.equals(other.background)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 83 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 83 * hash + (this.background != null ? this.background.hashCode() : 0);
        hash = 83 * hash + (this.description != null ? this.description.hashCode() : 0);
        return hash;
    }

}
