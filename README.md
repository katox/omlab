omlab
=====

Lab project for Om

## Prerequisites

You will need [Leiningen][1] 2.3.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Compilation (dev)

To compile the project run

    lein do deps, cljsbuild once none, test, jar

## Running (dev)

To start a web server for the application, run:

    lein ring server

To start a web server on custom port 8080, run

    lein ring server 8080

## Production

To create a production standalone jar run

    lein with-profile prod do cljsbuild once, ring uberjar

Run the server process with (the default port is 3000)

    java -jar omlab-standalone.jar

License
=======
Copyright © 2012 leafclick s.r.o.

Distributed under the Eclipse Public License, the same as Clojure.
