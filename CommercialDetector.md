# Introduction #

CommercialDetector is a SageTV7 plugin that automatically launches comskip or ShowAnalyzer after a show has recorded. It produces .edl files that can be read by the Comskip Playback plugin (which is available in the UI Mod section of the plugin manager.)

This plugin is designed to be easy to configure and maintain and is aimed at users who simply want to run a commercial detection program on their recordings. If you want to do more sophisticated processing there are many other suitable solutions such as SageJobQueue (SJQ) and DirMon2.

# Basic Installation #

  * Make sure you are on a Placeshifter, Extender or the UI on the Sage server EVEN IF you normally access sage via a SageClient.  If you have a SageClient you must also install the plugin on the SageClient, see below.

  * Go to the plugin manager (Setup->SageTV Plugins->All Available Plugins) and select the "UI Mod" section (from the top row of choices.)

  * Select the "CommercialDetector UI" plugin and install it.

  * Go to Setup->Detailed Setup->Customize and look for "Comskip Playback Options" at the end of the list.

  * From there you can configure the behavior and "look and feel" of the user interface.

  * This plugin will automatically process all NEW recordings.  If you have existing recordings that do not have comskip info you can process them all by selecting "Scan All Recordings Without comskip Info".  See the Basic Configuration Options description below.

If your Sage server is running on Windows no further configuration is necessary.

If your Sage server is running on linux do the following:

  1. You MUST install wine on your linux server under a normal user account (not root). CommercialDetector uses comskip to scan the recordings and comskip is a native Windows program.  comskip does work quite well under wine and in some cases actually runs faster under wine than under Windows. Do NOT install wine from the root account.
  1. If you are running the Sage server as root I suggest making a special account solely for the purpose of running comskip.  If you call the account comskip, or anything that has "commercial", "sage", "wine" or "detect" in it, the plugin should find that account and use it as the default.
  1. If you are not running Sage under the root account, install wine under the same user you are using for sage.
  1. Go to the plugin manager->All Installed Plugins and select the "General" section.
  1. Select the CommercialDetector plugin, then select "Configure".
  1. Navigate down the list of options until you find "SageTV is Running as Root".  Set this to True or False according to your setup.
  1. If the Sage server is running as root you must select a user account that will be used to run comskip.  This should be the user account where you installed wine in step 1.
  1. If the Sage server is not running as root, enter the user account that is running Sage.
  1. If you installed wine in the normal way the Home directory for wine default will be OK.  If you did something funky you can enter the appropriate Home directory for wine.

Macs servers are not currently supported.

## SageClient Installation ##

Before installing the plugin on a SageClient, ensure that you have already installed it on the server.  After you have installed the plugin on the server do the following to install it on the SageClient:

  1. Go to the plugin manager (Setup->SageTV Plugins->All Available Plugins) and select the "UI Mod" section (from the top row of choices.)
  1. Select the "CommercialDetector UI" plugin and install it AS A CLIENT PLUGIN.
  1. Go to Setup->Detailed Setup->Customize and look for "Comskip Playback Options" at the end of the list.
  1. From there you can configure the behavior and "look and feel" of the user interface.

A big caveat about SageClients:

  * The SageClient will only be able to "see" the commercial skipping information if you have setup your Sage server to use UNC paths (\\ServerName\Path) for recording and not used absolute paths (D:, E:, F:).

# The Basic Configuration Options #

