# PureEdgeSim

PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Build Status](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim.svg?branch=master)](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/7bcee5c75c3741b5923e0158c6e79b37)](https://www.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=CharafeddineMechalikh/PureEdgeSim&amp;utm_campaign=Badge_Grade) [![Maven Central](https://img.shields.io/maven-central/v/com.mechalikh/pureedgesim.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.mechalikh%22%20AND%20a:%22pureedgesim%22) [![Codacy Badge](https://app.codacy.com/project/badge/Coverage/7bcee5c75c3741b5923e0158c6e79b37)](https://www.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&utm_medium=referral&utm_content=CharafeddineMechalikh/PureEdgeSim&utm_campaign=Badge_Coverage)

## 📝 Please Cite It As (Kindly do not use the github link, othewise, your citation will not be counted):

Mechalikh, C., Taktak, H., & Moussa, F. (2021). PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments. Computer Science and Information Systems, 18(1), 43-66.

Bibtex:

```groovy
@article{mechalikh2021pureedgesim,
  title={PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments},
  author={Mechalikh, Charafeddine and Taktak, Hajer and Moussa, Faouzi},
  journal={Computer Science and Information Systems},
  volume={18},
  number={1},
  pages={43--66},
  year={2021}
}
```

## Version 5.0 Changelog (Mar 30th 2022)
 
*   Removed CloudSim Plus (Just by doing this, the time complexity has been reduced by more than 10 times. But, we are still relying on their efficient implementation of Events Queue).

*   Support of scenarios involving tens of thousands of devices. Compared to the previous version, the same simulation scenario (10 minutes simulation time, 10 000 devices, yes 10 thousands) took  1 minutes and 12 seconds on version 5.0 vs ... well the simulation didn't even start after more than 10 minutes, so I just canceled it. This is due to CloudSim Plus huge number of events, especially the broker and the vm allocation policy ones. A scenario of 10 minutes simulation time with 2000 devices, on the other hand, took 8 seconds on version 5.0 vs 2 minutes, 54 seconds on version 4.2. Even though the new version introduces many new features.

*   Support for scenarios with longer simulation time. A scenario of 24 hours with 200 edge devices (2110706 generated tasks), took 1 minutes and 31 seconds on version 5.0 vs "not possible" on version 4.2 due to out of memory exception.

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

## 📖 Overview

PureEdgeSim is a simulation framework that enables to study Internet of Things on a large scale, as a 
distributed, dynamic, and highly heterogeneous infrastructure, as well as the applications 
that run on these things. It features realistic infrastructure models, allowing research on the edge-to-cloud continuum to be conducted. It
encompasses all layers of edge computing modeling and simulation (shown in the figure below).
It features a modular design in which each module addresses a particular aspect of the 
simulation. The Datacenters Manager module, for example, is concerned with the creation 
of data centers, servers, as well as end devices and their heterogeneity. Their geo-distribution 
and mobility, however, are handled by the Location Manager module. Similarly, the 
Network Module is in charge of bandwidth allocation and data transfer. 

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/aspects.png)

Edge computing simualtion aspects

PureEdgeSim brings together analytical and numerical modeling techniques. The 
network, for example, is modeled by means of adjustable mathematical equations rather than 
at a low level. Such abstraction facilitates simulating thousands of edge devices and 
guarantees the evaluation of the simulation state at any moment. Finally, since PureEdgeSim 
is a discrete event simulator, the characteristics of the simulated environment, like network 
traffic and resources’ utilization, will evolve over time, thereby enabling support for online 
decision-making algorithms, a process that can be accomplished through the Workload 
Orchestrator module.

Each module provides a default implementation along with a set of adjustable settings to 
facilitate prototyping and experimentation. Thus, researchers can effortlessly implement 
their approaches without having to waste time specifying low-level details. Another key 
feature of PureEdgeSim is its extensibility, as it allows users to create and incorporate their 
own models without altering its codebase when the default models fail to satisfy their needs.
In discrete event simulators, the temporal complexity of the simulation is dependent on the 
number of events produced at runtime. To reduce simulation duration, PureEdgeSim allows 
for balancing the precision of the simulation and its duration thanks to a set of dedicated 
parameters. How precise the simulation will rely on the user’s settings, particularly the 
update intervals. Thus, the simulation will be more precise and realistic the shorter these 
intervals are, but it will also last longer.

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/scenario.JPG)
                       A simple representation of the simulation scenarios

## ⚠️ Important
---
*   The development and maintenance of this project requires a considerable effort. Thus, any form of contribution is encouraged. 
*   If you are willing to use the framework to implement your own project on top of it, creating a fork is a bad solution. You are not supposed to modify the code base to implement your project, but extend it by creating some subclasses. Unless you plan to redistribute your changes, you will end up with an incompatible and obsolete version of the framework. The project is constantly evolving and bug fixes are a priority. Your fork with personal changes will miss these updates and high performance improvements.
*   If you just came across this project and want to check it out later, don't forget to add a star :star: :wink:.

