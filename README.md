
Welcome to Damap: the DAta MAnager Project

This project regroups several sub-projects that have Maven defined dependencies.
The master project is Damai, a set of interfaces that act as a lingua franca for data management.
Damai means "data manager interfaces" but it also means "peace" in Indonesian.
The main objective is to have any expression in any scripting language that can target any data source (file system, database, network, ...)
The main strategy is to always program against the Damai interfaces and then use configuration files to load the implementations.
You can run the build-examples.sh script to build the project and then run scripts in the examples folder.

Damap is composed of several sub-projects:
- Damai: DAta MAnager Interfaces
  - core library that contains generic interfaces that an application needs to implement
  - contains a bootstrap main class that loads implementations from a properties file
- Damac: DAta MAnager Classes
  - contains some basic implementation classes of Damai interfaces
  - uses Guice as dependency injection manager 
  - the central piece is the GuiceConfiguration class which is used to implement the Configuration interface
- Resti: REST Interface
  - war package to run a REST API web application dependent on Damai and Damac
- Wai: Web Admin Interface
  - client side HTML and Javascript project
  - talks to Resti in order to read and write data
  - uses Web Components to build a UI admin to manage data
  - eases the process of building a webapp with visual building tools
- Nalai: NAtural LAnguage Interpreter
  - implements the ScriptEngine interface with a natural language syntax
  - provides computer programming with pseudo-natural language 
  - implements the ScriptEngine interface
  - translate text to RDF structures
  - can be extended with voice-recognizer and text-to-speech for vocal communication with computers
  - planned languages for now are french, english and spanish
- Maleco: MAchine LEarning COntroller
  - scripting interface to run and monitor machine learning jobs
  - data structures can be addressed with any scripting language implementing the ScriptEngine interface
  - can use either the command line or a web interface to control the learning processes
- Viundi: VIrtual UNiverse DIsplay
  - multi-user 3D interface for WebGL
  - uses WebSocket or HTTP/2 connections for real-time event comsumption
  - uses REST format to send and receive events
  - integrated physic engine with server side security to prevent game cheating
  - supports text, audio and video chat with avatars synchronized to user moves and faces

The order of the above sub-projects is important as it reflects several development aspects:
- it represents the order of creation of the sub-projects
  - only Damai, Damac and Resti sub-projects have been started as of September 2017
- it describes dependency hierarchy (latest is dependent on earliest)
- it displays an order of expected complexity (last one is the most complex)

This project is licensed under LGPL.


