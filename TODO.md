# Things to do

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
- Generate DB schema
  - Requires decision on DB Schema Change Manager
    - Which requires decision on DB type to be RDBMS (schema managers are only available for DBs with schemas)
  - A DSCM may use SQL or native programming language files
  - For Flyway, they're stored in `src/main/resources/db/migration` by default

### Code

- `CodeTool` should use Google Java Formatter to format the code properly before persisting a resource

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
