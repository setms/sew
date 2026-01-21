# Things to do

## End-to-end test

- Finish workflow


## IntelliJ

- Finish task window
- Annotator for acceptance tests
- Autocomplete
  - Terms from glossary


## Specific tools

### Architecture

- Check for decision on unit test framework
- Check for decision on build tool
- Check for decision on VCS repository mapping to modules

### Decision

- Render ADR

### Domain story

- Fix rendering
- Conversion to use case scenario:
  - handle multiple activities in single sentence
  - handle computerSystem as System-under-Design vs external system

### Glossary

- Propose domain terms

### Modules

- Other deployment options than monolith


## Technology / Code Generation

### GeneratedCode and the Artifact model

The `GeneratedCode` record (returned by `UnitTestGenerator`) needs to fit into the broader artifact
model. Concerns to resolve:

1. **Different lifecycle**: Artifacts live in the workspace (`.entity`, `.command` files), are
   parsed and validated. Generated code is source files (`.java`, `.py`) in `src/test/java/...` —
   a different part of the project with different semantics.

2. **Traceability**: Need to track "this generated test came from this acceptance test" without
   necessarily making generated code an artifact itself.

3. **Regeneration**: If an acceptance test changes, the generated code should be regenerated. This
   is different from how other artifacts evolve (they're edited directly).

### Unit test generation

- Implement `JavaJqwikUnitTestGenerator` (Java/JUnit/AssertJ/JQwik)
- Add `TopicProvider` implementations for test framework decisions:
  - TestFramework topic (option: JUnit)
  - AssertionLibrary topic (option: AssertJ)
  - PropertyTesting topic (option: JQwik)
- Wire `AcceptanceTestTool` to use `TechnologyProvider.unitTestGenerator()`
