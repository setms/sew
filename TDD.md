# TDD

This file contains test scenarios, each of which is meant to go through the TDD loop.
Each test scenario is an item in a list and should be marked with its status: `[ ]` for not started, `[~]` for in
progress, and `[x]` for completed.
Don't use headings in this document, it's explicitly designed to be a flat list of prioritized test scenarios.

- [~] `ServerSideHtmlGenerator` should add an `<h1>` heading derived from the container name (e.g. "Add todo item")
  to the `<div>` it generates for a container, so the page has a visible title matching the wireframe.
- [ ] The CSS generated for the form should include `display: flex` and `flex-direction: column` so that
  labels and inputs stack vertically, matching the wireframe's top-to-bottom layout.
- [ ] The CSS generated for inputs and button should include `width: 100%` so they span the full width
  of the form, matching the wireframe.
