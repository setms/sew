---
description: Write code following the TDD style. Use when there is a non-empty, non-finished test plan in `TDD.md`
---

`TDD.md` contains a plan with test scenarios.

1. Take the top test scenario from the plan that isn't yet marked as complete and mark it as in progress.
2. If the test scenario asks for changes in `EndToEndTest`, then stop immediately; this workflow isn't meant for that.
3. Work on a single unit test at a time.
4. Determine if you can update an existing test or have to introduce a new one.
  For changes in behavior, prefer updating an existing test.
5. If you're writing a new test, it's likely that you'll have to design an API of some sort, i.e. invent types and/or
  methods that don't exist yet.
  Think hard about getting that API right: it must make sense in isolation, follow the project's conventions, and use
  the exact same terminology used in the test scenario (ubiquitous language).
  Tests should follow the Arrange/Act/Assert pattern, but DO NOT put in comments like `// Arrange`.
6. Test for material things, like expected values for important properties of an object.
  For collections, make sure the items in the collection are as expected - if you do that, you don't need to check
  the number of items in the collection.
7. If you use helper methods, follow these naming conventions:

   - Methods that only construct an object MUST be called `newXyz()`.
   - Methods in the Arrange section MUST be called `givenXyz()`.
   - Methods in the Assert section MUST be called `assertThatXyz()`.
     Also ensure that the entire method name reads like an English sentence.
   - NEVER use technical terms, like `mock`, in a method name. Adopt the domain language of the project instead.
   - Helper methods must directly follow the first method that calls them.

8. ALWAYS add `.as()` to assertions and provide a descriptive message.
   Pretend we're working on something seemingly unrelated and the test unexpectedly fails: does it then give enough
   information to understand what's wrong?
   The failure message is critically important to get right.
   A message like `Expected size: 1 but was: 0` is NOT acceptable, because it doesn't reveal anything useful.
   Neither does `Expecting any element of: [] to satisfy the given assertions requirements but none did`.
   The message needs to show the failed expectation.
9. Make sure the test fails for the correct reason.
  If you invented new types or methods, you'll first have to write just enough code to make the test compile.
  Then run the test.
  **CRITICAL: If the test passes or fails for the wrong reason, update the test until it fails for the correct reason.**
  This is non-negotiable.
  In particular, a passing test is a mortal sin.
  Also make sure the test failure clearly indicates what's wrong.
10. Show the test to the user and ask them to review it.
  If the user asks for changes, make them and **present the test again for approval**.
  **CRITICAL: DO NOT PROCEED WITHOUT EXPLICIT APPROVAL OF THE LATEST VERSION OF THE TEST**.
11. Once you have approval, write code to make the failing test pass.
  Write the minimal amount of code you can get away with.
  Don't look ahead to other tests, just optimize for the minimum amount of code change that makes this single test pass.
12. Once this single test passes, run all the tests in the project to ensure the new code didn't break anything.
  If any test **other than `EndToEndTest`** fails, fix it and run all tests again until they all pass.
  **CRITICAL: DO NOT attempt to fix `EndToEnd` test.**
13. Once these tests pass, commit the changes.
  Write a very short commit message using conventional commits.
14. Now review the code by invoking the `code-review` skill.
15. Present the changes to the user, as well as the current token budget usage percentage.
  If the user asks for changes, make them and present the code again for approval.
  **CRITICAL: DO NOT PROCEED WITHOUT EXPLICIT APPROVAL OF THE LATEST VERSION OF THE CODE**.
16. If you made any changes to the structure, commit them.
  Write a very short commit message using conventional commits.
17. Once you have approval, consider whether the added unit test fully covers the test scenario.
  If it doesn't, return to step 2.
18. Re-read `TDD.md` and mark the test scenario as complete.
19. If token usage is over 40% of budget, tell the user to start a new session and halt.
20. If there are test scenarios that aren't complete, return to step 1.
  CRITICAL: You MUST have explicit approval to continue with the next test scenario.
21. If all test scenarios are complete, clear the test list.
22. If `EndToEndTest` still fails after all test scenarios are complete, invoke the `e2e` skill.

Implementation notes:

- Don't use tools like `sed` that require constant approval.
- Prefer tools from the `jetbrains` MCP server.
  The project prefix is `sew/`.

**CRITICAL: You must not conclude you're done until all tests pass (with the possible exception of `EndToEndTest`) and
all other checks succeed.**
This is non-negotiable.

**CRITICAL: You mustn't run any test more than once, unless there were changes to either the test or the code it tests.**
If you need more information, look at the test report in the `build` directory instead.
This is critical for not wasting time.
