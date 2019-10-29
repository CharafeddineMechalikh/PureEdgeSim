# PureEdgeSim: A simulation toolkit for performance evaluation of fog and pure edge computing environments

[![DOI](https://zenodo.org/badge/163447483.svg)](https://zenodo.org/badge/latestdoi/163447483)  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)  [![Build Status](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim.svg?branch=master)](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim)  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/25ee278611014a9bb242297480703cf9)](https://www.codacy.com/manual/CharafeddineMechalikh/PureEdgeSim?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=CharafeddineMechalikh/PureEdgeSim&amp;utm_campaign=Badge_Grade)

## 1. Background

Fog and edge computing, two emerging computing paradigms that aim to overcome the cloud computing limitations by bringing its applications at the edge of the network. Thus, reducing both the latency and the cloud workload and leading to a more scalable network. Nevertheless, in these distributed environments where many devices need to offload their tasks to one another (either to increase their lifetime or to minimize the task completion delay) many issues such as resources management strategies has to be solved. Instead of testing them on a real distributed system, the simulation makes it possible to evaluate the proposed strategies and algorithms in a repeatable, controllable and cost-effective way before their actual deployment. However, when it comes to simulation tools, edge and fog computing still did not get the attention they deserve (with only few simulators for fog computing such as iFogSim, edgeCloudSim. Meanwhile, no simulator for pure edge computing).

We introduce PureEdgeSim, a new simulator based on [CloudSim Plus](http://cloudsimplus.org) that is designed to simulate cloud, fog, and edge computing environments. It allows to evaluate the performance of resources management strategies in terms of network usage, latency, resources utilization, energy consumption, etc. and enables the simulation of several scenarios such as the Internet of Things (IoT), connected vehicles/ VANETs/MANET, pure edge computing environments (peer-to peer networks such as mobile devices cloud), and mobile edge computing. 

## Why it is named "Pure"EdgeSim  (Pure Edge Computing Simulator)

Although fog and edge computing are usually referred to as the same paradigm, the main difference between them is their locations. In the pure edge computing (which this simulator is named after), edge nodes are deployed in the edge devices themselves following peer-to-peer architecture [1]. Therefore, it provides even lower latency than fog computing, while in fog computing, the fog nodes are deployed on servers, mini-clouds, etc. following a client-server architecture (D’Angelo, M. 2016). That is why the PureEdgeSim is named so.
   
## 2. PureEdgeSim Architecture

PureEdgeSim enables the simulation of resource management strategies and allows to evaluate the performance of cloud, fog, and pure edge computing environments. It grantees high scalability by enabling the simulation of thousands of devices. Besides, it supports the edge devices heterogeneity (i.e. whether this device is mobile or not, whether battery-powered or not, different  applications requirements: tasks file size, tasks CPU utilization,and latency requirement, etc.) 

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/pes/PureEdgeSim/Files/scenario.JPG)

A simple representation of the simulation scenarios

It provides a task orchestrator module that orchestrates the tasks and enables the multi-tiers simulations scenarios where many computing paradigms can be used in conjunction with one another. Besides, it provides an even more realistic network model (as compared to state of the art simulators) that continuously changes the allocated bandwidth for each task being transferred depending on the network traffic. 

It consists of the following 7 modules:

*   Scenario Manager, that loads  the simulation parameters and the user scenario from the input files (`.xml` and `.prop` files in `/settings/` folder) where the user specifies. It consists of two classes, the File Parser that  checks the input files and loads the  the simulation parameters, and the Simulation Parameters class which represents a placeholder for the different parameters.


*   Simulation Manager, that initiates the simulation environment, schedules all the events and generates the output. It consists of two important classes, the Simulation Manager class which manages the simulation, schedules the tasks generation, etc. The Simulation Logger class that generates the simulation output saves it in comma-separated value (CSV) format in order to easily exploit them later using any spreadsheet editor (e.g. Microsoft Excel...).


*   Data Centers Manager: it generates and manages all the data centers and devices (i.e. cloud, fog or edge). It consists of two classes: the Edge Data Center class, that contains the specific properties of edge devices such as the location, the mobility, the energy source, and the capacity/ remaining energy if it is battery-powered. The second class is the Server Manager which generates the needed servers and edge devices, their hosts and their virtual machines.


*   Tasks Generator which is behind the tasks generation, -currently- it assigns an application such as e-health, smart-home, and augmented-reality (that can be defined in `settings/applications.xml` file) to each edge device. Then, it will generates the needed tasks according to the assigned type, which guarantees the heterogeneity of applications.  


*   The Network Module: that consists mainly of the Network Model class.which is behind the transfer of tasks/containers/ request... 


*   The Tasks Orchestrator, which is the decision maker, where the user can define the orchestration algorithm. 


*   The Location Manager, which generates the mobility path of mobile devices.
   
![Architecture](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/pes/PureEdgeSim/Files/modules.PNG)

PureEdgeSim architecture

## 3. What can be simulated with PureEdgeSim

*   Cloud, Fog, and pure Edge computing scenarios


*   And basicly, any scenario that involves computing on distributed nodes or mobility, for example:

VANETs/MANET networks, IoT applications, Mobile Devices Clouds, Mobile Edge Computing,... 
  

## 4. PureEdgeSim features

*   Realistic network and energy models as compared to other simulators.


*   Mobility support which is ignored by most simulators: 

A ready to use mobility model (mode models will be added).

The user can easily add new models based on his needs.

The user can specify the dimentions of the simulated area and the speed of mobile devices. 

The mobility model will assign a random location to each device.

Then the mobile devices will change their location according to the model in use.


*   The support for devices heterogeneity:

The user can define heterogenous edge device, Fog servers, and Cloud Data Centers in the corresponding `.xml` files. 

He will decide wether and edge device is mobile or not, wether it is battery-powered or not ( and the size of its battery), 

and how much computing capacity it has.

The devices without computing capacity are considered as simple sensors that only generate data/tasks. 

The user can also define the applications that are in use, their cpu utilization, their files sizes and their latency. requirements. 


*   The scalability, generate handreds of devices, with a single click. 


*   A rich collection of metrics:

The simulation output  (the `.csv` file) includes + 40 metrics ready to be plotted.

Also, new metrics can be derived from those.


*   Ease of use:

More than 60 charts can be generated automatically.

Other charts can be easily generated from the csv file using any spreadsheet software (e.g. Microsoft Excel).

Readable code and an architecture that is easy to understand.


*   Wide applicability and extensibility:

The support for many simulation scenarios : IoT, VANETs/MANET clouds, Fog computing environments..

The user can evaluate the orchestration algorithms, the architectures,...

The upport for many devices and applications types...

Various simulation parameters that meet the requirement of any scenario...

The user can implement new orchestration algorithms(machine learning algorithms: fuzzy decision trees for example)

He can also implement new network, energy, mobility, or tasks generation models. 

He can groupe edge devices into clusters, deploy the orchestrator node in the cluster head for example,

and form a sort of edge devices cloud.

He can also solve the registry scalability issue by mirroring the containers images close to the edge,

and so on...   

Basically any scenario that involves data centers, servers, or geo-distributed devices.


*   Full control of the simulation environment:

The user can trade-off between simulation duration and its accuracy. 

To decrease the simultion time, the user can also enable parralelism.

## 5. Getting started with PureEdgeSim

## 5.1 Running PureEdgeSim for the First Time

 (i)  Download the zip file from github.
 
 (ii) Import the project to your IDE.
 
 (iii) Launch the `main.java` class.
 
 *Running the simulation from command line will be added soon..*
    
![importation](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/pes/PureEdgeSim/Files/importing%20project.gif)

Importing PureEdgeSim and launching the simulation
  
## 5.2 The Simulation Settings

PureEdgeSim provides 5 input files located under the `/settings/` folder (you can check them [here](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/pes/PureEdgeSim/settings)):
  

  *   The `simulation parameters` file : It groups all the simulation parameters, including the simulation environment settings  (simulation time, initialization time, update intervals, ..), the models settings (the parameters used by the mobility, energy, network, and tasks generation models..), and so on.    


*   The `applications XML` file: This file decribes the types of applications that will be used by the tasks generator.

Each of these application has different characterestics (CPU utilization, files size, latency requirements..). When the     simulation starts, the tasks generator will associate one of these applications to each edge device. Then, based on the assigned application type, it will generate the tasks of these device, which enables the support for devices heterogeneity ( the heterogeneity of their applications, in this case).


*   The `edge devices XML` file:  PureEdgeSim also enables the user to generate hundreds or even thousands of heterogenous devices. To do this, the user will specify the types of edge devices that will be generated and their proportions/ percentages in the `edge_device.xml` file. The server manager will then generate the desired number of edge devices based on the proposed types. For instance, if the user define two types of devices and sets the percentage of each type to 50% and sets the number of edge devices (in the simulation parameters file) to 1000 devices, the server manager will generate 500 devices of type 1, and 500 devices of type 2. Each of those types can have different computing capacity, energy consumption,  and other settings as well that enable the heterogeneity of devices ( whether the device is mobile or not, battery-powered or not, and how much is its battery capacity...).


*   The `Fog datacenters XML` file : This file describes the Fog datacenters that will be generated during the simulation. However, unlike the edge devices file, this file contains the fog datacenters that need to be generated instead of their types. Therefore, if the user wants to generate 4 different servers, he must include each one of them in the file. Each datacenter is characterised by its compuitng capacity, its energy consumption, its location, and its hosts. Each host has a set of Virtual machines with a specified computing capacity. These virtual machines are responsible for executing the offloading tasks.


*   The `Cloud datacetners xml` file:  This file describes the cloud datacenters that will be generated during the simulation (similar to that of fog servers). 

## 5.3 The Simulation Parameters File

The parameters file contains the following set of parameters:

*   The simulation environment parameters:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`simulation_time`                     | Integer    | >= 1          | The simulation duration (in minutes)
`initialization_time`                 | Integer    | >= 0          | The time needed to generate all resources, which means that the tasks offloading process starts affter it
`parallel_simulation`                 | Boolean    | true or false | Enable or disable parallel simulations
`update_interval`                     | Double     | >= 0.01       | The interval between simulation environment events (in seconds)
`pause_length`                        | Integer    | >= 0          | The pause between iterations (in real seconds)
`display_real_time_charts`            | Boolean    | true or false | To display or not the simulation results in real time 
`auto_close_real_time_charts`         | Boolean    | true or false | Auto close real time charts after the end of iteration
`charts_update_interval`              | Double     | >= 0.01       | Interval of refreshing real time charts (in seconds)
`save_charts`                         | Boolean    | true or false | Whether to save charts in `.png` format or not
`wait_for_all_tasks`                  | Boolean    | true or false | Wait until all tasks get executed or stop the simulation on time (when the simulation time set by the user finishes)
`save_log_file`                       | Boolean    | true or false | Whether to save the log file or not
`clear_output_folder`                 | Boolean    | true or false | Delete the output folder at the beginning of each simulation
`deep_log_enabled`                    | Boolean    | true or false | Enableof disable deep logging





*   The Location Manager (mobility model) parameters:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`length`                              | Integer    | >= 1          | The simulation area length (in meters)
`width`                               | Integer    | >= 1          | The simulation area width (in meters)  
`edge_range`                          | Integer    | >= 1          | The range of edge devices (in meters)
`fog_coverage`                        | Integer    | >= 1          | The raius of the area covered by each fog server (in meters)
`speed`                               | Double     | >= 0          | The speed of mobile devices in meters/seconds) (0= disabled)




*   The Server Manager settings:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`min_number_of_edge_devices`          | Integer    | >= 1          | The number of edge devices at the beginning of the simulation
`max_number_of_edge_devices`          | Integer    | >= 1          | The number of edge devices at the end of the simulation
`edge_device_counter_size`            | Integer    | >= 1          | The growing rate in the number of devices in each iteration




*   The Network Model settings:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`wlan_bandwidth`                      | Integer    | >= 1          | The local area network bandwidth (in Mbps) 
`wan_bandwidth`                       | Integer    | >= 1          | The backhaul network bandwidth (in Mbps) 
`wan_propogation_delay`               | Double     | >= 0          | The propagation delay (when sending data/tasks to the cloud) (in seconds)  
`network_update_interval`             | Double     | >= 0.01       | The network model refresh interval (in seconds)




*   The Tasks Orchestration settings:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`enable_registry`                     | Boolean    | true or false | Enabling it means that before executing a task, a container will be pulled from the registry/cloud 
`enable_orchestrators`                | Boolean    | true or false | Enabling this means that the task/offloading request will be sent to the specifed/ nearest orchestrator. Disabling this means that each device will orchestrate its owb tasks.  
`deploy_orchestrator`                 | Boolean    | Cloud or Fog  | To deploy the orchestrator on the cloud, fog, or any custom location (e.g. cluster heads, the user in this case need to implement his clustering algorithm)
`applications_CPU_allocation_policy`  | Boolean    | SPACE_SHARED or TIME_SHARED | Time shared means that the tasks can be executed in same virtual machine at simae time (however this increases the simulation duration). Space shared means that the tasks are executed one after the otherby a virtual machine
`tasks_generation_rate`               | Integer    | >= 1          | The number of tasks generated by each device every minute
`orchestration_architectures`         | Boolean    | CLOUD_ONLY, FOG_AND_CLOUD,... | The computing paradigms that are used 
`orchestration_algorithms`            | Boolean    | (any algorithm name) | The algorithm used by the orchestrator to find the offloading destination




*   The Energy Model parameters:

Parameter                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`consumed_energy_per_bit`             | Double     | >= 0          | The enregy consumed when transferring 1 bit (in wh)
`amplifier_dissipation_free_space`    | Double     | >= 0          | The energy consumed by the amplifier in free space channel  (in wh)
`amplifier_dissipation_multipath`     | Double     | >= 0          | The energy consumed by the amplifier in multipath channel  (in wh)




## 5.4 The Edge Devices, Fog Servers, and Cloud Datacenters Files

These files contain the specification of edge devices, fog datacenters, and cloud datacenters.

*   Datacenters characteristics

Attribute                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`idleConsumption`                     | Double     | >= 0          | The energy consumption rate when the datacenter is idle  (in wh/s)
`maxConsumption`                      | Double     | >= 0          | The energy consumption rate when the datacenter CPU operates at 100%  (in wh/s)
`isOrchestrator`                      | Boolean    | true or false | To manually select this datacenter as orchestrator
`location`                            | -          | -             | The X and Y coordinates that define the location of this datacenter
`hosts`                               | -          | -             | The list of hosts  

*   The hosts have the following characteristics

Attribute                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`core`                                | Integer    | > 0           | The number of CPU cores 
`mips`                                | Integer    | > 0           | The processing power  (in MIPS)
`ram`                                 | Integer    | > 0           | RAM (in  MB)
`storage`                             | Integer    | > 0           | Storage capacity (in MB)
`VMs`                                 | -          | -             | The list of virtual machines  

*   Each virtual machine have the following characteristics

Attribute                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`core`                                | Integer    | > 0           | The number of CPU cores used by this VM 
`mips`                                | Integer    | > 0           | The allocated processing power  (in MIPS)
`ram`                                 | Integer    | > 0           | The allocated RAM (in  MB)
`storage`                             | Integer    | > 0           | The allocated storage (in MB) 


 *The sum of virtual machines attributes values (e.g. CPU cores) must be inferior than those of the host*

*   The edge devices file follows the same structure as the Fog and Cloud `.xml` files. However as we said previously, if we want to generate 100 devices for example, we will not define all these devices in this file, instead, we will define the types of devices that will be generated, for example 25% of the generated devices will be of type 1, etc. The edge devices are considered as datacenters that contains one host with one VM (the user can add more if needed, by editing this file). The device without a virtual machine is considered a simple sensor (no computing capabilities). The following table highlights the attributes that only edge devices have : 

Attribute                             | Type       | Options/Range | Description                                             
--------------------------------------|------------|---------------|---------------------------------------------------------
`mobility`                             | Boolean    | true or false | "True" means the devices of this type are mobile 
`battery`                              | Boolean    | true or false | "True" means that the devices of this type are battery-powered
`batterycapacity`                      | Double     | > 0           | The battery capacity (in Wh)
`percentage`                           | Integer    | > 0           | The percentage of devices of this type.
 

## 5.5 The PureEdgeSim output files

PureEdgeSim output files can be found under the `/output/` directory (you can check the output files [here](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/pes/PureEdgeSim/output/2019-10-28_10-04-01)). There are two types of text files resulted from the simulation: a `.txt` file and a `.csv` file. The `.txt` file contains a brief and easy to read ouput, while the `.csv` file contains more detailed simulations results that are ready to plot. The `.csv` file can be opened using any spreadsheet software (e.g. Microsoft Excel) by which the user can generate unlimited types of charts (with more than 40 metric available).

To ease prototyping and testing, pureEdgeSim can automatically generate more than 60 charts. It can also generate real time charts and display the simulation map. These charts and the map are then saved under the `/output/` folder in a `.png` image format. 

Example of real time charts :

![Real time charts](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/pes/PureEdgeSim/Files/real%20time.gif)

Real time analysis of simulation environment


## 6. Change log of the latest version

## New version 2.0.0 (oct 24th 2019)

*   The code has been revisited and cleaned, now it is more readable  

*   New mobility model and new parameters for mobility update 

Now it uses speed in m/s instead of intervals

The new mobility model works on demand, instead of generating a list for each device containing all its location changes (from the beginning of the simulation). 

The egde devices will request the next location only when needed (which reduces the use of memory)

*   New and more realistic energy model

*   Added initialization time to simulation parameters (in order to ignore the time when the resources are being generated)

*   Some bugs here and there has been fixed 

*   Added ram as a propoerty to EdgeDataCenter class

*   Added real time simulation map (now you can verify and check how your mobility model is working)  

*   Added some real time charts 

Showing the CPU utilization of Cloud, Fog and Edge resources, the WAN utilization, and the tasks success rate

*   Adding the possibility to generate charts at the end of the simulation and to save them in a *.PNG format 

More than 64 high resolution charts can be generated with one click, in order to make it easier for the user to check his simlation results

The user can always generate other charts using the generated `.csv` file.

*   Adding new simulation parameters regarding charts (displaying them, the refresh delay, saving them..)

*   The ability to enable or desable orchestrators

If disabled, the device will orchestrate its tasks by itself. 

If enabled, the user can select any devices/datacenters to be the orchestrators, 

Then, the tasks will be sent to the nearest orchestrator in order to find the best offloading destination

*   Now the containers network usage can be found in the `.csv` file.

A quick chart is also generated by the simulator to show the network used by containers if the registry is enabled.  

    
## Authors : Charafeddine MECHALIKH, Hajer TAKTAK, Faouzi MOUSSA




## Please cite it as 

Mechalikh Charafeddine, Taktak Hajer, & Moussa Faouzi. (2019, April 21). PureEdgeSim: A simulation framework for performance evaluation of Cloud, Fog, and pure Edge Computing Environments.  Zenodo. http://doi.org/10.5281/zenodo.3520915

*For any questions, contact me at charafeddine.mechalikh@gmail.com*   
 
