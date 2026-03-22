# TDD

This file contains test scenarios, each of which is meant to go through the TDD loop.
Each test scenario is an item in a list and should be marked with its status: `[ ]` for not started, `[~]` for in
progress, and `[x]` for completed.
Don't use headings in this document, it's explicitly designed to be a flat list of prioritized test scenarios.

- [~] Update the e2e expected output `e2e/07/outputs/default.css` to include `width: 100%` in `input`
  and `button`, and reflect the new selector ordering: `form`, `input`, `button`, then `label`.
- [ ] The generated CSS should order selectors alphabetically, and properties within a selector as well.
