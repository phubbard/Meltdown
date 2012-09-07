## Introduction
Meltdown (provisional name) is an open-source Android client for [Fever RSS](http://feedafever.com/). This is me trying to solve a personal need: I want a good RSS reader on my new hardware from Google I/O. And that lovely 7" Nexus screen really demands a good feed reader. Boom, off I go.

To use it, you **must have an installed copy of [Fever server](http://feedafever.com/).** This is just a client.

Please [email me](mailto:phubbard@gmail.com) if you want to hack on it; I could really use some help as my time is limited.

## Current status
* Group display works, though unread counts are wrong
* Item list works
* Items are correctly displayed in a web view
* Next item does a neat async mark-as-read in the background
* Load URL works
* Found some nice sample code in the Android kernel that will help making a custom list view of items.
* Pull, parse and save of group and feed data is working correctly. All in-memory and super fast.
* Redid the http code after re-reading [the Android docs](http://developer.android.com/guide/components/processes-and-threads.html) again.
* I've decided to use in-memory lists for display in the first pass, and go to SQLite/CursorAdapter once it all works. KISS principle.

## Current bugs and in-progress
* Crashes on first run post-setup. Repeatable.
* Mark as read works, but the list of items does not update to reflect this.
* Only pulls 200 items at launch. This logic needs lots of work and perhaps an IntentService.

### Background information, tools and credits
* [The Fever REST API](http://feedafever.com/api) is elegant, simple and returns either JSON or XML. I can think of no reason to use XML, so JSON it is.
* The [API widget](https://github.com/phubbard/Meltdown/blob/master/scripts/api-widget.html) is really helpful for poking at the API and responses. I'm also using this [graphical http client](http://httpclient.uservoice.com/) as well, which helped me sort out the header needed to get authentication working. The scripts directory has a copy of the widget for convenience.
* Since I'm developing for the Galaxy Nexus and Nexus 7, the current target API is 4.0, but it could probably run on older devices.
* The RSS icon is from [the iDroid S](http://iiro.eu/idroids/) icon set, for which I am thankful.
* The code from RssReader.java is copyright 2007 by the Android Open Source Project under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0) for which I am grateful. The ListItem stuff is poorly documented, so this was a big help. 

### License and credits

This code is open source under the [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/us/) terms. I'd appreciate a note if you make use of it, please.


