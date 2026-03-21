# TDD

This file contains test scenarios, each of which is meant to go through the TDD loop.
Each test scenario is an item in a list and should be marked with its status: `[ ]` for not started, `[~]` for in
progress, and `[x]` for completed.
Don't use headings in this document, it's explicitly designed to be a flat list of prioritized test scenarios.

- [x] Update `Wireframe` to contain a list of `Container`s.
  Each `Container` has a direction (left-to-right, right-to-left, top-to-bottom, or bottom-to-top).
- [x] A `Container` has a list of children, each of which can be either a `Container`, an `Affordance`, an
  `InputField`, a `View`, or a `Feedback`.
- [x] Update `Inputs.wireframes()` to use a new `XmlFormat`, which is like `SalFormat` but XML-based and can parse more
  deeply nested structures, like `Wireframe`.
- [x] Remove `affordances`, `views`, and `feedbacks` from `Wireframe`, since they're all contained in `Container`.
- [x] Implement `XmlFormat.newBuilder()`
- [~] Introduce `WireframeTool`, like `UseCaseTool`, whose `buildHtml()` method renders a `Wireframe` as a low-fidelity
  mockup, honoring the direction of `Container`s.
  See the [image](wireframe.jpg) for an example of a low-fi wireframe.
