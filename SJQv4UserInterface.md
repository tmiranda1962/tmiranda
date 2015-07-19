#Sage Job Queue v4 (SJQv4) User Interface Wiki



# Introduction #

This wiki explains how to use the User Interface (UI) for SJQv4. It is **highly** recommended you read the Background, Installation, and Configuration sections of this wiki before installing and configuring SJQv4.

This wiki is not meant to be the definitive guide on SJQv4 but rather it is meant to provide the overview necessary to get started and explain the various workings of the UI.  The official SJQv4 guide can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

Slugger has done most of the "heavy lifting" in creating SJQv4, this plugin simply puts a pretty face on the very powerful and flexible system he has created.  Slugger's guide provides tutorials and examples that will be very useful in helping you get the most out of SJQv4.

There is no license for using the SJQ UI.  If you want to make a donation to a cause meaningful to me you can make a donation to one of these organizations:

  * [The American Cancer Society](https://www.cancer.org/involved/donate/donateonlinenow/index)

  * [The Cystic Fibrosis Foundation](http://www.cff.org/GetInvolved/ManyWaysToGive/MakeADonation/)


# Background #

To effectively install, configure and use SJQv4 you must first understand the basic terminology and design of SJQv4.  This section provides that information and should be read and understood before going any further.

Conceptually SJQv4 consists of two distinct components: The SJQ Engine and one or more Task Clients.  The Engine runs on the same machine as the SageTV Server and the Task Client(s) run on any Windows or Linux machines connected to the network. Task Clients actually _do_ the work that needs to be done (a Task) so in order to do anything useful, you must install the Engine and at least one Task Client.

In this wiki "SJQ Engine" and "Engine" are used interchangeably as is "Task Client" and "Client".

More details on the SJQ Engine and Task Client can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).


# Installation #

You must install the SJQ Engine and at least one Task Client to do anything useful with SJQ.


## SJQv4 Engine Installation ##

First, the SJQ Engine must be installed on the same machine that the Sage Server is running. This can be accomplished by installing the Engine from an Extender, PlaceShifter, or from a UI running on the Sage Server.  (i.e. Anything but a SageClient.)

To install:
  1. Go to **Setup -> SageTV Plugins -> All Available Plugins**.
  1. Choose "General" from the top selection bar.
  1. Scroll down to "Sage Job Queue (SJQ)" and install the Plugin.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Plugin-SJQ.JPG' />


If you intend to use a SageClient you **must** install the SJQ Engine plugin on the server first.

After the Engine is installed you are ready to install the UI.

## User Interface Installation ##

To install:
  1. Go to **Setup -> SageTV Plugins -> All Available Plugins**.
  1. Choose "UI Mod" from the top selection bar.
  1. Scroll down to "Sage Job Queue (SJQ) UI" and install the Plugin.

If you want to use the UI on a SageClient you need to install the UI plugin on the SageClient.

Now that the Engine and the User Interface is installed you are ready to install one or more Task Clients.


## SJQv4 Task Client Installation ##

Task Clients can either be installed via the Sage plugin manager (Integrated Installation) or from a command line (Manual Installation).  If you choose to install manually you can either install the Task Client as a Windows service (Windows only of course) or as a console application (Windows or Linux).  The big difference between these methods is that Clients installed using the plugin manager are only available when SageTV is running.  So if you install a Client via the plugin manager from a SageClient, the Task Client will only be available when the SageClient is running.

Task Clients installed from an Extender, PlaceShifter, or UI running on the Sage Server are available as long as SageTV (or the SageTV service) is running. Most users will want a Task Client installed on the Sage Server, and installing from the plugin manager is easy, so if you're just getting started I'd suggest doing just that - install a single Task Client on the same machine as your Sage Server using the plugin manager. That's described in the [next section](#Integrated_Installation.md).

A good comparison and the pros and cons of each choice can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide#Task_Client_Versions).


### Integrated Installation ###

To install:
  1. Go to **Setup -> SageTV Plugins -> All Available Plugins**.
  1. Choose "General" from the top selection bar.
  1. Scroll down to "Sage Job Queue Agent (Task Client)" and install the plugin.

<img src='http://sagetv-addons.googlecode.com/svn/trunk/sjq4/media/images/wiki_install_agent_plugin.jpg' />

After the Task Client is installed it must be registered with the SJQ Engine.  If you use the plugin manager to install the Task Client SJQ will automatically register the Task Client.


### Manual Installation ###

Instructions for manually downloading and installing a Task Client can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide#Download_Task_Client).

A summary of the process is:

  1. Download the latest sjq-agent-XXX.zip (for Linux) or sjq-agent-winsvc-XXX.zip (for Windows) from [here](http://code.google.com/p/sagetv-addons/downloads/list?can=2&q=&sort=-uploaded&colspec=Filename%20Summary%20Uploaded%20Size%20DownloadCount). Note that you should **not** manually download sjq-agent-plugin-XXX.zip.
  1. Unzip the files.
  1. Edit the sjqagent.properties file.
  1. Install and start the Task Client.

After the Task Client is manually installed it must be registered with the SJQ Engine, see the [Configuration section](#Configuration.md).


# Configuration #

After you have installed the SJQ Engine and at least one Task Client, there are three steps that must be taken to make something happen:
  1. [Register](#Register_Task_Client.md) the Task Client with the Engine.
  1. [Create](#Create_Task.md) a Task that does something useful.
  1. [Tell the Engine](#Making_Something_Happen.md) when the Task should be queued.


## Register Task Client ##

If you installed the Task Client via the [plugin manager](#Integrated_Installation.md) the Task Client will be automatically registered and you can skip this section.

If you installed the Task Client [manually](#Manual_Installation.md) you must register the Task Client with the Engine.  This is done by navigating to **Setup -> Sage Job Queue (SJQ) -> Register Client**.  Simply type in the network name or IP address of the computer on which you installed the Task Client. Using the IP address is the preferable way to go to avoid any DNS issues.

After a Task Client is registered it may take up to 2 minutes before the SJQ Engine recognizes it.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-RegisterClient.JPG' />


Note that most of the text entry dialogs are "keyboard friendly" and have been specially created so you can edit the text using the arrow, Home, End, Backspace, Insert and Delete keys. I'm assuming that most of the configuration and setup will be done using a keyboard and _not_ be done using a remote control.


## Create Task ##

This section describes how to create a Task using the SJQ User Interface.  It does **not** describe the details of each component of the Task (Executable, Test Script, parameters, etc.)  Those details can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

At a high level, Tasks consist of two components: a test script and an executable.  The test script is used to determine if it's "safe" to run the executable.  For example, you may have a task that reboots the Sage server nightly but you want to make sure the server is not busy recording a show.  Your test script would check to see if a recording is in progress and if it is tell the SJQ Engine _not_ to run the executable _now_ but to try again in a little while. Once the test script determines that no recording is in progress the SJQ Engine will run the executable, which presumably reboots your machine.

Test scripts are written in [Groovy](http://groovy.codehaus.org/) which has a syntax similar to Java.  Slugger did a lot of work to make sure that all of the Sage APIs are available in Groovy, which means you can do most anything you want with a test script.

Test scripts should return one of the following:
  * 0 - if the test "passed" and the executable should be run.
  * 1 - if the test "failed" and the Task should be put back into the queue and tried again later.
  * 2 - if the test "failed" and the executable should not be run at all.

Creating a Task can either be done [manually](#Manually_Creating_a_Task.md) by editing a properties file using a text editor or via the [user interface](#Creating_a_Task_from_Within_the_User_Interface.md).


### Creating a Task from Within the User Interface ###

Navigate to **Setup -> Sage Job Queue (SJQ) -> Show Clients** and select (from the left-most column) the Client that will handle the Task.  (If the Client does not show up it probably means you have not [registered](#Register_Task_Client.md) the Client.)  From the right menu select "Create New Task" and the following menu will appear.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-AddTask.JPG' />

The following is an explanation of the fields:

  * Task ID - Enter a user-friendly name for the Task.

  * Executable - The full path to the executable that will be run if the test script passes.

  * Executable Arguments - The arguments for the executable.

  * Test Script - The path to the script that will be run when the task is first invoked.

  * Test Script Arguments - The arguments for the Test Script.

  * Maximum Instances - The maximum number of instances of this Task that can be simultaneously run on this Client.

  * Maximum Return Code - The maximum return code from the executable.

  * Minimum Return Code - The minimum return code from the executable.

  * Maximum Time - The maximum number of seconds that the Task should be allowed to run.  If execution exceeds this time limit the Engine will assume the Task has hung and will attempt to kill it.

  * Resources - The percentage of Client resources to allot to this Task.

  * Schedule - This field can be used to limit at what times the Client is capable of running the Task. The entry must be a valid crontab pattern as defined [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4Crontab) and [here](http://www.sauronsoftware.it/projects/cron4j/api/it/sauronsoftware/cron4j/SchedulingPattern.html) or the special values of ON or OFF.

  * Maximum Time Ratio - This is currently not used and should be left at its default value.

The details of these fields can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide#Configuring_sjqagent.properties).

After all required fields are entered you **must** select "Add New Task" or the new Task will not be saved.


### Manually Creating a Task ###

Manually creating a Task is beyond the scope of this wiki but is covered in detail [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide#Configuring_sjqagent.properties).


## Making Something Happen ##

If you've gotten this far it means you have:

  1. [Installed the SJQ Engine.](#SJQv4_Engine_Installation.md)
  1. [Installed the SJQ User Interface.](#User_Interface_Installation.md)
  1. [Installed at least one Task Client.](#SJQv4_Task_Client_Installation.md)
  1. [Registered the Task Client.](#Register_Task_Client.md)
  1. [Created a Task.](#Create_Task.md)

Now it's time to tell the SJQ Engine when to run the Task.  More specifically, we need to tell the Engine to _queue_ a Task to a Task Client that is capable of running Task.

Tasks can be queued in one or more of the following ways:

  1. [When a Favorite recording begins or ends.](#Triggering_Tasks_via_Favorites.md)
  1. [On a periodic basis.](#Running_Tasks_on_a_Periodic_Basis.md)
  1. [When certain Sage events occur.](#Running_Tasks_When_Certain_Sage_Events_Occur.md)
  1. [On demand.](#On_Demand.md)

Once a Task is queued the SJQ Engine will attempt to find a registered Task Client that is capable of running the Task.


### Triggering Tasks via Favorites ###

When you create or edit a Sage Favorite you will notice a new option called "SJQ Actions". Selecting this will bring up a dialog which allows you to select which Tasks to run against favorites and when to queue the Tasks.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-ManageFavorites-SelectTasks.JPG' />


The left column of the dialog shows which Sage Events are capable of triggering a Task and the right column shows which Tasks are available.  Currently, the SJQ Engine is capable of queuing a Task when a Favorite recording starts (RecordingStarted) or when it stops (RecordingStopped). For each of these Events, one or more Tasks can be selected.

Note that Tasks triggered by RecordingStarted and RecordingStopped should be aware that when a recording is stopped it may not be _complete_ and when a recording starts it may not be the _beginning_.  This is because the Sage Core may start and stop a recording several times for various reasons. Tasks triggered by these Events need to be aware that they may be triggered several times on the same recording.


### Running Tasks on a Periodic Basis ###

To have SJQ queue Tasks on a periodic basis navigate to **Setup -> Sage Job Queue (SJQ) -> Crontab (Schedule Tasks)**.  From there a dialog will appear that allows you to "Add a New Entry" or "Manually Add an Entry".  The difference between these two options is that "Add a New Entry" presents you with a user friendly dialog while "Manually Add an Entry" presents you with a text entry dialog that allows you to enter anything you want.  The dialog is easier to use, but less powerful than manually entering the data.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab.JPG' />



#### Dialog ####

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-Custom.JPG' />



The following is an explanation of the entries on this dialog:

  * Task - This is the Task that will be queued at the appropriate time. Selecting this entry will present you with a dialog that allows you to choose the Task.

  * Task Arguments - The arguments for the Task.

  * Frequency - The frequency that the Task should be queued.  Selecting this entry will present you with a dialog that lets you choose Daily, Weekly, Monthly, Annually or Custom.

  * Depending on the Frequency chosen the next several items will allow you to enter the Minute(s), Hours(s), Day(s) of Month, Month(s), and Day(s) of Week to queue the Task.

  * Next Run - This will display when the Task will next be queued given the current selections on the dialog.

It is possible to use the dialog to create the basic crontab pattern and then manually edit the entry to create more advanced patterns.  Note that if you do the opposite (create an entry manually and then use the dialog to edit it), you **will** **lose** any manual changes you made.


#### Manual ####

The format of the entry should be: `* * * * * TaskID Parm0 Parm1 ... ParmN`.  The first five asterisks must be a valid crontab scheduling pattern, TaskID must be a valid Task ID and Parm0 to ParmN are optional and if included should be valid parameters for the Task. The validity of the crontab pattern is the only parameter checking that is done.

More information on crontab and valid crontab entries can be found [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4Crontab) and [here](http://www.sauronsoftware.it/projects/cron4j/api/it/sauronsoftware/cron4j/SchedulingPattern.html).


### Running Tasks When Certain Sage Events Occur ###

SageTV generates "Events" when certain things of interest happen in the SageTV Core. SJQ has the ability to queue Tasks when certain Sage Events occur.  Currently supported Events are:

  * RecordingStarted
  * RecordingStopped
  * MediaFileImported
  * SystemMessagePosted

To have SJQ queue Tasks when one of these Events occur navigate to **Setup -> Sage Job Queue (SJQ) -> Assign Tasks to Events**.  Select the Event in the left column and then in the right column select which Task or Tasks to queue when that Event occurs.  So if you created a Task called GetMetaData that you want queued any time a new movie has been imported into the SageTV database, select MediaFileImported in the left column and then select GetMetaData in the right column.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-AssignTasksToEvents.JPG' />


### On Demand ###

Tasks can be queued on demand by navigating to **Setup -> Sage Job Queue (SJQ) -> Queue Task**.  Select the Task or Tasks you want to queue and they will be queued immediately.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-QueueTask.JPG' />

Note that when you queue a Task in this manner no MetaData will be available.  So doing something like queuing a comskip Task makes no sense because there will not be any recording data available.  This method of queuing Tasks is more useful for generic Tasks that you want to run immediately.


# SJQ Menus #

The following is an overview of the menus located under **Setup -> Sage Job Queue (SJQ)**.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu.JPG' />


## Register Client ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-RegisterClient.JPG' />

This menu allows you to register Task Clients with the Engine. Simply type in the name or IP address of the Client that you wish to register with the Engine.  Clients that are not registered will never be assigned work.

To un-register Clients use the Show Clients menu, described below.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-RegisterClient.JPG' />


## Show Clients ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-ShowClients.JPG' />

This menu shows information about registered Clients including their state, Tasks that they are currently running, and Tasks they are capable of running.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-ShowClients.JPG' />

  * State - The current state of the Task Client.  This will usually be ONLINE or OFFLINE. Clients can be OFFLINE because the computer hosting the Client is unavailable or the Client version is too old for the installed Engine.

  * Last Update - This shows the last time the Engine checked the status of the Client.  Selecting this item will force the Engine to update the status of the Client.

  * Maximum Resources - For details see [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

  * Free Resources - For details see [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

  * Host Name - The network name or IP address of the computer running the Task Client.

  * Port - The Port that the Task Client is listening on.

  * Schedule - This indicates when a Client is capable of running Tasks.  Valid values are ON, OFF, or a valid crontab pattern.  (crontab patterns are described [here](http://www.sauronsoftware.it/projects/cron4j/api/it/sauronsoftware/cron4j/SchedulingPattern.html)).  ON indicates that the Client can accept Tasks at any time. OFF indicates that the Client can't accept any Tasks.  If a crontab pattern is specified the Client can accept Tasks at the specified times.

  * Active Tasks - Selecting this shows the Tasks that are currently active and their state. Tasks that are RUNNING may be killed (stopped).  Tasks that are FAILED, WAITING, or RETURNED may be removed from the queue.

  * Supported Tasks - Selecting this shows which Tasks this Client is capable of running.

  * Create New Task - Selecting this allows you to create a new Task for this Client.  This is explained in more detail in the [Create Task](#Creating_a_Task_from_Within_the_User_Interface.md) section of this wiki.

  * Version - The version number of the Task Client.

  * Commit Changes - If any changes are made to the Client, **including** adding new Tasks, the changes **must** be committed before they take effect.

  * Enable ALL Clients - Selecting this sets the Schedule for all Clients to ON.

  * Disable ALL Clients - Selecting this sets the Schedule for all Clients to OFF.

To un-register a Task Client navigate to the proper Client in the left column and then select it (Enter on a keyboard or OK on a remote) or press the Delete key.


## Show Queued Tasks ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-ShowQueuedTasks.JPG' />

This menu shows you the status of Tasks that are currently queued.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-ShowQueuedTasks.JPG' />

The left column lists the queued Tasks.  The right column contains the following:

  * State - The state of the Task.  It can be RUNNING, WAITING, or RETURNED. Selecting the item will allow you to stop a RUNNING Task or remove a WAITING or RETURNED Task from the queue.

  * Client - The Task Client that will attempt to run the Task.

  * Created - The time the Task was queued.

  * Started - The time the Task execution started.

  * Completed - The time the Task completed.

  * MetaData - Selecting this item will display the MetaData associated with the Task.  For details on MetaData see [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

  * Queued ID - Each Task that is Queued receives a unique queue ID.  Most users need not worry about this value.

  * Show The Log - Selecting this item allows you to view the log file information for the Test Script, the Executable, or both.  This is useful for debugging purposes.

A Task can be stopped or removed from the queue either by selecting it from the left column or by pressing the Del key in the right column.

## Show Completed Tasks ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-ShowCompletedTasks.JPG' />

This menu shows you the Tasks that have completed either successfully or unsuccessfully.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-ShowCompletedTasks.JPG' />

The completed Tasks are listed in the left column.  The right column contains the following:

  * State - The state of the Task.  It can be COMPLETED, SKIPPED, or FAILED. Selecting the item will allow you to remove the Task from the queue.

  * Client - The Task Client that executed the Task.

  * Created - The time that the Task was queued.

  * Started - The time that the Task began execution.

  * Completed - The time that execution ended.

  * MetaData - Selecting this item will display the MetaData associated with the Task.  For details on MetaData see [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

  * Queued ID - Each Task that is Queued receives a unique queue ID.  Most users need not worry about this value.

  * Show The Log - Selecting this item allows you to view the log file information for the Test Script, the Executable, or both.  This is useful for debugging purposes.

  * Remove Tasks - Selecting this item will allow you to remove Tasks with specific States from the queue.

  * Remove ALL Tasks - Selecting this will remove all Tasks from the queue.

A Task can be removed from the queue either by selecting it from the left column or by pressing the Del key in the right column.


## Queue Task ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-QueueTask.JPG' />

This menu allows you to queue one or more Tasks immediately.  Just select the Task(s) and they will be queued.  For details see [On Demand](#On_Demand.md)

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-QueueTask.JPG' />


## Crontab (Schedule Tasks) ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-Crontab.JPG' />

This menu allows you to schedule Task to be queued on a periodic basis.  For details see [Running Tasks on a Periodic Basis](#Running_Tasks_on_a_Periodic_Basis.md).

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab.JPG' />


<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-Daily.JPG' />


<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-Monthly.JPG' />


<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-SelectHours.JPG' />


<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-SelectDays.JPG' />


<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-Crontab-Edit.JPG' />


## Assign Tasks To Events ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-AssignTasksToEvents.JPG' />

This menu allows you to trigger Tasks when certain Sage Events occur.  For details see [Running Tasks When Certain Sage Events Occur](#Running_Tasks_When_Certain_Sage_Events_Occur.md) in this wiki.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-AssignTasksToEvents.JPG' />


## Manage Favorites ##

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-ManageFavorites.JPG' />

This is a convenience link that takes you to Sage's "Manage Favorites" menu.

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-ManageFavorites.JPG' />


## Configure ##

This menu allows you to configure the SJQ Engine. Most users will not need to change anything, but if you do the details are [here](http://code.google.com/p/sagetv-addons/wiki/Sjq4UserGuide).

<img src='http://tmiranda.googlecode.com/svn/trunk/SJQ/ScreenShots/SJQ-MainMenu-Configure.JPG' />