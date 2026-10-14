<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Agent Guide for grails-core

> **IMPORTANT**: This is the Grails Framework source repository (60+ modules), NOT a Grails application.
> For building Grails apps, see `.agents/skills/grails-developer/SKILL.md`.

## Quick Reference

```bash
# Build (no tests)
./gradlew build -PskipTests

# Build single module
./gradlew :grails-core:build

# Run tests
./gradlew :<module>:test
./gradlew :<module>:test --tests "com.example.SomeSpec"

# Style check
./gradlew codeStyle

# Dependency health (duplicate classes on a classpath fail the build)
./gradlew buildHealth

# Out of memory? Set:
export GRADLE_OPTS="-Xms2G -Xmx5G"
```

## Critical Rules

1. **Use `jakarta.*` NOT `javax.*`** - All packages migrated to Jakarta EE 10
2. **Use `@GrailsCompileStatic`** - Not plain `@CompileStatic` in Grails artefact classes
3. **Use `GrailsWebRequest.lookup()`** - For thread-safe request context in tests
4. **No wildcard imports** - Use explicit imports
5. **4 spaces, no tabs** - See `.editorconfig`
6. **Apache license header** - Required on all new source files
7. **New features require docs** - Any user-facing change must include or update documentation in `grails-doc`; do not merge features without corresponding doc coverage
8. **No internal APIs in docs** - Only document public APIs; never reference internal or package-private classes and methods in user-facing documentation
9. **Test via public APIs** - Tests must exercise behavior through the same APIs an end user calls; never invoke internal implementations, package-private methods, or bypass the public surface directly
10. **Always review and extend tests** - Review existing unit and functional tests before making changes; every code change must include new or enhanced tests that cover the affected behavior
11. **The BOM must manage the latest version** - `validateDependencyVersions` enforces that the BOM (`dependencies.gradle`) manages a version `>=` every transitively-resolved version. When it fails, **bump the version in `dependencies.gradle`** so the BOM wins — never silence it with `allowedBomOverrides` or an exclusion unless there is an explicit, documented conflict or an agreed-upon workaround. See [Dependency Management](#dependency-management).
12. **GitHub Actions must use ASF-approved references** - Every third-party action SHA must appear in the ASF allowlist; `actions/*` use the full tag of their latest release and `apache/*` use version or branch references, never SHAs. See [GitHub Actions](#github-actions).
13. **Workflows cache Gradle through one policy** - Every `setup-gradle` step takes `cache-read-only` from `.github/actions/gradle-cache-policy`; never write `true` or `false` there, never cache `~/.gradle` with `actions/cache`, and never pass `cache-writer` outside the core build. See [Gradle caching](#gradle-caching).

## Available Skills

> **AI AGENTS - MANDATORY**: Before writing or modifying any code, you **MUST** read the relevant skill file(s) below. Do not write Groovy/Grails code without first loading these instructions:
> - Writing Grails code → Read `.agents/skills/grails-developer/SKILL.md`
> - Writing Groovy code → Read `.agents/skills/groovy-developer/SKILL.md`
> - Writing Java code → Read `.agents/skills/java-developer/SKILL.md`
>
> Use your file reading capability to load the skill content before proceeding with any code changes.

| Skill | Path | Use For |
|-------|------|---------|
| **grails-developer** | `.agents/skills/grails-developer/SKILL.md` | Grails 7 apps, GORM, controllers, views |
| **groovy-developer** | `.agents/skills/groovy-developer/SKILL.md` | Groovy 4 syntax, closures, DSLs, Spock |
| **java-developer** | `.agents/skills/java-developer/SKILL.md` | Java 17 features, Groovy interop |

## Technology Stack

| Component | Version |
|-----------|---------|
| JDK | 17+ (baseline 17) |
| Groovy | 4.0.x |
| Spring Boot | 3.5.x |
| Spring Framework | 6.2.x |
| Spock | 2.3-groovy-4.0 |
| Gradle | 8.14.x |
| Jakarta EE | 10 |

## Project Structure

This repository contains multiple independent Gradle projects:

| Project | Description | Build Command |
|---------|-------------|---------------|
| **grails-core** (root) | Main framework with 60+ modules | `./gradlew build` |
| **build-logic/** | Gradle convention plugins for the build | `cd build-logic && ../gradlew build` |
| **grails-gradle/** | Grails Gradle plugins | `cd grails-gradle && ./gradlew build` |
| **grails-forge/** | Application generator (like Spring Initializr) | `cd grails-forge && ./gradlew build` |

Each project has its own `settings.gradle` and independent build. When working on a specific project, run Gradle commands from that project's directory.

## Dependency Management

All managed dependency versions live in `dependencies.gradle` (the single source of truth for the BOM projects). The `validateDependencyVersions` task — run automatically in CI — enforces the rules below.

- **The BOM must manage the latest (winning) version.** Validation fails when a transitive dependency resolves to a version *newer* than the BOM manages. The fix is to **bump the version in `dependencies.gradle`** so the BOM's version is `>=` everything on the classpath and stays authoritative. This is the *purpose* of the check — keeping the BOM ahead of its transitives.
- **Do not suppress validation to work around a bump.** `allowedBomOverrides` (per-project ext) and dependency exclusions are reserved for an explicit, documented conflict or an agreed-upon workaround — never as a shortcut to silence a version the BOM should simply manage. Comment the reason when you must use one.
- **A dependency managed in more than one BOM must use the *same* version everywhere.** Versions appear in `gradleBomDependencyVersions` (build tooling / `grails-gradle-bom`), `bomDependencyVersions` (`grails-bom`), and per-BOM `customBomVersions` blocks (e.g. `grails-micronaut-bom`). `grails-bom` re-declares the gradle-BOM constraints, and the Micronaut/Hibernate BOMs are consumed via `enforcedPlatform`. Declaring one coordinate (e.g. `org.ow2.asm:asm`) at two different versions across these maps produces irreconcilable strict constraints and breaks `enforcedPlatform` resolution. Pin it once, consistently.
- **Prefer inheriting from the Spring Boot BOM.** Do not re-pin a coordinate that `spring-boot-dependencies` (3.5.x) already manages unless you are intentionally overriding it to a newer version (e.g. a security fix); note the reason inline.
- **No two dependencies may provide the same class.** The `buildHealth` task (the `com.autonomousapps.build-health` settings plugin, configured in `gradle/dependency-analysis.settings.gradle` and applied to every build in the repository) fails when two dependencies on a classpath provide the same fully qualified class name that the module's own code references, because compile and runtime behavior then depend on classpath order. The Build Health workflow runs it on every push and pull request, alongside the code style checks. Resolve a report by removing or excluding the redundant dependency; its other categories (unused or transitive dependencies, wrong configurations) are advice only.

## Key Modules

**Core**: `grails-core`, `grails-bootstrap`, `grails-spring`, `grails-common`

**Web**: `grails-web-core`, `grails-web-mvc`, `grails-controllers`, `grails-url-mappings`, `grails-interceptors`

**GORM**: `grails-datastore-core`, `grails-datamapping-core`, `grails-domain-class`, `grails-validation`, `grails-databinding`

**Views**: `grails-views-core`, `grails-views-gson`, `grails-views-markup`

**Testing**: `grails-testing-support-core`, `grails-testing-support-web`, `grails-geb`

**Other**: `grails-bom` (dependency management), `grails-doc`, `grails-shell-cli`, `grails-forge`

## Artefact Types

| Type | Pattern | Handler |
|------|---------|---------|
| Domain | `**/domain/**/*.groovy` | `DomainClassArtefactHandler` |
| Controller | `**/*Controller.groovy` | `ControllerArtefactHandler` |
| Service | `**/*Service.groovy` | `ServiceArtefactHandler` |
| TagLib | `**/*TagLib.groovy` | `TagLibArtefactHandler` |
| Interceptor | `**/*Interceptor.groovy` | `InterceptorArtefactHandler` |

## Code Patterns

### Spock Test Structure
```groovy
class MyServiceSpec extends Specification implements ServiceUnitTest<MyService> {
    def "feature description"() {
        given: "preconditions"
        def input = "test"

        when: "action"
        def result = service.process(input)

        then: "assertions"
        result != null
        result.status == "OK"
    }
}
```

### Mocking
```groovy
def repo = Mock(BookRepository)
1 * repo.save(_) >> savedBook  // expect 1 call, return savedBook
```

### Data-Driven Tests
```groovy
@Unroll
def "#a + #b == #c"() {
    expect: a + b == c
    where:
    a | b || c
    1 | 2 || 3
    4 | 5 || 9
}
```

### Framework Access
```groovy
// Thread-safe request context
GrailsWebRequest webRequest = GrailsWebRequest.lookup()

// Artefact registry
grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
```

## Groovy Style

```groovy
// DO: Safe navigation
book?.author?.name

// DO: Elvis operator
name ?: 'Unknown'

// DO: GStrings
"Hello ${user.name}"

// DO: Spread operator
books*.title

// DO: Static compilation
@GrailsCompileStatic  // or @CompileStatic for non-artefact classes
class MyService { }

// DON'T: Wildcard imports
// import java.util.*  ❌

// DON'T: javax packages
// import javax.servlet.*  ❌ → use jakarta.servlet.*

// DON'T: Section separator or grouping comments
// // --- Domain classes ---  ❌
// // ===== Helpers =====     ❌
```

## Test Isolation

> **WARNING**: Tests run in parallel (`maxParallelForks > 1`). Static state that is not properly reset in test cleanup can cause flaky tests in subsequent tests within the same fork.

- Use `GrailsWebRequest.lookup()` for thread-local context
- Clear artefacts: `grailsApplication.artefactInfo.clear()`
- Use `@Shared` for fields that should be reused by multiple feature methods in a Spec

## Build Commands

| Task | Command |
|------|---------|
| Build (no tests) | `./gradlew build -PskipTests` |
| Build module | `./gradlew :grails-core:build` |
| Test module | `./gradlew :grails-core:test` |
| Single test | `./gradlew :module:test --tests "pkg.MySpec"` |
| Single feature | `./gradlew :module:test --tests "pkg.MySpec.feature name"` |
| Force rerun | `./gradlew :module:test --rerun-tasks` |
| Style check | `./gradlew codeStyle` |
| Dependency health | `./gradlew buildHealth` |
| Build docs | `./gradlew :grails-doc:publishGuide -x aggregateGroovydoc` |
| Debug | `./gradlew bootRun --debug-jvm` |

## GitHub Actions

Apache GitHub Actions policy blocks third-party actions unless they are on the organization allowlist. A workflow that uses an unlisted `uses:` SHA fails at **startup** before any job runs.

The allowlist source of truth is:

https://github.com/apache/infrastructure-actions/blob/main/approved_patterns.yml

Rules:

- Pin every third-party action, including `github/*`, to a **full commit SHA** that appears in that file, with a trailing `# version` comment.
- `actions/*` and `apache/*` (including our own `apache/grails-github-actions`) are allowed by namespace, so every version is approved. Reference `actions/*` by the full tag of the latest release (for example `actions/checkout@v7.0.1`, not `@v7`), and our own actions by branch (for example `apache/grails-github-actions/pre-release@asf`). Never pin either by SHA.
- Do not use a newer third-party SHA, tag, or major version until it is on the allowlist. If you need a new pin, open a PR against `apache/infrastructure-actions` (`actions.yml`, not the generated `approved_patterns.yml`).
- Before adding or bumping a third-party `uses:` line, search `approved_patterns.yml` for that action and copy an approved SHA.

### Gradle caching

GitHub keeps about 10 GB of Actions cache per repository and evicts the least recently used entries. A run can restore entries only from its own branch and the default branch, and a pull request also from its base branch. The workflows are arranged around those two limits:

- The Gradle User Home is cached only by `gradle/actions/setup-gradle`, with its default provider. Do not add an `actions/cache` step for `~/.gradle`, `cache-disabled`, or `cache-provider: basic`; release verification is the one job left uncached on purpose.
- Every `setup-gradle` step is preceded by the local action `.github/actions/gradle-cache-policy` and takes `cache-read-only` from its `read-only` output. Do not write `true` or `false` there: the policy lives in that action, so changing it means editing one file on the oldest release branch and merging up.
- Only the core build job (`build` in `gradle.yml`) passes `cache-writer`, from the one matrix entry per OS flagged `cache_writer: true`. A new job or workflow never passes it.
- The action lets a writer save only on a push or dispatch, only on a branch whose major version is at least the default branch's, and, for Windows and macOS, only on that major's first branch (`x.0.x`). With 8.0.x as the default branch, 7.x never writes, Linux writes on every 8.x and 9.x branch, and Windows and macOS write on 8.0.x and 9.0.x. The rule follows the default branch: when a new one is promoted, older majors stop writing without an edit.

Why it is this way:

- When every job saved on every push (about 25 entries and 3.5 GiB per push on 8.x), the cache stayed over its limit and evicted entries within hours. Jobs then started with an empty Gradle User Home and downloaded everything, and a single transient "Could not find" from Maven Central or the Gradle plugin portal failed them. Gradle does not retry a "not found".
- Minor branches cannot read their major's entries, only the default branch's, so an older major's entries serve only the branch that wrote them. Writing them took space the active lines needed.
- `setup-gradle` restores the newest entry whose key starts with the job's id before it falls back to the newest entry for the OS. With many writers, the core `build` job restored the `buildForge` job's entry and lacked artifacts only the core build uses. One writer per OS leaves one candidate.
- A writer saves about 0.9 GiB per OS and branch, so the rule keeps the cache near 7 GiB.

## Branch Naming (Auto-Labels PRs)

| Prefix | Label |
|--------|-------|
| `fix/` | bug |
| `feat/`, `feature/` | feature |
| `docs/` | documentation |
| `chore/`, `refactor/`, `test/`, `ci/`, `perf/`, `build/` | maintenance |
| `deps/` | deps |

## Pull Request Guidelines

1. **Fork & branch** from the target release branch (e.g., `7.0.x`)
2. **Run tests** before submitting: `./gradlew build --rerun-tasks`
3. **Run code style checks**: `./gradlew codeStyle`
4. **Run dependency health checks**: `./gradlew buildHealth`
5. **Squash commits** into a single meaningful commit message
6. **Reference issues** in PR description (e.g., "Fixes #1234")

### Review Process

| Change Type | Review Policy | Reviewers | Wait Period |
|-------------|---------------|-----------|-------------|
| Build/CI changes | Commit then Review | - | - |
| Documentation | Commit then Review (obvious fixes) | 1 minimum | - |
| Groovy/Spring dependency changes | Review then Commit | 2-3 required | 3 days (weekend) / 1 day (weekday) |
| All other changes | Review then Commit | 1 required | - |

See `CONTRIBUTING.md` for full details.

## Common Issues

| Problem | Solution |
|---------|----------|
| Out of memory | `export GRADLE_OPTS="-Xms2G -Xmx5G"` |
| Container missing | Use `-PskipTests` or install Docker/Podman |
| Flaky tests | Check static state pollution, ensure proper cleanup in tests |
| Cache issues | `./gradlew --rerun-tasks` |
| Deprecation details | `./gradlew <task> --warning-mode all` |


### Reporting Vulnerabilities

Please see the page of the [ASF Security Team](https://www.apache.org/security/) for further information and contact information.

## Resources

- **Grails 7 Guide**: https://grails.apache.org/docs/latest/guide/single.html
- **Groovy 4 Docs**: https://groovy-lang.org/documentation.html#all-versions (select latest 4.0.x)
- **Spock 2.3 Docs**: https://spockframework.org/spock/docs/2.3/all_in_one.html
- **GORM Docs**: https://grails.apache.org/docs/latest/grails-data/
- **Issues**: https://github.com/apache/grails-core/issues
- **Slack**: https://grails.slack.com
