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


## ANTLR grammar changes

When modifying `.g4` grammar files in `src/main/antlr/`:
1. Grammar parsers are auto-generated during compilation
2. Generated code goes to `build/generated-src/org/setms/*/lang/`
3. Clean build to regenerate: `./gradlew clean build`


## Adding new artifact types

To add a new artifact type:

1. Create domain model class in `swe/src/main/java/org/setms/swe/domain/model/sdlc/`
2. Extend `Artifact` base class
3. Add object type to SAL grammar (`Sal.g4`)
4. Create corresponding `ArtifactTool<T>` implementation
5. Register tool via ServiceLoader in `META-INF/services/`
6. Add file type in `intellij/src/main/java/org/setms/sew/intellij/plugin/filetype/`
7. Add icon in `intellij/src/main/resources/icons/`


## IntelliJ plugin development

Grammar-Kit plugin generates PSI from `.bnf` files in `intellij-lang-sal` and `intellij-lang-acceptance`.

