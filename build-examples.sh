#!/bin/sh

mvn clean install || exit
cd damac
mvn compile assembly:single || exit
cd ..
echo "examples have been built, you can now cd examples to execute them"
cat examples/README.md

