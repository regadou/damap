
Welcome to Damap: the DAta MAnager Project

Damap is composed of several sub-projects:
- Damai: DAta MAnager Interface
  - core library that contains generic interfaces an application needs to implement
- Damac: DAta MAnager Classes
  - contains some basic implementation classes of Damai interfaces
  - uses Guice as dependency injection manager 
  - the central piece is the Context class which is used to access everything else
- Resti: REST Interface
  - war package to build a REST servlet application dependent on Damac and Damai
- Wai: Web Admin Interface
  - client side HTML and Javascript project
  - talks to Resti in order to read and write data
  - uses Web Components to build a UI admin to manage data
  - eases the process of building a webapp with visual building tools
- Nalai: NAtural LAnguage Interface
  - provides natural language programming
  - implements the ScriptEngine interface
  - planned languages are french, english and spanish
- Malei: MAchine LEarning Interface
  - scripting interface to run and monitor machine learning jobs
  - data structures can be addressed with any scripting language supported by Damac
  - can use either command line or web interface to control the learning processes
- Viuni: VIrtual UNiverse Interface
  - multi-user 3D interface for WebGL
  - uses HTTP/2 connections for real-time event comsumption
  - uses REST format to send and receive events
  - integrated physic engine with server side security to prevent game cheating
  - supports text, audio and video chat with avatars synchronized to user moves and faces

The order of the above sub-projects is important as it reflects several development aspects:
- it represents the order of creation of the sub-projects
  - only Damai and Damac have been started as of July 2017
- it describes dependency hierarchy (latest is dependent on earliest)
- it displays an order of expected complexity (last one is the most complex)

This project is licensed under LGPL.