---
## 🧰 Exclusive Features

PureEdgeSim offers many exclusive features: 

1.    Realistic modeling of edge computing scenarios (and its related computing paradigms).

*   It supports devices heterogeneity (sensors, mobile devices, battery-powered..).
*   It supports the heterogeneity of their means of communication (WiFi, 5G, 4G, Ethernet..).
*   Realistic modeling of computing infrastructure (modeling computational tasks, infrastructure topology...).
*   Realistic network model (Latency, bandwidth allocation, support for peer to peer communications, routing..).
*   Realistic mobility model.
*   Realistic energy model.

2.    The support for online decision making (tasks orchestration, scheduling, etc.).

3.    The study of QoS-aware architectures and topologies.

4.    Efficiency and Scalability.

*   PureEdgeSim supports the simulation of thousands of devices (even +10000 devices).
*   PureEdgeSim supports scenarios with longer simualtion time (even +24 hours).

5.    A wide collection of metrics

*   Delays: execution time, waiting time, network time.
*   CPU utilization.
*   Network utilization: Average bandwidth, total traffic...
*   Energy consumption: The energy consumption of computing nodes (Cloud, edge/fog servers,edge devices), the remaining energy of battery-powered devices, the energy consumption of WAN, MAN, and LAN networks, and the enrgy consumption of WiFi, Cellular (5G, 4G,..) and Ethernet.
*   Task success rate and reason of failure (the rate of tasks failed due to the lack of resources, due to mobility, due to latency, or due to the death of edge devices when they run out of battery).
*   and many others.

6.    The visualization of the simulation environement (which helps to debug and understand the course of the simualtion, refer to the figure below).

7.    Extensibility: Users can implement their scenarios and integrate their models without modeifying PureEdgeSim codebase.

8.    Wide Applicability

*   Mist computing scenarios, mobile ad hoc edge computing, fog computing,...
*   Worklaod orchestration, scheduling, caching...

9.    Ease of use.

10.    Reliability.

11.    Correctness.

![Real time charts](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/real%20time.gif)
The live visualization of the simulated environement

### Efficiency and Scalability 
A system is said to be efficient when it can maintain a specified level of proficiency without utilizing an excessive amount of resources, i.e., execution time and memory. Determining the theoretical time complexity of the given simulation is not trivial because the execution time depends entirely on the user scenario (i.e., the devices and tasks count, the mobility model, the types of resources, etc.) and the number of events that occur during the simulation. Furthermore, some parameters strongly influence the number of simulations and, obviously, the number of runs also directly influences the execution time.
To demonstrate the scalability of PureEdgeSim, a few experiments were conducted on a single Intel Core i7-8550U.

1.  A 10-minute simulation scenario with 10000 devices (ten thousand, which generated 1015000 tasks) took 72 seconds. 
2.  A 24-hour simulation scenario with 200 devices (total of 2110706 tasks generated) took 91 seconds. 
3.  A 60-minute scenario with 100 devices took only 3 seconds, meaning that the simulator was able to run through as much as 1200 seconds of simulated time in just one second of real time. 

Of course, this time is highly dependent on the complexity of the decision making algorithm itself and the number of generated events  (as more devices = more tasks = more events), which is why it took longer with 10,000 devices. That said, it is still faster than real-time and outperforms all CloudSim/ CloudSim plus based simulators that either struggle to start due to the large number of devices or run out of memory due to the large number of events generated.

It is therefore easy to conclude that PureEdgeSim meets the “efficiency and scalability” criteria as it allows for simulated experiments involving hundreds if not thousands of devices and tasks and runs considerably faster than in wall-clock time. An important step to reduce the simulation duration and solve its dependency on the number of runs is to implement multi-threading to execute simulation runs in parallel, which is already supported by PureEdgeSim.

### Simulation Accuracy

As mentioned previously, PureEdgeSim offers the most realistic network, energy, and mobility models as compared to existing solutions. However, since it is a DES, the time complexity of the simulation is dependent on the number of events generated at runtime. To decrease simulation time, PureEdgeSim provides fast and complete control over the simulation environment by its collection of parameters, allowing users to compromise between simulation precision and length. Hence, how accurate the simulation is will be influenced by the user’s settings, particularly the update intervals. The shorter these intervals, the more precise and realistic the simulation, but also the longer it takes. 

### Correctness

