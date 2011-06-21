
package tmiranda.navix;

/**
 *
 * @author Tom Miranda.
 */
public enum PlaylistType {
    //HEAD ("head"),       // The page heading.
    AUDIO ("audio"),
    VIDEO ("video"),
    IMAGE ("image"),
    SCRIPT ("script"),
    TEXT ("text"),
    DOWNLOAD ("download"),
    PLUGIN ("plugin"),
    PLAYLIST ("playlist"),
    RSS ("rss"),
    RSS_RSS ("rss:rss"),
    RSS_HTML ("rss:html"),
    RSS_IMAGE ("rss:image"),
    ATOM ("atom"),
    HTML_YOUTUBE ("html_youtube"),
    NAVIX ("navi-x"),
    XML_SHOUTCAST ("xml_shoutcast"),
    XML_APPLEMOVIE ("xml_applemovie"),
    RSS_FLICKR_DAILY ("rss_flickr_daily"),
    PLX ("plx"),
    HTML ("html"),
    SEARCH ("search"),
    LIST_NOTE ("list_note"),
    OPML ("opml"),
    PLAYLIST_YOUTUBE ("playlist_youtube");

    private final String stringVal;

    PlaylistType(String sVal) {
        stringVal = sVal;
    }

    @Override
    public String toString() {
        return stringVal;
    }

    public static PlaylistType toEnum(String sVal) {

        if (sVal==null) {
            Log.getInstance().write(Log.LOGLEVEL_ERROR, "toEnum: null parameter.");
            return null;
        }


        for (PlaylistType val : PlaylistType.values()) {
            if (sVal.equalsIgnoreCase(val.toString()))
                return val;
        }

        Log.getInstance().write(Log.LOGLEVEL_WARN, "toEnum: No corresponding PlaylistType for " + sVal);
        return null;
    }
}
