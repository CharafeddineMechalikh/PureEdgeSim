# Contributing to PureEdgeSim

PureEdgeSim is a research simulator, and most contributions start from an experiment: an orchestration algorithm that should be compared against the default one, a mobility model that matches a real campus, a metric that was counted twice. This page is what a change needs before it is merged. The long version, with coverage details, Javadoc rules, the documentation site and the Maven Central release procedure, is the [contributing guide](docs/contributing-guide.md).

## Extend, do not fork

Do not modify the framework to run your project. Register a subclass through one of the eight setter-based extension points (mobility model, computing node, task generator, edge orchestrator, network model, simulation manager, topology creator, computing nodes generator), and your model keeps its own package while the repository keeps its bug fixes. A fork with personal edits stops receiving them.

Every hook has a runnable example in [`PureEdgeSim/examples`](PureEdgeSim/examples/README.md). Start from the closest one, not from an empty file.

## Quick start

You need a JDK and Maven 3.6 or newer. The sources target Java 8, so JDK 8 through 17 all build them; continuous integration runs 17.

```bash
git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git
cd PureEdgeSim
mvn clean test
mvn exec:java -Dexec.mainClass="examples.Example1"
```

Both commands must be run from the repository root: the default settings live in `PureEdgeSim/settings`, and the examples read their own `settings/` folder relative to it.

## Tests

Tests are JUnit 5 and come in two layers. Unit tests exercise one class or one collaboration. Integration tests run a short, complete simulation and assert on the produced metrics; they are tagged `integration` or live in the `integrationtests` package.

| Command | What runs |
| --- | --- |
| `mvn clean test -Punit-tests` | Unit tests only, integration tags excluded. |
| `mvn clean test -Pintegration-tests` | Everything, unit and integration. |
| `mvn clean test` | The default profile: everything except the `integrationtests` package, tags ignored. |
| `mvn test -Dtest=DefaultOrchestratorTest` | One class, or `Class#method` for one test. |

A failing test fails the build, which is the point. Fix it rather than adding `-Dmaven.test.failure.ignore=true`.

A test that replaces the scheduler, the network or the energy model with a stub proves very little about this simulator. Use real collaborators and a short real run where it is affordable, and keep the scenario small enough that the seed determines the outcome: few devices, a short horizon, a low task rate.

## Code

- **Java 8 only.** No `var`, no records, no text blocks.
- **Match the surrounding file.** Tabs for indentation, an existing style, and no formatting pass over lines you did not otherwise touch.
- **Constructor signatures are contracts.** Models are instantiated by reflection, so changing a parameter list breaks every scenario that loads them.
- **Custom event tags start at 12000.** Lower values belong to the framework.
- **No threads inside models.** Simulation time advances on a single event loop, so blocking it stops the clock.
- **Preserve units.** Seconds, bits per second, megabytes, joules. A unit change is a behavior change and needs a changelog entry.
- **Defaults are results.** Published experiments were produced with the current defaults. Change one knowingly, and say so in the pull request.
- **Verify before documenting.** Describe what the code does today, not what the model is meant to do.

## Documentation and changelog

Behavior changes update the relevant guide in [`docs/`](docs/README.md) in the same change, not in a follow-up. Every user-visible change gets an entry in [`CHANGELOG.md`](CHANGELOG.md); refactors with no observable effect and documentation-only edits do not.

If you add a guide, link it from `docs/README.md`, give it a `description:` front matter line, and add its slug to the sidebar in `_includes/head-custom.html`. Files named `CONTRIBUTING`, `README`, `LICENSE` and `CODE_OF_CONDUCT` are on Jekyll's denylist, so this page is shown by GitHub and not built into the documentation site.

Use sentence case for headings and American spelling, as in this page. Do not use em dashes: use a comma, a parenthesis, or a new sentence.

## Pull request checklist

- [ ] The change does what the description says, and nothing unrelated is mixed in.
- [ ] Tests cover the new behavior, including the branch you expect experiments to hit.
- [ ] `mvn clean test -Punit-tests` passes, and integration tests pass if you touched the simulation loop, the network or the energy model.
- [ ] `mvn javadoc:javadoc` reports no new warnings, if you touched the public API.
- [ ] `CHANGELOG.md` and the matching guide are updated.
- [ ] The description names the command and the seed that reproduce your numbers.

Write the description for a reader who has not seen the branch. State what behavior changed, what it changed from, and how you verified it. If a number moved, give the old and the new one.

Commit messages follow the history of the repository: one imperative line that names the change, for example `Add battery-aware orchestrator example` or `Fix WAN latency counted once per hop`. Do not mix formatting-only changes with functional changes.

## Continuous integration

Three workflows run on GitHub. `coverage.yml` is the one that gates the code: it runs `mvn -B test` on JDK 17 under a virtual display, because a few visualizer tests open a Swing frame, then commits the JaCoCo report and the badges back to `master`. `javadoc.yml` builds the API documentation. `test.yml` is an older JDK 11 duplicate, disabled in the Actions UI.

Because the coverage workflow pushes to `master`, rebase before you push, and confirm the remote advanced afterwards:

```bash
git fetch origin
git rebase origin/master
git push origin master
git log --oneline origin/master -1
```

## Reporting a bug

Include the version from `pom.xml` or the release tag, the settings that reproduce it, the command or example that triggers it, the exception together with the relevant part of `simulation.log`, and what you expected against what happened. For a metric, attach the CSV row and say which column is wrong and what value you expected.

Surprising results are not automatically bugs. Check [Output and metrics](docs/output-and-metrics.md) first: several columns have non-obvious definitions, and tasks still in flight at the horizon are neither successes nor failures.

Questions and bug reports go to the [issue tracker](https://github.com/CharafeddineMechalikh/PureEdgeSim/issues). The planned usability work, as tickable items, is in the [roadmap](README.md#roadmap); pick one and say so in the issue before starting, so two people do not write the same code.

## Code of conduct and license

Participation is governed by [Codeofconduct.md](Codeofconduct.md). Be direct about technical problems, be generous with questions, and assume good faith.

PureEdgeSim is released under the GNU General Public License v3.0, see [LICENSE](LICENSE). By contributing, you agree that your contribution is distributed under the same license. If PureEdgeSim helps your work, cite the papers listed in the [README](README.md#citation) rather than the repository.
