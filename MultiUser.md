#Wiki page for the Multi-User SageTV Plugin.



# Introduction #

Multi-User is a Plugin for SageTV that adds the capability to define multiple users who can each maintain their own list of recordings, favorites and most other information normally kept on a global basis in SageTV such as:

  * Watched status
  * Like/Don't Like status
  * Archived status.

# Installation #

Go to Setup -> SageTV  Plugins -> All Available Plugins.  Make sure "UI Mod" is highlighted and selected from the top row.  Install "Multi-User Support".  If you are installing on an extender or placeshifter you are done.

If you are using a SageClient, you must install the Plugin on the server first and then also on the SageClient.

# Setup #

After the Plugin is installed there will be a new entry on the Setup menu called "Users". Initially the only option you will see is "Log In".  Choose that option and log in as "Admin" (no quotes).  The password by default is "Admin" (no quotes). Note that user names and passwords are always case sensitive.

If you are impatient and want to get started immediately, go to "Add User", enter one or more user names and passwords, and you're done :) If I've done my job correctly most everything should be self explanatory.  Read on if you want more details.

The following options are available on the Setup menu.

## Log Out / Log In ##

Logs in a user or logs out the currently logged in user.

Note that you can perform a "Fast User Switch" which bypasses this menu entirely as follows:

  1. Make sure "Use Passwords" is disabled (see Configuration).
  1. Make sure you are on the Main Menu.
  1. Press Ctrl-f on your keyboard or press "Skip Fw" on the Sage remote.

If no user is logged in SageTV will behave just as if this Plugin is not installed.

The currently logged in user name will appear in the lower left hand corner of the main menu.

## Switch User ##

Logs out the currently logged in user and logs in the selected user.

## Add User ##

This option only shows up when logged in as Admin and allows you to add new users to the system. You will be prompted to enter a User name and password.  Select "Commit" to create the user.

Please note:

  * Names and passwords are case sensitive.
  * You must always enter a password, even if you disable the password check.  (See Configuration below.)

## Remove User ##

This option only shows up when logged in as Admin and allows you to remove users from the system.

## Change User Settings ##

This option only shows up when logged in as Admin and allows you to change certain user settings.  For now the only thing that can be changed is the user password.

## Database Maintenance ##

This option only shows up when logged in as Admin and allows you to manipulate the database used by the Plugin.  Normally you should not have to use these command at all but they have been included "just in case".

Technical note: The Multi-User Plugin uses the UserRecordAPI to store all information.

### Reset MediaFile Database ###

