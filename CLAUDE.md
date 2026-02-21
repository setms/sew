# CLAUDE.md

## Project overview

SEW (Software Engineering Workbench) is a domain modeling platform that combines compiler techniques, IDE integration, 
and visual modeling for complex software design.
It provides custom DSLs for software engineering artifacts (domains, use cases, aggregates, events, etc.) with IntelliJ
IDEA plugin support.

## Validating

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :km:test

# Run specific test class
./gradlew :swe:test --tests "org.setms.swe.e2e.EndToEndTest"

# Run all checks, including tests
./gradlew check
```

- CRITICAL: **Never** report a task as done until `./gradlew test` passes with zero failures.
- CRITICAL: After making any code change, always verify by running the relevant tests before declaring success.

## Project structure

The project is a multi-module Gradle build with the following modules:

### km (Knowledge Management)

The foundational core module providing:
- **Artifact Model**: Base `Artifact` class from which all domain entities inherit
- **Workspace Management**: Resource abstraction (`Workspace`, `Resource`, `DirectoryWorkspace`)
- **Validation Framework**: `Diagnostic` system with `Location` tracking and severity levels
- **Tool Framework**: Base classes for `ArtifactTool<T>` and `StandaloneTool` implementations
- **Diagram Rendering**: JGraphX-based visualization

### swe (Software Engineering)

Domain-specific models and tools organized under `org.setms.swe.domain.model.sdlc`:

- **stakeholders**: `User`, `Owner`, `Person`
- **domainstory**: `DomainStory` for narrative requirements
- **usecase**: `UseCase`, `Scenario` for behavior specifications
- **design**: `Entity`, `Field`, `Command`, `Aggregate`, `ReadModel`
- **ddd**: `Domain`, `Subdomain`, `EventStorm`, `Term`, `Sequence`
- **architecture**: `Decision` for architectural decisions
- **acceptance**: Acceptance testing support
- **code**: Programming language models

Domain Services in `org.setms.swe.domain.services`.

### intellij-lang-sal

IntelliJ language support for SAL (Structured Artifact Language).

### intellij-lang-acceptance

IntelliJ language support for Acceptance test format.

### intellij

Full IntelliJ IDEA plugin that integrates all components:
- **File Type System**: 20+ custom file types (`.entity`, `.command`, etc.)
- **Editor Providers**: Visual editors for domains, domain stories, etc
- **Language Support**: Syntax highlighting, completion, and annotations
- **IDE Integration**: `IntellijWorkspace` bridges `VirtualFile` system to workspace resources
- **Validation**: Background validation with `KmStartupActivity` and `FileListener`
- **UI**: Task window for displaying diagnostics and suggestions

### Softure

Future Spring Boot web application providing web UI for SEW.

## High-level architecture

### Artifact system

All domain entities inherit from `Artifact`:
- `FullyQualifiedName` - Package + name identification (e.g., `org.example.UserAggregate`)
- Validation with location-based diagnostics
- Links to other artifacts via references

### Tool pattern

The system uses a plugin-based Tool architecture:
- Tools declare inputs via `Inputs` class (what artifacts they depend on)
- Tools discovered via ServiceLoader/reflection
- Outputs include diagnostics (`.km/diagnostics/`) and reports (`.km/reports/`)

### Workspace & file watching

- `Workspace` abstracts file system access
- `DirectoryWorkspace` monitors project directory for changes
- `.km/inputs/` caches resolved artifact dependencies per tool

### Data flow
```
User Files (.entity, .command, etc.)
        ↓
Workspace (monitors changes)
        ↓
ProcessOrchestrator (main orchestrator)
        ├→ Parse via Format.newParser()
        └→ Trigger Tools
        ↓
Tool Pipeline:
     ├→ Resolve inputs (gather dependencies)
     ├→ Validate (produce diagnostics)
     └→ Generate reports
        ↓
Output:
  ├→ Diagnostics JSON (.km/diagnostics/)
  └→ Reports (.km/reports/)
```

See [Custom DSLs and file formats](docs/format.md)

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

Each iteration runs three steps in order:

1. **Verify outputs** - Check that files generated by the previous iteration's suggestions match the
   expected content in `NN/outputs/`. Comparison is by exact content.
2. **Copy inputs** - Copy input files from `NN/inputs/` into the workspace at their specified
   locations, triggering the `ProcessOrchestrator` to parse and validate.
3. **Assert diagnostics** - Wait (up to 5 seconds) for exactly the expected diagnostics to appear.
   Every diagnostic must have suggestions. Apply all suggestions and verify no new diagnostics
   result. Files created or changed by suggestions become the outputs verified in the next iteration.

Running `EndToEndTest` takes a lot longer than unit tests, so make sure to capture any required output from 
`swe/build/reports/tests/test/org.setms.swe.e2e.EndToEndTest/shouldGuideSoftwareEngineering().html`.
Use this test output to avoid running the test repeatedly without changes.

The resulting files created by `EndToEndTest` are in `swe/build/e2e`.
Look here to see what files were created during the run of the test.


### Test frameworks

- JUnit 5 (JUnit Platform)
- Test data builders with JQwik
- Mockito for test mocking

## Development notes

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

### Tool usage

- CRITICAL: **Never** use Bash commands like `ls`, `find`, `cat`, `head`, `tail`, `sed`, `awk`, `grep`, or `echo` for
  file operations. Always use the dedicated tools instead:
  - Find files: use `Glob` or `mcp__jetbrains__find_files_by_glob` / `mcp__jetbrains__find_files_by_name_keyword`
  - Search content: use `Grep` or `mcp__jetbrains__search_in_files_by_text` / `mcp__jetbrains__search_in_files_by_regex`
  - Read files: use `Read` or `mcp__jetbrains__get_file_text_by_path`
  - List directories: use `mcp__jetbrains__list_directory_tree`
  - Edit files: use `Edit` or `mcp__jetbrains__replace_text_in_file`
  - Write files: use `Write` or `mcp__jetbrains__create_new_file`
  - Reserve `Bash` exclusively for running build/test commands (e.g., `./gradlew test`)

### Code style

- Code formatting enforced via Spotless plugin
- Lombok used for reducing boilerplate — use `@RequiredArgsConstructor` instead of handwritten
  constructors that only assign `final` fields
- Java 25 language features available
- Run `./gradlew spotlessApply` before committing
- Don't discuss implementation details in JavaDoc - that's for documenting the API
- CRITICAL: Use the `var` keyword where possible:
  ```java```
  var name = "John"; // Good
  String name = "John"; // Bad
  var list = new ArrayList<String>(); // Good
  List<String> list = new ArrayList<>(); // Bad
  String name = null; // Good
  var name = (String) null; // Bad
  ```
- Helper methods directly follow the method that first calls them
- If a data object has chained setters, use chaining
- If a variable is used in a `return`, it must be called `result`
- If a variable is used in assertions or verifications, it must be called `actual`
- Test methods must follow the Arrange/Act/Assert pattern
  - These sections MUST be separated by a blank line
  - No other blank lines are allowed
  - No section may be more than 10 lines, extract helper methods is necessary
- CRITICAL: Always re-use as much as possible, both in production and in test code
- CRITICAL: Prefer `Stream` and `Optional` over `for` and `if`, especially to avoid nested blocks
- CRITICAL: Instead of writing a comment before a block of code, extract the code into a method and name it properly.
  This is non-negotiable.
- CRITICAL: Import types rather than use their fully qualified names. This is non-negotiable.
- CRITICAL: Tests MUST consist of at most three sections: Arrange, Act, and Assert.
  Separate the sections by blank lines and don't use any other blank lines in the test method.
