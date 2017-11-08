#!/bin/sh

mvn clean || exit
echo waiting 5 seconds ...
sleep 5
mvn install || exit
cd damac
mvn compile assembly:single || exit
cd ..
echo "Damap has been fully built, you can now do 'cd examples' and execute them"
cat examples/README.md
