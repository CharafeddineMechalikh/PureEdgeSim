# Changelog

## Version 6.0 Changelog (October 27th 2026)

*   An application can now declare a service graph: a request generates several
    subtasks linked by dependencies, declared as `<subtask>` elements in
    `applications.xml`. A subtask is submitted only after all the subtasks it
    depends on have returned their results, and a failure propagates to the whole
    subtree. An application without `<subtask>` elements keeps producing exactly one
    independent task per request, with the same fields, timing, and counts as before.
*   Service graphs now model the data and the deadline of a real pipeline. The
    results of each parent are added to the request size of its children, so the
    payload of a dependency travels over the network. The application `latency` is
    drawn once per request and shared by every subtask as an end-to-end deadline:
    a result that returns at or after it fails with `FAILED_DUE_TO_LATENCY`, even
    when the subtask met its own bound.
*   A subtask can be declared `optional="true"`. When one of its dependencies
    fails it is skipped with status `SKIPPED` instead of failing the request, its
    dependents are released without the missing input, and it is reported in its
    own text line and CSV column. An optional subtask must have at least one
    dependency, because an optional root could never be skipped.
*   The report now measures requests, not only tasks: the number generated, the
    percentage that completed successfully, the number that failed, and the
    average end-to-end makespan of the successful ones. The text report has a
    `Requests` block and the CSV has four new columns appended at the end of the
    row (skipped tasks, requests generated, successful requests, average request
    makespan), so the positions of the existing columns do not change.
*   New `Example24` recomputes the tier of every subtask of a service graph every
    30 seconds and replans immediately when a planned tier loses all of its
    nodes. Against the same scenario with a frozen plan, it moves 114 subtasks
    between tiers and lifts the request success rate from 41.7 to 80.1 percent
    while lowering the average request makespan from 6.60 to 5.85 seconds.
*   The report has a new "Tasks not executed because a dependency failed" line in the
    text log and a matching column in the CSV, inserted after the dead-device column.
    Positions of the following CSV columns shift by one.
*   New `Example9` and `Example10` scenarios partition the same service graph. `Example9`
    splits each connected component into balanced partitions with a graph-theory
    heuristic (heaviest step first, then Kernighan-Lin style pair swaps), and maps the
    partitions to mist, edge, and cloud. `Example10` searches the node of every subtask
    with a genetic algorithm whose fitness estimates the pipeline makespan, the result
    transfers, and the load imbalance. Both clear the application placement after each
    decision so that each subtask is routed on its own.
*   New `Example11` replaces the placement decision with a tabular Q-learning agent
    over the cloud, edge, and mist tiers. The agent learns from the total delay of
    each returned task, explores ten percent of the decisions, and picks the least
    loaded node of the chosen tier, so its results can be compared with `TRADE_OFF`
    under the same seed.
*   New `Example12` models multi-access MEC: 300 devices reach four edge data
    centers over Wi-Fi or cellular, and an access-aware orchestrator scores each
    tier from the transfer delay of the device access link, the compute delay,
    and the tasks pending on the node. The cellular and Wi-Fi rates in the
    settings file change the decision.
*   New `Example13` places a smart-home workload over a three-layer fog: 200
    sensors generate tasks, 100 gateways form the mist layer, one edge data
    center covers the neighbourhood, and the cloud is the fallback. The
    `FOG_LAYERS` orchestrator fills the innermost feasible layer first.
*   New `Example14` restricts the Q-learning agent of `Example11` to the mist
    layer. All 200 devices both generate and execute tasks, and the `MIST_ONLY`
    architecture leaves device-to-device placement as the only option.
*   New `Example15` runs the Q-learning agent over four linked edge data centers
    and 500 devices, with the decision delegated to an edge orchestrator. The
    agent learns the layer and a guided heuristic picks the least loaded node
    inside it.