The following is an explanation of the options available:

  * Maximum Concurrent Jobs - The maximum number of comskip (or ShowAnalyzer) programs to run at the same time. If you have a powerful CPU you can increase this from its default value of 1.

  * Location of comskip.exe - This option can be used to change the comskip.exe location. By default the plugin is configured to use the pre-packaged comskip.exe. (That will be placed under the SageTV/comskip directory.) Note that if you want to use another version of comskip you should NOT overwrite the comskip that comes with this plugin. The best way to use another version of comskip is to install it in another location and use this configuration option to point CommercialDetector at it.  You should NOT add, remove or change anything in the SageTV/comskip folder.

  * Location of comskip.ini - This option can be used to change the comskip.ini location. By default the plugin is configured to use the pre-packaged comskip.ini. Note that if you want to alter the comskip.ini you should NOT to overwrite the comskip.ini that comes with this plugin.  You should NOT add, remove or change anything in the SageTV/comskip folder.

  * Run More Slowly - Slows down comskip (freeing up the CPU and disk) by pausing 10ms after each frame is decoded.

  * Restricted Times - Used to select which times comskip should NOT be run.

  * Cleanup Files with These Extensions - When a recording is deleted CommercialDetector will look for corresponding files ending in these extensions and delete them.  These are the files produced by comskip (or ShowAnalyzer) and read by the plugin.  Once the recording has been deleted there is no need to keep these files any longer.

  * File Extensions for Valid Video Files - Used to specify the extensions of files that should be considered to be video files.  CommercialDetector will not perform cleanup if a valid video file exists.

  * Do Not Run comskip on These Channels - If you do not want comskip (or ShowAnalyzer) run on recordings from specific channels enter the channel numbers or names here separated with a comma. (Example: 22,HBO,75,212,MOV)

  * Start comskip as Soon as Recording Starts - Normally CommercialDetector will wait until a recording has finished before processing begins. Setting this to true will launch comskip (or ShowAnalyzer) as soon as the recording starts. This will also cause LiveTV to be processed as soon as you start watching.

  * Debug Logging Level - Can usually be left at Warn. If you are experiencing problems set it to Trace and send me the logfile. If set to Verbose you will also see the stdout and stderr output from comskip.exe.

  * Show Advanced options - If set to True the settings below will be displayed.  If set to False they will not be displayed.

  * Use Intelligent Scheduling - If set to True CommercialDetector will only process files if Sage is not recording and the file can be processed before the next recording starts.  Files that can't be processed will be placed in a queue and processed when enough time is available. This is useful if your Sage server has a slower CPU and/or slow disk IO and is therefore unable to record and detect commercials at the same time.  This is an experimental feature.

  * Other comskip Parameters - Anything you enter here will be passed to the comskip.exe command line. This should usually be left blank.

  * Scan All Recordings Without comskip Info - Runs comskip (or ShowAnalyzer) on all recordings that do not already have .edl or .txt files associated with them.  If you select this button and nothing appears to happen it means all of your recordings have an .edl or .txt file associated with them.  If you select this button and see "xx Queued" it means xx recordings have been queued.  This option is not available from SageClients.

  * Delete All Orphaned and Extraneous Files - Deletes all files that should have been cleaned up but were not.  Upon entering the options this setting will read "Scan".  Selecting that will begin a scan and when finished the number of orphaned files will be displayed.  Selecting this again will delete the files.  This option is not available from SageClients.

  * Show Jobs That Are Queued - Display the recordings that are queued for processing.   This option is not available from SageClients.

  * Show Jobs That Are Running - Display the recordings that are currently being processed.  This option is not available from SageClients.

  * Use ShowAnalyzer - If set to True ShowAnalyzer will be used to process files instead of comskip.  If this is set to True you will need to make sure the next two options are set properly.  This option is not available if your Sage server is running on linux.

  * Location of ShowAnalyzerEngine.exe - Used to tell CommercialDetector where you have installed ShowAnalyzer.  Note that you must select ShowAnalyzerEngine.exe and NOT ShowAnalyzer.exe.  This option is not available if your Sage server is running on linux.

  * Location of ShowAnalyzer's Profiles - Used to tell CommercialDetector where to find any profiles you have created.  For more details see "Using ShowAnalyzer Instead of comskip" below.  This option is not available if your Sage server is running on linux.

  * Show All Channels - If set to True the options will be expanded to include one new option for every channel available on your system.  Selecting one of the channels will bring up a dialog that allows you to choose which program to use to detect commercials for that channel: comskip, ShowAnalyser, the default program or none.  This option is not available if your Sage server is running on linux.

# comskip Information #

comskip comes in two versions, a free version and a donator's version.  The big difference between the two is that the donator's version supports recordings made by the HD-PVR (H.264) and the free version does not.  If you donate to Erik and want to use the donator's version please follow the directions in the next section.  Do NOT simply overwrite the comskip directory, or any of the files in the directory, that is installed with this plugin.

More information on comskip can be found here:  http://www.kaashoek.com/comskip/

If you like it please consider making a donation to Erik, I'm sure he has spent MANY hours working on this program.

## Using a Different Version of comskip or Using a Custom comskip.ini ##

If you want to use a different version of comskip unzip the new comskip files to a location of your choice but do NOT alter the comskip directory that is installed with this plugin. If you do alter any files the Sage plugin installer will produce error messages when you try to uninstall or upgrade the plugin. After unzipping the files make sure you update the configuration to point to the new comskip.exe and comskip.ini.

