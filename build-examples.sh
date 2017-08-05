#!/bin/sh

cd damai
mvn clean install || exit
cd ../damac
mvn clean compile assembly:single || exit
cd ..
AGENDA=../nalasys-apps/agenda/WEB-INF
if [ -d $AGENDA ]; then
   echo "$AGENDA found :) copying files ... "
   cp examples/damai-agenda.properties $AGENDA/classes
   cp damac/target/damac-jar-with-dependencies.jar $AGENDA/lib
   touch $AGENDA/web.xml
fi

