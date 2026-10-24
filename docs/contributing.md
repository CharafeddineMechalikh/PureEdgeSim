# Contributing to PureEdgeSim

PureEdgeSim is a research simulator, and most contributions come from people who use it to run experiments: a new orchestration algorithm, a mobility model that matches a real campus, a fix for a metric that was counted twice. This guide explains how to make such a contribution without breaking the behavior that other researchers depend on.

You do not need to be a software engineer to contribute. You do need to verify what the code actually does before documenting or changing it, and you need to leave the test suite and the changelog in a state that the next person can trust.

## Where things live

| Path | Contents |
| --- | --- |
| `src/main/java/com/mechalikh/pureedgesim/` | Framework source, one package per module. |
| `src/main/java/com/mechalikh/pureedgesim/examples/` | Runnable scenarios and sample custom models. Not part of the published Javadoc. |
| `src/test/java/com/mechalikh/pureedgesim/` | JUnit 5 tests, mirroring the source packages. |
| `docs/` | This manual. Start at [docs/README.md](README.md). |
| `PureEdgeSim/settings/` | Default configuration files and the default output folder. |
| `.github/workflows/` | Continuous integration: tests, coverage, and Javadoc. |
| `CHANGELOG.md` | Every user-visible change, newest first. |
| `pom.xml` | Build, profiles, dependencies, publishing. |

The package layout and module responsibilities are described in [Architecture](architecture.md).

## Development environment

You need:

- A JDK. The project compiles with `source` and `target` set to 1.8, so any JDK from 8 upward can build it. CI uses JDK 11 and JDK 17, so avoid APIs newer than Java 8 in framework code.
- Maven 3.6 or newer.
- Git.

Verify the setup by compiling and running the test suite:

```bash
mvn clean test
```

A first run downloads dependencies and can take a few minutes. Later runs are faster.

## Building and running

Compile only:

```bash
mvn clean compile
```

Run an example from an IDE by running its `main` method. From the command line, the examples are plain classes with a `main` method, for example:

```bash
mvn -q exec:java -Dexec.mainClass=com.mechalikh.pureedgesim.examples.Example1
```

The `exec` plugin is not configured in the POM, so Maven downloads it on first use. Running examples from the IDE is the path used by most contributors. See [Getting started](getting-started.md) for the expected output layout.

Build the library JAR without tests:

```bash
mvn -DskipTests package
```

## The test suite

Tests use JUnit 5. They are organized in two layers:

- **Unit tests** exercise one class or one collaboration at a time. They live next to the package they test.
- **Integration tests** run a complete, short simulation and assert on the produced scenario and metrics. They are marked with `@Tag("integration")`. The main one is `SimulationIntegrationTest`.

The POM defines three profiles:

| Command | What runs |
| --- | --- |
| `mvn clean test -Punit-tests` | Unit tests only. Integration-tagged tests are excluded. Use this when measuring unit coverage. |
| `mvn clean test -Pintegration-tests` | Unit and integration tests, plus anything in the `integrationtests` package. |
| `mvn clean test` | The default profile. Excludes the `integrationtests` package but does not filter by tag, so tagged tests outside that package still run. Use a named profile when you need a precise subset. |

Run a single test class or method:

```bash
mvn test -Dtest=DefaultOrchestratorTest
mvn test -Dtest=DefaultOrchestratorTest#testRoundRobin
```

Run integration tests only:

```bash
mvn test -Dtest=SimulationIntegrationTest
```

### Test failures do not fail the build

The Surefire configuration sets `testFailureIgnore` to `true`, so `mvn test` exits successfully even when tests fail. This is a historical convenience for IDE users, not a statement that failures are acceptable.

Always read the summary and the reports instead of trusting the exit code:

- Console summary: `Tests run: ..., Failures: ..., Errors: ..., Skipped: ...`
- Per-class reports: `target/surefire-reports/*.txt`
- Machine-readable reports: `target/surefire-reports/TEST-*.xml`

To make failures fail the build for a local run:

```bash
mvn clean test -Dmaven.test.failure.ignore=false
```

CI inherits the same setting, which means a red test can pass the workflow. Review the workflow log, not only the check mark.

### Coverage

