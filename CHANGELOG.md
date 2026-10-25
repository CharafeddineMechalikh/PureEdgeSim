# Changelog

## Version 5.7 Changelog (October 25th 2026)

*   Added the `distribution` package: sixteen named probability distributions
    (constant, uniform, triangular, normal, log-normal, exponential, gamma,
    Erlang, Weibull, Pareto, beta, Poisson, Bernoulli, geometric, binomial and
    categorical), the `BoundedDistribution` and `ScaledDistribution` wrappers, and
    the `SpecialFunctions` helpers behind the gamma and beta laws.
*   Every numeric parameter of the scenario files may now be a distribution
    instead of a fixed number. Task sizes, task length, latency, generation rate,
    device resources, speed, pause durations, battery level and node coordinates
    all draw their values, so a scenario can model heterogeneous hardware and
    bursty traffic rather than an average device replayed a thousand times.
*   Added the optional `<arrival>` element to `applications.xml`, which replaces
    the evenly spaced task pattern of each minute by a stochastic arrival process.
*   The reports print the mean of each distribution, so a randomized scenario
    stays comparable with the fixed-value scenario it was derived from.
*   Added eight movement models: random walk, Gauss-Markov, Manhattan, waypoint
    list, circular, Levy walk, Brownian motion and reference point group. They are
    selected per device type with `<mobility model="...">`, and
    `MobilityModelFactory` resolves the built-in names, their aliases, or the name
    of a custom class.
*   Extended `MobilityModel` with what the new models need: case insensitive typed
    option accessors, step distance, tick count, and the geometry helpers
    `moveTowards`, `reached`, `randomInRange` and `reflectLocation`.
*   Devices can be placed around a point rather than uniformly over the map, by
    giving distribution-valued coordinates to a `<location>` element, which is how
    a hotspot of users is modeled.
*   Added the `random_seed` parameter and the `Randomness` service behind it. A
    seeded run is reproducible; each parallel scenario keeps its own stream, so
    scenarios that run at the same time do not replay the same draws.
*   Fixed a `nextInt(0)` failure when the pool of generating devices ran out, and
    the same failure when a device was configured with equal minimum and maximum
    pause or mobility durations.
*   The default mobility model no longer waits for `SecureRandom.getInstanceStrong()`
    at startup; every model now draws from the single `Randomness` stream, so a run
    is reproducible from the seed and the location tables are generated faster.
*   The scalar setters of `Application` also reset the matching distribution, so
    assigning a fixed value to a parameter removes its randomness.
*   Configuration errors now name the file, the element and the offending value
    instead of failing with a bare exception message.
*   Extended the automated test suite from 523 to 911 unit tests, covering every
    distribution, every movement model, and the parsing of distribution-valued
    scenario parameters. Unit-only coverage is 99% of instructions and 97% of
    branches.
*   Bumped the project version to 5.7.

## Version 5.6 Changelog (October 25th 2026)

*   Rewrote the shipped simulation parameter file with sectioned documentation covering units, defaults, valid ranges, and runtime effects for every parameter.
*   Applied the same structure to the Example 3, 6, 7, and 8 settings, preserving their scenario-specific values and notes; removed the unused `initialization_time` key from Example 7.
*   Corrected misleading guidance for `edge_devices_range`, `edge_datacenters_coverage`, `deploy_orchestrator`, and `registry_mode`.
*   Changed the default `registry_mode` from `CACHE` to `CLOUD` so that enabling the registry works with the default network model. `CACHE` remains available through Example 7's custom network model.
*   Documented the test settings file and synchronized the duplicated example settings directories.
*   Bumped the project version to 5.6.

## Version 5.5 Changelog (October 24th 2026)

*   Added a comprehensive JUnit 5 test suite (500+ tests) covering the simulation engine, network, energy, mobility, data centers, task generation, orchestration, and visualization packages.
*   Reached 98% instruction and 94% branch coverage with JaCoCo.
*   Added a Maven `unit-tests` profile that excludes integration-tagged tests, allowing unit-only coverage measurement.
*   Added a GitHub Actions coverage workflow that runs the tests and updates the JaCoCo coverage badges and report.
*   Removed the non-functional Codacy coverage reporter and its coverage badge. The code quality badge remains.
*   Fixed a null check in `Event.compareTo`.
*   Added a `docs/` manual for new researchers, covering installation, architecture, configuration, the task lifecycle, models, orchestrator development, output metrics, a research guide, an FAQ, an API reference, and contribution guidelines.
*   Linked the manual and the API documentation from the README.
*   Expanded Javadoc across the public API and configured the Javadoc plugin to report every warning during the build.
*   Added a Javadoc workflow that builds the API documentation and uploads it as a workflow artifact, leaving the GitHub Pages site and its CNAME unchanged.

## Version 5.3 Changelog (April 4th 2024)

*   Improved code quality (Listener Design Pattern)
*   Switched back to Java 8 for broader compatibility

## Version 5.2 Changelog (June 11th 2023)

*   Improved performance
*   Improved code readability

## Version 5.1 Changelog (October 13th 2022)

*   Updated the energy model with the ability to set an initial battery level for generated devices, either programmatically or via the xml file. 
*   Now users can extend the simulation manager to change its behavior, and easily integrate it in the simulation.
*   Users can now easily integrate a custom task class.
*   All entities are now notified at the end of the simulation through onSimulationEnd() method.
*   Failure Model: now tasks can fail due to insufficient resources: RAM, STORAGE, CPU Cores.
*   Disabled some unnecessary network links events/loops for better performance.
*   Now tasks can be scheduled in batches instead of being scheduled all at once at the beginning of the simulation (hence, decreasing the number of events and therefore, faster simulations and less memory usage).
*   Removed unnecessary loops when orchestrating tasks (e.g., no need to browse through edge devices when the cloud only architecture is used, etc.) 
*   Overall version 5.1 can be 30 times faster and supports up to 8 times the number of tasks compared to previous version 5.0.
*   More extensibility: Users can now easily incorporate a custom task class, custom simulation manager, as well as a custom topology creator.
*   Now applications placement decisions are done after each failure (default behavior, can be changed by extending the DefaultSimulationManager and DefaultOrchestrator classes).
*   Reduced duplication and improved code quality.
*   Other fixes..

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
*   Latency is now more realistic and independent of the update interval (The latency-sensitivity can be defined in milliseconds, and the update interval has no effect on the failure rate of latency-sensitive tasks).
*   Similarly, energy measurement is now independent of the update interval, no need to decrease the update interval (which increases simulation time) to get precise energy measurement.
*   This independence from the update interval means that it can be set to high, which decreased the simulation time further, without reducing its accuracy.
*   Improved reliability (More exceptions handling + proper guidance).
*   Implemented the Null Object Design Pattern in order to avoid NullPointerException.

## Version 4.2 (May 31st 2021)

*   Upgraded to CloudSim Plus 6.2.7
*   Updated dependencies 
*   Improved code quality 

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

## Version 2.2.3 (nov 8th 2019) 

*   Now you can add applications easily.

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

*   The code has been revisited and cleaned, now it is more readable  
*   New mobility model and new parameters for mobility update: Now it uses speed in m/s instead of intervals. The new mobility model works on demand, instead of generating a list for each device containing all its location changes (from the beginning of the simulation). The egde devices will request the next location only when needed (which reduces the use of memory)
*   New and more realistic energy model and new energy parameters 
*   Added initialization time to simulation parameters (in order to ignore the time when the resources are being generated)
*   Some bugs here and there has been fixed 
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
