## Introduction
Meltdown (provisional name) is an open-source Android client for [Fever RSS](http://feedafever.com/). This is me trying to solve a personal need: I want a good RSS reader on my new hardware from Google I/O. And that lovely 7" Nexus screen really demands a good feed reader. Boom, off I go.

To use it, you **must have an installed copy of [Fever server](http://feedafever.com/).** This is *just a client*.

Please [email me](mailto:phubbard@gmail.com) if you want to hack on it; I could really use some help as my time is limited.

## Current status
* As of build 12 (9/21/12), basic functionality is in place and working well. Groups, feeds, webview, local caching, garbage collection. Tested on over 9000 items from 800 feeds. (There's a reason I wrote this, I adore my RSS!)
* Next item does a neat async mark-as-read in the background
* Load URL works
* Added the sharing button to the action bar in group view - send via whatever mechanism you like.
* Redid the http code after re-reading [the Android docs](http://developer.android.com/guide/components/processes-and-threads.html) again.
* After testing SQLite to hold the HTML,the write rate was piss poor and I now just store the entire item on disk in JSON. Total abuse of the filesystem, but even 5000 items takes around 25MB of disk and it's fast. More tuning needed here.
* Data is refreshed every 30 minutes by a background intent service, which should also sync if you read on the web, in Reedr, etc.

## Current bugs and in-progress
* Several thread-safety issues caused by async updates. Working on this.

## Future plans and ideas
* 'Send this to me in N hours/days/months' - use a cron or webservice to send you items, like a tickler file.
* Pay for nicer icon (freelancer.com or similar)
* Use persistent notifications to show background fetches; nice and unobtrusive.
* About / stats screen
* More preferences
* Tabbed interface for unread/read/sparks/kindling/saved

### Background information, tools and credits
* [The Fever REST API](http://feedafever.com/api) is elegant, simple and returns either JSON or XML. I can think of no reason to use XML, so JSON it is.
* The [API widget](https://github.com/phubbard/Meltdown/blob/master/scripts/api-widget.html) is really helpful for poking at the API and responses. I'm also using this [graphical http client](http://httpclient.uservoice.com/) as well, which helped me sort out the header needed to get authentication working. The scripts directory has a copy of the widget for convenience.
* Since I'm developing for the Galaxy Nexus and Nexus 7, the current target API is 4.0, but it could probably run on older devices.
* The RSS icon is from [the iDroid S](http://iiro.eu/idroids/) icon set, for which I am thankful.
* The code from RssReader.java is copyright 2007 by the Android Open Source Project under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0) for which I am grateful. The ListItem stuff is poorly documented, so this was a big help. 

### License and credits

This code is open source under the [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/us/) terms. I'd appreciate a note if you make use of it, please. Other than that, I'd be pleased if you found it useful either whole or in part.


