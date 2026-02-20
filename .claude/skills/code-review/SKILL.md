---
description: Review changed code. Use when code was changed.
---

Review changed code and make sure it follows the following style guide:

- Ask the `jetbrains` MCP server for issues in the changed files and address them.
  The IntelliJ project lives at `~/dev/setms/sew`.
- If a method has only one `return` statement that returns a local variable, the name of that variable must be `result`.


CRITICAL: After each change, run all tests.
If a change made any test **other than `EndToEndTest`** fail, revert the change and try a different approach.
Only `EndToEndTest` is allowed to fail.
Try at most 3 times, then ask the user for feedback on whether and how to address the issue.