JaCoCo runs automatically during `test` and writes:

- `target/site/jacoco/index.html`, the browsable report.
- `target/site/jacoco/jacoco.xml`, the machine-readable report copied to the repository root by CI.

Package folders under `target/site/jacoco/` show line, branch, and instruction coverage per class. The `examples` package and anything under `libs` are excluded from measurement.

Coverage is a diagnostic, not a target to game. A getter with no test is less important than an untested branch in failure accounting. When you add a model with branches (battery limits, routing cases, retry paths), add a test for each branch you expect experiments to hit.

### Writing tests

Follow the existing patterns:

- Use real collaborators and short real simulations where possible. A test that replaces the scheduler, the network, or the energy model with a stub proves very little about this simulator.
- When you need determinism, keep the scenario small (few devices, short horizon, low task rate) and assert on invariants such as "delays are non-negative" or "success plus failure never exceeds generated tasks", not on exact floating point values.
- Use `AbstractTest` when a test needs a configured simulation, a temporary settings folder, or a captured `SimLog`.
- Assert on observable behavior, not on private fields. Avoid reflection in tests unless you are testing serialization or registration.
- Name tests after the behavior: `failedTaskIsCountedOnce`, not `testFailure2`.

If a test requires a specific fixture, put it under `src/test/resources/` and load it through the class loader so it works from IDEs and Maven alike.

## Code conventions

The framework is small and deliberately dependency-light. Match the surrounding code:

- **Java 8 only.** No records, no `var`, no text blocks, no private interface methods.
- **No new runtime dependency** unless the benefit is large and the license is compatible. The project uses JGraphT and JFreeChart. Do not add a logging framework; use `SimLog`.
- **One package per module.** Put new default implementations beside the existing ones, and put user-facing examples in `examples`.
- **Constructor signatures are contracts.** Reflection instantiates models by constructor, so the exact parameter list matters. For example, a computing node is constructed as `(SimulationManager, double mipsPerCore, int cores, double storage, double ram)`. The full list is in [API reference](api-reference.md) and [Customizing models](customizing-models.md).
- **Event tags for custom entities start at 12000.** Values below that range are reserved by the framework. Two entities using the same tag will process each other's events.
- **No threads and no `Thread.sleep` inside models.** The simulator is a single-threaded discrete-event engine; blocking the event loop stops simulation time.
- **Preserve units.** Time is in seconds inside the simulator, bandwidth in bits per second, storage in megabytes, energy in joules unless a method name says otherwise. Do not silently change a unit in a bug fix; that is a behavioral change that needs a changelog entry.
- **Fail loudly at construction, tolerate at runtime.** Invalid configuration should throw during initialization with a message that names the file and field. Runtime model code should follow the framework's null-object conventions (`ComputingNode.NULL`, and so on) rather than throwing `NullPointerException` deep in the event loop.
- **Do not change default behavior casually.** Defaults determined the results of published experiments. If a fix changes them, document it in the changelog and, when relevant, explain the old and new values.

Formatting: use tabs, as the existing sources do. Keep lines readable; the project does not enforce a formatter, so do not reformat whole files in a functional pull request.

## Javadoc

Public classes and methods in `src/main/java` should have Javadoc that states the contract, not a restatement of the signature.

Write:

```java
/**
 * Returns the number of tasks that have finished execution on this node.
 * Tasks that failed before execution are not included.
 *
 * @return completed task count, never negative
 */
public int getTasksCompleted() {
```

Avoid:

```java
/**
 * Gets the tasks completed.
 *
 * @return the tasks completed
 */
```

Rules that the Javadoc tool enforces (and that contributors should run before submitting):

- Every `@param` names a real parameter; every `@return` appears only on non-void methods; every `@throws` names a declared exception.
- Use `{@link Type#member}` for references. Use `.` between package and class, `#` before a member. Do not link a method with the wrong arity.
- Do not use `@return` on constructors or `void` methods.
- Wrap type names that start with an uppercase letter inside `{@code}` when they appear in running text that the tool might interpret as HTML.

Build the Javadoc locally:

```bash
mvn javadoc:javadoc
```

