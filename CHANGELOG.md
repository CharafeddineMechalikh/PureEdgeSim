# Changelog
## New version 2.3 (jan 24th 2020) 

*   Fixed the exception when initialization time is < 60 seconds

*   Fixed the task generator (tasks were generated during the initialization time, which shouldn't happen, the initialization time aims to ignore the time needed to generate the resources. generating tasks at that time will cause many of them to fail.

*   Fixed the file parser, now it checks if the sum of percentages of edge devices types is equal to 100% ( in the edge_devices.xml file).

*   Now you can add your custom network model via the setCustomNetworkModel() method

*   File parser was not checking the edge devices xml file, now it is fixed.

*   Simulation manager used the update interval instead of the charts update interval, when updating charts, now it is fixed.

*   Now you can define which devices generate tasks/data via the edge_devices.xml file.

*   Fixed the registry related bugs

*   New example added "Example 6" to show how to use a custom network model, to do so a cooperative caching algorithm has been implemented in which devices keep replicas of the containers they downloaded in order to minimize the network usage. This required implementing a clustering algorithm by which edge devices will be grouped into clusters, and the cluster head orchestrates their tasks. So, a custom edge devices class was created which inherits the DefaultDataCenter class.

## New version 2.2.3 (nov 7th 2019) 

*   Now you can add applications easily without exiting the code.

## New version 2.2.2 (nov 7th 2019) 

*   Fixed some minor bugs  

## New version 2.2.0 (nov 2nd 2019)

*   The code has been improved  

*   Fixed some minor bugs 

*   Added some tutorials and exapmles

## New version 2.1.0 (oct 31th 2019)

*   The code has been improved  

*   Fixed some minor bugs 

## New version 2.0.0 (oct 24th 2019)

*   The code has been revisited and cleaned, now it is more readable  

*   New mobility model and new parameters for mobility update: Now it uses speed in m/s instead of intervals. The new mobility model works on demand, instead of generating a list for each device containing all its location changes (from the beginning of the simulation). The egde devices will request the next location only when needed (which reduces the use of memory)
  
*   New and more realistic energy model and new energy parameters 

*   Added initialization time to simulation parameters (in order to ignore the time when the resources are being generated)

*   Some bugs here and there has been fixed 

*   Added ram as a propoerty to EdgeDataCenter class

*   Added real time simulation map (now you can verify and check how your mobility model is working)  

*   Added some real time charts 

   Showing the CPU utilization of Cloud, Fog and Edge resources, the WAN utilization, and the tasks success rate
  
*   Adding the possibility to generate charts at the end of the simulation and to save them in a *.PNG format: More than 64 high resolution charts can be generated with one click, in order to make it easier for the user to check his simlation results. The user can always generate other charts using the generated CSV file.
  
*   Adding new simulation parameters regarding charts (displaying them, the refresh delay, saving them..)  

## New version 1.1.5 (apr 21st 2019)

*   Minor fixes

## New version 1.1.4 (mar 11th 2019)

*   Improved parallel simulations

*   Improved network model

## Version 1.1.3 (mar 3rd 2019)

*   Fixed dependencies 

## Version 1.1.2 (mar 3rd 2019)

*   Improved parallel simulation 

## Version 1.1.1 (mar 2nd 2019) 

*   Minor fixes

## Version 1.1 (feb 26th 2019)

*   Added support for registry and containers

*   Improved the network model:  Added support for containers,bugs fixing, added fog servers coverage, added edge  wireless range...
  
*   Improved mobility model:  More realistic mobility model.  Added a map/ simulation area (height x width)
  
*   Adding support for physical depolyement of the orchestrator:  Deploying the orchestrator on the cloud for example, on fog servers...etc.
  
*   New simulation parameters:   New parameters for the aforementioned changes.  New simulation parameters in order to control the trade off between simulation accuracy and simulation time, etc. 
  
*   Improved simulation time

