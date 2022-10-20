
<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196671093-21ba3438-719d-4dd4-ad79-bfddd1395663.png" align="right" />

# PureEdgeSim

PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Build Status](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim.svg?branch=master)](https://travis-ci.com/CharafeddineMechalikh/PureEdgeSim) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/7bcee5c75c3741b5923e0158c6e79b37)](https://www.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=CharafeddineMechalikh/PureEdgeSim&amp;utm_campaign=Badge_Grade) [![Maven Central](https://img.shields.io/maven-central/v/com.mechalikh/pureedgesim.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.mechalikh%22%20AND%20a:%22pureedgesim%22) [![Codacy Badge](https://app.codacy.com/project/badge/Coverage/7bcee5c75c3741b5923e0158c6e79b37)](https://www.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&utm_medium=referral&utm_content=CharafeddineMechalikh/PureEdgeSim&utm_campaign=Badge_Coverage)

![image](https://user-images.githubusercontent.com/46229052/196698541-569de31b-0df8-470b-907c-02ba5eb5015f.png)

## Please Cite It As (Kindly do not use the github link):

Mechalikh, C., Taktak, H., & Moussa, F. (2021). PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments. Computer Science and Information Systems, 18(1), 43-66.

Bibtex:

```js
@article{mechalikh2021pureedgesim,
  title={PureEdgeSim: A simulation framework for performance evaluation of cloud, edge and mist computing environments},
  author={Mechalikh, Charafeddine and Taktak, Hajer and Moussa, Faouzi},
  journal={Computer Science and Information Systems},
  volume={18}
  number={1},
  pages={43--66},
  year={2021}
}
```

## Version 5.1 Changelog (October 18th 2022)

<details>
<summary>"Click to expand"</summary>

*   Updated the energy model with the ability to set an initial battery level for generated devices, either programmatically or via the xml file. 

*   Now users can extend the simulation manager to change its behavior, and easily integrate it in the simulation.

*   Users can now easily integrate a custom task class.

*   All entities are now notified at the end of the simulation through onSimulationEnd() method.

*   Failure Model: now tasks can fail due to insufficient resources: RAM, STORAGE, CPU Cores.

*   Disabled some unnecessary network links events/loops for better performance.

*   Now tasks can be scheduled in batches instead of being scheduled all at once at the beginning of the simulation (hence, decreasing the number of events and therefore, faster simulations and less memory usage).

*   Removed unnecessary loops when orchestrating tasks (e.g., no need to browse through edge devices when the cloud only architecture is used, etc.) 

*   Overall version 5.1 can be 30 times faster and supports up to 8 times the number of tasks compared to previous version 5.0 (see figure below).

*   Improved extensibility: Users can now easily incorporate a custom task class, custom simulation manager, as well as a custom topology creator.

*   Now applications placement decisions are done after each failure (default behavior, can be changed by extending the DefaultSimulationManager and DefaultOrchestrator classes).

*   Reduced duplication and improved code quality.

*   Other fixes..

<p align="center">
  <img  width="500" src="https://user-images.githubusercontent.com/46229052/196478109-29442658-19c1-498e-9c0e-593780e12f44.png">
</p>

<p align="center">A comparison of simulation duration between the previous version 5.0 and the latest version 5.1</p>

</details>

<br></br>
![image](https://user-images.githubusercontent.com/46229052/196704278-4d04778b-1a9e-46da-9ae2-18e6a7a1bae5.png)
<a id="overview"></a>

<img width="20%" src="https://user-images.githubusercontent.com/46229052/196671599-c2c33b14-be0b-4f7a-92b2-533978afb029.png" align="right" />
<p align="justify">
PureEdgeSim is a simulation framework that enables to study Internet of Things on a large scale, as a 
distributed, dynamic, and highly heterogeneous infrastructure, as well as the applications 
that run on these things. It features realistic infrastructure models, allowing research on the edge-to-cloud continuum to be conducted. It
encompasses all layers of edge computing modeling and simulation.
It features a modular design in which each module addresses a particular aspect of the 
simulation. The Datacenters Manager module, for example, is concerned with the creation 
of data centers, servers, as well as end devices and their heterogeneity. Their geo-distribution 
and mobility, however, are handled by the Location Manager module. Similarly, the 
Network Module is in charge of bandwidth allocation and data transfer. </p>

<br clear="left"/>
<img  width="300" src="https://user-images.githubusercontent.com/46229052/196689838-65b94184-845f-4f3a-b749-a9833e595580.png" align="left">
<p align="justify">
PureEdgeSim brings together analytical and numerical modeling techniques. The 
network, for example, is modeled by means of adjustable mathematical equations rather than 
at a low level. Such abstraction facilitates simulating thousands of edge devices and 
guarantees the evaluation of the simulation state at any moment. Finally, since PureEdgeSim 
is a discrete event simulator, the characteristics of the simulated environment, like network 
traffic and resources’ utilization, will evolve over time, thereby enabling support for online 
decision-making algorithms, a process that can be accomplished through the Workload 
Orchestrator module.</p>

<br clear="left"/>
<p align="justify">
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
intervals are, but it will also last longer. You can read more in the <a href="https://github.com/CharafeddineMechalikh/PureEdgeSim/wiki">WiKi</a></p>.

<p align="center">
  <img  width="500" src="https://user-images.githubusercontent.com/46229052/196690369-54350b0f-5382-427a-8f12-c582f3f7f2b7.png">
</p>

<p align="center">A simple representation of the simulation scenarios</p>

<br></br>
![image](https://user-images.githubusercontent.com/46229052/196704803-9a9f53b2-8255-4042-9c16-6c8470489791.png)
<a id="important"></a>

>* <p align="justify">The development and maintenance of this project requires a considerable effort. Thus, any form of contribution is encouraged. </p>
>* <p align="justify">If you are willing to use the framework to implement your own project on top of it, creating a fork is a bad solution. You are not supposed to modify the code base to implement your project, but extend it by creating some subclasses. Unless you plan to redistribute your changes, you will end up with an incompatible and obsolete version of the framework. The project is constantly evolving and bug fixes are a priority. Your fork with personal changes will miss these updates and high performance improvements.</p>
>* <p align="justify">If you just came across this project and want to check it out later, don't forget to add a star :star: :wink:.</p>


<br></br>
![image](https://user-images.githubusercontent.com/46229052/196704930-a1ef92c7-b62e-42a2-8e82-b6ba8b949070.png)
<a id="exclusive-features"></a>

<p align="justify">
PureEdgeSim offers many exclusive features: 

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196670148-8f647e7f-ffe2-49ea-865b-2c37ca044cb2.png" align="right" />

- Realistic modeling of edge computing scenarios (and its related computing paradigms).
  - It supports devices heterogeneity (sensors, mobile devices, battery-powered..).
  - It supports the heterogeneity of their means of communication (WiFi, 5G, 4G, Ethernet..).
  - Realistic modeling of computing infrastructure (modeling computational tasks, infrastructure topology...).
  - Realistic network model (Latency, bandwidth allocation, support for peer to peer communications, routing..).
  - Realistic mobility model.
  - Realistic energy model.
- The support for online decision making (tasks orchestration, scheduling, etc.).
- The study of QoS-aware architectures and topologies.
- Efficiency and Scalability.
  - PureEdgeSim supports the simulation of thousands of devices (even +30000 devices).
  - PureEdgeSim supports scenarios with longer simualtion time (even +24 hours).
- A wide collection of metrics
  - Delays: execution time, waiting time, network time.
  - CPU utilization.
  - Network utilization: Average bandwidth, total traffic...
  - Energy consumption: The energy consumption of computing nodes (Cloud, edge/fog servers,edge devices), the remaining energy of battery-powered devices, the energy consumption of WAN, MAN, and LAN networks, and the enrgy consumption of WiFi, Cellular (5G, 4G,..) and Ethernet.
  - Task success rate and reason of failure (the rate of tasks failed due to the lack of resources, due to mobility, due to latency, or due to the death of edge devices when they run out of battery).
  - and many others.
- The visualization of the simulation environement (which helps to debug and understand the course of the simualtion, refer to the figure below).
- Extensibility: Users can implement their scenarios and integrate their models without modifying PureEdgeSim codebase.
- Versatiliy and Wide Applicability
- Ease of use.
- Reliability.
- Correctness.
</p>

<p align="center">
  <img  width="500" src="https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/real%20time.gif">
</p>

<p align="center">The live visualization of the simulated environement</p>

### Efficiency and Scalability 

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196665651-e27976b0-8908-4a75-ac54-dc838c7abf52.png" align="right" />


<p align="justify">
A system is said to be efficient when it can maintain a specified level of proficiency without utilizing an excessive amount of resources, i.e., execution time and memory. Determining the theoretical time complexity of the given simulation is not trivial because the execution time depends entirely on the user scenario (i.e., the devices and tasks count, the mobility model, the types of resources, etc.) and the number of events that occur during the simulation. Furthermore, some parameters strongly influence the number of simulations and, obviously, the number of runs also directly influences the execution time.
To demonstrate the scalability of PureEdgeSim, a few experiments were conducted on a single Intel Core i7-8550U.
</p>
<br clear="left"/>

>  A 10-minute simulation scenario with 10000 devices (which generated 1131000 tasks) took 58 seconds. 
>
>  A 24-hour simulation scenario with 200 devices (total of 3221280 generated tasks generated) took 51 seconds. 
>
>  A 60-minute scenario with 100 devices took only 1 seconds.

<p align="justify">
Of course, this time is highly dependent on the complexity of the decision making algorithm itself and the number of generated events  (as more devices = more tasks = more events), which is why it took longer with 10,000 devices. That said, it is still faster than real-time and outperforms all CloudSim/ CloudSim plus based simulators that either struggle to start due to the large number of devices or run out of memory due to the large number of events generated.</p>
<p align="justify">
It is therefore easy to conclude that PureEdgeSim meets the “efficiency and scalability” criteria as it allows for simulated experiments involving hundreds if not thousands of devices and tasks and runs considerably faster than in wall-clock time. An important step to reduce the simulation duration and solve its dependency on the number of runs is to implement multi-threading to execute simulation runs in parallel, which is already supported by PureEdgeSim.
</p>

<p align="center">

![image](https://user-images.githubusercontent.com/46229052/196690925-1088b91a-f576-42d4-86ca-0c34f4be7d40.png#gh-dark-mode-only)

![image](https://user-images.githubusercontent.com/46229052/196690607-821202ec-f8af-476c-9b2c-281525114d04.png#gh-light-mode-only)
</p>

<p align="center">PureEdgeSim support for large-scale scenarios</p>

### Simulation Accuracy

<p align="justify">
As mentioned previously, PureEdgeSim offers the most realistic network, energy, and mobility models as compared to existing solutions. However, since it is a DES, the time complexity of the simulation is dependent on the number of events generated at runtime. To decrease simulation time, PureEdgeSim provides fast and complete control over the simulation environment by its collection of parameters, allowing users to compromise between simulation precision and length. Hence, how accurate the simulation is will be influenced by the user’s settings, particularly the update intervals. The shorter these intervals, the more precise and realistic the simulation, but also the longer it takes. 
</p>

### Versatiliy and Wide Applicability

<p align="justify">
If we classify edge computing and its related paradigms based on their placement in the IoT architecture, we will have three categories, each with specific properties. For example, paradigms of the perception layer like mist computing rely on resource-constrained devices, that can join or leave the network at any moment.
</p>

![image](https://user-images.githubusercontent.com/46229052/196697530-fa0e8316-6f6e-4d0c-9024-6c8386a24324.png#gh-light-mode-only)
![image](https://user-images.githubusercontent.com/46229052/196697387-e12d8836-e4bc-44d6-b629-742534d1a4ac.png#gh-dark-mode-only)

<p align="center">The computing paradigms supported by PureEdgeSim</p>

<p align="justify">
Accordingly, the model must be versatile enough to support all kinds of resources and topologies. For this, PureEdgeSim dedicates a configuration file for each layer. The computing nodes, be they mobile devices, servers, or data centers, are all defined in these files. For instance,  this figure shows how a smartphone is modeled. As you can see, a set of attributes is also provided to specify the remaining characteristics, such as power consumption rates and mobility.
</>

```xml
<?xml version="1.0"?>
<edge_devices>
	<device> <!-- this is a smartphone, for example -->
		<mobility>true</mobility><!-- the device is mobile or fixed --> 
		<connectivity>4g</connectivity><!--the connectivity type 4g, ethernet, Wi-Fi-->
		<speed>1.4</speed><!-- the speed of the device in meters per second : 1.4m/s = 5km/h, 0 = non mobile-->
		<minPauseDuration>100</minPauseDuration><!-- the minimum delay before moving to a new location-->
		<maxPauseDuration>400</maxPauseDuration><!-- the maximum delay before moving to a new location-->
		<minMobilityDuration>60</minMobilityDuration><!-- the minimum delay before stopping-->
		<maxMobilityDuration>100</maxMobilityDuration><!-- the maximum delay before stopping-->
		<battery>true</battery> <!-- relies on battery? -->
		<percentage>30</percentage> <!-- percentage of this device type -->
		<batteryCapacity>18.75</batteryCapacity> <!-- battery capacity in Watt-Hour -->
		<idleConsumption>0.078</idleConsumption><!-- idle energy consumption/ 
			second-->
		<maxConsumption>3.3</maxConsumption><!-- max energy consumption/second, 
			when device cpu is use at 100%-->
		<isOrchestrator>false</isOrchestrator> <!-- whether it orchestrates tasks or not -->
		<canGenerateTasks>true</canGenerateTasks> <!-- whether it generates data/tasks or not -->
		<canProcessTasks>true</canProcessTasks> <!-- whether it can process data/tasks or not -->
		<hosts>
			<host>
				<core>8</core> <!-- Number of CPU cores -->
				<mips>25000</mips> <!-- Processing power in MIPS -->
				<ram>4096</ram> <!-- Ram capacity in MBytes -->
				<storage>128000</storage> <!-- Storage in MBytes -->
				<VMs
					<VM>
						<core>8</core>
						<mips>25000</mips>
						<ram>4096</ram>
						<storage>128000</storage> 
					</VM>
				</VMs>
			</host>
		</hosts>
	</device>
...
</edge_devices>
```

<p align="center">How a smartphone can be modeled in PureEdgeSim</p>

### Ease of Use 

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196668625-54203eac-5472-4c0e-ade2-39bc1e6e5d21.png" align="right" />

<p align="justify">Simulators can be used to determine which factors have the greatest impact on performance, by evaluating different configurations, for example, the number of generated entities, the types of resources, the network settings, etc. Dealing with all these parameters programmatically can be challenging. Software is said to be usable if it can be easily comprehended and run by any user. While the usability of PureEdgeSim may be limited by the fact that a lot of <a href="https://github.com/CharafeddineMechalikh/PureEdgeSim/wiki">specific foundational knowledge</a> is required, from a technical standpoint, it is implemented as a simple application, with only a <a href="#how-to-use">few steps to follow to run the simulation</a>.The provided Javadoc documentation and <a href="https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/examples">simulation examples</a> offer proper guidance to the user, and the console directly informs the user of any type of incorrect input. To save time and effort, each module comes with a default implementation (e.g., the Default Mobility Model, the Default Computing Node class, etc.). These off-the-shelf models also provide a highly customizable environment with a multitude of settings, so users can adjust their behavior without changing their original code. These parameters are preset, which also decreases the complexity of the tool.</p>

<br clear="left"/>

### Extensibility

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196666860-fc166c82-21f8-48e3-80a6-342d4c3ae11b.png" align="right" />

<p align="justify">
Another key feature of PureEdgeSim is its high extensibility. It provides code reusability and ensures adherence to software engineering guidelines and principles for improvements, accuracy, and extensibility. Although it uses its pre-built models by default, users can still create and incorporate their customized ones into their simulation scenarios if any of these default implementations do not meet their needs, all while not changing the PureEdgeSim code base. An example is provided below to show how custom models can be effortlessly integrated to the simulation.
</p>

<br clear="left"/>

```js
//Create a PureEdgeSim simulation
Simulation sim = new Simulation();

// To use your custom mobility model instead of the default one:
sim.setCustomMobilityModel(YourCustomMobilityModel.class);

// To change the tasks orchestrator:
sim.setCustomEdgeOrchestrator(YourCustomOrchestrator.class);
		
// To change the computing node class:
sim.setCustomComputingNode(YourCustomComputingNode.class);
		
// To change the tasks generator:
sim.setCustomTaskGenerator(YourCustomTaskGenerator.class); 
		
// To change the network model
sim.setCustomNetworkModel(YourCustomNetworkModel.class); 
		
// To change the simulation manager
sim.setCustomSimulationManager(YourCustomSimulationManager.class); 

// To change the topology
sim.setCustomTopologyCreator(YourCustomTopologyCreator.class); 
		
/* to use the default models you can simply delete or comment those lines */

// Finally, you can launch the simulation
sim.launchSimulation();
```

### Correctness

<p align="justify">
The tool is said to be correct if it fully complies with the stated requirements, which can be verified by checking test cases. To verify PureEdgeSim’s correctness through unit and integration testing, several test cases have been implemented. Nevertheless, this alone does not provide an assessment of which portions of the code were tested and which were not, neither gives a percentage of the overall coverage of tests alongside the project source code. To evaluate the amount of code that the available unit tests cover, code coverage reports were added to the project with the help of the Java Code Coverage Library (JaCoCo). 
It is important to clarify that the concern of such test cases is the technical feasibility of the scenarios and not the evaluation of the simulation outcome. It is simply a matter of testing whether the given results are logically reasonable and not of actually validating them. To validate the software, a case study was conducted using the simulator. <a href="https://app.codacy.com/gh/CharafeddineMechalikh/PureEdgeSim/dashboard?utm_source=github.com&utm_medium=referral&utm_content=CharafeddineMechalikh/PureEdgeSim&utm_campaign=Badge_Coverage">At least 90% of PureEdgeSim code is covered by those tests</a>. This achievement was made possible by testing all the new features and also by the considerable decrease in code duplication since duplicating code brings neglect of testing. </p>

<p align="justify">
In conclusion, we can say that the proposed simulator performs as intended. Every function has finished without abortions and gave the expected output; All the input files were parsed successfully, and all the personalized models were integrated and verified with the predicted results. The plotting of the simulation results gave a reasonable graphical output. In terms of intended errors, such as incorrect inputs, proper error messages were displayed, and correct information was written into the log file. Moreover, the parameters can be modified, stored, and loaded without any error.
</p>

### Reliability 

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196667682-5c506d9f-0b00-455b-b9d1-196f93d7ba76.png" align="right" />

<p align="justify">
Reliability is the ability of a system to operate consistently under specified conditions while meeting all requirements. When it comes to the components of PureEdgeSim, reliability is ensured by extensive exception handling that catches any error occurring at runtime. Consequently, the errors will not cause the execution to stop (unless they are related to critical parameters without which the simulation cannot continue). While testing it, the proposed simulator never froze and satisfied all functional requirements. Furthermore, data reliability is high since the models are read-only and unmodifiable, meaning no data loss is to be feared. The only files that can be modified are the output files and the configuration files, which can be restored easily to the default values. In addition, PureEdgeSim uses two continuous integration services: Travis  and AppVeyor, that automate tests and builds execution on Linux and Windows virtual machines, respectively. PureEdgeSim’s users will benefit from such services as any test or build failure will be communicated promptly, thus, ensuring greater reliability. 
</p>

<br clear="left"/>

<br></br>
![image](https://user-images.githubusercontent.com/46229052/196705113-434c90dc-3c38-40cd-af52-d5ed43774e19.png)
<a id="how-to-use"></a>

<p align="justify">
There are several ways to use PureEdgeSim; however, it is strongly advisable to run PureEdgeSim via a Java development environment, like Eclipse IDE. A set of predefined examples is provided under the <a href="https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/examples">“/examples“</a> directory, which should allow anyone to become familiar with PureEdgeSim.
</p>

### Using an IDE

<p align="justify">
The simplest and recommended method to run this project is to use an IDE like Eclipse, IntelliJ IDEA, or NetBeans. The required steps to get started with the project are listed below:
</p>

<br clear="left"/>

1. Downloading and extracting the project source code archive from the project repository : This can be done using the web browser or the command line `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`.
2. <p align="justify">Importing the project to the IDE:</P>
  - <p align="justify">In NetBeans, simply select the "Open project" option and chose the project directory.</P>
  - <p align="justify">In Eclipse or IntelliJ IDEA, the project must be imported by selecting the location where it was extracted or cloned.</p>
3. <p align="justify">The imported project consists of ten packages: the main application package, the above-mentioned modules, and the examples package. The main application and the modules are the simulator source code that normally should not be modified. The examples, however, are where the users can start.</p>
4. <p align="justify">It is necessary to convert the project into a Maven project in order to download all the required libraries.</p>
5. <p align="justify">Once all the necessary libraries are downloaded, users can start with the most basic examples by running any of the classes located in the <a href="https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/examples">“examples“</a> package.</p>
6. <p align="justify">To build a new simulation scenario, the simplest way is to create another class in this package.</p>
</p>

<p align="center">
  <img  width="500" src="https://github.com/CharafeddineMechalikh/PureEdgeSim/blob/master/PureEdgeSim/files/importingproject.gif">
</p>

<p align="center">Importing PureEdgeSim project (click to zoom)</p>

### Adding PureEdgeSim as a Dependency

It is possible to use PureEdgeSim as a dependency in a Maven project, by inserting the dependency  below into the pom.xml file:
```xml
<dependency>
	<groupId>com.mechalikh</groupId>
	<artifactId>pureedgesim</artifactId>
	<version>5.1.0</version>
</dependency>
```
Or on Gradle :

```groovy
dependencies {
 implementation 'com.mechalikh:pureedgesim:5.1.0'
}
````

### Via Command Line

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196673345-3f36346b-af6a-4925-b1af-613017521508.png" align="right" />

Assuming that git  and maven  are already installed, PureEdgeSim can be run from the command line as follows:
1. First, the project source code must be downloaded by cloning the repository via the command `git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git`.
2. Now that the project is cloned, it can be built using Maven by executing the  `mvn clean install ` command in the directory where it was cloned.
3. Now, the examples can be executed on Windows, Linux, or Mac operating systems, using the command  `mvn exec:java -Dexec.mainClass="package.Class_Name" `. For instance, to execute “Example1”, the command is  `mvn exec:java -Dexec.mainClass="examples.Example1" `


<br></br>
![image](https://user-images.githubusercontent.com/46229052/196705277-26862b5e-5e17-4e22-b973-d1628368b598.png)
<a id="related-work"></a>

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196673852-b25feb52-2f7e-4486-b10d-b7c3cd79cead.png" align="right" />
<p align="justify">
Here are some works that are based on PureEdgeSim to give an idea about its wider applicability and the possibilities it offers. Feel free to edit the readme file and add yours!
</p>

### Rienforcement and Deep Learning Scenarios

1.    <p align="justify">Mechalikh, C., Taktak, H., & Moussa, F. (2020, April). A fuzzy decision tree based tasks orchestration algorithm for edge computing environments. In International Conference on Advanced Information Networking and Applications (pp. 193-203). Springer, Cham. <a href="https://github.com/CharafeddineMechalikh/FDT_based_workload_orchestration">The algorithm can be found here</a>.</p>
2.    <p align="justify">Safavifar, Z., Ghanadbashi, S., & Golpayegani, F. (2021, November). Adaptive Workload Orchestration in Pure Edge Computing: A Reinforcement-Learning Model. In 2021 IEEE 33rd International Conference on Tools with Artificial Intelligence (ICTAI) (pp. 856-860). IEEE.</p>
3.    <p align="justify">Neelakantam, G., Onthoni, D. D., & Sahoo, P. K. (2020). Reinforcement learning based passengers assistance system for crowded public transportation in fog enabled smart city. Electronics, 9(9), 1501.</p>
4.    <p align="justify">Iftikhar, S., Golec, M., Chowdhury, D., Gill, S. S., & Uhlig, S. (2022). FogDLearner: A Deep Learning-based Cardiac Health Diagnosis Framework using Fog Computing. In Australasian Computer Science Week 2022 (pp. 136-144).</p>

### Satellite Edge Computing

5.    <p align="justify">Wei, J., Cao, S., Pan, S., Han, J., Yan, L., & Zhang, L. (2020, June). SatEdgeSim: A toolkit for modeling and simulation of performance evaluation in satellite edge computing environments. In 2020 12th International Conference on Communication Software and Networks (ICCSN) (pp. 307-313). IEEE.</p>
6.    <p align="justify">Qin, J., Guo, X., Ma, X., Li, X., & Yang, J. (2022). Application and Performance Evaluation of Resource Pool Architecture in Satellite Edge Computing. Aerospace, 9(8), 451.</p>

### Data Caching

7.    <p align="justify">Mechalikh, C., Taktak, H., & Moussa, F. (2019, July). Towards a scalable and QoS-aware load balancing platform for edge computing environments. In 2019 International Conference on High Performance Computing & Simulation (HPCS) (pp. 684-691). IEEE.</p>
8.    <p align="justify">Epifâneo, L., Correia, C., & Rodrigues, L. (2021, November). Cathode: A Consistency-Aware Data Placement Algorithm for the Edge. In 2021 IEEE 20th International Symposium on Network Computing and Applications (NCA) (pp. 1-10). IEEE.</p>

### Task Scheduling and Security

9.    Tiburski, R. T. (2021). Task scheduling and security for edge devices in internet of things applications.

### Self-Organized Architectures

10.    <p align="justify">Mechalikh, C., Taktak, H., & Moussa, F. (2019, June). A scalable and adaptive tasks orchestration platform for IoT. In 2019 15th International Wireless Communications & Mobile Computing Conference (IWCMC) (pp. 1557-1563). IEEE.</p>
11.    <p align="justify">Mordacchini, M., Ferrucci, L., Carlini, E., Kavalionak, H., Coppola, M., & Dazzi, P. (2021, September). Self-organizing Energy-Minimization Placement of QoE-Constrained Services at the Edge. In International Conference on the Economics of Grids, Clouds, Systems, and Services (pp. 133-142). Springer, Cham.</p>

### Software-Defined Networking

12.    <p align="justify">Okwuibe, J., Haavisto, J., Kovacevic, I., Harjula, E., Ahmad, I., Islam, J., & Ylianttila, M. (2021). SDN-Enabled Resource Orchestration for Industrial IoT in Collaborative Edge-Cloud Networks. IEEE Access, 9, 115839-115854.</p>

### Latency-Aware Application Placement

13.    <p align="justify">Khosroabadi, F., Fotouhi-Ghazvini, F., & Fotouhi, H. (2021). SCATTER: service placement in real-time fog-assisted IoT networks. Journal of Sensor and Actuator Networks, 10(2), 26.</p>
14.    <p align="justify">Ferrucci, L., Mordacchini, M., Coppola, M., Carlini, E., Kavalionak, H., & Dazzi, P. (2020, June). Latency preserving self-optimizing placement at the edge. In Proceedings of the 1st Workshop on Flexible Resource and Application Management on the Edge (pp. 3-8).</p>

### Energy-Aware Application Placement

15.    <p align="justify">Kavalionak, H., Coppola, M., & Dazzi, P. Self-organizing Energy-Minimization Placement of QoE-Constrained Services at the Edge. In Economics of Grids, Clouds, Systems, and Services: 18th International Conference, GECON 2021, Virtual Event, September 21–23, 2021, Proceedings (p. 133). Springer Nature.</p>
16.    <p align="justify">Mordacchini, M., Ferrucci, L., Carlini, E., Kavalionak, H., Coppola, M., & Dazzi, P. (2022). Energy and QoE aware Placement of Applications and Data at the Edge.</p>

### Other Use Cases

17.    <p align="justify">Kaneko, Y., Yokoyama, Y., Monma, N., Terashima, Y., Teramoto, K., Kishimoto, T., & Saito, T. (2020, September). A Microservice-Based Industrial Control System Architecture Using Cloud and MEC. In International Conference on Edge Computing (pp. 18-32). Springer, Cham.</p>
18.    <p align="justify">Javaid, H., Saleem, S., Wajid, B., & Khan, U. G. (2021, May). Diagnose a Disease: A Fog Assisted Disease Diagnosis Framework with Bidirectional LSTM. In 2021 International Conference on Digital Futures and Transformative Technologies (ICoDT2) (pp. 1-6). IEEE.</p>

<br></br>
![image](https://user-images.githubusercontent.com/46229052/196705340-8324e7b4-6b71-40e5-86b9-6cc148a17bc1.png)
<a id="license"></a>

This project is licensed under [GNU GPLv3](http://www.gnu.org/licenses/gpl-3.0).

<p align="right"><a href="#top">:arrow_up:</a></p>

<img width= "20%" src="https://user-images.githubusercontent.com/46229052/196670788-1b7ff2fc-4958-4645-8254-0ea516e035bc.png" align="left" />
