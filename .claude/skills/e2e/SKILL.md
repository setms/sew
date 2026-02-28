---
description: Create a test list to kick-start TDD. Use when `EndToEndTest` fails.
---

[Canon TDD](https://tidyfirst.substack.com/p/canon-tdd) starts with writing a list of the test scenarios you want to 
cover.
We'll store those test scenarios in `TDD.md`.
For outside-in TDD, we base the test list on a failing acceptance test:

1. Check `TDD.md`.
  If it's missing, create it.
  If it exists and all items are marked as complete, clear it.
  Otherwise, stop and ask the user how to proceed.
2. Analyze the failures in `EndToEndTest` as documented in 
  `swe/build/reports/tests/test/org.setms.swe.e2e.EndToEndTest/shouldGuideSoftwareEngineering().html`.
  Limit yourself to the last iteration, you most likely don't need anything before that to understand the issue.
3. For each issue that you discover during this analysis, add a test scenario to `TDD.md`.
  Note that a test scenario doesn't have to be a new test, it could also mean updating an existing test.
  In fact, the latter is preferred, since we don't want to add a test that contradicts an existing test.
4. Order the test scenarios (see below). 
5. Treat `TDD.md` as a plan, where test scenarios are todo items.
6. Ask the user to review the test list and address any concerns they may have.
7. Invoke the `tdd` skill.

## Ordering tests

The test list should order tests from least to most specific.
Here's a non-exhaustive list of tests for implementing a set:

1. A set should start empty.
  Adding an item to it makes it no longer empty.
2. A set should have a size.
  An empty set has size 0.
  Adding a unique item should increase the size by 1.
3. A set should answer whether it contains an item.

Here's why this is the correct order:

- The first test is the most generic: it forces only the distinction between empty and non-empty.
  The set doesn't need to know anything about the items added at all.
- The second test is more specific, because it forces the set to distinguish between different forms of non-empty: the
  set now needs to count the number of added items.
- The third test forces the set to remember exactly which items were added, not just their count.
