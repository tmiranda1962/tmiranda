# Introduction #

Basically, I'm going to discuss what I'm thinking for the UI and then wait for you to counter back with what's possible/how much you actually want to tackle.

I've decided to think big and then let you "bring me back to reality", so to speak. :)  Warning, my screen mockups are terrible, but I don't claim to be artistic. :)  So here we go...

# Main Menu #

We'll need to inject a "main menu" for SJQ somewhere.  I'm not too fussy about where that menu ends up, but I'd expect it ends up off the Setup menu.  Doesn't have to be the first item and probably shouldn't be, but it was way easier for me to put it first in my mockup. :)

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_main_loc.jpg' />

Within that menu there should be options for the various config settings of SJQ.

  * Register Client: Allow the registration of a new task client
  * Show Clients: Show a table of all registered task clients; this table will also show the state of each client (online, offline, etc.)
  * View Task Queue: Show a table of the active task queue
  * Configure: Not sure if this will be needed, I think these options (how often the agent pinger runs, how often the task queue runs, etc.) will go in the plugin config screen, but it seems like it would fit better here.  To be discussed.

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_main_opts.jpg' />

## Register Client ##

Simple form that accepts a hostname and a port number.  Hostname can't be empty and port number must be > 0.  Take the input and use the DataStore to save it.  You probably would want to check if the given client is already registered and give an error if it is, though it's not totally necessary.  (Sorry, no mock up of this screen :) )

## Show Clients ##

Presents a table of each registered task client along with its current state and the last time it was updated (all available from Client objects).  Each row of the table should be selectable and present options to:

  * Delete the task client (not implemented, but will be added to `ServerClient`)
    * This op can fail if the task client is actively running tasks, etc.
  * Ping the task client (not implemented yet, just thought of it)
    * Quick way for users to bring a task client back to `ONLINE` state once they fix errors
  * (Optional) View stats about the client (resource utilization, which tasks the client is running, quantity of tasks client is running, etc.)

Derek - For this I am considering a menu similar to the "Detailed Setup" menu.  The leftmost column will be a scrollable list of Clients, the middle column will be where information will be displayed (state, last updated, ping, delete, stats, anything else...) and the rightmost column will be where the user selects to take action on the information or display additional detailed information.

## View Task Queue ##

