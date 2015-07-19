#Explanation of the ActiveAspectRatios Plugin.



# Introduction #

ActiveAspectRatios is a plugin for SageTV that works in conjunction with comskip to automatically detect the aspect ratio of video and then switch the display aspect ratio to the correct mode.


# Quick and Dirty Installation Instructions #

If you don't like to read a lot and pretty much know what you are doing then this section is for you.  If you want to understand more about the plugin you will need to read further.

Quick and dirty instructions:

  1. Install the ActiveAspectRatios plugin from the UI section of the plugin manager.
  1. Edit comskip.ini to include the lines "verbose=10" and "output\_aspect=1".
  1. Run comskip on the videos you are interested in.  This allows comskip.exe to generate the .aspects file needed by the plugin.
  1. Bring up the options menu for any video that has a corresponding .aspects file.  (These videos have an icon that looks like a blue box with a diagonal arrow on it.)
  1. Bring up the ActiveAspectRatios dialog and select the second item "Aspect Ratio Mappings for This Show".
  1. For each aspect ratio listed select which Sage AR Mode you want to use.  Generally 4x3 video has an aspect ratio close to 1.50 and 16x9 video has an aspect ratio close to 1.98.

# Installation #

## Installing and Configuring comskip ##

ActiveAspectRatios depends on the aspect ratio information produced by comskip.exe.  If you do not have comskip installed (or a plugin/program that uses comskip) you must first install it.  Once comskip is installed you must configure it to produce the aspect ratio information needed by ActiveAspectRatios.

### If You Do Not Have comskip Installed ###

Without the aspect ratio information provided by comskip the ActiveAspectRatios plugin will not function properly.  If you do not have comskip installed you can do so by installing either the CommercialDetector UI Mod plugin, the Sage Job Queue (SJQ) plugin, Comskip Monitor or DirMon. There are other options available, but these are the most popular.

After you get comskip installed follow the directions in the next section.

### If You Are Already Using comskip ###

Edit the comskip.ini file and add or edit the following lines:

```
verbose=10
output_aspect=1
```

This will tell comskip to produce files ending in .aspects that contain the information needed by the ActiveAspectRatios plugin.

_Note that after editing the .ini file .aspects file will only be produced for new files that are processed by comskip.  If you want to generate the aspect ratio information for existing video you must rerun comskip on these files._

## Install the Sage Plugin ##

Go to Setup -> SageTV Plugins -> All Available plugins.  Make sure "UI Mod" is selected on the top of the menu.  Look down the list, select "ActiveAspectRatios", and install it.

# Configuration #

Before configuring the plugin you will need to understand a few terms used in this wiki:

  * aspect ratio - When this term is used I am referring to the aspect ratio information provided by comskip.exe. The aspect ratio is always a floating point number.  For example: 1.51, 1.98.
  * AR Mode - When this term is used I am referring to the aspect ratio modes available in the SageTV UI.  For example: Source, Fill, 16x9, 16x10, ZoomA, ZoomB, and ZoomC.

What this plugin allows you to do is to select which AR Mode is used in the Sage User Interface when comskip detects a certain aspect ratio in the video.

## Initial Configuration ##

From the main menu go to Setup -> ActiveAspectRatios.  The following dialog will appear:

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/OptionsFromSetup.JPG' />

  * Enabled or Disabled - This allows you to enable or disable the plugin.
  * Channels to Exclude From Switching - This allows you to select channels that are excluded from AR mode switching.  (So no AR Mode switching will occur for any shows recorded on these channels even if the shows have a corresponding .aspects file.)
  * Shows to Exclude From Switching - This allows you to select shows that are excluded from AR mode switching.  (So no AR Mode switching will occur for any of these shows even if they have a corresponding .aspects file.)
  * Channels with Fixed Aspect Ratios - This allows you to select fixed AR Modes for individual channels.
  * Advanced Options - This brings up the Advanced Options menu.  See the next section for a description.

## Advanced Options ##

The default settings in this dialog will most likely for most users so if you are impatient you don't have to read this section.