Output goes to `target/site/apidocs/index.html`. The build is configured with `failOnError=false`, which means a green build does not prove the Javadoc is clean. Read the warnings. The codebase currently builds with no documentation warnings, so treat a new one as a regression. The `javadoc` workflow runs the same goal on every push and pull request and uploads the generated site as a downloadable artifact; the GitHub Pages site and its CNAME are not modified.

Generate the Javadoc and source JARs that are attached during deployment:

```bash
mvn -P default package
```

The `examples` package is excluded from published Javadoc.

## Documentation

The manual in `docs/` is part of the project. Changes to behavior should update the relevant guide in the same change, not in a follow-up.

When writing docs:

- Explain the behavior the code implements today. Verify it against the source; do not describe what the model is supposed to do in theory.
- Prefer complete, runnable snippets over fragments with `...`.
- Keep links relative within `docs/`, for example `[Configuration](configuration.md)`.
- Do not use em dashes. Use commas, parentheses, or a new sentence.
- Use sentence case for headings and American spelling, matching the source comments.
- Give every table and code block a reason to exist. Do not restate the same fact in three sections.

If you add a guide, link it from [docs/README.md](README.md) in both the reading paths and the documentation map.

## Changelog and versioning

Every user-visible change gets an entry in `CHANGELOG.md`, under a version heading. The entry describes the effect on users: new behavior, fixed behavior, removed behavior. Internal refactors with no observable effect do not need an entry.

Bump the version only for functional work: new features, behavior changes, bug fixes, performance changes. Documentation, comments, tests, CI maintenance, and README edits do not require a version bump. When you do bump, update these together:

1. `<version>` in `pom.xml`.
2. The new `CHANGELOG.md` heading with the date.
3. The Maven and Gradle snippets in `README.md`, but only after the new version is actually published. Until then, keep the snippet on the latest published release.

The changelog format is a heading followed by bullet points:

```markdown
## Version 5.6 Changelog (Month Dayth Year)

*   Added ...
*   Fixed ...
*   Changed ...
```

Newest version first. Keep the wording concrete enough that a reader can tell whether the change affects their experiments.

## Submitting a change

A good contribution is small enough to review and complete enough to trust:

1. Branch from `master`.
2. Make the change, with tests and documentation in the same branch.
3. Run the relevant tests (`mvn clean test -Punit-tests` at minimum) and read the reports.
4. Run `mvn javadoc:javadoc` if you touched public API, and check for new warnings.
5. Update `CHANGELOG.md`.
6. Describe in the pull request what behavior changed and how you verified it.

Commit messages follow the recent history, an imperative sentence that states the change:

```
Add battery-aware orchestrator example
Fix WAN latency counted once per hop
```

Do not mix formatting-only changes with functional changes.

### Continuous integration

Two workflows run on GitHub:

- `test.yml` runs `mvn test` on JDK 11 for every push.
- `coverage.yml` runs the tests on JDK 17, generates JaCoCo badges, and commits `jacoco.xml` and `.github/badges` back to `master` on pushes.

Because the coverage workflow pushes to `master`, always rebase before pushing your own work:

```bash
git fetch origin
git rebase origin/master
git push origin master
```

After pushing, verify that the remote advanced:

```bash
git fetch origin
git log --oneline origin/master -1
```

A local commit is not a completed push.

## Reporting a bug

Include:

- PureEdgeSim version (the `<version>` in `pom.xml` or the release tag).
- The configuration files that reproduce it, or the smallest relevant excerpts.
- The command or example that triggers it.
- The full exception and the relevant part of `simulation.log`.
- What you expected and what happened.

If the bug is about a metric, attach the CSV row and explain which column is wrong and what value you expected. If it is about a chart, say whether `save_charts` and the real-time charts were enabled and whether the run was sequential or parallel.

Experimental results that look surprising are not automatically bugs. Check [Output and metrics](output-and-metrics.md) first; several columns have documented quirks, and tasks in flight at the horizon are neither successes nor failures.

## Code of conduct

Participation is governed by [Codeofconduct.md](../Codeofconduct.md). Be direct about technical problems, be generous with questions, and assume good faith.

## License

PureEdgeSim is released under the license in [LICENSE](../LICENSE). By contributing, you agree that your contribution is distributed under the same license.
