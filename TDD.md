# TDD

## Test list

### `EventToolTest`

- ~~`shouldNotWarnAboutMissingCodeWhenEventHasNoPayload`~~
- ~~`shouldReportDiagnosticsFromResolverWhenCodeIsMissing`~~
- **[in progress]** `shouldWarnAboutMissingEventDto`: validate an `Event` whose payload resolves to an `Entity` but
  no matching `CodeArtifact` exists in `ResolvedInputs`
  → expect a single `WARN` diagnostic with message `"Missing event DTO"` and suggestion
  `"Generate event DTO"`
- `shouldWarnAboutMissingEventDtoWhenCodeIsInWrongPackage`: same as above but a `CodeArtifact`
  exists whose package does not end with `.domain.model`
  → same single `WARN` diagnostic
- `shouldCreatePayloadForEvent`: apply the `CREATE_PAYLOAD` suggestion against a workspace
  containing a `.event` file with an unresolved payload link
  → a new `.entity` file is created at `src/main/design/<PayloadName>.entity`
- `shouldGenerateCodeForEvent`: apply the `GENERATE_CODE` suggestion for an `Event` with a
  resolved payload `Entity` (with at least one field) and all prerequisite decisions present
  → `applySuggestion` returns a non-empty `createdOrChanged` set

### `JavaCodeGeneratorTest` (extension of existing tests)

- `shouldGenerateEventDto`: call `generate(Event event, Entity payload)` with an `Event` whose
  `FullyQualifiedName` is `("design", "TodoItemAdded")` and a payload `Entity` with a single
  `TEXT` field named `title`
  → returns a `CodeArtifact` whose `code` is a Java record `TodoItemAdded(String title)` in a
  package ending with `.domain.model`
