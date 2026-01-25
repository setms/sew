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

- Check internal consistency
- Implement `JavaUnitTestGenerator`
- Implement `CodeParser` and `CodeBuilder`
- Check whether unit test exits
  - If not, create it using `UnitTestGenerator`
  - If it does, check that it implements all scenarios 