If you want to alter the .ini file that is provided as part of this plugin you should copy the .ini file to a new location, edit it, and then update the configuration to use the updated .ini file.  Do NOT alter the contents of the comskip directory that is installed with this plugin.

## Per-Show or Per-Channel .ini Files ##

If you want to use custom .ini files on a per-show or per-channel basis, put the .ini files in the /SageTV/CommercialDetector/comskip directory.  (This is not the same directory where comskip.exe is installed.  comskip.exe is installed in the SageTV/comskip directory.)

  * The plugin will first look for a .ini file with a name that matches the Show name of the recording WITH ALL SPACES REMOVED.  So if the name of the recorded show is "House Hunters", the plugin will look for HouseHunters.ini.  Note that on linux systems the name is case sensitive.

  * If no .ini file is found that matches the title of the recorded show the plugin will look for a .ini file with a name that matches the channel name.  So if the recording is from "MILI" the plugin will look for MILI.ini.  Note that on linux systems the name is case sensitive.

  * If no .ini file is found that matches the Show name or channel name, the default .ini file will be used.

# Using ShowAnalyzer Instead of comskip #

ShowAnalyzer is currently only available if your Sage server is running Windows.  I am making some progress getting ShowAnalyzer to run on a linux server but for now it's not ready for prime time.

First make sure you have installed and configured ShowAnalyzer.  ShowAnalyzer is not free but does have a 14 day trial period.  More info on that can be found here:  http://www.dragonglobal.biz/showanalyzer.html

This wiki will not attempt to explain ShowAnalyzer but one thing to be aware of is that many anti-virus program will identify ShowAnalyzer as having a virus.  I suggest you turn off your anti-virus software before installing ShowAnalyzer (turn back on after the install is complete) and then make sure your anti-virus program does not scan the ShowAnalyzer install directory.

Once you have ShowAnalyzer installed do the following:

  * Go to the CommercialDetector General plugin and select Configure.

  * Select "Show Advanced Options".

  * Set "Use ShowAnalyzer" to True.

  * Set the Location of ShowAnalyzerEngine.exe.  ShowAnalyzerEngine.exe will be located in the ShowAnalyzer install directory.

  * If you are using ShowAnalyzer 1.0 you can leave the "Location of ShowAnalyzer Profiles" empty.  Profiles are only used by the ShowAnalyzer beta.  If you are using the Beta and want to use profiles select the location for the files.  Be aware that the Profiles are located in a Windows hidden directory that MUST BE UNHIDDEN before you are able to select it. (On Windows XP it is \Documents and Settings\All Users\Application Data\Dragon Global\ShowAnalyzerSuite\Settings).

  * The plugin will first look for a Profile with a name that matches the Show name of the recording WITH ALL SPACES REMOVED.  So if the name of the recorded show is "American Idol", the plugin will look for AmericanIdol.saconfig.

  * If no Profile is found that matches the title of the recorded show the plugin will look for a Profile with a name that matches the channel name.  So if the recording is from "WABC" the plugin will look for WABC.saconfig.

  * If no Profile is found that matches the channel name then ShowAnalyzer's default profile will be used.

# Using comskip on Some Recordings and ShowAnalyzer on Others #

The CommercialDetector plugin can use comskip to process recordings from some channels and ShowAnalyzer to process shows from other channels.  To configure:

  * First decide which program, comskip or ShowAnalyzer, you want to make the default choice.  When the plugin is first installed it will be comskip.  To make ShowAnalyzer the default go to the advanced options (previously explained) and set Use ShowAnalyzer Instead of comskip to True.

  * Once the advanced options are displayed set Show All Channels to True.  This will then display configuration options for every channel available on your Sage system.

  * When you select the option for a particular channel you will be able to choose comskip, ShowAnalyzer, None, or Default.  Choosing None is the same as entering the channel in the "Do Not Run comskip on These Channels" option.

# Background #

Commercial skipping is not a function native to SageTV.  In order to have the ability to skip commercials in SageTV two things need to happen:

  1. A program such an comskip or ShowAnalyzer must be used to scan recordings and create files ending in .edl or .txt that contain information about where in the recording the commercials are located.
  1. The Sage user interface must be modified to read these .edl or .txt files, display the commercial information and skip commercials depending on input from the user.