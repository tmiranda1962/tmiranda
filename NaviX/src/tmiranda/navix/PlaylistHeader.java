
package tmiranda.navix;

import java.util.*;

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
    private String view         = null;

    int loadDescription(int startLocation, String beginning, List<String> allLines) {

        int numberConsumed = 0;

        String delimiter = "/" + PlaylistEntry.COMPONENT_DESCRIPTION;

        // Check for special case of a single line description.
        if (beginning.lastIndexOf(delimiter)!=-1) {
            setDescription(beginning.substring(0, beginning.lastIndexOf(delimiter)));
            Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadDescription: " + numberConsumed + ": <" + description + ">");
            return numberConsumed;
        }

        String desc = beginning;
        boolean found = false;

        for (int i=startLocation; i<allLines.size() && !found; i++) {
            String nextLine = allLines.get(i);

            if (nextLine==null || nextLine.isEmpty()) {
                Log.getInstance().write(Log.LOGLEVEL_WARN, "loadDescription: null line.");
                continue;
            }

            nextLine = nextLine.trim();

            if (nextLine.lastIndexOf(delimiter)!=-1) {
                desc = desc + "" + nextLine.substring(0, nextLine.lastIndexOf(delimiter));
                found = true;
            } else {
                desc = desc + " " + nextLine;
            }
        }

        setDescription(desc);

        if (!found)
            Log.getInstance().write(Log.LOGLEVEL_WARN, "loadDescription: Did not find delimiter.");

        Log.getInstance().write(Log.LOGLEVEL_TRACE, "loadDescription: " + numberConsumed + ": <" + description + ">");

        return numberConsumed;
    }

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

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
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
        if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
            return false;
        }
        if ((this.logo == null) ? (other.logo != null) : !this.logo.equals(other.logo)) {
            return false;
        }
        if ((this.view == null) ? (other.view != null) : !this.view.equals(other.view)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.version != null ? this.version.hashCode() : 0);
        hash = 73 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 73 * hash + (this.background != null ? this.background.hashCode() : 0);
        hash = 73 * hash + (this.description != null ? this.description.hashCode() : 0);
        hash = 73 * hash + (this.logo != null ? this.logo.hashCode() : 0);
        hash = 73 * hash + (this.view != null ? this.view.hashCode() : 0);
        return hash;
    }

}
