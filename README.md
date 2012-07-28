## Introduction
Meltdown (provisional name) is an open-source Android client for [Fever RSS](http://feedafever.com/). This is me trying to solve a personal need: I want a good RSS reader on my new hardware from Google I/O!


Please [email me](mailto:phubbard@gmail.com) if you want to hack on it; I could really use some help as my time is limited.

## Current status

* Setup screen works - gather server info, save to private preferences, test login
* Groups screen fetches and displays a list of groups
* As per the 4.0 APIs, fetching is done with an AsyncTask and interface callbacks. 
* I've decided to use in-memory lists for display in the first pass, and go to SQLite/CursorAdapter once it all works. KISS principle.

### Background information

* [The Fever REST API](http://feedafever.com/api) is elegant, simple and returns either JSON or XML. I can think of no reason to use XML, so JSON it is.
* The [API widget](http://feedafever.com/gateway/public/api-widget.html.zip) is really helpful for poking at the API and responses. I'm also using this [graphical http client](http://httpclient.uservoice.com/) as well, which helped me sort out the header needed to get authentication working.
* Since I'm developing for the Galaxy Nexus and Nexus 7, the current target API is 4.0, but it could probably run on older devices.

### License

This code is open source under the [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/us/) terms. I'd appreciate a note if you make use of it, please.
