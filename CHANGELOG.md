# Changelog

## Version 5.0 (MAJOR UPDATE - Mar 30th 2022)
 
*   Removed CloudSim Plus (Just by doing this, the time complexity has been reduced by more than 10 times. But, we are still relying on their efficient implementation of Events Queue).
*   Support of scenarios involving tens of thousands of devices. Compared to the previous version, the same simulation scenario (10 minutes simulation time, 10 000 devices, yes 10 thousands) took  1 minutes and 12 seconds on version 5.0 vs ... well the simulation didn't even start after more than 10 minutes, so I just canceled it. This is due to CloudSim Plus huge number of events, especially the broker and the vm allocation policy ones. A scenario of 10 minutes simulation time with 2000 devices, on the other hand, took 8 seconds on version 5.0 vs 2 minutes, 54 seconds on version 4.2. Even though the new version introduces many new features.
*   Support for scenarios with longer simulation time. A scenario of 24 hours with 200 edge devices (2110706 generated task), took 1 minutes and 31 seconds on version 5.0 vs "not possible" on version 4.2 due to out of memory exception.
*   Updated dependencies (...and removed some).
*   Improved code quality and JavaDoc.
*   More refactoring to improve extensibility even more.
*   Reduced simulation time further by adding more parallel operations.
*   Improved orchestrator task placement which had also an important impact on the simulation complexity. 
*   Added Wan Up link and Down link bandwidths.
*   Added a graph topology, now the user can link the data centers as he wants.
*   A topology will also mean that the user can incorporate a routing algorithm of his choice.
*   Now, energy consumption of WAN, LAN, and MAN networks is measured, in addition to that of edge devices and data centers.
*   Now, we can define the type of connectivity (Ethernet, 5G, 4G, WiFi) of each device (in terms of latency, bandwidth, and energy consumption rates).
*   Now, energy consumption of Ethernet, WiFi, Cellular (5G, 4G,..) networks is also measured.
*   The network model is more realistic than ever before.
*   Latency is now more realistic and dependent of the update interval (The latency-sensitivity can be defined in milliseconds, and the update interval has no effect on the failure rate of latency-sensitive tasks).
*   Similarly, energy measurement is now dependent of the update interval, no need to decrease the update interval (which increases simulation time) to get precise energy measurement.
*   This independence from the update interval means that it can be set to high, which decreased the simulation time further, without reducing its accuracy.
*   Improved reliability (More exceptions handling + proper guidance).
*   Implemented the Null Object Design Pattern in order to avoid NullPointerException.

## Version 4.2 (May 31st 2021)

*   Upgraded to CloudSim Plus 6.2.7
*   Updated dependencies 
*   Improved code quality 

## Version 4.1 (April 10th 2021)

*   Upgraded to CloudSim Plus 6.1.4
*   Fixed minor bugs (example 6, map chart)
*   Improved code quality
*   Improved examples

## Version 4.0 (march 1st 2021)

*   Updated task generator
*   Cleaning and refactoring
*   Fixed minor bugs
*   Improved example 6 (clustering and cooperative caching)

## Version 3.3 (jul 21st 2020)

*   Improved code quality

## Version 3.2 (jul 18th 2020)

*   Improved mobility model (new attributes added in edge_devices.xml file).
*   Fixed an energy model bug.
*   Fixed a live charts bug.
*   Fixed minor network model bug.

## Version 3.1 (may 7th 2020)

*   Fixed Bugs (CPU utilization when update interval is high >= 1).
*   Improved performance, up to 25% shorter simulation time.
*   New parameter (realitic_network_model) in order to decrease simulation time.
*   New example  "Example9" to show how to make decisions on Edge/ Fog Data Centers.

## Version 3.0 (may 5th 2020)

*   Added MinTimeBetweenEvents to MainApplication.java .
*   Fixed bugs (network model, charts, output).
*   Improved tasks generator.
*   New example "Example8" with a fuzzy logic based workload orchestration.

## Version 2.4 (apr 7th 2020)

*   Updated to the latest CloudSim Plus version (5.4.0).
*   Improved code quality.
*   Fixed minor issues.

## Version 2.4 (mar 3rd 2020)

*   Improved code quality and removed duplication.
*   Added support for custom settings and output folders.
*   Added new example "example 7" to show how to link custom configurations files and custom folders.
*   Improved examples.

## Version 2.3 (jan 24th 2020) 

*   Fixed the exception when initialization time is < 60 seconds
*   Fixed the task generator (tasks were generated during the initialization time, which shouldn't happen, the initialization time aims to ignore the time needed to generate the resources. generating tasks at that time will cause many of them to fail.
*   Fixed the file parser, now it checks if the sum of percentages of edge devices types is equal to 100% ( in the edge_devices.xml file).
*   Now you can add your custom network model via the setCustomNetworkModel() method.
*   File parser was not checking the edge devices xml file, now it is fixed.
*   Simulation manager used the update interval instead of the charts update interval, when updating charts, now it is fixed.
*   Now you can define which devices generate tasks/data via the edge_devices.xml file.
*   Fixed the registry related bugs
*   Update from CloudSim Plus 3.0.1 to 4.3.0
*   New example added "Example 6" to show how to use a custom network model, to do so a cooperative caching algorithm has been implemented in which devices keep replicas of the containers they downloaded in order to minimize the network usage. This required implementing a clustering algorithm by which edge devices will be grouped into clusters, and the cluster head orchestrates their tasks. So, a custom edge devices class was created which inherits the DefaultDataCenter class.

## Version 2.2.3 (nov 7th 2019) 

*   Now you can add applications easily without exiting the code.

## Version 2.2.2 (nov 7th 2019) 

*   Fixed some minor bugs  

## Version 2.2.0 (nov 2nd 2019)

*   The code has been improved  
*   Fixed some minor bugs 
*   Added some tutorials and exapmles

## Version 2.1.0 (oct 31th 2019)

*   The code has been improved  
*   Fixed some minor bugs 

## Version 2.0.0 (oct 24th 2019)

*   New mobility model and new parameters for mobility update: Now it uses speed in m/s instead of intervals. The new mobility model works on demand, instead of generating a list for each device containing all its location changes (from the beginning of the simulation). The egde devices will request the next location only when needed (which reduces the use of memory)
*   New and more realistic energy model and new energy parameters 
*   Added initialization time to simulation parameters (in order to ignore the time when the resources are being generated)
*   Some bugs here and there have been fixed 
*   Added ram as a property to EdgeDataCenter class
*   Added real time simulation map (now you can verify and check how your mobility model is working)  
*   Added some real time charts showing the CPU utilization of Cloud, Fog and Edge resources, the WAN utilization, and the tasks success rate
*   Adding the possibility to generate charts at the end of the simulation and to save them in a *.PNG format: More than 64 high resolution charts can be generated with one click, in order to make it easier for the user to check his simlation results. The user can always generate other charts using the generated CSV file.
*   Adding new simulation parameters regarding charts (displaying them, the refresh delay, saving them..)  

## Version 1.1.5 (apr 21st 2019)

*   Minor fixes

## Version 1.1.4 (mar 11th 2019)

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
*   New simulation parameters for the aforementioned changes. The ability to control the trade off between simulation accuracy and simulation time, etc.
*   Improved simulation time
*   Adding support for physical deployment of the orchestrator:  Deploying the orchestrator on the cloud for example, on fog servers...etc.
