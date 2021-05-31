# Please cite it as

**Mechalikh, C., Taktak, H., Moussa, F.: PureEdgeSim: A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments. Computer Science and Information Systems, Vol. 18, No. 1, 43–66. (2021), https://doi.org/10.2298/CSIS200301042M**

Or

Mechalikh, C., Taktak, H., Moussa, F.: PureEdgeSim: A Simulation Toolkit for Performance Evaluation of Cloud, Fog, and Pure Edge Computing Environments. The 2019 International Conference on High Performance Computing & Simulation (2019) 700-707

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Build Status](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim.svg?branch=master)](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim) [![Build status](https://ci.appveyor.com/api/projects/status/u6hwmktmbji8utnf?svg=true)](https://ci.appveyor.com/project/CharafeddineMechalikh/pureedgesim) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/25ee278611014a9bb242297480703cf9)](https://www.codacy.com/manual/CharafeddineMechalikh/PureEdgeSim?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=CharafeddineMechalikh/PureEdgeSim&amp;utm_campaign=Badge_Grade) [![Maintainability](https://api.codeclimate.com/v1/badges/a1ffecb5230fc5771b93/maintainability)](https://codeclimate.com/github/CharafeddineMechalikh/PureEdgeSim/maintainability) [![codebeat badge](https://codebeat.co/badges/bbe172a2-1169-4bbe-b6a6-0505631babc6)](https://codebeat.co/projects/github-com-charafeddinemechalikh-pureedgesim-master) [![Maven Central](https://img.shields.io/maven-central/v/com.mechalikh/pureedgesim.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.mechalikh%22%20AND%20a:%22pureedgesim%22)

## For more information

**The Fuzzy Decision Tree Based algorithm presented in**

Mechalikh, C., Taktak, H., & Moussa, F. (2020, April). A Fuzzy Decision Tree Based Tasks Orchestration Algorithm for Edge Computing Environments. In International Conference on Advanced Information Networking and Applications (pp. 193-203). Springer, Cham.

is now available [Here](https://github.com/CharafeddineMechalikh/FDT_based_workload_orchestration).

Read the wiki here: [PureEdgeSim WIKI](https://github.com/CharafeddineMechalikh/PureEdgeSim/wiki) 

Other publication using PureEdgeSim can be found [here](https://www.researchgate.net/profile/Charafeddine_Mechalikh/research)

*For any questions, contact me at <charafeddine.mechalikh@gmail.com>*   

## Works that are based on PureEdgeSim and extensions

*   SatEdgeSim: A Toolkit for Modeling and Simulation of Performance Evaluation in Satellite Edge Computing Environments, read more about it [here](https://github.com/wjy491156866/SatEdgeSim).

## 1. Background

Edge and Mist (Extreme Edge) computing, two emerging computing paradigms that aim to overcome the Cloud computing limitations by bringing its applications at the Edge of the network. Thus, reducing both the latency and the Cloud workload and leading to a more scalable network. Nevertheless, in these distributed environments where many devices need to offload their tasks to one another (either to increase their lifetime or to minimize the task completion delay) many issues such as resources management strategies has to be solved. Instead of testing them on a real distributed system, the simulation makes it possible to evaluate the proposed strategies and algorithms in a repeatable, controllable and cost-effective way before their actual deployment. However, when it comes to simulation tools, Mist computing still did not get the attention they deserve (with only few simulators for Edge computing such as iFogSim, EdgeCloudSim).

We introduce PureEdgeSim, a new simulator based on [CloudSim Plus](http://Cloudsimplus.org) that is designed to simulate Cloud, Edge, and Mist computing environments. It allows to evaluate the performance of resources management strategies in terms of network usage, latency, resources utilization, energy consumption, etc. and enables the simulation of several scenarios such as the Internet of Things (IoT), connected vehicles, Mist computing environments (peer-to peer networks such as mobile devices Cloud), and mobile Edge computing. 
   
## 2. PureEdgeSim Architecture

PureEdgeSim enables the simulation of resource management strategies and allows to evaluate the performance of Cloud, Edge, and Mist computing environments. It grantees high scalability by enabling the simulation of thousands of devices. Besides, it supports the Edge devices heterogeneity (i.e. whether this device is mobile or not, whether battery-powered or not, different  applications requirements: tasks file size, tasks CPU utilization,and latency requirement, etc.) 

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/scenario.JPG)

A simple representation of the simulation scenarios

It provides a task orchestrator module that orchestrates the tasks and enables the multi-tiers simulations scenarios where many computing paradigms can be used in conjunction with one another. Besides, it provides an even more realistic network model (as compared to state of the art simulators) that continuously changes the allocated bandwidth for each task being transferred depending on the network traffic. 

It consists of the following 7 modules:

*   Scenario Manager, that loads the simulation parameters and the user scenario from the input files (`.xml` and `.prop` files in `/settings/` folder). It consists of two classes, the File Parser that checks the input files and loads the simulation parameters, and the Simulation Parameters class which represents a placeholder for the different parameters.


*   Simulation Manager, that initiates the simulation environment, schedules all the events and generates the output. It consists of two important classes, the Simulation Manager class which manages the simulation, schedules the tasks generation, etc. The Simulation Logger class that generates the simulation output saves it in comma-separated value (CSV) format in order to easily exploit them later using any spreadsheet editor (e.g., Microsoft Excel...).


*   Data Centers Manager: it generates and manages all the data centers and devices (i.e., Cloud, Edge or Mist). It consists of two classes: the Data Center class, that contains the specific properties of Edge devices such as the location, the mobility, the energy source, and the capacity/remaining energy if it is battery-powered. The second class is the Server Manager which generates the needed servers and Edge devices, their hosts and their virtual machines.


*   Tasks Generator which is behind the tasks generation, -currently- it assigns an application such as e-health, smart-home, and augmented-reality (that can be defined in `settings/applications.xml` file) to each Edge device. Then, it will generates the needed tasks according to the assigned type, which guarantees the heterogeneity of applications.  


*   The Network Module: that consists mainly of the Network Model class.which is behind the transfer of tasks/containers/ request... 


*   The Tasks Orchestrator, which is the decision maker, where the user can define the orchestration algorithm. 


*   The Location Manager, which generates the mobility path of mobile devices.
   
![Architecture](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/modules.PNG)

PureEdgeSim architecture

## 3. What can be simulated with PureEdgeSim

*   Cloud, Edge, and Mist computing scenarios


*   And basically, any scenario that involves computing on distributed nodes or mobility, for example: IoT applications, Mobile Devices Clouds, Mobile Edge Computing,... 
  
## 4. PureEdgeSim features

*   Realistic network and energy models as compared to other simulators.

*   Mobility support which is ignored by most simulators: 

A ready to use mobility model (mode models will be added).

The user can easily add new models based on his needs.

The user can specify the dimensions of the simulated area and the speed of mobile devices. 

The mobility model will assign a random location to each device.

Then the mobile devices will change their location according to the model in use.
*   The support for devices heterogeneity:

The user can define heterogeneous Edge device, Edge data centers, and Cloud Data Centers in the corresponding `.xml` files. 

He or She will decide whether and Edge device is mobile or not, whether it is battery-powered or not (and the size of its battery), 

and how much computing capacity it has.

The devices without computing capacity are considered as simple sensors that only generate data/tasks. 

The user can also define the applications that are in use, their CPU utilization, their files sizes and their latency. requirements. 
*   The scalability, generate hundreds of devices, with a single click. 
*   A rich collection of metrics:

The simulation output  (the `.csv` file) includes + 40 metrics ready to be plotted.

Also, new metrics can be derived from those.

*   Ease of use:

More than 60 charts can be generated automatically.

Other charts can be easily generated from the csv file using any spreadsheet software (e.g. Microsoft Excel).

Readable code and an architecture that is easy to understand.

*   Wide applicability and extensibility:

The support for many simulation scenarios : IoT, VANETs/MANET Clouds, Edge and Mist computing environments..

The user can evaluate the orchestration algorithms, the architectures,...

The support for many devices and applications types...

Various simulation parameters that meet the requirement of any scenario...

The user can implement new orchestration algorithms (machine learning algorithms for example)

He or She can also implement new network, energy, mobility, or tasks generation models. 

He or She can group Edge devices into clusters, deploy the orchestrator node in the cluster head for example,

and form a sort of Edge devices Cloud.

He or She can also solve the registry scalability issue by mirroring the containers images close to the Edge,

and so on...   

Basically any scenario that involves data centers, servers, or geo-distributed devices.

*   Full control of the simulation environment:

The user can trade-off between simulation duration and its accuracy. 

To decrease the simulation time, the user can also enable parallelism. 

Example of real time charts :

![Real time charts](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/real%20time.gif)

Real time analysis of simulation environment