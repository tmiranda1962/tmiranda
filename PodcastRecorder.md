#Wiki for PodcastRecorder



# Introduction #

PodcastRecorder is a plugin for SageTV that allows you manually record podcasts or select them as favorites and have them automatically downloaded for you.

This is a Beta release.  Don't expect perfection but do expect me to address bugs as they are found.


# Installation #

  * From SageTV's main menu go to Setup -> SageTV Plugins -> All Available Plugins
  * Select "UI Mod" from the top of the page.
  * Page down to PodcastRecorder and install.

Once installed you can navigate to an online video and press "Record" (Ctrl-y on a keyboard) or "Favorite" (Ctrl-k on a keyboard).  Pressing Record will bring up a dialog that lets you record a single podcast, or a group of podcasts.  Similarly, pressing Favorite will bring up a dialog that allows you to make a podcast a favorite.  Both of these dialogs are explained below.

After a podcast has been recorded it will appear as an imported video which can be accessed via the "Video" menu on the main menu.

## Known Issues and Limitations ##

The following is a list of known issues:
  * The plugin will not allow Hulu content to be recorded.  (Hulu requires the PlayOn/Hulu plugin.)
  * In the Video browser "Folder" view, the recorded podcasts will always appear at the top level folder.

# Record Dialog #

After pressing Record (Ctrl-y) on a podcast or a group of podcasts a dialog will be presented with the following options:
  * Show Title - This is the title of the show to be recorded.  You can alter it by selecting the "Change" button.
  * Base Directory - This is the directory where the downloaded podcast will be stored.  You can change it by selecting the "Change" button.
  * SubDirectory - This is the subdirectory where the downloaded podcast will be stored.  You can change it by selecting the "Change" button.
  * Use Show Title As Subdirectory - If set to "Yes" the SubDirectory setting will be ignored and the podcast will be downloaded into a subdirectory with the same name as the Show.
  * Use Show Title In File Name - If set to "Yes" the file name of the recorded podcast will be prefixed with the show title.

# Favorite Dialog #

After pressing Favorite (Ctrl-k) on a podcast or a group of podcasts a dialog will be presented with the following options:
  * Record New Episodes - If set to "Yes" as new episodes are posted to the web they will be automatically downloaded.  The plugin will check approximately every 12 hours for new podcasts.
  * Show Title - This is the title of the show to be recorded.  You can alter it by selecting the "Change" button.
  * Base Directory - This is the directory where the downloaded podcast will be stored.  You can change it by selecting the "Change" button.
  * SubDirectory - This is the subdirectory where the downloaded podcast will be stored.  You can change it by selecting the "Change" button.
  * Use Show Title As Subdirectory - If set to "Yes" the SubDirectory setting will be ignored and the podcast will be downloaded into a subdirectory with the same name as the Show.
  * Use Show Title In File Name - If set to "Yes" the file name of the recorded podcast will be prefixed with the show title.
  * Maximum Number of Episodes to Keep - This is the maximum number of podcasts to record.  To change, select the button that reads "Unlimited" and enter the new value.  By default it is set to 0, which is the same as "Unlimited".
  * Auto Delete - If set to "Yes" previously recorded podcasts will be deleted to make room for new podcasts as necessary.  Podcasts will be deleted from oldest watched to newest watched, and then oldest unwatched to newest unwatched.  If Maximum Number of Episodes to Keep is set to "Unlimited" no podcast will ever be deleted.
  * Re-Record Deleted Episodes - If set to "Yes" podcasts that you have deleted will automatically be recorded again on the next cycle, assuming they are still available on the web server.
  * Statistics - this will show information about the Favorite such as when the web server was last queried for new episodes, the number of episodes available on the web server, the number of podcasts currently recorded, and the total number of podcasts ever recorded.

Once a Podcast has been selected as a Favorite it will appear in the Online Favorites manager menu.  To enter this menu go to Online -> Online Favorites.

# Online Favorites Menu #

The Online Favorites menu lets you alter the options for a previously defined Favorite, check the status of podcasts that are recording, and set the default values.

Down the left side of the menu you will have the following options:
  * Sort By - Favorites can be sorted by Priority or by Title.  Currently "Priority" has no particular meaning.
  * Download Status - This will show you the status of any currently downloading podcast, how many podcasts are waiting to be recorded, how many podcasts have been recorded (resets after the Sage server is rebooted) and how many podcasts have failed to record because of an error (resets after the Sage server is rebooted.)
  * General Options - This will allow you to set the default options for the Record and Favorite dialogs and also has the following options:
    * Maximum Length of File Name - Some podcasts have very long names.  This sets the maximum number of characters that will be used.
    * Favorite Recording Cycle Time - This sets the time (in hours) between checking for new podcasts on the web.
    * Enable Cleanup - For various technical issues sometimes SageTV and this plugin leave large temporary files in your operating system's temp directory.  Setting this to True will allow the plugin to delete these files on a periodic basis.
    * Debug Logging Level - Normally set this to "Warn".  If you find a bug please set it to "Trace" and send me the resulting Sage logfile.

On the right side of the menu you will see a list of your currently selected Favorite podcasts.  Selecting one of them will bring up the Favorite dialog which allows you to change the various options.

Please report bugs to me in the PodcastRecorder thread in the SageTV forums.

Happy recording.