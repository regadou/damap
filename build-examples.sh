#!/bin/sh

mvn clean install || exit
cd damac
mvn compile assembly:single || exit
cd ..
touch resti/target/resti/WEB-INF/web.xml
echo "examples have been built, you can now do 'cd examples' and execute them"
cat examples/README.md

