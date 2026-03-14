# Things to do

## Generic

- Add Checkstyle


## End-to-end test

- Finish workflow


## IntelliJ

- Finish task window
- Annotator for acceptance tests
- Autocomplete
  - Terms from glossary


## Tools

### Acceptance test

- Check whether unit test exits
  - If it does, check that it implements all scenarios
- Check internal consistency
  - All variables used in scenarios
  - No undeclared variables used in scenarios

### Aggregate

- Generate domain object
- Generate entity
  - Requires decision on framework (e.g. Spring Boot / Spring Data)

### Code

- `CodeTool` should use Google Java Formatter to format the code properly before persisting a resource
- Run linters (without tests)

### Decision

- Render ADR

### Domain story

- Fix rendering
- Conversion to use case scenario:
  - handle multiple activities in single sentence
  - handle computerSystem as System-under-Design vs external system

### Entity

- Move generation of entity object to `AggegrateTool`

### Glossary

- Propose domain terms

### Modules

- Other deployment options than monolith

### Unit test

- Make test pass
