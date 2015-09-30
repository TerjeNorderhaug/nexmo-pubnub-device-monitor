# nexmo-pubnub.cljs

Clojurescript based reference SPA on Heroku based on Nexmo and Pubnub,
using node express, bootstrap, reactjs/reagent and Kioo templates,
demonstrating "isomorphic" clojurescript shared between frontend and backend
and Figwheel hotloading code changes to both.

Check it out at https://nexmo-pubnub.herokuapp.com

## Deploy to Heroku

To start a server on Heroku:

    heroku apps:create
    heroku addons:create pubnub
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

Based on an app concept by Marian Schubert.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
