# Things to do

## End-to-end test

- Finish workflow


## IntelliJ

- Finish task window
- Annotator for acceptance tests
- Autocomplete
  - Terms from glossary


## Specific tools

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

- Other deployment options than monolith - use `TechnologyResolver`

### Acceptance tests

- Generate unit tests - Implement `JavaUnitTestGenerator`
- Resolve unit tests
  - `ArtifactPathProvider` interface - `JavaLanguage` implements it 
  - `Inputs.unitTests()` returns `Set<Input<UnitTest>>` by querying all providers via `ServiceLoader` 
  - Tools add all elements to their context sets 
  - Non-existing paths resolve to empty lists - no special handling needed
  