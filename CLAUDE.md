# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SEW (Software Engineering Workbench) is a domain modeling platform that combines compiler techniques, IDE integration, and visual modeling for complex software design. It provides custom DSLs for software engineering artifacts (domains, use cases, aggregates, events, etc.) with IntelliJ IDEA plugin support.

## Build Commands

### Building the Project
```bash
./gradlew build
```

### Running Tests
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

### Building the IntelliJ Plugin
```bash
./gradlew :intellij:buildPlugin

# Run the plugin in a sandbox IntelliJ instance
./gradlew :intellij:runIde
```

### Code Quality
```bash
# Check code formatting
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply
```

### Cleaning Build Artifacts
```bash
./gradlew clean

# Also cleans test resource build directories
./gradlew :swe:cleanTestResourcesBuildDirs
```

## Project Structure

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
- `DiscoverDomainFromUseCases` - Reverse engineers domains
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

### softure
Spring Boot web application providing web UI for SEW concepts:
- Uses MapStruct for entity mapping
- Spring validation framework integration

## High-Level Architecture

### Artifact System
All domain entities inherit from `Artifact` base class:
- `FullyQualifiedName` - Package + name identification (e.g., `org.example.UserAggregate`)
- Validation with location-based diagnostics
- Links to other artifacts via references

### Tool Pattern
The system uses a plugin-based Tool architecture:
- **ArtifactTool\<T>** - Tools that validate/process specific artifact types
- **StandaloneTool** - Tools that perform cross-artifact analysis
- Tools declare inputs via `Inputs` class (what artifacts they depend on)
- Tools discovered via ServiceLoader/reflection
- Outputs include diagnostics (`.km/diagnostics/`) and reports (`.km/reports/`)

### Workspace & File Watching
- `Workspace` abstracts file system access
- `DirectoryWorkspace` monitors project directory for changes
- `.km/inputs/` caches resolved artifact dependencies per tool
- IntelliJ plugin uses `IntellijWorkspace` to bridge VirtualFile system

### Data Flow
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

## Custom DSLs and File Formats

### SAL (Structured Artifact Language)
Text-based format for all artifacts with structure:
```
package org.example

object_type ObjectName {
    property = value
    list_prop = [item1, item2]
}

# Comments with #
```

**Object Types** (30+ types including):
- Data modeling: `entity`, `field`, `command`, `aggregate`, `readModel`, `valueObject`
- DDD: `domain`, `subdomain`, `term`
- Processes: `activity`, `scenario`, `useCase`, `domainStory`
- Context: `module`, `modules`, `component`, `components`
- Constraints: `policy`, `decision`, `hotspot`
- Events: `event`, `calendarEvent`, `clockEvent`
- Actors: `person`, `user`, `owner`, `externalSystem`

Grammar defined in ANTLR4 format at `km/src/main/antlr/Sal.g4`

### Acceptance Test Format
Table-based BDD format for acceptance testing:
```
| type      | name             |
| --------- | ---------------- |
| aggregate | package.Aggregate|

| variable | type           | definition       |
| -------- | -------------- | ---------------- |
| cmd      | command(Name)  | Field=value      |

| Given | When   | Then       |
| ----- | ------ | ---------- |
| "..."	| cmd    | event(Evt) |
```

Grammar defined in `km/src/main/antlr/Acceptance.g4`

## Testing Approach

### Mutation Testing with PiTest
- Enabled for `km` and `swe` modules
- Runs automatically with `./gradlew check`
- Current thresholds: km=38%, swe=60%
- History cached in `~/.pitest/cache/` for faster incremental runs
- Excludes: generated classes, Spring config, and swe's `EndToEndTest`

### Test Frameworks
- JUnit 5 (JUnit Platform)
- Property-based testing with JQwik (1.9.3)
- Mockito for test mocking

### Running Single Tests
```bash
# Run specific test class
./gradlew :km:test --tests "org.setms.km.domain.model.artifact.ArtifactTest"

# Run test method
./gradlew :swe:test --tests "org.setms.swe.domain.model.sdlc.usecase.UseCaseTest.testValidation"

# Run tests matching pattern
./gradlew :swe:test --tests "*UseCase*"
```

## Development Notes

### ANTLR Grammar Changes
When modifying `.g4` grammar files in `src/main/antlr/`:
1. Grammar parsers are auto-generated during compilation
2. Generated code goes to `build/generated-src/org/setms/*/lang/`
3. Clean build to regenerate: `./gradlew clean build`

### IntelliJ Plugin Development
When working on the IntelliJ plugin:
1. Grammar-Kit plugin generates PSI from `.bnf` files in `intellij-lang-sal` and `intellij-lang-acceptance`
2. Test plugin in sandbox: `./gradlew :intellij:runIde`
3. Plugin targets IntelliJ 2025.2.4 with backward compatibility to build 223

### Adding New Artifact Types
To add a new artifact type:
1. Create domain model class in `swe/src/main/java/org/setms/swe/domain/model/sdlc/`
2. Extend `Artifact` base class
3. Add object type to SAL grammar (`Sal.g4`)
4. Create corresponding `ArtifactTool<T>` implementation
5. Register tool via ServiceLoader in `META-INF/services/`
6. Add file type in `intellij/src/main/java/org/setms/sew/intellij/plugin/filetype/`
7. Add icon in `intellij/src/main/resources/icons/`

### Code Style
- Code formatting enforced via Spotless plugin
- Lombok used for reducing boilerplate (getters, builders, etc.)
- Java 25 language features available
- Run `./gradlew spotlessApply` before committing

### Known Issues (from TODO.md)
- End-to-end test workflow incomplete
- IntelliJ task window in progress
- Acceptance test annotator pending
- Autocomplete for glossary terms needs enhancement
- Domain story rendering needs fixes