The tool is said to be correct if it fully complies with the stated requirements, which can be verified by checking test cases. To verify PureEdgeSim’s correctness through unit and integration testing, several test cases have been implemented. Nevertheless, this alone does not provide an assessment of which portions of the code were tested and which were not, neither gives a percentage of the overall coverage of tests alongside the project source code. To evaluate the amount of code that the available unit tests cover, code coverage reports were added to the project with the help of the Java Code Coverage Library (JaCoCo). 
It is important to clarify that the concern of such test cases is the technical feasibility of the scenarios and not the evaluation of the simulation outcome. It is simply a matter of testing whether the given results are logically reasonable and not of actually validating them. To validate the software, a case study was conducted using the simulator. [At least 95% of PureEdgeSim code is covered by those tests](https://app.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&utm_medium=referral&utm_content=CharafeddineMechalikh/PureEdgeSim&utm_campaign=Badge_Coverage). 

This achievement was made possible by testing all the new features and also by the considerable decrease in code duplication since duplicating code brings neglect of testing. 
In conclusion, we can say that the proposed simulator performs as intended. Every function has finished without abortions and gave the expected output; All the input files were parsed successfully, and all the personalized models were integrated and verified with the predicted results. The plotting of the simulation results gave a reasonable graphical output. In terms of intended errors, such as incorrect inputs, proper error messages were displayed, and correct information was written into the log file. Moreover, the parameters can be modified, stored, and loaded without any error.

### Reliability 

Reliability is the ability of a system to operate consistently under specified conditions while meeting all requirements. When it comes to the components of PureEdgeSim, reliability is ensured by extensive exception handling that catches any error occurring at runtime. Consequently, the errors will not cause the execution to stop (unless they are related to critical parameters without which the simulation cannot continue). While testing it, the proposed simulator never froze and satisfied all functional requirements. Furthermore, data reliability is high since the models are read-only and unmodifiable, meaning no data loss is to be feared. The only files that can be modified are the output files and the configuration files, which can be restored easily to the default values. In addition, PureEdgeSim uses two continuous integration services: Travis  and AppVeyor , that automate tests and builds execution on Linux and Windows virtual machines, respectively. PureEdgeSim’s users will benefit from such services as any test or build failure will be communicated promptly, thus, ensuring greater reliability. 

## 👩🏽‍💻 How to Use

There are several ways to use PureEdgeSim; however, it is strongly advisable to run PureEdgeSim via a Java development environment, like Eclipse IDE. A set of predefined examples is provided under the “/examples“ directory, which should allow anyone to become familiar with PureEdgeSim.

### Using an IDE

The simplest and recommended method to run this project is to use an IDE like Eclipse, IntelliJ IDEA, or NetBeans. The required steps to get started with the project are listed below:

1.    Downloading and extracting the project source code archive from the project repository : This can be done using the web browser or the command line `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`.
2.    Importing the project to the IDE:

*   In NetBeans, simply select the "Open project" option and chose the project directory.
*   In Eclipse or IntelliJ IDEA, the project must be imported by selecting the location where it was extracted or cloned .

3.    The imported project consists of ten packages: the main application package, the above-mentioned modules, and the examples package. The main application and the modules are the simulator source code that normally should not be modified. The examples, however, are where the users can start.
4.    It is necessary to convert the project into a Maven project in order to download all the required libraries.
5.    Once all the necessary libraries are downloaded, users can start with the most basic examples by running any of the classes located in the “examples” package.
6.    To build a new simulation scenario, the simplest way is to create another class in this package.

![Environment](https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/importingproject.gif)
Importing PureEdgeSim project

### Adding PureEdgeSim as a Dependency

It is possible to use PureEdgeSim as a dependency in a Maven project, by inserting the dependency  below into the pom.xml file:
```xml
<dependency>
	<groupId>com.mechalikh</groupId>
	<artifactId>pureedgesim</artifactId>
	<version>5.0.0</version>
</dependency>
```
Or on Gradle :

```groovy
dependencies {
 implementation 'com.mechalikh:pureedgesim:5.0.0'
}
````

### Via Command Line

Assuming that git  and maven  are already installed, PureEdgeSim can be run from the command line as follows:
1.    First, the project source code must be downloaded by cloning the repository via the command `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`. 
2.    Now that the project is cloned, it can be built using Maven by executing the  `mvn clean install ` command in the directory where it was cloned.
3.    Now, the examples can be executed on Windows, Linux, or Mac operating systems, using the command  `mvn exec:java -Dexec.mainClass="package.Class_Name" `. For instance, to execute “Example1”, the command is  `mvn exec:java -Dexec.mainClass="examples.Example1" `

## 🧩 Works That Are Based on / Using PureEdgeSim

Here are some works that are based on PureEdgeSim to give an idea about its wider applicability and the possibilities it offers. Feel free to edit the readme file and add yours!.

### Rienforcement and Deep Learning Scenarios

1.    Mechalikh, C., Taktak, H., & Moussa, F. (2020, April). A fuzzy decision tree based tasks orchestration algorithm for edge computing environments. In International Conference on Advanced Information Networking and Applications (pp. 193-203). Springer, Cham.  ([The algorithm can be found here](https://github.com/CharafeddineMechalikh/FDT_based_workload_orchestration)).
2.    Safavifar, Z., Ghanadbashi, S., & Golpayegani, F. (2021, November). Adaptive Workload Orchestration in Pure Edge Computing: A Reinforcement-Learning Model. In 2021 IEEE 33rd International Conference on Tools with Artificial Intelligence (ICTAI) (pp. 856-860). IEEE.
3.    Neelakantam, G., Onthoni, D. D., & Sahoo, P. K. (2020). Reinforcement learning based passengers assistance system for crowded public transportation in fog enabled smart city. Electronics, 9(9), 1501.
4.    Iftikhar, S., Golec, M., Chowdhury, D., Gill, S. S., & Uhlig, S. (2022). FogDLearner: A Deep Learning-based Cardiac Health Diagnosis Framework using Fog Computing. In Australasian Computer Science Week 2022 (pp. 136-144).

### Simulation tools

5.    Wei, J., Cao, S., Pan, S., Han, J., Yan, L., & Zhang, L. (2020, June). SatEdgeSim: A toolkit for modeling and simulation of performance evaluation in satellite edge computing environments. In 2020 12th International Conference on Communication Software and Networks (ICCSN) (pp. 307-313). IEEE.

### Data Caching

6.    Mechalikh, C., Taktak, H., & Moussa, F. (2019, July). Towards a scalable and QoS-aware load balancing platform for edge computing environments. In 2019 International Conference on High Performance Computing & Simulation (HPCS) (pp. 684-691). IEEE.
7.    Epifâneo, L., Correia, C., & Rodrigues, L. (2021, November). Cathode: A Consistency-Aware Data Placement Algorithm for the Edge. In 2021 IEEE 20th International Symposium on Network Computing and Applications (NCA) (pp. 1-10). IEEE.

### Task Scheduling and Security

8.    Tiburski, R. T. (2021). Task scheduling and security for edge devices in internet of things applications.

### Self-Organized Architectures

9.    Mechalikh, C., Taktak, H., & Moussa, F. (2019, June). A scalable and adaptive tasks orchestration platform for IoT. In 2019 15th International Wireless Communications & Mobile Computing Conference (IWCMC) (pp. 1557-1563). IEEE.
10.    Mordacchini, M., Ferrucci, L., Carlini, E., Kavalionak, H., Coppola, M., & Dazzi, P. (2021, September). Self-organizing Energy-Minimization Placement of QoE-Constrained Services at the Edge. In International Conference on the Economics of Grids, Clouds, Systems, and Services (pp. 133-142). Springer, Cham.

### Software-Defined Networking

11.    Okwuibe, J., Haavisto, J., Kovacevic, I., Harjula, E., Ahmad, I., Islam, J., & Ylianttila, M. (2021). SDN-Enabled Resource Orchestration for Industrial IoT in Collaborative Edge-Cloud Networks. IEEE Access, 9, 115839-115854.

### Application Placement

12.    Khosroabadi, F., Fotouhi-Ghazvini, F., & Fotouhi, H. (2021). SCATTER: service placement in real-time fog-assisted IoT networks. Journal of Sensor and Actuator Networks, 10(2), 26.
13.    Ferrucci, L., Mordacchini, M., Coppola, M., Carlini, E., Kavalionak, H., & Dazzi, P. (2020, June). Latency preserving self-optimizing placement at the edge. In Proceedings of the 1st Workshop on Flexible Resource and Application Management on the Edge (pp. 3-8).
14.    Kavalionak, H., Coppola, M., & Dazzi, P. Self-organizing Energy-Minimization Placement of QoE-Constrained Services at the Edge. In Economics of Grids, Clouds, Systems, and Services: 18th International Conference, GECON 2021, Virtual Event, September 21–23, 2021, Proceedings (p. 133). Springer Nature.

### Other Use Cases

15.    Kaneko, Y., Yokoyama, Y., Monma, N., Terashima, Y., Teramoto, K., Kishimoto, T., & Saito, T. (2020, September). A Microservice-Based Industrial Control System Architecture Using Cloud and MEC. In International Conference on Edge Computing (pp. 18-32). Springer, Cham.
16.    Javaid, H., Saleem, S., Wajid, B., & Khan, U. G. (2021, May). Diagnose a Disease: A Fog Assisted Disease Diagnosis Framework with Bidirectional LSTM. In 2021 International Conference on Digital Futures and Transformative Technologies (ICoDT2) (pp. 1-6). IEEE.

## ⚖️ License

This project is licensed under [GNU GPLv3](http://www.gnu.org/licenses/gpl-3.0).

<p align="right"><a href="#top">:arrow_up:</a></p>