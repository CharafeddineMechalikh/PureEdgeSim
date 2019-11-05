# PureEdgeSim: A simulation toolkit for performance evaluation of Fog and pure Edge computing environments

[![DOI](https://zenodo.org/badge/163447483.svg)](https://zenodo.org/badge/latestdoi/163447483)  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)  [![Build Status](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim.svg?branch=master)](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim)  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/25ee278611014a9bb242297480703cf9)](https://www.codacy.com/manual/CharafeddineMechalikh/PureEdgeSim?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=CharafeddineMechalikh/PureEdgeSim&amp;utm_campaign=Badge_Grade)   [![Maintainability](https://api.codeclimate.com/v1/badges/a1ffecb5230fc5771b93/maintainability)](https://codeclimate.com/github/CharafeddineMechalikh/PureEdgeSim/maintainability)   [![codebeat badge](https://codebeat.co/badges/bbe172a2-1169-4bbe-b6a6-0505631babc6)](https://codebeat.co/projects/github-com-charafeddinemechalikh-pureedgesim-master)
## 1. Background

Fog and Edge (Mist) computing, two emerging computing paradigms that aim to overcome the Cloud computing limitations by bringing its applications at the Edge of the network. Thus, reducing both the latency and the Cloud workload and leading to a more scalable network. Nevertheless, in these distributed environments where many devices need to offload their tasks to one another (either to increase their lifetime or to minimize the task completion delay) many issues such as resources management strategies has to be solved. Instead of testing them on a real distributed system, the simulation makes it possible to evaluate the proposed strategies and algorithms in a repeatable, controllable and cost-effective way before their actual deployment. However, when it comes to simulation tools, Edge and Fog computing still did not get the attention they deserve (with only few simulators for Fog computing such as iFogSim, EdgeCloudSim. Meanwhile, no simulator for pure Edge computing).

We introduce PureEdgeSim, a new simulator based on [CloudSim Plus](http://Cloudsimplus.org) that is designed to simulate Cloud, Fog, and Edge computing environments. It allows to evaluate the performance of resources management strategies in terms of network usage, latency, resources utilization, energy consumption, etc. and enables the simulation of several scenarios such as the Internet of Things (IoT), connected vehicles/ VANETs/MANET, pure Edge computing environments (peer-to peer networks such as mobile devices Cloud), and mobile Edge computing. 

## Why it is named "Pure"EdgeSim  (Pure Edge Computing Simulator)

Although Fog and Edge computing are usually referred to as the same paradigm, the main difference between them is their locations. In the pure Edge computing (which this simulator is named after), Edge nodes are deployed in the Edge devices themselves following peer-to-peer architecture. Therefore, it provides even lower latency than Fog computing, while in Fog computing, the Fog nodes are deployed on servers, mini-Clouds, etc. following a client-server architecture (D’Angelo, M. 2016). That is why the PureEdgeSim is named so.
   
## 2. PureEdgeSim Architecture

PureEdgeSim enables the simulation of resource management strategies and allows to evaluate the performance of Cloud, Fog, and pure Edge computing environments. It grantees high scalability by enabling the simulation of thousands of devices. Besides, it supports the Edge devices heterogeneity (i.e. whether this device is mobile or not, whether battery-powered or not, different  applications requirements: tasks file size, tasks CPU utilization,and latency requirement, etc.) 

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/scenario.JPG)

A simple representation of the simulation scenarios

It provides a task orchestrator module that orchestrates the tasks and enables the multi-tiers simulations scenarios where many computing paradigms can be used in conjunction with one another. Besides, it provides an even more realistic network model (as compared to state of the art simulators) that continuously changes the allocated bandwidth for each task being transferred depending on the network traffic. 

It consists of the following 7 modules:

*   Scenario Manager, that loads  the simulation parameters and the user scenario from the input files (`.xml` and `.prop` files in `/settings/` folder) where the user specifies. It consists of two classes, the File Parser that  checks the input files and loads the  the simulation parameters, and the Simulation Parameters class which represents a placeholder for the different parameters.


*   Simulation Manager, that initiates the simulation environment, schedules all the events and generates the output. It consists of two important classes, the Simulation Manager class which manages the simulation, schedules the tasks generation, etc. The Simulation Logger class that generates the simulation output saves it in comma-separated value (CSV) format in order to easily exploit them later using any spreadsheet editor (e.g. Microsoft Excel...).


*   Data Centers Manager: it generates and manages all the data centers and devices (i.e. Cloud, Fog or Edge). It consists of two classes: the Edge Data Center class, that contains the specific properties of Edge devices such as the location, the mobility, the energy source, and the capacity/ remaining energy if it is battery-powered. The second class is the Server Manager which generates the needed servers and Edge devices, their hosts and their virtual machines.


*   Tasks Generator which is behind the tasks generation, -currently- it assigns an application such as e-health, smart-home, and augmented-reality (that can be defined in `settings/applications.xml` file) to each Edge device. Then, it will generates the needed tasks according to the assigned type, which guarantees the heterogeneity of applications.  


*   The Network Module: that consists mainly of the Network Model class.which is behind the transfer of tasks/containers/ request... 


*   The Tasks Orchestrator, which is the decision maker, where the user can define the orchestration algorithm. 


*   The Location Manager, which generates the mobility path of mobile devices.
   
![Architecture](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/modules.PNG)

PureEdgeSim architecture

## 3. What can be simulated with PureEdgeSim

*   Cloud, Fog, and pure Edge computing scenarios


*   And basicaly, any scenario that involves computing on distributed nodes or mobility, for example:

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

The user can define heterogenous Edge device, Fog servers, and Cloud Data Centers in the corresponding `.xml` files. 

He will decide wether and Edge device is mobile or not, wether it is battery-powered or not ( and the size of its battery), 

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

The support for many simulation scenarios : IoT, VANETs/MANET Clouds, Fog computing environments..

The user can evaluate the orchestration algorithms, the architectures,...

The upport for many devices and applications types...

Various simulation parameters that meet the requirement of any scenario...

The user can implement new orchestration algorithms(machine learning algorithms: fuzzy decision trees for example)

He can also implement new network, energy, mobility, or tasks generation models. 

He can groupe Edge devices into clusters, deploy the orchestrator node in the cluster head for example,

and form a sort of Edge devices Cloud.

He can also solve the registry scalability issue by mirroring the containers images close to the Edge,

and so on...   

Basically any scenario that involves data centers, servers, or geo-distributed devices.

*   Full control of the simulation environment:

The user can trade-off between simulation duration and its accuracy. 

To decrease the simultion time, the user can also enable parallelism.

## 5. Getting started with PureEdgeSim

### 5.1 Running PureEdgeSim for the First Time

 (i)  Download the zip file from github.
 
 (ii) Import the project to your IDE (New -> Java Project -> Use default location -> and then chose the PureEdgeSim folder)
 
 Wait (1 or 2 minutes) for the dependencies to be downlaoded (you need internet connection).
 
 Once the required libriaries are downloaded the errors will disappear, and you can use the simulator.
 
 If the errors don't disappear after few minutes, you need to add maven to your IDE, or simply add the required libraries
 
 manually( you can find them [here](https://drive.google.com/open?id=1tc3_1UnIYO0AtAXDVf18AHj5g8MdFMNs) ).  
 
 (iii) Launch the `MainApplication.java` class, to launch the simulation and to test PureEdgeSim 
 
 *Running the simulation from command line will be added soon..*
    
![importation](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/importing%20project.gif)

Importing PureEdgeSim and launching the simulation
  
### 5.2 The Simulation Settings

PureEdgeSim provides 5 input files located under the `/settings/` folder (you can check them [here](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/settings)):
  
*   The `simulation parameters` file : It groups all the simulation parameters, including the simulation environment settings  (simulation time, initialization time, update intervals, ..), the models settings (the parameters used by the mobility, energy, network, and tasks generation models..), and so on.    

*   The `applications XML` file: This file decribes the types of applications that will be used by the tasks generator.

Each of these application has different characterestics (CPU utilization, files size, latency requirements..). When the     simulation starts, the tasks generator will associate one of these applications to each Edge device. Then, based on the assigned application type, it will generate the tasks of these device, which enables the support for devices heterogeneity ( the heterogeneity of their applications, in this case).

*   The `Edge devices XML` file:  PureEdgeSim also enables the user to generate hundreds or even thousands of heterogenous devices. To do this, the user will specify the types of Edge devices that will be generated and their proportions/ percentages in the `Edge_device.xml` file. The server manager will then generate the desired number of Edge devices based on the proposed types. For instance, if the user define two types of devices and sets the percentage of each type to 50% and sets the number of Edge devices (in the simulation parameters file) to 1000 devices, the server manager will generate 500 devices of type 1, and 500 devices of type 2. Each of those types can have different computing capacity, energy consumption,  and other settings as well that enable the heterogeneity of devices ( whether the device is mobile or not, battery-powered or not, and how much is its battery capacity...).

*   The `Fog datacenters XML` file : This file describes the Fog datacenters that will be generated during the simulation. However, unlike the Edge devices file, this file contains the Fog datacenters that need to be generated instead of their types. Therefore, if the user wants to generate 4 different servers, he must include each one of them in the file. Each datacenter is characterised by its compuitng capacity, its energy consumption, its location, and its hosts. Each host has a set of Virtual machines with a specified computing capacity. These virtual machines are responsible for executing the offloading tasks.

*   The `Cloud datacetners xml` file:  This file describes the Cloud datacenters that will be generated during the simulation (similar to that of Fog servers). 

### 5.3 The Simulation Parameters File

The parameters file contains the following set of parameters:

*   The simulation environment parameters:

|Parameter                    |Type   |Options/Range|Description                                                                                                         |
|-----------------------------|-------|-------------|--------------------------------------------------------------------------------------------------------------------|
|`simulation_time`            |Integer|>= 1         |The simulation duration (in minutes)                                                                                |
|`initialization_time`        |Integer|>= 0         |The time needed to generate all resources, which means that the tasks offloading process starts affter it           |
|`parallel_simulation`        |Boolean|true or false|Enable or disable parallel simulations                                                                              |
|`update_interval`            |Double |>= 0.01      |The interval between simulation environment events (in seconds)                                                     |
|`pause_length`               |Integer|>= 0         |The pause between iterations (in real seconds)                                                                      |  
|`display_real_time_charts`   |Boolean|true or false|To display or not the simulation results in real time                                                               | 
|`auto_close_real_time_charts`|Boolean|true or false|Auto close real time charts after the end of iteration                                                              |
|`charts_update_interval`     |Double |>= 0.01      |Interval of refreshing real time charts (in seconds)                                                                |
|`save_charts`                |Boolean|true or false|Whether to save charts in `.png` format or not                                                                      |
|`wait_for_all_tasks`         |Boolean|true or false|Wait until all tasks get executed or stop the simulation on time (when the simulation time set by the user finishes)|
|`save_log_file`              |Boolean|true or false|Whether to save the log file or not                                                                                 |
|`clear_output_folder`        |Boolean|true or false|Delete the output folder at the beginning of each simulation                                                        |
|`deep_log_enabled`           |Boolean|true or false|Enable or disable deep logging                                                                                      |     

*   The Location Manager (mobility model) parameters:

|Parameter     |Type   |Options/Range|Description                                                 | 
|--------------|-------|-------------|------------------------------------------------------------|
|`length`      |Integer|>= 1         |The simulation area length (in meters)                      |
|`width`       |Integer|>= 1         |The simulation area width (in meters)                       |
|`Edge_range`  |Integer|>= 1         |The range of Edge devices (in meters)                       |
|`Fog_coverage`|Integer|>= 1         |The raius of the area covered by each Fog server (in meters)|
|`speed`       |Double |>= 0         |The speed of mobile devices in meters/seconds) (0= disabled)|

*   The Server Manager settings:

|Parameter                   |Type   |Options/Range|Description                                                  | 
|----------------------------|-------|-------------|-------------------------------------------------------------|
|`min_number_of_Edge_devices`|Integer|>= 1         |The number of Edge devices at the beginning of the simulation|
|`max_number_of_Edge_devices`|Integer|>= 1         |The number of Edge devices at the end of the simulation      |
|`Edge_device_counter_size`  |Integer|>= 1         |The growing rate in the number of devices in each iteration  |

*   The Network Model settings:

|Parameter                |Type   |Options/Range|Description                                                              |
|-------------------------|-------|-------------|-------------------------------------------------------------------------|
|`wlan_bandwidth`         |Integer|>= 1         |The local area network bandwidth (in Mbps)                               |
|`wan_bandwidth`          |Integer|>= 1         |The backhaul network bandwidth (in Mbps)                                 |
|`wan_propogation_delay`  |Double |>= 0         |The propagation delay (when sending data/tasks to the Cloud) (in seconds)|  
|`network_update_interval`|Double |>= 0.01      |The network model refresh interval (in seconds)                          |

*   The Tasks Orchestration settings:

|Parameter                           |Type   |Options/Range                 |Description                                                                                                                                                                                                                    |
|------------------------------------|-------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|`enable_registry`                   |Boolean|true or false                 |Enabling it means that before executing a task, a container will be pulled from the registry/Cloud                                                                                                                             |
|`enable_orchestrators`              |Boolean|true or false                 |Enabling this means that the task/offloading request will be sent to the specifed/ nearest orchestrator. Disabling this means that each device will orchestrate its owb tasks.                                                 |
|`deploy_orchestrator`               |Boolean|Cloud or Fog                  |To deploy the orchestrator on the Cloud, Fog, or any custom location (e.g. cluster heads, the user in this case need to implement his clustering algorithm)                                                                    |
|`applications_CPU_allocation_policy`|Boolean|SPACE_SHARED or TIME_SHARED   |Time shared means that the tasks can be executed in same virtual machine at simae time (however this increases the simulation duration). Space shared means that the tasks are executed one after the otherby a virtual machine|
|`tasks_generation_rate`             |Integer|>= 1                          |The number of tasks generated by each device every minute                                                                                                                                                                      |
|`orchestration_architectures`       |Boolean|Cloud_ONLY, Fog_AND_Cloud,....|The computing paradigms that are used                                                                                                                                                                                          |
|`orchestration_algorithms`          |Boolean|(any algorithm name)          |The algorithm used by the orchestrator to find the offloading destination                                                                                                                                                      |

*   The Energy Model parameters:

|Parameter                         |Type  |Options/Range|Description                                                        |
|----------------------------------|------|-------------|-------------------------------------------------------------------|
|`consumed_energy_per_bit`         |Double|>= 0         |The enregy consumed when transferring 1 bit (in wh)                |
|`amplifier_dissipation_free_space`|Double|>= 0         |The energy consumed by the amplifier in free space channel  (in wh)|
|`amplifier_dissipation_multipath` |Double|>= 0         |The energy consumed by the amplifier in multipath channel  (in wh) |

### 5.4 The Edge Devices, Fog Servers, and Cloud Datacenters Files

These files contain the specification of Edge devices, Fog datacenters, and Cloud datacenters.

*   Datacenters characteristics

|Attribute        |Type   |Options/Range|Description                                                                    |
|-----------------|-------|-------------|-------------------------------------------------------------------------------|
|`idleConsumption`|Double |>= 0         |The energy consumption rate when the datacenter is idle  (in wh/s)             |
|`maxConsumption` |Double |>= 0         |The energy consumption rate when the datacenter CPU operates at 100%  (in wh/s)|
|`isOrchestrator` |Boolean|true or false|To manually select this datacenter as orchestrator                             |
|`location`       |-      |-            |The X and Y coordinates that define the location of this datacenter            |
|`hosts`          |-      |-            |The list of hosts                                                              |

*   The hosts have the following characteristics

|Attribute|Type   |Options/Range|Description                    |
|---------|-------|-------------|-------------------------------|
|`core`   |Integer|> 0          |The number of CPU cores        |
|`mips`   |Integer|> 0          |The processing power  (in MIPS)|
|`ram`    |Integer|> 0          |RAM (in  MB)                   |
|`storage`|Integer|> 0          |Storage capacity (in MB)       |
|`VMs`    |-      |-            |The list of virtual machines   |

*   Each virtual machine have the following characteristics

|Attribute|Type   |Options/Range|Description                              |
|---------|-------|-------------|-----------------------------------------|
|`core`   |Integer|> 0          |The number of CPU cores used by this VM  |
|`mips`   |Integer|> 0          |The allocated processing power  (in MIPS)|
|`ram`    |Integer|> 0          |The allocated RAM (in  MB)               |
|`storage`|Integer|> 0          |The allocated storage (in MB)            |

 *The sum of virtual machines attributes values (e.g. CPU cores) must be inferior than those of the host*

*   The Edge devices file follows the same structure as the Fog and Cloud `.xml` files. However as we said previously, if we want to generate 100 devices for example, we will not define all these devices in this file, instead, we will define the types of devices that will be generated, for example 25% of the generated devices will be of type 1, etc. The Edge devices are considered as datacenters that contains one host with one VM (the user can add more if needed, by editing this file). The device without a virtual machine is considered a simple sensor (no computing capabilities). The following table highlights the attributes that only Edge devices have : 

|Attribute        |Type   |Options/Range|Description                                                   |
|-----------------|-------|-------------|--------------------------------------------------------------|
|`mobility`       |Boolean|true or false|"True" means the devices of this type are mobile              |
|`battery`        |Boolean|true or false|"True" means that the devices of this type are battery-powered|
|`batterycapacity`|Double |> 0          |The battery capacity (in Wh)                                  |
|`percentage`     |Integer|> 0          |The percentage of devices of this type.                       |

### 5.5 The PureEdgeSim output files

PureEdgeSim output files can be found under the `/output/` directory (you can check the output files [here](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/output/2019-10-28_10-04-01)). There are two types of text files resulted from the simulation: a `.txt` file and a `.csv` file. The `.txt` file contains a brief and easy to read ouput, while the `.csv` file contains more detailed simulations results that are ready to plot. The `.csv` file can be opened using any spreadsheet software (e.g. Microsoft Excel) by which the user can generate unlimited types of charts (with more than 40 metric available).

To ease prototyping and testing, pureEdgeSim can automatically generate more than 60 charts. It can also generate real time charts and display the simulation map. These charts and the map are then saved under the `/output/` folder in a `.png` image format. 

Example of real time charts :

![Real time charts](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/real%20time.gif)

Real time analysis of simulation environment

### 5.6 Building your scenario

### The evaluated architecture (paradigm/ combination of computing paradigms):

In the `simulation_parameters.properties` file under the `settings/` folder, you can specify the architecture/ architectures that will be used by the task orchestrator during the simulation :

*   To use the Cloud alone for processing data (no processing of data on the Edge/Fog) set the `orchestration_architectures`  as

```

orchestration_architectures = CLOUD_ONLY

```

*   To use the Cloud with the Fog, for processing data set the `orchestration_architectures`  as

```

orchestration_architectures = FOG_AND_CLOUD

```

*   To use the edge devices for processing data (no processing of data on the Fog and the Cloud) set the `orchestration_architectures`  as

```

orchestration_architectures = EDGE_ONLY


```

*   To process data on the Cloud and the edge devices (no Fog servers) set the `orchestration_architectures`  as

```

orchestration_architectures = CLOUD_ONLY

```

*   To use all the three paradigms simultaniously set it to 

```

orchestration_architectures = ALL


```

*   You can also set it to FOG_ONLY if the cloud is absent in your scenario.

### The load balancing algorithm :

To use your custom load balancing algorithm, follow example 5. You can find it in the `examples/` folder.

### Custom Mobility Model

If your scenario involves a specific mobility model, you can follow example 1, and implement your custom model. Remember to edit the `edge_devices.xml` file in order to specify which devices are mobile or not. If you don't add a custom mobility model, PureEdgeSim will use the default one.

### Custom Energy Model

Similar to the custom mobility model, you can follow example 2 to see how to add your energy model. if you don't add it, pureEdgeSim will use the default one.

### The cloud datacenters

You can add as many datacenters, hosts or virtual machines in the `cloud.xml` file.

### The fog servers

Similar to the cloud, but remember to set the location of each server ( in the `fog_servers.xml` file). 

You also need to make sure that these servers covers the simulation area by editing these lines from the `simulation_parameters.properties` file

```

fog_coverage=50

length=200

width=200

```     

In this example, the radius of the coverage area of Fog servers is set to 50 meters, while the simulation area is set to 200x 200 meters. 

If we only generate one Fog server, this means that some edge device may be not in the coverage area, which causes there tasks to fail. In this case we need to increase the coverage area (e.g. from 50 to 200 meters), minimize the simulation area (e.g. from 200x200 to 50x50 ) or add more fog servers in order to cover the whole area. 

### The edge devices

You can define the types of edge devices in the `edge_devices.xml` file. if you want to add mobile devices. set `<mobility>` to true. You can specifify the speed of mobile device in the `simulation_parameters.properties` file as follows:

```

speed = 1.4

```

in this example the speed is set to 1.4 meters per second (approximately 5 km/h).

You can also set whether the devices of that type are battery powered or not, by changing the `<battery>` value. `true` which means battery powered)

To test the scalability of the selected architecture (the computing paradigms) you can use a growing number of devices. To do so, edit the following values  

```

min_number_of_edge_devices=100

max_number_of_edge_devices=500

edge_device_counter_size=100

```

In this case, the simulation will start with 100 devices, and grow up to 500 devices (from 100 , to 200, to 300, to 400, to 500). The counter value is the growth rate between the iterations.  

### Simulation duration VS accuracy

This is a discrete event simulator, this means that the simulation duration depends on the number of generated events. The more the events, the longer is the simulation.

To decrease the simulation duration you can change these parameters in the `simulation_parameters.properties`  file:

```

simulation_time=10

parallel_simulation=false

update_interval=1 

pause_length=5

display_real_time_charts=true

charts_update_interval=1

wait_for_all_tasks=true  

network_update_interval=1

```  

The simulation time in this case is set to 10 minutes. you can increase it or decrease it dependiing on your needs.  the pause length is set to 5 seconds, you can set it to 0 if needed (to gain some time between iterations). 

You can also disable the real time charts by setting the `display_real_time_charts` to `false`. Finally, you can set the value of `wait_for_all_tasks` to `false` , which means that the simulation manager will ends the simualtion right after the set 10 minutes (in this exmaple) of time and will not for the tasks that are being executed to finish. This may affect the simulation results so be aware.

Other parameters that help to reduce the simulation time are the `update_interval` and the `network_update_interval`. if you set these two to `0.01`  you will get a higher accuracy (a realistic simulation results), however this may take hours or days. To reduce the simulation time you can trade-off between the simulation delay and its accuracy by setting them to a higher value, for example `0.1` or `1` or even more...

Lakily PureEdgeSim offers the possibility to launch parallel simulations which can be done by setting the value of `parallel_simulation` to `true`.

### 5.7 Examples 

We provide a set of examples to show how to implement custom mobility model, tasks generation model, custom edge devices/datacenters, custom tasks orchestration and load balancing algorithms, and custom energy model.

These examples can be found under the `examples/` folder. 

## 6. Change log of the latest versions

## New version 2.2.0 (nov 2nd 2019)

*   The code has been improved  

*   Fixed some minor bugs 

*   Added some tutorials and examples (more to be added soon) on how to implement your own energy model, mobility model, your custom edge devices/ datacenters , custom orchestrator and custom tasks generator

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

*   Added the possibility to generate charts at the end of the simulation and to save them in a *.PNG format 

More than 64 high resolution charts can be generated with one click, in order to make it easier for the user to check his simlation results

The user can always generate other charts using the generated `.csv` file.

*   Added new simulation parameters regarding charts (displaying them, the refresh delay, saving them..)

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
