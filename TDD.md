# TDD

This file contains test scenarios, each of which is meant to go through the TDD loop.
Each test scenario is an item in a list and should be marked with its status: `[ ]` for not started, `[~]` for in
progress, and `[x]` for completed.
Don't use headings in this document, it's explicitly designed to be a flat list of prioritized test scenarios.

- [~] Create interface `UiGenerator`, with method `List<CodeArtifact> generate(Wireframe, DesignSystem)`.
  Add a method `TechnologyResolver.uiGenerator()` that returns it, similar to existing methods there.
  `TechnologyResolverImpl.uiGenerator()` checks for a decision in the topic `UserInterface` and issues a diagnostic if
  there isn't one, with suggestion to create it.
- [ ] Introduce `ServerSideHtmlLanguage`, which implements `ProgrammingLanguageConventions`, and register it.
  Its `extension()` is `html` and its `codePath()` is `src/main/resources/templates`.
- [ ] Introduce new marker interfaces `BackendLanguage` and `FrontendLanguage`.
  Let `JavaLanguage` implement `BackendLanguage` and let `ServerSideHtmlLanguage` implement `FrontendLanguage`.
- [ ] Update the methods in `Inputs` that iterate over `ProgrammingLanguageConventions` and let them filter for
  `BackendLanguage`.
- [ ] Introduce `Inputs.uiCode()`, which returns `UiArtifact` derived from `CodeArtifact`.
  It uses `ProgrammingLanguageConventions` filtered by `FrontendLanguage`.
- [ ] Add `Inputs.uiCode()` to `WireframeTool.validationContext()`.
- [ ] `WireframeTool` should get a `TecnologyResolver`, just like `CommandTool`.
  It should call `TechnologyResolver.uiGenerator()` if and only if there is a `DesignSystem` but no `UiArtifact` that
  corresponds to the wireframe.
  If that returns something, then it should issue a warning about the missing UI code, with suggestion to create it.
- [ ] `WireframeTool.doApply()` should handle this suggestion, by calling `UiGenerator.generate()`.
- [ ] `TechnologyResolverImpl.uiGenerator()` should return an instance of `