*   New `Example16` replaces epsilon-greedy exploration with a Boltzmann
    (softmax) policy over the cloud, edge, and mist layers for 300 smart-city
    devices. The learning rate, discount, temperature, and failure penalty are
    constants of the orchestrator, and the normalized reward keeps the values
    independent of the task size.
*   New `Example17` replaces the table with a small neural network whose inputs
    include the battery level, the access technology, and the device mobility.
    The agent trains online with a target network, so layers can be valued for
    contexts that never appeared during the run.
*   New `Example18` decides the tier with an ant colony: every tier carries a
    pheromone trail that grows with the latency margin of the tasks it completed
    and evaporates on failure, and a placement is sampled from the trail and the
    inverse of the estimated delay.
*   New `Example19` places each task class with its own objective: the hard
    real-time tasks keep the largest margin before their latency bound, the soft
    real-time ones minimize the normalized delay, and the background ones
    minimize the estimated energy.
*   New `Example20` learns the delay and the energy of each layer in two
    separate value tables and combines them with adjustable weights at decision
    time, so the trade-off between the objectives can be changed without
    retraining.
*   New `Example21` keeps the application containers at the edge in a
    storage-aware least-recently-used cache. A node serves a request from its
    cache when it already holds the application, stores the container when the
    download finishes, and evicts the least recently used entry when its storage
    budget is full. Against a `registry_mode=CLOUD` run, the cache removes 60.2
    percent of the WAN usage and lifts the success rate from 10.49 to 50.77
    percent under the same seed.
*   New `Example22` has edge devices elect their own cluster heads from local
    scores (capacity, battery, stability, neighbor count) and refresh the
    clusters every 30 seconds without a central coordinator. A cluster-aware
    orchestrator places each task inside the origin's cluster, preferring the
    head when the estimated transfer plus execution time is close. Against the
    same scenario with `TRADE_OFF`, it lowers the average task execution time by
    42 percent and the network usage by 24 percent while lifting the success
    rate from 77.83 to 79.94 percent.
*   New `Example23` demonstrates the four model hooks that the other examples leave
    at their defaults: a computing nodes generator that reserves one device for a
    declared type that rounding would drop, a task generator that synchronizes the
    minute bursts of the whole fleet, a topology creator that completes the edge
    ring into a full mesh, and a simulation manager that audits the failures by
    reason when the run ends. Together with the earlier examples, all eight
    setter-based extension points now have a runnable scenario.
*   The repository is organized around `PureEdgeSim/`: the framework sources are in
    `PureEdgeSim/main/java` and `PureEdgeSim/main/resources`, the tests in
    `PureEdgeSim/test`, the default configuration in `PureEdgeSim/settings/`, and the
    default output in `PureEdgeSim/output/`. Each example is a self-contained folder
    under `PureEdgeSim/examples/ExampleN/` that holds its sources, a `README.md`, its
    `settings/`, and an `output/` folder; `PureEdgeSim/examples/README.md` indexes
    them. The README images moved to `resources/` and the bundled jar to `libs/` at the
    repository root. Simulation defaults, the build (`sourceDirectory`), the tests, and
    the workflows follow the new paths.
*   The documentation and the example READMEs now include diagrams of the task
    lifecycle, the service graph tiers, the two partitioning searches, and the
    dynamic replanning comparison.
*   Bumped the project version to 6.0.

## Version 5.9 Changelog (October 25th 2026)

*   Each node now keeps a distinct uplink and downlink. The `DOWN_LINK` orientation
    used to read and write the same field as `UP_LINK`, so an access link could report
    the device as its own source and the per-link counters of the downlink were
    charged to the uplink. Fixes issue #47. Results change: the delays are unchanged,
    but the link that carries each direction now accumulates its own energy and usage.
*   Fixed the wireless energy model charging the wrong direction: transmission now
    charges `transmissionEnergyPerBits` and reception charges `receptionEnergyPerBits`.
    The two branches were swapped, so devices were billed at the rate of the opposite
    direction. Results change for scenarios with asymmetric rates.
