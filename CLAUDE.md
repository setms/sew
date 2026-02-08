# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

SEW (Software Engineering Workbench) is a domain modeling platform that combines compiler techniques, IDE integration, and visual modeling for complex software design. It provides custom DSLs for software engineering artifacts (domains, use cases, aggregates, events, etc.) with IntelliJ IDEA plugin support.

## Build commands

### Building the project

```bash
./gradlew build
```

### Running tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :km:test
./gradlew :swe:test
./gradlew :intellij:test

# Run mutation tests (automatically runs with 'check')
./gradlew pitest
./gradlew :km:pitest
./gradlew :swe:pitest
```

### Building the IntelliJ plugin

```bash
./gradlew :intellij:buildPlugin

# Run the plugin in a sandbox IntelliJ instance
./gradlew :intellij:runIde
```

### Code quality

```bash
# Check code formatting
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply
```

### Cleaning build artifacts

```bash
./gradlew clean

# Also cleans test resource build directories
./gradlew :swe:cleanTestResourcesBuildDirs
```

## Project structure

The project is a multi-module Gradle build with the following modules:

### km (Knowledge Management)

The foundational core module providing:
- **Artifact Model**: Base `Artifact` class from which all domain entities inherit
- **Workspace Management**: File-based resource abstraction (`Workspace`, `Resource`, `DirectoryWorkspace`)
- **Validation Framework**: `Diagnostic` system with `Location` tracking and severity levels
- **Tool Framework**: Base classes for `ArtifactTool<T>` and `StandaloneTool` implementations
- **Diagram Rendering**: JGraphX-based visualization with lane patterns and layouts

### swe (Software Engineering)

Domain-specific models and tools organized under `org.setms.swe.domain.model.sdlc`:
- **design**: `Entity`, `Field`, `Command`, `Aggregate`, `ReadModel`
- **ddd**: `Domain`, `Subdomain`, `EventStorm`, `Term`, `Sequence`
- **domainstory**: `DomainStory` for narrative requirements
- **usecase**: `UseCase`, `Scenario` for behavior specifications
- **stakeholders**: `User`, `Owner`, `Person`
- **architecture**: `Decision` for architectural decisions
- **acceptance**: Acceptance testing support
- **eventstorming**: Event sourcing artifacts
- **code**: Programming language models

Domain Services in `org.setms.swe.domain.services`:
- `DomainStoryToUseCase` - Converts domain stories to formal use cases
- `CreateAcceptanceTest` - Generates acceptance tests from scenarios
- `DiscoverDomainFromUseCases` - Discovers domains based on use cases
- `DeployModulesInComponents` - Architecture deployment planning

### intellij-lang-sal

IntelliJ language support for SAL (Structured Artifact Language):
- Lexer/parser definitions using JetBrains Grammar-Kit
- Generates PSI (Program Structure Interface) for IDE features
- Located in `src/main/java/org/setms/sew/intellij/lang/sal/`

### intellij-lang-acceptance

IntelliJ language support for Acceptance test format:
- Table-based BDD test specifications
- Grammar-Kit based generation

### intellij

Full IntelliJ IDEA plugin that integrates all components:
- **File Type System**: 20+ custom file types (`.entity`, `.command`, `.aggregate`, `.useCase`, etc.)
- **Editor Providers**: Visual editors for domains, domain stories, modules, use cases, acceptance tests
- **Language Support**: Syntax highlighting, completion, annotations for SAL and Acceptance languages
- **IDE Integration**: `IntellijWorkspace` bridges VirtualFile system to workspace resources
- **Validation**: Background validation with `KmStartupActivity` and `FileListener`
- **UI**: Task window for displaying diagnostics and suggestions

### Softure

Spring Boot web application providing web UI for SEW concepts:
- Uses MapStruct for entity mapping
- Spring validation framework integration

## High-level architecture

### Artifact system

All domain entities inherit from `Artifact` base class:
- `FullyQualifiedName` - Package + name identification (e.g., `org.example.UserAggregate`)
- Validation with location-based diagnostics
- Links to other artifacts via references

### Tool pattern

The system uses a plugin-based Tool architecture:
- **ArtifactTool\<T>** - Tools that validate/process specific artifact types
- **StandaloneTool** - Tools that perform cross-artifact analysis
- Tools declare inputs via `Inputs` class (what artifacts they depend on)
- Tools discovered via ServiceLoader/reflection
- Outputs include diagnostics (`.km/diagnostics/`) and reports (`.km/reports/`)

### Workspace & file watching

- `Workspace` abstracts file system access
- `DirectoryWorkspace` monitors project directory for changes
- `.km/inputs/` caches resolved artifact dependencies per tool
- IntelliJ plugin uses `IntellijWorkspace` to bridge VirtualFile system

### Data flow
```
User Files (.entity, .command, etc.)
        ↓
