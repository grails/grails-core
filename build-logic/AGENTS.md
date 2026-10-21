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

# Agent Guide for build-logic

> **IMPORTANT**: `build-logic` is the Gradle convention-plugin layer that the root build, `grails-gradle` and
> `grails-forge` consume through `includeBuild`. It is build tooling, not Grails application code, so two of the
> root rules do not apply here (see below). Root [`AGENTS.md`](../AGENTS.md) still governs PR, branch and review
> conventions and repository-wide policy.

## Quick Reference

Run from inside `build-logic/`:

```bash
./gradlew build                     # all three projects
./gradlew :build-logic:test         # the convention plugins (Spock and TestKit specs)
./gradlew :grails-docs-core:test    # the documentation engine
```

## Rules That Differ From Root

1. **Use plain `@CompileStatic`, not `@GrailsCompileStatic`.** Nothing here is a Grails artefact; no source under this directory uses `@GrailsCompileStatic`.
2. **`javax.inject.Inject` is correct here.** Gradle's constructor injection uses JSR-330's `javax.inject`, which is unrelated to the Jakarta EE migration. Do not change it to `jakarta.*`.
3. **Versions are not pinned here.** `plugins/build.gradle` and `docs-core/build.gradle` apply the root's `dependencies.gradle`; do not hardcode a version in this build.
4. **There is no dedicated CI job for this build.** Every other build includes it and exercises the plugins through its own CI runs, so a missing job is not a sign of neglect.

## Project Structure

| Directory | Gradle project | Role |
|---|---|---|
| `plugins/` | `:build-logic` | The convention plugins (19 plugin ids registered in `plugins/build.gradle`), the repository-conventions and violation-aggregation tasks |
| `docs-core/` | `:grails-docs-core` | The user-guide generation engine and the BOM extraction used by the documentation builds |
| `vulnerability-scan/` | `:vulnerability-scan` | The dependency vulnerability scan run by the Vulnerability Scan workflow |

The `gradle-developer` skill (`../.agents/skills/gradle-developer/SKILL.md`) covers the convention plugins, the BOM
platforms and the Gradle 9 task-configuration rules; read it before changing a plugin.

## Keep Other Documents In Step

Root `AGENTS.md` and the `hibernate-developer` and `violation-fixer` skills describe behaviour implemented by plugins in
this build: the code-analysis and violation reports, the repository-conventions check, JaCoCo aggregation. When a plugin's
behaviour changes, update those documents in the same change.
