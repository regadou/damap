#!/bin/sh

cd damai
mvn clean install || exit
cd ../damac
mvn clean compile assembly:single || exit
cd ..
AGENDA=../nalasys-apps/agenda/WEB-INF
if [ -d $AGENDA ]; then
   echo "$AGENDA found, copying files ... "
   cp examples/damai-agenda.properties $AGENDA/classes
   cp examples/damai-agenda-init.js $AGENDA/classes
   cp damac/target/damac-jar-with-dependencies.jar $AGENDA/lib
   cp examples/damai-agenda-web.xml $AGENDA/web.xml
fi
echo "examples have been built, you can now cd examples to execute them"
