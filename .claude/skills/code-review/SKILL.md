---
description: Review changed code. Use when code was changed.
---

Review changed code and make sure it follows the following style guide:

- Ask the `jetbrains` MCP server for issues in the changed files and address them.
  The IntelliJ project lives at `~/dev/setms/sew`.
- There MUST NOT be duplication.
- If a method has only one `return` statement that returns a local variable, the name of that variable must be `result`.
- Methods annotated with `@Test` must consist of at most 3 blocks: Arrange, Act, and Assert.
  Those blocks must be separated by blank lines and no other blank lines must exist in the method.
  Each block MUST NOT start with a comment.
  Each block MUST NOT be longer than 10 lines; extract helper methods where necessary.
  Helper methods in the Arrange block MUST have a name starting with `given` to follow the BDD convention.
  Helper methods in the Assert block MUST have a name starting with `assertThat`.
  If the Act block stores an outcome in a single local variable that is then tested in the Assert block, then that
  variable MUST be named `actual`.
- Use `import` statements instead of using a fully qualified name.
- Don't use technical terms like `mock` in identifier names.
- Method declarations MUST use the most generic type possible for parameters and return types.
  For example, use `Map` instead of `HashMap` and `Collection` instead of `ArrayList`.

CRITICAL: After each change, run all tests.
If a change made any test **other than `EndToEndTest`** fail, revert the change and try a different approach.
Only `EndToEndTest` is allowed to fail.
Try at most 3 times, then ask the user for feedback on whether and how to address the issue.
