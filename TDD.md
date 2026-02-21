# TDD

## Test list

- [-] `EndToEndTest` shouldn't fail when the contents of file in an `outputs` directory contains `<anything>`.
  In that case, it should only fail if the file wasn't created after applying suggestions.
  This reduces maintenance for files not directly under this project's control, like `gradlew`, which can change with
  every Gradle release.
- [ ] `GradleBuildTool` should use Gradle's Tool API, as documented in `GRADLE_TOOL_API.md`, to initialize a Gradle
  project using Groovy as the build script language.
  This approach guarantees that we can run Gradle builds, even if the specific files needed to do that change between
  Gradle versions.
  It should use Gradle's version catalog feature, i.e. use `gradle/libs.versions.toml` to define dependency versions.
