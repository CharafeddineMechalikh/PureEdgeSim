# PureEdgeSim: A simulation toolkit for performance evaluation of fog and pure edge computing environments.

## Background
   Fog and edge computing, two emerging computing paradigms that aim to overcome the cloud computing limitations by bringing its applications at the edge of the network. Thus, reducing both the latency and the cloud workload and leading to a more scalable network. Nevertheless, in these distributed environments where many devices need to offload their tasks to one another (either to increase their lifetime or to minimize the task completion delay) many issues such as resources management strategies has to be solved. Instead of testing them on a real distributed system, the simulation makes it possible to evaluate the proposed strategies and algorithms in a repeatable, controllable and cost-effective way before their actual deployment. However, when it comes to simulation tools, edge and fog computing still did not get the attention they deserve (with only few simulators for fog computing such as iFogSim, edgeCloudSim. Meanwhile, no simulator for pure edge computing).

   We introduce PureEdgeSim (Based on CoudSim Plus), a new simulator that is designed to simulate cloud, fog, and edge computing environments. It allows to evaluate the performance of resources management strategies in terms of network usage, latency, resources utilization, energy consumption, etc. and enables the simulation of several scenarios such as the Internet of Things (IoT), connected vehicles/ VANETs/MANET, pure edge computing environments (peer-to peer networks such as mobile devices cloud), and mobile edge computing. 
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
## New version 1.1.1 (mar 2nd 2019)
## Changelog 
* Minor fixes
## New version 1.1 ( feb 26th 2019)
## Changelog  
* Added support for registry and containers

* Improved the network model: 

  Adding support for containers,bugs fixing, 
  
  Adding fog servers coverage, 
  
  Adding edge  wireless range.
  
  The ability to chose edge network type (fully peer to peer or sharing a same access point).
  
* Improved mobility model

  More realistic mobility model
  
  Added a map/ simulation area (height x width)
  
* Adding support for physical depolyement of the orchestrator 

  Deploying the orchestrator on the cloud for example, on fog servers...etc
  
* New simulation parameters

  New parameters for the aforementioned changes
  
  New simulation parameters in order to control the trade off between simulation accuracy and simulation time, etc. 
  
* Improved simulation time

 

## Authors : Charafeddine MECHALIKH, Hajer TAKTAK, Faouzi MOUSSA

# Please cite it as 
Mechalikh Charafeddine, Taktak Hajer, & Moussa Faouzi. (2019, March 2). PureEdgeSim: A simulation toolkit for performance evaluation of fog and pure edge computing environments. (Version 1.1.1). Zenodo. http://doi.org/10.5281/zenodo.2581967