*   The `Network usage (s)` column of the CSV report now sums the LAN, MAN and WAN
    usage, as the text report already did. It held the LAN usage alone.
*   Tasks whose generating device died before they were offloaded are now counted in
    the "Tasks not generated due to the death of devices" line instead of being
    reported as failed tasks. The counter was never incremented before.
*   The report counts dead devices with a counter maintained by the energy model
    instead of scanning every device at the end of the run.
*   Device types are now allocated with the largest remainder method: a percentage
    that does not divide the device count evenly no longer truncates the earlier
    types, and the missing devices are no longer filled with copies of the last type.
    Percentages that do not sum to 100 print a warning; the warning itself used to
    raise a formatting exception that aborted the device generation.
*   An arrival pattern whose first gap is below one second no longer schedules the
    first task at time zero, which broke the assumption that a device starts idle.
*   Runtime files moved to the repository root. The default configuration is now read
    from `settings/` and results are written to `output/`; the historical
    `PureEdgeSim/settings` and `PureEdgeSim/output` paths are gone. Example scenario files
    moved to `examples/ExampleX_settings/`, next to the default `settings/` folder.
    Custom paths passed to `setCustomSettingsFolder`, `setCustomFilePath`, and
    `setCustomOutputFolder` are unaffected, but scripts that relied on the old defaults
    must pass the new paths.
*   Removed the legacy `PureEdgeSim/com` and `PureEdgeSim/examples` source copies. They
    duplicated `src/main/java` and were never compiled by the build.
*   Every run now writes `run.properties` into its timestamped output folder, with the
    framework version, the git commit, the random seed, the parallelism flag, and the
    absolute path of each input file. The five resolved settings files are copied into a
    `settings/` subfolder next to the results, so a run can be reproduced after the
    original scenario files change.
*   Errors while reading or instantiating the nodes of an XML file now name the file, the
    element, and the cause, and stop the run. They used to print a stack trace and
    continue with an empty node list.
*   Configuration errors no longer print a stack trace. A settings file that cannot be
    read, a missing or misspelled property, or a value of the wrong type stops the run
    with one console line naming the file, the property or element, and the found
    value. These cases used to fail with a raw `NullPointerException` or
    `NumberFormatException`, or printed a stack trace and continued.
*   A boolean property that is neither `true` nor `false` is now rejected. A value such
    as `ture` used to be read silently as `false`.
*   Unknown property names in `simulation_parameters.properties` print a warning that
    suggests the closest known name when there is one (for example `wan_bandwidht`
    suggests `wan_bandwidth`). The simulation still runs. A missing XML element or
    attribute whose name is close to a known one gets the same hint in its error.
*   The build fails when a test fails. The Surefire configuration no longer sets
    `testFailureIgnore`.
*   The documentation manual is published on GitHub Pages alongside the repository readme.
*   The unit test suite grew from 911 to 927 tests, all passing.
*   Bumped the project version to 5.9.

## Version 5.8 Changelog (October 25th 2026)

*   The default build now compiles `Example1`, the example the quick start tells you to run.
    It was excluded in the compiler configuration although it needs nothing beyond the
    framework itself, so `mvn exec:java -Dexec.mainClass="examples.Example1"` failed with a
    `ClassNotFoundException` after a clean install. `Example5`, `Example8` and
    `Example8FuzzyLogicOrchestrator` stay excluded: the first uses the second, and both need
    the bundled jFuzzyLogic jar. Fixes issue #43.
*   XChart updated from 3.8.0 to 3.8.8, which clears the four medium-severity advisories
    reported against 3.8.0 (GHSA-2h3j-m7gr-25xj, GHSA-6vqp-h455-42mr, GHSA-7grw-6pjh-jpc9,
    GHSA-fg3j-q579-v8x4) and keeps the same 3.8 chart API. Fixes issue #45.
*   The unit test suite is unchanged at 911 tests, all passing, with unit-only coverage of
    99% of instructions and 97% of branches.
*   Bumped the project version to 5.8.


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
