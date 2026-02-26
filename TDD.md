# TDD

## Test list

### `EventToolTest`

- [ ] `shouldGenerateCodeForEvent`: apply the `GENERATE_CODE` suggestion for an `Event` with a
  resolved payload `Entity` (with at least one field) and all prerequisite decisions present
  → `applySuggestion` returns a non-empty `createdOrChanged` set

### `JavaCodeGeneratorTest` (extension of existing tests)

- `shouldGenerateEventDto`: call `generate(Event event, Entity payload)` with an `Event` whose
  `FullyQualifiedName` is `("design", "TodoItemAdded")` and a payload `Entity` with a single
  `TEXT` field named `title`
  → returns a `CodeArtifact` whose `code` is a Java record `TodoItemAdded(String title)` in a
  package ending with `.domain.model`
