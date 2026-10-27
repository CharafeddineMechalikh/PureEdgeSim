---
description: "How to contribute to PureEdgeSim: build and test commands, coverage, code conventions, Javadoc, documentation, changelog, and the pull request process."
---
# Contributing to PureEdgeSim

PureEdgeSim is a research simulator, and most contributions come from people who use it to run experiments: a new orchestration algorithm, a mobility model that matches a real campus, a fix for a metric that was counted twice. This guide explains how to make such a contribution without breaking the behavior that other researchers depend on.

You do not need to be a software engineer to contribute. You do need to verify what the code actually does before documenting or changing it, and you need to leave the test suite and the changelog in a state that the next person can trust.

## Where things live

| Path | Contents |
| --- | --- |
| `PureEdgeSim/main/java/com/mechalikh/pureedgesim/` | Framework source, one package per module. |
| `PureEdgeSim/examples/ExampleX/` | One folder per runnable scenario: its Java sources, `README.md`, `settings/`, and `output/`. Not part of the published Javadoc. |
| `PureEdgeSim/test/java/com/mechalikh/pureedgesim/` | JUnit 5 tests, mirroring the source packages. |
| `docs/` | This manual. Start at [docs/README.md](README.md). |
| `PureEdgeSim/settings/` | Default configuration files. |
| `PureEdgeSim/output/` | Default output folder, created at run time and ignored by git. |
| `libs/` | Bundled third-party jars (jFuzzyLogic for the optional examples). |
| `resources/` | Images used by the README and the documentation. |
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
mvn -q exec:java -Dexec.mainClass=examples.Example1
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

### Test failures fail the build

Surefire runs the suite with the default failure handling: a failing test makes `mvn test` exit with a non-zero status, and CI goes red. Fix the failure before merging; do not add `-Dmaven.test.failure.ignore=true` to a commit or workflow.

The console summary (`Tests run: ..., Failures: ..., Errors: ..., Skipped: ...`) and the per-class reports in `target/surefire-reports/` remain the fastest way to find what broke.

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

If a test requires a specific fixture, put it under `PureEdgeSim/test/resources/` and load it through the class loader so it works from IDEs and Maven alike.

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

Public classes and methods in `PureEdgeSim/main/java` should have Javadoc that states the contract, not a restatement of the signature.

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

Output goes to `target/site/apidocs/index.html`. The build is configured with `failOnError=false`, which means a green build does not prove the Javadoc is clean. Read the warnings. The release build reports no errors and one warning, about the JDK 1.8 API link running against a modular JDK; treat any new warning as a regression. The `javadoc` workflow runs the same goal on every push and pull request and uploads the generated site as a downloadable artifact; it does not publish to GitHub Pages.

### Publishing the documentation website

GitHub Pages builds the website from the root of the `master` branch with Jekyll. `_config.yml` selects the Cayman theme and enables relative links and the sitemap, so `README.md` becomes the home page, `docs/README.md` becomes `/docs/`, and links between markdown files are rewritten to their `.html` pages. Add a guide as a plain markdown file in `docs/`, give it a `description:` front matter line, link it from `docs/README.md`, push, and it is published. The description is used for the page meta description and social card; GitHub shows it as a one-row table at the top of the file, so keep it to that single key. `docs/README.md` additionally carries `permalink: /docs/` because Jekyll's README index only applies to files without front matter; keep it so the manual stays at `/docs/`. `_config.yml` also sets `kramdown.parse_block_html: true`, so fenced blocks nested inside `<details>` sections stay rendered and highlighted; keep that line if you change the Kramdown options. The two JaCoCo badge SVGs live in `.github/badges/`, a dot directory the Pages build refuses to publish even when `_config.yml` lists it under `include:` (measured on 2026-10-27: `/PureEdgeSim/.github/badges/jacoco.svg` still answered 404 with that line in place), so the README points at absolute `raw.githubusercontent.com` URLs instead. Those render on GitHub and on the site, and always show the badge of the current `master`; keep them absolute. The `_includes/head-custom.html` partial renders fenced mermaid blocks and builds the documentation sidebar, which opens on every page under `/docs/` and on the project home page; when you add a guide, add its slug and label to the `pages` array in that file. Avoid Jekyll's filename denylist for pages (`README`, `LICENSE`, `CONTRIBUTING`, `CODE_OF_CONDUCT`, `ISSUE_TEMPLATE`, `PULL_REQUEST_TEMPLATE`): those files are copied but not rendered.

### Publishing a release to Maven Central

Releases go to the Central Publisher Portal at `central.sonatype.com`. It replaced the OSSRH staging repository at `s01.oss.sonatype.org`, which was retired on 2025-06-30, so the pom no longer carries a `distributionManagement` block and no longer needs one.

