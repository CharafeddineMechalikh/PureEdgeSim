# PureEdgeSim: A simulation toolkit for performance evaluation of fog and pure edge computing environments.

## Background
   Fog and edge computing, two emerging computing paradigms that aim to overcome the cloud computing limitations by bringing its applications at the edge of the network. Thus, reducing both the latency and the cloud workload and leading to a more scalable network. Nevertheless, in these distributed environments where many devices need to offload their tasks to one another (either to increase their lifetime or to minimize the task completion delay) many issues such as resources management strategies has to be solved. Instead of testing them on a real distributed system, the simulation makes it possible to evaluate the proposed strategies and algorithms in a repeatable, controllable and cost-effective way before their actual deployment. However, when it comes to simulation tools, edge and fog computing still did not get the attention they deserve (with only few simulators for fog computing such as iFogSim, edgeCloudSim. Meanwhile, no simulator for pure edge computing).

   We introduce PureEdgeSim, a new simulator based on [CloudSim Plus](http://cloudsimplus.org) that is designed to simulate cloud, fog, and edge computing environments. It allows to evaluate the performance of resources management strategies in terms of network usage, latency, resources utilization, energy consumption, etc. and enables the simulation of several scenarios such as the Internet of Things (IoT), connected vehicles/ VANETs/MANET, pure edge computing environments (peer-to peer networks such as mobile devices cloud), and mobile edge computing. 
## Why it is named "Pure"EdgeSim  (Pure Edge Computing Simulator)
   Although fog and edge computing are usually referred to as the same paradigm, the main difference between them is their locations. In the pure edge computing (which this simulator is named after), edge nodes are deployed in the edge devices themselves following peer-to-peer architecture [1]. Therefore, it provides even lower latency than fog computing, while in fog computing, the fog nodes are deployed on servers, mini-clouds, etc. following a client-server architecture. That is why the PureEdgeSim is named so.
   
   *[1] D’Angelo, M., & Caporuscio, M. (2016, July). Pure Edge Computing Platform for the Future Internet. In Federation of International Conferences on Software Technologies: Applications and Foundations (pp. 458-469). Springer, Cham.* 
## Overview
   PureEdgeSim enables the simulation of resource management strategies and allows to evaluate the performance of cloud, fog, and pure edge computing environments. It grantees high scalability by enabling the simulation of thousands of devices. Besides, it supports the edge devices heterogeneity (i.e. whether this device is mobile or not, whether battery-powered or not, different  applications requirements: tasks file size, tasks CPU utilization,and latency requirement, etc.) 
   It provides a task orchestrator module that orchestrates the tasks and enables the multi-tiers simulations scenarios where many computing paradigms can be used in conjunction with one another. Besides, it provides an even more realistic network model (as compared to state of the art simulators) that continuously changes the allocated bandwidth for each task being transferred depending on the network traffic. 
   It consists of the following 7 modules:
   * Scenario Manager, that loads  the simulation parameters and the user scenario from the input files (.xml and .prop files in settings/ folder) where the user specifies. It consists of two classes, the File Parser that  checks the input files and loads the  the simulation parameters, and the Simulation Parameters class which represents a placeholder for the different parameters.
   * Simulation Manager, that initiates the simulation environment, schedules all the events and generates the output. It consists of two important classes, the Simulation Manager class which manages the simulation, schedules the tasks generation, etc. The Simulation Logger class that generates the simulation output saves it in comma-separated value (CSV) format in order to easily exploit them later using any spreadsheet editor (e.g. Microsoft Excel...).
   * Data Centers Manager: it generates and manages all the data centers and devices (i.e. cloud, fog or edge). It consists of two classes: the Edge Data Center class, that contains the specific properties of edge devices such as the location, the mobility, the energy source, and the capacity/ remaining energy if it is battery-powered. The second class is the Server Manager which generates the needed servers and edge devices, their hosts and their virtual machines.
   * Tasks Generator which is behind the tasks generation, -currently- it assigns an application such as e-health, smart-home, and augmented-reality (that can be defined in settings/applications.xml file) to each edge device. Then, it will generates the needed tasks according to the assigned type, which guarantees the heterogeneity of applications.  
   * The Network Module: that consists mainly of the Network Model class.which is behind the transfer of tasks/containers/ request... 
   * The Tasks Orchestrator, which is the decision maker, where the user can define the orchestration algorithm. 
   * The Location Manager, which generates the mobility path of mobile devices.
   
   
More detailed description, tutorials, and use case  will be added soon....
## Changelog
## New version 2.0.0 (oct 24th 2019)
* The code has been revisited and cleaned, now it is more readable  
* New mobility model and new parameters for mobility update 
  Now it uses speed in m/s instead of intervals
  The new mobility model works on demand, instead of generating a list for each device containing all its location changes (from the beginning of the simulation). 
  The egde devices will request the next location only when needed (which reduces the use of memory)
* New and more realistic energy model
* Added initialization time to simulation parameters (in order to ignore the time when the resources are being generated)
* Some bugs here and there has been fixed 
* Added ram as a propoerty to EdgeDataCenter class
* Added real time simulation map (now you can verify and check how your mobility model is working)  
* Added some real time charts 
  Showing the CPU utilization of Cloud, Fog and Edge resources, the WAN utilization, and the tasks success rate
* Adding the possibility to generate charts at the end of the simulation and to save them in a *.PNG format 
  More than 64 high resolution charts can be generated with one click, in order to make it easier for the user to check his simlation results
  The user can always generate other charts using the generated CSV file.
* Adding new simulation parameters regarding charts (displaying them, the refresh delay, saving them..)
* The ability to enable or desable orchestrators
  If disabled, the device will orchestrate its tasks by itself. 
  If enabled, the user can select any devices/datacenters to be the orchestrators, 
  Then, the tasks will be sent to the nearest orchestrator in order to find the best offloading destination
* Now the containers network usage can be found in the CSV file.
  A quick chart is also generated by the simulator to show the network used by containers if the registry is enabled.  
     

## What can be simulated with PureEdgeSim?
* Cloud, Fog, and pure Edge computing scenarios
* And basicly, any scenario that involves computing on distributed nodes, for example:
  VANETs/MANET networks, IoT applications, Mobile Devices Clouds, Mobile Edge Computing,...
  
## PureEdgeSim features?
* Realistic network and energy model as compared to other simulators
* Mobility support which is ignored by most simulators 
  a ready to use mobility model (mode models will be added)
  the user can easily add new models based on his needs.
  the user can specify the dimentions of the simulated area and the speed of mobile devices. 
  The mobility model will assign a random location to each device.
  Then the mobile devices will change their location according to the model in use.
* The support for devices heterogeneity
  The user can define heterogenous edge device, Fog servers, and Cloud Data Centers in the corresponding XML files. 
  he will decide wether and edge device is mobile or not, wether it is battery-powered or not ( and the size of its battery), 
  and how much computing capacity it has.
  The devices without computing capacity are considered as simple sensors that only generate data/tasks 
  The user can also define the applications that are in use, their cpu utilization, their files sizes and their latency requirements 
* The scalability, generate handreds of devices, with a single click. 
* A rich collection of metrics
  The simulation output  (the csv file) includes + 40 metrics ready to be plotted.
  Also, new metrics can be derived from those.
* Ease of use
  more than 60 charts can be generated automatically
  other charts can be easily generated from the csv file using any spreadsheet software (e.g. Microsoft Excel)
  readable code and an architecture that is easy to understand
* Wide applicability and extensibility
  The support for many simulation scenarios : IoT, VANETs/MANET clouds, Fog computing environments..
  The user can evaluate the orchestration algorithms, the architectures,...
  The upport for many devices and applications types...
  Various simulation parameters that meet the requirement of any scenario...
  the user can implement new orchestration algorithms(machine learning algorithms: fuzzy decision trees for example)
  he can also implement new network, energy, mobility, or tasks generation models 
  he can groupe edge devices into clusters, deploy the orchestrator node in the cluster head for example,
  and form a sort of edge devices cloud.
  he can also solve the registry scalability issue by mirroring the containers images close to the edge,
  and so on...   
  Basically any scenario that involves data centers, servers, or geo-distributed devices.
* Full control of the simulation environment
  The user can trade-off between simulation duration and its accuracy 
  To decrease the simultion time, the user can also enable parralelism
   
## Tutorials are coming soon..    
  
## Authors : Charafeddine MECHALIKH, Hajer TAKTAK, Faouzi MOUSSA

# Please cite it as 
Mechalikh Charafeddine, Taktak Hajer, & Moussa Faouzi. (2019, April 21). PureEdgeSim: A simulation framework for performance evaluation of cloud, fog, and pure edge computing environments. (Version 1.1.5). Zenodo. http://doi.org/10.5281/zenodo.2648161
