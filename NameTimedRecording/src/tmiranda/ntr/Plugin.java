/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tmiranda.ntr;

import sage.*;
import sagex.api.*;

/**
 *
 * @author Default
 */
public class Plugin implements sage.SageTVPlugin, SageTVEventListener {

    final static String VERSION = "1.20 05.30.2011";

    private sage.SageTVPluginRegistry registry;
    private sage.SageTVEventListener listener;

    /**
     * Constructor.
     * <p>
     * @param registry
     */
    public Plugin(sage.SageTVPluginRegistry Registry) {
        registry = Registry;
    }

    public Plugin(sage.SageTVPluginRegistry Registry, boolean reset) {
        registry = Registry;
        if (reset)
            resetConfig();
    }

    // This method is called when the plugin should startup.
    @Override
    public void start() {
        System.out.println("NameTimedRecording: Starting. Version = " + VERSION);

        if (Global.IsClient()) {
            System.out.println("NameTimedRecording: Plugin running as a SageClient.");
            return;
        }

        System.out.println("NameTimedRecording: Subscribing to events.");

        listener = this;
        registry.eventSubscribe(listener, "RecordingStopped");
        registry.eventSubscribe(listener, "RecordingCompleted");
        return;
    }
    
    // This method is called when the plugin should shutdown
    @Override
    public void stop() {
        System.out.println("NameTimedRecording: Stopping.");
        registry.eventUnsubscribe(listener, "RecordingStopped");
        registry.eventUnsubscribe(listener, "RecordingFinished");
    }

    // This method is called after plugin shutdown to free any resources
    // used by the plugin
    @Override
    public void destroy() {
        return;
    }


    // Returns the names of the settings for this plugin
    @Override
    public String[] getConfigSettings() {
        return null;
    }

    // Returns the current value of the specified setting for this plugin
    @Override
    public String getConfigValue(String setting) {
        return null;
    }

    // Returns the current value of the specified multichoice setting for
    // this plugin
    @Override
    public String[] getConfigValues(String setting) {
        return null;
    }

    //public static final int CONFIG_BOOL = 1;
    //public static final int CONFIG_INTEGER = 2;
    //public static final int CONFIG_TEXT = 3;
    //public static final int CONFIG_CHOICE = 4;
    //public static final int CONFIG_MULTICHOICE = 5;
    //public static final int CONFIG_FILE = 6;
    //public static final int CONFIG_DIRECTORY = 7;
    //public static final int CONFIG_BUTTON = 8;
    //public static final int CONFIG_PASSWORD = 9;

    // Returns one of the constants above that indicates what type of value
    // is used for a specific settings
    @Override
    public int getConfigType(String setting) {
        return 0;
    }

    // Sets a configuration value for this plugin
    @Override
    public void setConfigValue(String setting, String value) {
        return;
    }

    // Sets a configuration values for this plugin for a multiselect choice
    @Override
    public void setConfigValues(String setting, String[] values) {
        return;
    }

    // For CONFIG_CHOICE settings; this returns the list of choices
    @Override
    public String[] getConfigOptions(String setting) {
        return null;
    }

    // Returns the help text for a configuration setting
    @Override
    public String getConfigHelpText(String setting) {
        return null;
    }

    // Returns the label used to present this setting to the user
    @Override
    public String getConfigLabel(String setting) {
        return null;
    }

    // Resets the configuration of this plugin
    @Override
    public final void resetConfig() {
        return;
    }

