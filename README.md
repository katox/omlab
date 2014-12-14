omlab
=====

Lab project for Om

## Prerequisites

You will need [Leiningen][1] 2.3.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Compilation (dev)

To compile the project run

    lein do deps, cljsbuild once, test

## Running (dev)

To start a web server for the application, run:

    lein ring server

To start a web server on custom port 8080, run

    lein ring server 8080

The default dev login is `root`/`root`.

## REPL session

To start a REPL development session run:

	lein repl
	(go) ; init and run ring server
	(load-test-data) ; load a testing data set
	(cljs-repl)	; create cljs REPL accepting connections at 9001

You can now connect to the app at http://localhost:4000 and login
with default dev credentials. When the application is loaded you should see
the message `Opened Websocket REPL connection` in javascript Console.

You can now access the browser from the REPL session.

    cljs.user=> (+ 1 2)
    3

Open a popup dialog in the connected browser:

    cljs.user=> (js/alert "Ahoy!")
    nil


## Production

To create a production standalone jar run

    lein with-profile prod do cljsbuild once, ring uberjar

Run the server process with (the default port is 3000)

    java -jar omlab-standalone.jar

### Testing production build

Create a production uberjar and run it with the test config 

    CONFIG_FILE=dev-resources/config_dev.edn \
    PORT=4000 \
    java -jar target/omlab-standalone.jar


License
=======
Copyright © 2012 leafclick s.r.o.

Distributed under the Eclipse Public License, the same as Clojure.
