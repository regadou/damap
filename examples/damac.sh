#!/bin/sh

debugargs="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
debug=""
for  i in $@; do
   if [ "$i" = "-debug" ] || [ "$i" = "--debug" ]; then
      debug=$debugargs
   fi
done

java $debug -jar ../damac/target/damac-jar-with-dependencies.jar $@