Resets all of the user data (Favorites, Manuals, Watched, Like/Don't Like, Archived) to the same values that are in the SageTV core.

### Erase The Database ###

Erases all of the data used by the Plugin.  This can be used if you intend to uninstall the Plugin and want to remove all unneeded data.

### Clean MediaFile Database ###

Searches the database and removes data pertaining to MediaFiles that have been removed. This command should normally not be needed.

### Clean Airing Database ###

Searches the database and removes data pertaining to Airings that have been removed.  This command should normally not be needed.

### Clean Orphaned Users ###

Searches the database and removes data pertaining to Users that have been removed.  This command should normally not be needed.

### Show Database Statistics ###

Shows some semi-useful information about the database:

  * Number of Users - The number of User database records.
  * Number of Favorites - The number of Favorite database records.
  * Number of MediaFiles - The number of MediaFile database records.
  * Number of Airings - The number of Airing database records.
  * Number of Store Records - The total number of database records in use INCLUDING database records used by other Plugins.

## Configuration ##

This option only shows up when logged in as Admin and allows you to configure certain global settings for the Plugin.

### Login Last User ###

If set to "Yes" if the Sage server or User Interface is rebooted, the last user that was logged in will remain logged in.  If set to "No" then no user will be logged in after a reboot.

### Use Passwords ###

If set to "Yes" a password is required each time a user logs in, or switches from one user to another.  If set to "No" passwords are not required to login or switch users.

### IR Users Effect Core IR ###

Intelligent Recording is enabled or disabled on a per-user basis.  If this option is set to "Yes" then actions that effect IR (Set Watched/Clear Watched, Like/Don't Like) performed by users that have IR enabled are passed to the Sage core.  If set to "No" then these actions are not passed to the Sage core and will not effect future IR selections.

Note that actions performed by "Admin" or when no user is logged in are always passed to the Sage core.

### Show User Disk Usage ###

If set to "Yes" AND you have the disk space bar enabled (Malore menus) then a small icon will appear in the disk space bar to show you how much of the used space is being used by the currently logged in user.  The icon does not appear if no user is logged in or Admin is logged in.

Some notes about the disk space bar:

  * The icon takes several seconds to update after switching users.
  * The green area shows the total space used by all users.
  * The yellow "upcoming recording" indicator shows the space that will be used for the currently logged on user.

Note that the current version has a small bug that causes the icon to stay all the way to the left when the Plugin is first loaded.  When the UI is reloaded the icon will move to where it actually should be.  This bug will be corrected in the next Sage beta. (Thanks Andy.)

### Log Level ###

Sets the current debug logging level.  This should normally be set to "Warn" but if you are having a problem you should set it to "Trace" and send me the logfile.

### UI Mod Plugin Version ###

The version number of the UI Mod Plugin.

### Support Plugin Version ###

The version number of the General Plugin.

# Behavior #

## General ##

When a user is created their view is exactly the same as if the Plugin were not installed. The user has access to all recordings and favorites defined in SageTV. All recordings will inherit their Watched, Like/Don't Like, Archived and viewing position from the SageTV core.

Once a user modifies their view other users' views are not changed.  So one user can delete a recording and have it disappear from their view and it will remain in view for other users. One user can add or delete a Favorite and it will remain unchanged in other users' views.  One user can mark a show as watched and it will remain unchanged in other users' views.

The following explains actions in more detail.

## Deleting ##

Once all users have deleted a recording it is physically deleted from SageTV. Before then the recording it is still in SageTV but it is "hidden" from users that have deleted it.

If Admin or the null user deletes something it is deleted from all views and from SageTV.

## Favorites ##

Once all users have deleted a Favorite it is physically deleted from SageTV.  Before then the Favorite is still in SageTV but it is "hidden" from users that have deleted (or never defined) it.

Favorites may be added by one user do not show up in the view for other uses.

Favorite recordings show up in the view of users that have defined the Favorite, but not in the view of users who have not defined it.

## Manual Recordings ##

Manual recordings are scheduled on a user-by-user basis. Once recorded, manuals only show up in the view of the user (or users) that requested it.

## Intelligent Recording ##

If any user enables IR it will be enabled in the SageTV core.  Once all users disable IR it is disabled in the core.  The only exception to this is that if Admin disables IR it becomes disabled for all users.  If Admin subsequently enables IR it must be reenabled by each user individually.

Recordings made as a result of IR being enabled only show up in the view of the users that have IR enabled.

# Strategy After Installing Plugin #

Once installed, I'd recommend the following strategy to get the system setup to the way you like it:

  1. Create the users:  It's probably easiest if you create all the users you need right away.
  1. Setup Intelligent Recording:  For each user, log in and enable or disable Intelligent Recording.  This will ensure that recordings made by the IR engine will only appear in the view of the users that have IR enabled.
  1. Delete unneeded Favorites: For each user, log in and remove all of the Favorites that should not appear in that user's view.  Remember that if ALL users remove the Favorite it is totally deleted from the Sage core.
  1. Delete unneeded Recordings:  For each user, log in and delete all of the recordings that should not appear in that user's view.  Rememebr that if ALL users delete a recording it is totally deleted from the Sage core.

# Option Dialogs #

When the Plugin is installed several new items will appear in the Options dialog for a recording or group of recordings.

## Give to Another User ##

Removes the selected recordings from the view of the currently logged on user and makes them appear in the view of the selected user.

## Share with Another User ##

Makes the selected recordings visible (as a Manual Record) in the view for the user selected.

# Recording Schedule #

When browsing the upcoming recordings schedule you will notice that some recordings have the "MultiUser" icon displayed.  This indicates that the show will be recorded, but it will not appear in the view of the currently logged on user.

If you select the show you will notice a new entry in the options dialog called "Show Requester".  Clicking on this will show you which user or users have requested this show be recorded (either because it's a Favorite or a Manual Record).

If the dialog says that "no user has requested this show" it means the recording is an Intelligent Suggestion.