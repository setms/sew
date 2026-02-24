# TDD

## Test list

- [x] If the top-level package ends with the same segment as the `Command`'s package, they shouldn't be appended.
  This prevents ugly packages like `com.company.project.project` for a top-level package `com.company.project` and
  a command in the `project` package.
- [x] `CommandTool` must not report missing code if it has no payload (since the generated code depends on the payload).
- [x] `CommandTool` must look for corresponding code in the correct package, which ends in `.domain.model`.
- [x] Name of generated code artifact for `Command` must NOT end in `Command`, since that's not what the generated unit test uses.
- [ ] Test for generated code for `Command` must check the code of the generated code artifact.
- [ ] `CommandTool` must not issue a `Missing code` diagnostic if no programming language has been decided on.
  Instead, it must emit `Missing decision on programming language`.
  Note that it must NOT do this itself; it must ask `TechnologyResolver` for a `CodeGenerator` and that must result in
  the diagnostic.
  Compare this with how `AcceptanteTestTool` generates a unit test.
- [ ] When running `EndToEndTest`, I see no diagnostic `Missing code` for the command.
  The command is there, so that diagnostic is expected.
  Figure out what's wrong and write a test that proves there is a problem.
  CRITICAL: DO NOT CHANGE `EndToEndTest`.