Workspace (monitors changes)
        ↓
ProcessOrchestrator (main orchestrator)
        ├→ Parse via Format.newParser()
        ├→ Register artifact definitions
        └→ Trigger Tools
        ↓
Tool Pipeline:
  ├→ ArtifactTools (validate specific types)
  │  ├→ Resolve inputs (gather dependencies)
  │  ├→ Validate (produce diagnostics)
  │  └→ Generate reports
  └→ StandaloneTools (cross-artifact analysis)
        ↓
Output:
  ├→ Diagnostics JSON (.km/diagnostics/)
  ├→ Reports (.km/reports/)
  └→ IDE Annotations/Markers
```

## Custom DSLs and file formats

### SAL (Structured Artifact Language)

Text-based format for all artifacts with structure:
```
package org.example

object_type ObjectName {
    property = value
    list_prop = [item1, item2]
}
```

**Object types** (30+ types including):
- Data modeling: `entity`, `field`, `command`, `aggregate`, `readModel`, `valueObject`
- DDD: `domain`, `subdomain`, `term`
- Processes: `activity`, `scenario`, `useCase`, `domainStory`
- Context: `module`, `modules`, `component`, `components`
- Constraints: `policy`, `decision`, `hotspot`
- Events: `event`, `calendarEvent`, `clockEvent`
- Actors: `person`, `user`, `owner`, `externalSystem`

Grammar defined in ANTLR4 format at `km/src/main/antlr/Sal.g4`

### Acceptance test format

Table-based format for acceptance testing:
```
| type      | name             |
| --------- | ---------------- |
| aggregate | package.Aggregate|

| variable | type                        | definition       |
| -------- | --------------------------- | ---------------- |
| event    | event(WhenSomethingHappend) | Field=value      |

| scenario     | init | handles | state |
| ------------ | ---- | ------- |------ |
| "Happy path" |      | event   |       |
```

Grammar defined in `swe/src/main/antlr/Acceptance.g4`

## Testing approach

### Acceptance test-driven development

The preferred workflow in outside in:

1. Add an expectation that makes `EndToEndTest` fail.
2. Then add a unit test that localizes (part of) that failure.
3. Then update/add code to fix the failing unit test.
4. Repeat with step 2 until `EndToEndTest` passes.

- CRITICAL: **Never** write or change production code without a failing test that demands the change
- CRITICAL: **Always** let the user review the unit test before proceeding with the implementation.

Sometimes the user will start the workflow at step 2 (regular TDD).

### How the end-to-end test works

`EndToEndTest` (`swe/src/test/java/org/setms/swe/e2e/EndToEndTest.java`) simulates a human
software engineer working through an iterative design process. It uses resources in
`swe/src/test/resources/e2e/`, organized as numbered iteration directories (`01/`, `02/`, etc),
each containing an `iteration.yaml` that defines:

- **outputs** - Expected file paths that should exist in the workspace at the start of the iteration,
  generated by applying suggestions in the previous iteration.
- **inputs** - Artifact files (with `file` and `location`) to copy into the workspace, simulating
  human-created artifacts.
- **diagnostics** - Expected diagnostic messages that SEW should produce after processing the inputs.

For example, `03/iteration.yaml`:
```yaml
outputs:
  - src/main/analysis/Todo.domain
  - src/main/design/AddTodoItem.command

inputs:
  - file: TodoItems.aggregate
    location: src/main/design

diagnostics:
  - Missing acceptance test for aggregate TodoItems
  - Subdomains aren't mapped to modules
```

Each iteration runs three steps in order:

1. **Verify outputs** - Check that files generated by the previous iteration's suggestions match the
   expected content in `NN/outputs/`. Comparison is by exact content.
2. **Copy inputs** - Copy input files from `NN/inputs/` into the workspace at their specified
   locations, triggering the `ProcessOrchestrator` to parse and validate.
3. **Assert diagnostics** - Wait (up to 5 seconds) for exactly the expected diagnostics to appear.
   Every diagnostic must have suggestions. Apply all suggestions and verify no new diagnostics
   result. Files created or changed by suggestions become the outputs verified in the next iteration.


### Mutation testing with Pitest

- Enabled for `km` and `swe` modules
- Runs automatically with `./gradlew check`
- History cached in `~/.pitest/cache/` for faster incremental runs
- Excludes: generated classes, Spring config, and swe's `EndToEndTest`

### Test frameworks

- JUnit 5 (JUnit Platform)
- Property-based testing with JQwik
- Mockito for test mocking

### Running single tests

```bash
# Run specific test class
./gradlew :km:test --tests "org.setms.km.domain.model.artifact.ArtifactTest"

