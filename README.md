## Introduction
Meltdown is an open-source Android client for [Fever RSS](http://feedafever.com/). This is me trying to solve a personal need: I want a good RSS reader on my new hardware from Google I/O. And that lovely 7" Nexus screen really demands a good feed reader. 

It's written for people with lots of RSS feeds, preferably organized into groups, with lots of news items. I also put a lot of effort into the sync code, so you can easily read news in multiple places and not see duplicates.

To use it, you **must have an installed copy of [Fever server](http://feedafever.com/).** This is *just a client*.

Please [email me](mailto:phubbard@gmail.com) if you want to hack on it; I can always use help as my time is limited.

## Current Features
* **Speed**. In-memory data structures, with on-disk JSON files for entries; nice and fast. No excess graphical crap, just fast text and listview display. Designed for high-volume consumers of RSS like myself. **My #1 goal was to make this super fast.**
* Low battery usage - uses Alarm service and background process to refresh the lists every fifteen minutes, using the 'inexact repeating' mode to avoid excess wakeups.
* Compressed downloads - if your Apache is configured, it'll transfer the data using gzip compression to save time and bytes.
* Local cache - all items are encoded as JSON and written to disk, so if you reboot or kill the app it won't need to re-pull everything again.
* Asynchronous mark-as-read and mark-as-saved using AsyncTask - when you hit 'Next' it's done in the background.
* In the Item view, in addition to the ususal sharing menu I've also added a tickler-file function. This lets you set a calendar entry to remind yourself in a week (or whenever). I use this to do things like 'Remind me to check this out in a couple weeks when it's due to ship.' Very handy! 
* Mark group as read - also runs in background.
* Uses efficient and secure LocalBroadcastManager to communicate between the background service and the foreground views.
* **No excessive permissions** Requires only Internet and 'run in background' permissions, does not use or need any data or other apps. Inspect the code and build it yourself if you like!
* **No phone-home BS**. I hate that. So should you. At no time will it ever contact anything other than the server you designate.
* Read in multiple places - the next sync will remove anything you've already seen, or you can hit refresh manually at any time.
* Does not have group management or feed management. The Fever API has not added these, and I tend to think they're better done via the Fever web app anyway.
* Simple flow-oriented interface - Feed title and author at the top of the screen, followed by title and content. The bottom of the screen has the 'load URL' button, the timestamp (using human-readable relative-time strings such as '2 hours ago') and the 'Next' button. Two buttons! Everything else is in the share menu and options menu.

## Current status
* Working code! 

## Current bugs and in-progress
* [Available on the Google App Store](https://play.google.com/store/apps/details?id=net.phfactor.meltdown&feature=search_result#?t=W251bGwsMSwyLDEsIm5ldC5waGZhY3Rvci5tZWx0ZG93biJd) and submitted to the Amazon App Store.


## Future plans and ideas
* SSL is not supported. Doing this with self-signed certs in a correct way will require some work.
* The ListViews work fine on my nexus7 tablet, and present an info-dense display, but I'd like to try fragments and a landscape-mode, two-column display there. Especially now that the nexus 10 is out.
* Add favicons to item list - I wrote the code to download the favicons, but am still trying to figure out where they add utility.
* Pay for nicer icon (freelancer.com or similar)
* Use persistent notifications to show background fetches; nice and unobtrusive.
* More preferences
* Tabbed interface for unread/read/sparks/kindling/saved

### Background information, tools and credits
* [The Fever REST API](http://feedafever.com/api) is elegant, simple and returns either JSON or XML. I can think of no reason to use XML, so JSON it is.
* The [API widget](https://github.com/phubbard/Meltdown/blob/master/scripts/api-widget.html) is really helpful for poking at the API and responses. I'm also using this [graphical http client](http://httpclient.uservoice.com/) as well, which helped me sort out the header needed to get authentication working. The scripts directory has a copy of the widget for convenience.
* Since I'm developing for the Galaxy Nexus and Nexus 7, the current target API is 4.0, but it could probably run on older devices. I also develop and test on my Galaxy S3 (Verizon).
* Icons from the [the iDroid S](http://iiro.eu/idroids/) icon set, high-res RSS icon from [findicons.com](http://findicons.com/icon/46967/feeds?width=512). I have an order pending with the IconFactory for a final version.
* The code from RssReader.java is copyright 2007 by the Android Open Source Project under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0) for which I am grateful. The ListItem stuff is poorly documented, so that code was a big help. I used their ListView and excerpt code nearly verbatim.

### License

This code is open source under the [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/us/) terms. I'd appreciate a note if you make use of it, please. Other than that, I'd be pleased if you found it useful either whole or in part.


