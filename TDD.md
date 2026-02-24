# TDD

## Test list

- [x] `TechnologyResolver.unitTestGenerator()` and `codeGenerator()` should accept `ResolvedInputs`
  rather than `Decisions`, so prerequisite validation can access both decisions and initiatives.
- [x] A `JavaArtifactGenerator` base class should emit "Missing initiative" (with a suggestion to
  create one) when no initiative is present, because the top-level package decision is derived from
  the initiative.
- [x] `JavaArtifactGenerator` should emit "Missing decision on top-level package" (with a suggestion
  to decide) when an initiative is present but the top-level package has not been decided yet.
- [x] `JavaArtifactGenerator.applySuggestion()` should handle `CREATE_INITIATIVE` by creating an initiative.
- [x] `JavaArtifactGenerator.applySuggestion()` should handle `PICK_TOP_LEVEL_PACKAGE` by creating a
  top-level package decision.
- [x] `TechnologyResolverImpl.applySuggestion()` should delegate to `JavaArtifactGenerator.applySuggestion()`
  for `CREATE_INITIATIVE` and `PICK_TOP_LEVEL_PACKAGE`.
- [x] `JavaUnitTestGenerator` and `JavaCodeGenerator` should extend `JavaArtifactGenerator`, so they
  get prerequisite validation for free.
- [x] `TechnologyResolverImpl.unitTestGenerator()` and `codeGenerator()` should delegate to
  `JavaUnitTestGenerator` and `JavaCodeGenerator`, respectively, for prerequisite validation, knowing only about the
  programming language.
- [~] `TechnologyResolver` should have a method `codeGenerator()`, analogous to `unitTestGenerator()` which returns a
  `JavaCodeGenerator` when the decision for programming language is `Java`.
- [ ] `CommandTool.validate()` should check if there is a `CodeArtifact` corresponding to the command and issue a
  diagnostic with suggestion to generate if not.
- [ ] `CommandTool.applySuggestion` should use `TechnologyResolver.codeGenerator()` to generate code for the command.