# Run test method
./gradlew :swe:test --tests "org.setms.swe.domain.model.sdlc.usecase.UseCaseTest.testValidation"

# Run tests matching pattern
./gradlew :swe:test --tests "*UseCase*"
```

## Development notes

### ANTLR grammar changes

When modifying `.g4` grammar files in `src/main/antlr/`:
1. Grammar parsers are auto-generated during compilation
2. Generated code goes to `build/generated-src/org/setms/*/lang/`
3. Clean build to regenerate: `./gradlew clean build`

### IntelliJ plugin development

When working on the IntelliJ plugin:
1. Grammar-Kit plugin generates PSI from `.bnf` files in `intellij-lang-sal` and `intellij-lang-acceptance`
2. Test plugin in sandbox: `./gradlew :intellij:runIde`
3. Plugin targets IntelliJ 2025.2.4 with backward compatibility to build 223

### Technology decisions (topics & resolver)

Technology choices are modeled as `Decision` artifacts (`.decision` files) with a topic and a choice.
Topics are declared by `TopicProvider` implementations, discovered via ServiceLoader
(`META-INF/services/org.setms.swe.domain.model.sdlc.architecture.TopicProvider`).

Key classes:
- **`TopicProvider`** — declares `topics()`, `dependsOn()`, and `isValidChoice(topic, choice)`.
  A provider can declare a topic without validating choices (validation may live in another provider).
- **`Topics`** — static registry loaded via ServiceLoader. Provides `names()` and `isValidChoice()`.
- **`TechnologyResolverImpl`** — reads `Decision` artifacts, groups them by topic, and chains
  requirements: each missing decision emits a diagnostic with a suggestion that creates a template
  `.decision` file. Once all decisions are present, it creates the appropriate generator
  (e.g., `JavaUnitGenerator`).

To add a new technology decision:
1. Create a `TopicProvider` in `swe/.../sdlc/code/` declaring the topic and its `dependsOn()`.
2. Add choice validation in the language-specific provider (e.g., `JavaLanguage.isValidChoice()`) if the choice depends
  on the programming language.
3. Register in `META-INF/services/org.setms.swe.domain.model.sdlc.architecture.TopicProvider`.
4. In `TechnologyResolverImpl`: extract the new topic from `groupByTopic()`, add a missing-decision
   diagnostic with suggestion code, and handle `applySuggestion()` via `pickDecision()`.
5. Update e2e iterations: insert a new iteration between the prerequisite decision and the
   generation step (the new iteration provides the decision as input and expects the next diagnostic).

### Adding new artifact types

To add a new artifact type:
1. Create domain model class in `swe/src/main/java/org/setms/swe/domain/model/sdlc/`
2. Extend `Artifact` base class
3. Add object type to SAL grammar (`Sal.g4`)
4. Create corresponding `ArtifactTool<T>` implementation
5. Register tool via ServiceLoader in `META-INF/services/`
6. Add file type in `intellij/src/main/java/org/setms/sew/intellij/plugin/filetype/`
7. Add icon in `intellij/src/main/resources/icons/`

### Code style

- Code formatting enforced via Spotless plugin
- Lombok used for reducing boilerplate — use `@RequiredArgsConstructor` instead of hand-written
  constructors that only assign `final` fields
- Java 25 language features available
- Run `./gradlew spotlessApply` before committing
- Comments MUST explain **why**, NOT **what**
- Don't discuss implementation details in JavaDoc - that's for documenting the API
- Use the `var` keyword where possible, but if that requires a type cast
  - Good: `var name = "John";` Bad: `String name = "John";`
  - Good: `var list = new ArrayList<String>();` Bad: `List<String> list = new ArrayList<>();`
  - Good: `String name = null;` Bad: `var name = (String) null;`
- Helper methods directly follow the method that first calls them
- If a data object has chained setters, use chaining
- If a variable is used in a `return`, it must be called `result`
- If a variable is used in assertions or verifications, it must be called `actual`
- Test methods must follow the Arrange/Act/Assert pattern
  - These sections MUST be separated by a blank line
  - No other blank lines are allowed
  - No section may be more than 10 lines, extract helper methods is necessary

### Known issues

See TODO.md.