Presents a table of the active task queue as read from `ServerClient`.  Basically display all tasks that are running or waiting to run along with the date they were inserted, date they were started, etc. (basically all the info provided in a `QueuedTask` object.

Each row should be clickable and provide abilities to see additional info for the entry such as the attached metadata and perhaps the logged output of the task.  Also, the option to delete the task from the queue (if it's in `WAITING` state) or kill the task on the assigned client (if it's in `RUNNING` state) would also be presented here.

Being able to "select" multiple rows (of tasks in the same state) and perform actions on them would also be useful, for example, being able to select all `WAITING` tasks and bulk delete them would be nice.  Same with `RUNNING` tasks except you can bulk kill them.

Derek - This will be difficult so I'd like to hold off until after we get the basic functionality working.  I can imagine adding a "filter" as an option so the user can select a filter to view just the tasks wanted. (Running, waiting, per client, etc.)  Something to think about for a follow-up release.

(Optional) Select all `RUNNING` tasks for a single client and bulk kill them.  Instead, this could be an option for the View Cients screen.  When you select a client you can choose to "Kill All Running Tasks on this Client" kind of thing?

Derek - I'd like to keep the "look and feel" as consistent as possible so I am also thinking about using the same menu layout as above.  The leftmost column will be a scrollable list of tasks, the center will contain informaation and the leftmost column will allow the user to change the data or display more detailed information.

## Configure ##

Allow modification of engine settings.  Not sure if this should go here or be part of the plugin config.  Probably the latter.

Derek - For this I can just jump to the Plugin config options menu so we get the best of both worlds.  I personally like the idea of making it easy to access the config options without backing all the way out of the menus and then going all the way to Settings->Plugins-> yada yada....

## Crontab ##

This would go above the Configure option, I just forgot about it and didn't want to remock the whole screen. :)

I would just provide a big textarea box that allows free form editing of the crontab file, which you'll be able to read from `ServerClient`.  If you're feeling particularly inspired then you could format the crontab file as a table and allow editing of it row by row, but I wouldn't worry about that.  We'll discuss it more when you get there.

Derek - I'm inclined to make it editable row by row.  If that proves too difficult or time consuming I'll go with a general text dialog.

# Inserting Tasks into Queue #

Now the rest of the UI is injecting SJQ options into the existing STV.  All of these STV injections are about adding the ability to put tasks in the queue.

Any Sage object can be queued up as a task with appropriate metadata attached to the task for injection into the task's execution environment.  How much support is added for this is totally up to you.

Favs and media files are the big ones, after that I'll discuss my thoughts, but how much of it gets done is totally up to you.

## Attaching Tasks to Favourites ##

Users can attach a list of tasks that they want to queue up each time a favourite starts recording.  It's a simple comma separated list of task ids to queue up each time the favourite starts recording.  I see two choices for this:

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_favs1.jpg' />

This puts the option under the Advanced Options of a fav.  My only reservation about this is that it's buried rather deep.  But I'm not sure if you can add it directly to the screen below?

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_favs2.jpg' />

Not sure if this screen can scroll to add options or not?  Might be a better fit for it if you can add it there?

Regardless of where it ends up, the idea is the same.  The user edits a comma separated list of task ids, which you then save as a `FavoriteAPI.SetFavoriteProperty("SJQ4_TASKS", value)` API call.  Each time a recording for this fav starts, SJQ looks at this property and queues up the desired task list.

## Attaching Tasks to Manual Recordings ##

Users can also attach tasks to manual recordings.  Pretty much the same idea except the task list is saved via `AiringAPI.SetManualRecordProperty()` API call.  Again, SJQ will view this property when the manual recording starts and queue up the tasks.

So this is a screen show showing the options for a manual recording:

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_manrec.jpg' />

Basically I'd just add an option here "SJQ Task Options" and then allow the user to edit the task list for the manual recording.

## Queueing Up Existing Media Files ##

Most of the time, users will queue up tasks via the favs or manual recording approach, but sometimes users will want to manually queue up a task for an existing media file (perhaps because a task failed or they want to run additional tasks, etc.).  They can do this from the Recordings menu:

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_manq_group.jpg' />

So here's an options screen for multiple recordings.  Just noticed this one, but you could add an option here to queue up tasks for all recording in this group.

But my original thoughts were doing it on a single recording like this:

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_manq_single.jpg' />

Basically, the user drills down to the options for a single media file and then you add an option to "Send to SJQ" or something where they then provide a single task id or a list of task ids and then they get added to the queue.

Imported videos, music, and photos all have similar options screens where SJQ can be injected into.  I'm not too worried about music and photos, but I suspect Imported Videos would be a popular request.

# Optional Goodies #

So what's above is really the heart of what I'm thinking.  But as I said earlier, SJQv4 supports the queueing of **any** Sage object.  So I can't really think of a popular use case for these, but users may want to send other objects to the queue.

## Airing and Show Objects ##

Airing and Show objects can be added to the queue via the EPG:

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_epg.jpg' />

So here you'd add options to "Send Airing to SJQ" and/or "Send Show to SJQ".  If the airing also has a media file object (it's a completed or active recording) then you'd provide the same options to send the media file to SJQ, etc.

## System Messages ##

System messages will be handled separate such that users can provide a list of task ids to queue up for all generated system messages.  But there could be a need to requeue a system message task (can't think of a really good reason, but I'm sure someone will).

<img src='http://gateway.battams.ca/sjq4/screens/sjq4_sysmsgs.jpg' />

I don't have any system messages right now, but I'm sure you get the idea.  User selects a message and then a dialog to "Send to SJQ" shows up and then they can provide the list of task ids to queue for the sys msg they selected.

## And Anything Else You Can Think Of... ##

And so on! :)  SJQ supports sending Plugins and so on... any Sage object.  So you can add options at appropriate places to support "Send to SJQ" functionality.  I really can't think of why someone would want to process a Plugin object, for example, so I really, really wouldn't worry about that. :)  But if you want to, you can add it. :)