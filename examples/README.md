The examples folder contains some examples of using Damai interfaces
Execute build-examples.sh in damap folder before running them
You must be in the examples folder to run the sh scripts

- simple.sh just loads simple.properties file with Bootstrap class and displays loaded configuration
  - try running command "./simple.sh -debug" to see more configuration info
- damac.sh loads any file or url passed on command line or starts a REPL if no arguments given
  - this version uses the Guice framework to resolve dependency injection
  - at the prompt, you can type "all properties" (without the quotes) to see available functions
- You can also look in resti/target folder for the resti.war file to deploy in your favorite servlet container

