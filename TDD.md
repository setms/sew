# TDD

## Test list

- [ ] If the top-level package ends with the same segment as the `Command`'s package, they shouldn't be appended.
  This prevents ugly packages like `com.company.project.project` for a top-level package `com.company.project` and
  a command in the `project` package.
- [ ] `CommandTool` must not report missing code if it has no payload (since the generated code depends on the payload).
- [ ] `CommandTool` must look for corresponding code in the correct package, which ends in `.domain.model`.
