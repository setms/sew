# TDD

## Test list

- [x] `CodeBuilder` should have an `assemblePackage()` method. `Gradle` should implement that by running the `assemble`
  Gradle task.
- [x] `CodeTool.buildReportsFor()` should call `CodeBuilder.assemblePackage()`
- [x] `TechnologyResolver` should have a `codePackager` method returning a new `CodePackager` interface.
  This should force a decision for the `Packaging` topic.
- [x] For choice `Docker` of decision topic `Packaging`, `TechnologyResolverImpl` should return a new `Docker` class as
  implementation of `CodePackager`.
- [x] The `CodePackager` interface should have a `packageCode()` method. `Docker` should implement this by running
  `docker build -t <project> .`, where <project> is the title of the `Inititative` artifact.
- [x] `Docker` should report any issues with building as diagnostics.
- [x] `Docker` should recognize `Dockerfile: no such file or directory` in the build output as a missing `Dockerfile`
  and report it as a diagnostic with a suggestion to create the `Dockerfile`.
- [>] Move `Docker` to the `org.setms.swe.domain.model.sdlc.packaging.docker` package.
- [ ] A new `PackagerTool` derived from `ArtifactTool` should call `CodePackager.packageCode()` from its
  `validate()` method.
  It should work similar to `AggregateTool` in getting the `CodePackager` from `TechnologyResolver`.
  It should delegate the suggestion to create `Dockerfile`.
- [ ] Introduce a new `Packager` interface with a `packagingDescriptions()` method that works similarly to
  `ProgrammingLanguageConvention.buildConfigurationFiles()`.
- [ ] Add `Inputs.packageDescriptions()` that works similar to `buildConfiguration()`, but uses
  `Packager.packagindDescriptions()`.
- [ ] `PackagerTool.validationTargets()` should include `Inputs.packageDescriptions()`.
- [ ] `PackagerTool.validationContext()` should include `Inputs.code()`.