The Advanced Options dialog has the following options:

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/AdvancedOptions.JPG' />

  * Default Mode - Use this to select what AR Mode to use if the show being played does not have any aspect ratio information.
  * Show AR On Screen When Change Occurs - If enabled, the AR Mode will briefly appear in the upper right hand corner of the screen when the plugin changes the AR Mode.
  * Ratio Tolerance - When comskip.exe detects the aspect ratio it is soemtimes slightly off due to differences in how your broadcaster presents shows.  For example, sometimes SD shows will have an aspect ratio of 1.51, sometimes 1.53 and sometimes 1.49.  The Ratio Tolerance is used to map aspect ratios to the closest ratio already defined.  So if the Ratio Tolerance is set to .05 and you have the aspect ratio 1.51 mapped to "ZoomC", any show with an aspect ratio of 1.51 plus or minus 5% will also be mapped to "ZoomC".  The default value should work for most people.
  * STV Version - The version number for the STV portion of the plugin.
  * JAR Version - The version number for the JAR portion of the plugin.
  * Log Level - The current log level for the plugin.  In most cases you should leave this at "Warn".  If you experience issues set the level to Trace and turn on debugging for Sage itself.

## Mapping aspect ratios to AR Modes ##

Telling Sage what AR mode to use when it is playing a video with a certain aspect ratio is really the heart of this plugin.  By default the plugin will always use "Source". Telling Sage to use a different AR mode for specific aspect ratios is referred to as "mapping".

The first thing to do is find a video that has aspect ratio information available (i.e. You have run comskip on the video and the video has a corresponding .aspects file.) Videos that have aspect ratio information will have an icon next to them that looks like a blue square with a diagonal line across it.  The following is an example of what it looks like if you are using the Malore menus.  Note the icon next to the video thumbnail in the upper right corner.  (The icon will also appear if you are using the standard menus as well.)

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/IconExample.JPG' />

Once you find a video with aspect ratio information bring up the options and then select "Active Aspect Ratios" from the right panel. (Pressing the "Aspect" key will also bring up this dialog.) If you do not see the right panel you must enable the Advanced Options.

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/SelectFromRecordingOptions.JPG' />

The following ActiveAspectRatios dialog will appear.  Note that there are several new options available that are not available from Setup->ActiveAspectRatios.

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/OptionsFromBrowser.JPG' />

  * Aspect Ratio Mappings for This Show - Described below.
  * Aspect Ratio Switching for This Show - Allows you to either enable or disable AR mode switching for the show.  If you set this to Disabled the default AR mode will always be used when this show is played.
  * AR Switching for This Channel (Channel Name) - This option allows you to either enable or disable AR mode switching for this channel.  If you set this to Disabled the default AR mode will always be used when any show recorded from this channel is played.

If you select Aspect Ratio Mappings for This Show, the following dialog will appear:

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/RatioMapping.JPG' />

This is the dialog that allows you to map specific aspect ratios to specific Sage AR modes.  The left side of the dialog shows all of the aspect ratios found in the video.  The right side of the dialog shows what AR mode will be used for the corresponding aspect ratio.

Next to the aspect ratio, in parenthesis, the amount of time that aspect ratio is used in the video is displayed.  This information is useful because it will help you figure out what aspect ratio is used for the actual show and what aspect ratios are used for commercials.  Typically the aspect ratio used for the most time is the primary aspect ratio for the show.

Some aspect ratios may have another number listed after the time information.  (These entries will also be denoted by an asterisk next to the AR mode on the right side of the dialog.)  In the screenshot above this can be seen on the first entry where "2.05" appears after the time (4 secs).  This denotes that the aspect ratio (2.07) falls within the tolerance of another mapping (2.05).  For an explanation of "tolerance" see the description of Advanced Options.

## OSD ##

When the OSD is displayed you will notice a small change on the timeline.

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/OSD.JPG' />

The number in parenthesis is the aspect ratio for that particular point in the video.  This is useful information that may help you decide what AR mode to use for a specific aspect ratio.

If you press the Options key and then "Aspect Ratio Mode" the ActiveAspectRatios dialog will appear.  Note that you can get directly to this dialog by pressing the "Aspect" key while a video is playing.

<img src='http://tmiranda.googlecode.com/svn/trunk/ActiveAspectRatios/ScreenShots/OptionsFromOSD.JPG' />

Notice that there is an additional option "Temporarily Disable".  This allows you to disable AR mode switching while this particular video is playing.  Pressing the "Aspect" key will toggle this between "Off" and "On".