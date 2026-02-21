---
description: Write code following the TDD style. Use when there is a non-empty, non-finished test plan in `TDD.md`
---

`TDD.md` contains a plan with test scenarios.

1. Take the top test scenario from the plan that isn't yet marked as complete and mark it as in progress.
2. Write a single unit test for the test scenario.
  It's likely that you'll have to design an API of some sort, i.e. invent types and/or methods that don't exist yet.
  Think hard about getting that API right: it must make sense in isolation, follow the project's conventions, and use
  the exact same terminology used in the test scenario (ubiquitous language).
3. Make sure this test fails for the correct reason.
  If you invented new types or methods, you'll first have to write just enough code to make the test compile.
  Then run the test.
  If it passes or fails for the wrong reason, update the test until it fails for the correct reason.
  Also make sure the test failure clearly indicates what's wrong.
4. Show the test to the user and ask them to review it.
  Address any concerns they may have.
5. Once you have approval, write code to make the failing test pass.
  Write the minimal amount of code you can get away with.
  Don't look ahead to other tests, just optimize for the minimum amount of code change that makes this single test pass.
6. Once this single test passes, run all the tests in the project to ensure the new code didn't break anything.
  If any test **other than `EndToEndTest`** fails, fix it and run all tests again until they all pass.
  **CRITICAL: DO NOT attempt to fix `EndToEnd` test.**
7. Once these tests pass, commit the changes.
  Write a very short commit message using conventional commits.
8. Now review the code by invoking the `code-review` skill.
9. Present the changes to the user, as well as the current state of `EndToEndTest`.
  Address any concerns the user may have about the changes.
10. If you made any changes to the structure, commit them.
  Write a very short commit message using conventional commits.
11. Once you have approval, consider whether the added unit test fully covers the test scenario.
  If it doesn't, return to step 2.
12. Mark the test scenario as complete.
13. If there are test scenarios that aren't complete, return to step 1.
  CRITICAL: You MUST have explicit approval to continue with the next test scenario.
14. If `EndToEndTest` still fails after all test scenarios are complete, invoke the `e2e` skill.

Implementation notes:

- Don't use tools like `sed` that require constant approval.
- Prefer tools from the `jetbrains` MCP server.
  The IntelliJ project lives at `~/dev/setms/sew`.

CRITICAL: You must not conclude you're done until all tests pass (with the possible exception of `EndToEndTest`) and
all other checks succeed.
This is non-negotiable.

CRITICAL: You mustn't run any test more than once, unless there were changes to either the test or the code it tests.
If you need more information, look at the test report in the `build` directory instead.
This is critical for not wasting time.
