# TDD

## Test list

- [~] `TechnologyResolver` should have a method `codeGenerator()`, analogous to `unitTestGenerator()` which returns a
  `JavaCodeGenerator` when the decision for programming language is `Java`.
- [ ] `CommandTool.validate()` should check if there is a `CodeArtifact` corresponding to the command and issue a
  diagnostic if not. The diagnostic must have a suggestion to create the code.
- [ ] `CommandTool.applySuggestion` should use `TechnologyResolver.codeGenerator()` to generate code for the command.
