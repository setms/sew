# Gradle Tooling API — Research & Integration Plan for `GradleBuildTool`

## Overview

The Gradle Tooling API is Gradle's official embedding API, designed for use by IDEs and other
build tools that need to interact with Gradle programmatically. It supports running builds,
fetching project models, and listening to build progress — all without spawning a raw process.

Key properties:
- Forward- and backward-compatible across Gradle versions.
- Each Gradle release ships a matching Tooling API artifact with the same version number.
- `ProjectConnection` instances are thread-safe; `GradleConnector` instances are not.

---

## Dependency

Add to `swe/build.gradle` (matches the project's own Gradle version from
`gradle/wrapper/gradle-wrapper.properties`):

```groovy
implementation "org.gradle:gradle-tooling-api:9.3.1"
```

The artifact is published on Maven Central.

---

## Key API Classes

| Class / Interface        | Purpose                                                                   |
|--------------------------|---------------------------------------------------------------------------|
| `GradleConnector`        | Entry point. Configure the project directory and Gradle distribution.     |
| `ProjectConnection`      | Thread-safe connection to one project. Run tasks or fetch project models. |
| `BuildLauncher`          | Configure and execute a build via `forTasks()`, `withArguments()`, `run()`.|

---

## Minimal Usage Pattern

```java
try (var connection = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)           // java.io.File pointing to project root
        .useGradleVersion("9.3.1")                 // download/use cached Gradle 9.3.1
        .connect()) {
    connection.newBuild()
        .forTasks("init")
        .withArguments(
            "--type",         "java-library",
            "--dsl",          "groovy",
            "--java-version", "25",
            "--project-name", projectName,
            "--no-split-project",
            "--use-defaults"
        )
        .setStandardOutput(OutputStream.nullOutputStream())
        .setStandardError(OutputStream.nullOutputStream())
        .run();
}
```

`useGradleVersion("9.3.1")` solves the bootstrap problem: when a project is being initialised
there is no wrapper yet in the target directory, so the connector must be told which Gradle to
use explicitly.

---

## `gradle init` Arguments

| Argument                  | Effect                                                                   |
|---------------------------|--------------------------------------------------------------------------|
| `--type java-library`     | Generates a Java library project skeleton                                |
| `--dsl groovy`            | Groovy DSL (`build.gradle` instead of `build.gradle.kts`)                |
| `--java-version 25`       | Sets the Java toolchain target to Java 25                                |
| `--project-name <name>`   | Uses the project's title as the root project name                        |
| `--no-split-project`      | Single-module layout (no `app/` subdirectory). Available since Gradle 8. |
| `--use-defaults`          | Skips all interactive prompts. Available since Gradle 7.6.               |

Running `gradle init` with these arguments generates:

```
build.gradle
settings.gradle
gradlew
gradlew.bat
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

These are exactly the seven files that iteration 10 of the e2e test expects.

---

## Integration Plan for `GradleBuildTool`

### Context

`GradleBuildTool` (`swe/.../code/java/GradleBuildTool.java`) currently:
1. Validates that `build.gradle` and `settings.gradle` exist.
2. When the `gradle.generate.build.config` suggestion is applied, writes those two files from a
   hardcoded template (`BUILD_GRADLE_CONTENT`).

The e2e test (iteration 10) expects seven files (the two above plus wrapper files and
`libs.versions.toml`). Those expected output files currently hold the placeholder `<anything>`
because the generation is not yet implemented.

### Files to Change

| File                                              | Change                                                        |
|---------------------------------------------------|---------------------------------------------------------------|
| `swe/build.gradle`                                | Add `gradle-tooling-api:9.3.1` dependency                    |
| `swe/.../code/java/GradleBuildTool.java`          | Replace manual file generation with Tooling API `init` call  |
| `swe/src/test/resources/e2e/10/outputs/*`         | Replace `<anything>` placeholders with actual generated files |

### Suggested `applySuggestion()` Implementation

```java
case GENERATE_BUILD_CONFIG -> {
    var projectDir = toFile(resource);  // resolve actual filesystem path from Resource
    try (var connection = GradleConnector.newConnector()
            .forProjectDirectory(projectDir)
            .useGradleVersion("9.3.1")
            .connect()) {
        connection.newBuild()
            .forTasks("init")
            .withArguments(
                "--type",         "java-library",
                "--dsl",          "groovy",
                "--java-version", "25",
                "--project-name", projectName,
                "--no-split-project",
                "--use-defaults"
            )
            .setStandardOutput(OutputStream.nullOutputStream())
            .setStandardError(OutputStream.nullOutputStream())
            .run();
    }
    yield appliedSuggestion(resource,
        "build.gradle",
        "settings.gradle",
        "gradlew",
        "gradlew.bat",
        "gradle/libs.versions.toml",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties");
}
```

The `BUILD_GRADLE_CONTENT` constant and the manual `writeTo` calls are removed; `gradle init`
owns all file generation.

### How to Resolve a `Resource` to a `java.io.File`

The `Resource` abstraction in the `km` module wraps the filesystem. Check whether `Resource` (or
`DirectoryWorkspace`) exposes a `toFile()` or `root()` method that returns a `java.io.File`.
If no direct method exists, the path obtained from `resource.path()` can be resolved against the
workspace root directory.

### Validation

The existing `validate()` method checks for `build.gradle` and `settings.gradle`. No change is
required there — those files are still generated as part of `init`.

### e2e Test Update Workflow

1. Implement the changes above.
2. Run: `./gradlew :swe:test --tests "org.setms.swe.e2e.EndToEndTest"`
3. Inspect `swe/build/e2e/` for the files the tool actually generated.
4. Copy those files verbatim into `swe/src/test/resources/e2e/10/outputs/`.
5. Run the test again — it must pass.

---

## Considerations and Caveats

| Topic | Detail |
|---|---|
| **First-run latency** | `useGradleVersion("9.3.1")` downloads the Gradle distribution (~110 MB) on the first call if not already cached in `~/.gradle/wrapper/dists/`. Subsequent calls use the cache. |
| **Thread safety** | Always call `GradleConnector.newConnector()` per invocation. `ProjectConnection` is thread-safe once created. |
| **Error handling** | Catch `GradleConnectionException` and `BuildException`; surface them as diagnostics or wrap in an `IOException`. |
| **`init` not idempotent** | Running `init` on a directory that already contains `build.gradle` fails. The suggestion is only applied once, so this is not a problem in practice. |
| **Generated `build.gradle` differs** | `gradle init --type java-library` generates a version-catalog-aware `build.gradle` (different from the current hardcoded template). The existing `BUILD_GRADLE_CONTENT` constant is deleted. |
| **`--use-defaults` availability** | Added in Gradle 7.6. Present in 9.3.1. |
| **`--no-split-project` availability** | Added in Gradle 8.x. Present in 9.3.1. |

---

## References

- [Gradle Tooling API User Guide](https://docs.gradle.org/current/userguide/tooling_api.html)
- [GradleConnector Javadoc](https://docs.gradle.org/current/javadoc/org/gradle/tooling/GradleConnector.html)
- [ProjectConnection Javadoc](https://docs.gradle.org/current/javadoc/org/gradle/tooling/ProjectConnection.html)
- [BuildLauncher Javadoc](https://docs.gradle.org/current/javadoc/org/gradle/tooling/BuildLauncher.html)
- [Build Init Plugin](https://docs.gradle.org/current/userguide/build_init_plugin.html)