The pom publishes under the group id `io.github.charafeddinemechalikh`, because Central validates that every coordinate in a bundle starts with a namespace the uploading account has verified. The older group id `com.mechalikh` was granted in 2021 against the domain `mechalikh.com`. A domain namespace is claimed by publishing a DNS TXT record on that domain, so once the domain expires it cannot be re-verified: uploading a `com.mechalikh` bundle to the Portal on 2026-10-28 passed the pom, signature, sources and Javadoc checks and was rejected with `Namespace 'com.mechalikh' is not allowed`. The GitHub namespace `io.github.<username>` needs no domain, since it is verified automatically for a Portal account created with a GitHub login. Only the pom group id is compared against the namespaces, not the contents of the jars, so the Java packages stay `com.mechalikh.pureedgesim` and existing imports keep working; releases up to 5.1.0 remain downloadable under `com.mechalikh`.

Three things must exist before the upload can succeed:

1. A GPG private key whose public part is reachable on a key server (`gpg --send-keys <fingerprint>`). The Portal verifies every signature against that public key.
2. The fingerprint in `gpg.keyname`, and the key passphrase available to Maven. Passing it through the environment is what worked here; the settings.xml lookup by `passphraseServerId` left the plugin to call `gpg` with `--pinentry-mode error`, which fails with `No pinentry`.
3. A Portal user token, generated once in the web interface (Account, Token Generator) for the account that owns the `io.github.charafeddinemechalikh` namespace, stored in `~/.m2/settings.xml` under a server with the id `central`.

With those in place:

```bash
export MAVEN_GPG_PASSPHRASE="$(cat ~/.gnupg/release_pw.txt)"
mvn -B -Prelease clean deploy -DskipTests -Dgpg.keyname=<fingerprint>
```

The `release` profile runs at the `verify` phase, so `clean deploy` reaches it; `package` alone builds the jars but never signs them, which is easy to mistake for a signed bundle. It attaches the sources and Javadoc jars, signs four artifacts (jar, sources, javadoc, pom), then `central-publishing-maven-plugin` packs them under `target/central-publishing/central-bundle.zip` and uploads it. `<autoPublish>true</autoPublish>` means a bundle that passes validation is released without a click in the Portal.

Without a token the same command still leaves a publishable bundle, because the upload is the only step that fails; measured with placeholder credentials it answers HTTP 401 and `Invalid token`, the build stops after two and a half minutes with `-DskipTests`, and `target/central-publishing/central-bundle.zip` stays on disk with its 24 entries. That zip can be uploaded from the Publishing area of the Portal while signed in to the account that owns the namespace, which is the only way to publish without generating a token. Upload the zip and not the jar: a lone jar is rejected with `Bundle has content that does NOT have a .pom file`, because the pom, the sources, the Javadoc and the four signatures are expected beside it inside the archive. A manual upload always waits for the Publish button, since `<autoPublish>` belongs to the plugin and not to the web form, and publishing cannot be undone, only superseded by a newer version.

Two pom details exist only because of what the Portal checks, and are worth keeping:

- The Javadoc uses `<subpackages>com.mechalikh.pureedgesim</subpackages>` rather than `<excludePackageNames>examples</excludePackageNames>`. Javadoc applies the exclusion only to packages it finds by walking a source path, and since every example folder is added as a compile source root its files are handed over explicitly, so the exclusion silently did nothing. Measured: the subpackages form gives 349 entries and zero `examples/` pages, the exclude form gave 470 entries with 121 of them under `examples/`. Because the examples are now absent, four `@see examples.Example7` tags in `SimulationParameters` became broken references and were replaced by links to the example folder on GitHub.
- `jFuzzyLogic` is declared inside the `local-fuzzylogic` profile, activated by the presence of `libs/jFuzzyLogic_v3.0.jar`, and not in the top-level dependencies. The jar is on no public repository, and a `systemPath` pointing inside this repository resolves on no consumer machine, so the pom that Central serves must not mention it. Example5 and Example8 still compile, because the profile is active whenever the jar is checked out next to the pom.

## Documentation

The manual in `docs/` is part of the project. Changes to behavior should update the relevant guide in the same change, not in a follow-up.

When writing docs:

- Explain the behavior the code implements today. Verify it against the source; do not describe what the model is supposed to do in theory.
- Prefer complete, runnable snippets over fragments with `...`.
- Keep links relative within `docs/`, for example `[Configuration](configuration.md)`.
- Label every fenced block with its language: `java`, `xml`, `properties`, `bash`, `bibtex`, `mermaid`. Use `text` for console output and directory trees, which should stay uncolored. An unlabeled block loses its highlighting on the website.
- Start each guide with a `description:` front matter line of about 155 characters that says what the page covers, in the words a reader would search for. It becomes the search snippet; duplicate descriptions across pages hurt the site.
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

```text
Add battery-aware orchestrator example
Fix WAN latency counted once per hop
```

Do not mix formatting-only changes with functional changes.

### Continuous integration

Three workflows run on GitHub:

