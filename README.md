# Nexmo Pubnub Monitor

Pubnub is commonly used to communicate messages from devices. In case any of the
devices go offline, it is typically beneficial to become aware of it as soon as
possible. By installing this project on Heroku, the server will continuously
monitor your devices and notify via SMS or other Nexmo-provided channels when
any of your devices stop sending messages on Pubnub. It also provides a web page
where you optionally can inspect the status of your devices.

## Implementation

Clojurescript based reference SPA on Heroku based on Nexmo and Pubnub,
using node express, bootstrap, reactjs/reagent and Kioo templates,
demonstrating "isomorphic" clojurescript shared between frontend and backend
and [Figwheel](https://github.com/bhauman/lein-figwheel) hotloading code changes to both.

In addition to the server monitoring a pubnub channel, the implementation also
**uses Pubnub internally** to communicate between the server and browser.
The details about the monitored pubnub channel is shielded from exposure
to those accessing the web monitor. It would be trivial to filter the messages
to limit what is shown in the browser.

The implementation is highly *isomorphic*: Most of the code is shared between
the browser and the server. See the /src/cljs directory for the shared code,
/src/browser for the browser specific and /src/node for the server only code.

## Demonstration

Open the demo monitor page at https://nexmo-pubnub.herokuapp.com

The page will display a panel for each device that are monitored
by the server (possibly none). Each panel shows the id of the device
on top, and the more recent values from the device as transmitted
in a pubnub message.

To test out the monitoring and alert functionality,
create device emulators by clicking the **+Emulate Device**
button on the navigation bar of the demo page.
The device emulators run in the browser,
sending randomly created messages on the pubnub channel
used to monitor devices.

Open a second demo page using the url above (optionally on another computer).
Verify that the emulated devices are displayed and updated on the second page.
You may create additinal devices on this second page, which will show up on
any other open demo monitor.

## Deploy to Heroku

These instructions assumes you already have installed
[Leiningen](http://leiningen.org/) and the
[Heroku CLI](https://devcenter.heroku.com/articles/heroku-command).

To configure a server on Heroku:

    heroku apps:create
    heroku addons:create pubnub
    heroku config:set NEXMO_KEY=<nexmo-key>
    heroku config:set NEXMO_SECRET=<nexmo-secret>

Optionally specify a channel as source for device messages:

    heroku config:set PUBNUB_SOURCE_KEY=<pubnub-key>
    heroku config:set PUBNUB_SOURCE_CHANNEL=<pubnub-channel>

Start the server:

    git push heroku master
    heroku open

This will open the site in your browser.

## Run Locally

To start a server on your own computer using the environment config from heroku:

    heroku config -s >> .env
    heroku local:run lein do clean, deps, compile
    heroku local

Point your browser to the displayed local port.
Click on the displayed text to refresh.

## Development Workflow

Start figwheel for interactive development with automatic builds
and code loading while using the config from heroku:

    heroku local:run lein figwheel app server

Wait until Figwheel is ready to connect, then
start a local server in another terminal as explained in
another section.

Open the displayed URL in a browser.
Figwheel will push code changes to the app and server.

## License

Copyright © 2015 Terje Norderhaug

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