    /**
     * Interface definition for implementation classes that listen for events
     * from the SageTV core
     *
     * Variable types are in brackets[] after the var name unless they are the
     * same as the var name itself.
     * List of known core events:
     *
     * MediaFileImported - vars: MediaFile
     * ImportingStarted
     * ImportingCompleted
     * RecordingCompleted (called when a complete recording is done)
     * 	vars: MediaFile
     * RecordingStarted (called when any kind of recording is started)
     *	vars: MediaFile
     * RecordingStopped (called whenever a recording is stopped for any reason)
     *	vars: MediaFile
     * AllPluginsLoaded
     * RecordingScheduleChanged
     * ConflictStatusChanged
     * SystemMessagePosted
     *	vars: SystemMessage
     * EPGUpdateCompleted
     * MediaFileRemoved
     * 	vars: MediaFile
     * PlaybackStopped (called when the file is closed)
     * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
     * 		ChapterNum[Integer], TitleNum[Integer]
     * PlaybackFinished (called at the EOF)
     * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
     * 		ChapterNum[Integer], TitleNum[Integer]
     * PlaybackStarted
     * 	vars: MediaFile, UIContext[String], Duration[Long], MediaTime[Long],
     * 		ChapterNum[Integer], TitleNum[Integer]
     * FavoriteAdded
     * 	vars: Favorite
     * FavoriteModified
     * 	vars: Favorite
     * FavoriteRemoved
     * 	vars: Favorite
     * PlaylistAdded
     * 	vars: Playlist, UIContext[String]
     * PlaylistModified
     * 	vars: Playlist, UIContext[String]
     * PlaylistRemoved
     * 	vars: Playlist, UIContext[String]
     * ClientConnected
     * 	vars: IPAddress[String], MACAddress[String] (if its a
     * 		placeshifter/extender, MACAddress is null otherwise)
     * ClientDisconnected
     * 	vars: IPAddress[String], MACAddress[String] (if its a
     * 		placeshifter/extender, MACAddress is null otherwise)
     */


    // This is a callback method invoked from the SageTV core for any
    // events the listener has subscribed to.
    // See the sage.SageTVPluginRegistry interface definition for details
    // regarding subscribing and unsubscribing to events.
    // The eventName will be a predefined String which indicates the event
    // type.
    // The eventVars will be a Map of variables specific to the event
    // information. This Map should NOT be modified.
    // The keys to the eventVars Map will generally be Strings; but this
    // may change in the future and plugins that submit events
    // are not required to follow that rule.

    @Override
    public void sageEvent(String eventName, java.util.Map eventVars) {

        if (!(eventName.startsWith("RecordingStopped") || eventName.startsWith("RecordingCompleted"))) {
            System.out.println("NameTimedRecording: Received unsubscribed event " + eventName);
            return;
        }

        Object MediaFile = eventVars.get("MediaFile");

        if (MediaFile==null) {
            System.out.println("NameTimedRecording: null MediaFile.");
            return;
        }

        Object Airing = MediaFileAPI.GetMediaFileAiring(MediaFile);

        if (Airing==null) {
            System.out.println("NameTimedRecording: null Airing for MediaFile " + MediaFileAPI.GetMediaTitle(MediaFile));
            return;
        }

        String title = AiringAPI.GetAiringTitle(MediaFile);

        // All timed recordings will start with the same thing.  If it's not a timed
        // recording there is nothing to do.
        if (title==null || !title.startsWith(API.TIMED_RECORDING)) {
            System.out.println("NameTimedRecording: Not a timed recording " + MediaFileAPI.GetMediaTitle(MediaFile));
            return;
        }

        // The name will be stored in the ManualRecordProperty of the Airing or if it's
        // a recurring recording we will have to look into the property map.
        String airingName = AiringAPI.GetManualRecordProperty(Airing, API.PROPERTY_NAME);

        if (airingName==null || airingName.isEmpty()) {

            // If there is no ManualRecordProperty it may be a recurring timed recording.
            System.out.println("NameTimedRecording: No ManualRecordProperty for Airing " + AiringAPI.GetAiringTitle(Airing));
            
            airingName = API.getNameForRecurring(Airing);
            
            if (airingName==null || airingName.isEmpty()) {
                System.out.println("NameTimedRecording: No NameForRecurring.");
                return;
            }
        }

        // Store the name in the MediaFile metadata.
        MediaFileAPI.SetMediaFileMetadata(MediaFile, API.PROPERTY_NAME, airingName);

        System.out.println("NameTimedRecording: Set name for MediaFile " + airingName);
        return;
    }

}