- `coverage.yml` is the one that gates the code. It runs `mvn -B test` on JDK 17 under Xvfb, because the visualizer tests open a real Swing frame and abort with `HeadlessException` on a machine with no display. It then generates the JaCoCo badges and commits `jacoco.xml` and `.github/badges` back to `master` on pushes, which is why the README badges are absolute `raw.githubusercontent.com` URLs.
- `javadoc.yml` builds the API documentation and uploads it as a workflow artifact.
- `test.yml` is an older JDK 11 job that duplicates the coverage workflow. It is disabled in the Actions UI, so its badge reports no status; leave it alone unless you mean to re-enable it.

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

## Roadmap

These are the planned usability improvements, in priority order. Tick an item in the same pull request that implements it, and keep the XML formats and the setter-based extension API unchanged so that existing scenarios and published results keep working.

### Phase 1: trust and hygiene

- [x] Make the build fail when a test fails, instead of ignoring test failures.
- [x] Replace the remaining `printStackTrace()` calls in node generation with parser-style errors (file, element, value, cause) that stop the run.
- [x] Move `settings/` and `output/` to the repository root, update the defaults, and remove the legacy `PureEdgeSim/com` and `PureEdgeSim/examples` copies.
- [x] Write a `run.properties` file into each output folder with the resolved settings, the seed, the version, and the git commit.

Acceptance: a broken XML fails with one readable message, and every output folder can be replayed as is.

### Phase 2: one command to run and inspect

- [ ] Add a single `java -jar` entry point that runs a settings folder with overrides and prints a compact result table.
- [ ] Add `validate`, which aggregates all configuration errors, and `explain`, which prints the resolved scenario.
- [ ] Make charts headless-safe and optional, and write a self-contained `report.html` per run.
- [ ] Replace the verbose console stream with a progress line and an ETA, moving the event log behind `--verbose`.

Acceptance: a first-time user runs a scenario, reads the summary, and inspects the plots without editing Java.

### Phase 3: sweeps and reproducibility

- [ ] Add a `sweep` command that runs a parameter grid, merges the rows into one CSV, and plots the comparison.
- [ ] Ship `create` scenario scaffolding and a Maven archetype for custom models.
- [ ] Publish a shaded CLI jar as a release artifact.
- [ ] Add a troubleshooting page that maps common exceptions to their causes and fixes.

Acceptance: a paper-grade sweep is one command, and every figure can be traced back to a stored configuration snapshot.

### Phase 4: extensibility

What is already available:

- [x] Eight setter-based extension points on `SimulationAbstract`, plus `setCustomTaskClass` on the task generator: mobility model, computing node, task generator, edge orchestrator, network model, simulation manager, topology creator, and computing nodes generator.
- [x] A runnable example for every point, so each hook can be copied from working code. [Example 23](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/PureEdgeSim/examples/Example23) covers the four that the earlier examples leave at their defaults.
- [x] Scenario behavior that needs no code: distributions for every numeric parameter, nine mobility models selectable per device type by name, and the architecture and algorithm matrix.
- [x] The constructor signatures, the event-tag range, and the topology-refresh rule documented in [Customizing models](customizing-models.md), with unit and integration tests for the custom models.
- [x] Every model replaced by composition: no compile-time switch, no fork, and no generated code.

What comes next:

- [ ] A model registry so a settings file can name any custom class (`task_generator`, `topology_creator`, and the rest) instead of calling a setter.
- [ ] `create model` scaffolding and a Maven archetype that generate a compilable custom model and its test.
- [ ] A compatibility test that builds and boots every custom model of the examples, catching constructor or API drift before a release.
- [ ] A cookbook page with the smallest working recipe per hook, each one linking to its runnable example.
- [ ] `ServiceLoader` discovery for third-party model jars, so a custom model needs no edit to the scenario class.
- [ ] A documented extension API policy: which base classes and methods stay compatible across minor releases.

Acceptance: a user picks a hook, generates or copies a model, names it in the settings file, and runs it without editing framework code.

## Reporting a bug

Include:

- PureEdgeSim version (the `<version>` in `pom.xml` or the release tag).
- The configuration files that reproduce it, or the smallest relevant excerpts.
- The command or example that triggers it.
- The full exception and the relevant part of `simulation.log`.
- What you expected and what happened.

If the bug is about a metric, attach the CSV row and explain which column is wrong and what value you expected. If it is about a chart, say whether `save_charts` and the real-time charts were enabled and whether the run was sequential or parallel.

Experimental results that look surprising are not automatically bugs. Check [Output and metrics](output-and-metrics.md) first; several columns have non-obvious definitions, and tasks in flight at the horizon are neither successes nor failures.

## Code of conduct

Participation is governed by [Codeofconduct.md](../Codeofconduct.md). Be direct about technical problems, be generous with questions, and assume good faith.

## License

PureEdgeSim is released under the license in [LICENSE](../LICENSE). By contributing, you agree that your contribution is distributed under the same license.
