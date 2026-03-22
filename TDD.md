# TDD

This file contains test scenarios, each of which is meant to go through the TDD loop.
Each test scenario is an item in a list and should be marked with its status: `[ ]` for not started, `[~]` for in
progress, and `[x]` for completed.
Don't use headings in this document, it's explicitly designed to be a flat list of prioritized test scenarios.

- [x] The CSS that `WireframeTool` generates use properties that aren't actual CSS properties.
  It has to do a translation.
- [x] The HTML that `WireframeTool` generates needs to set the title to something human-readable, using
  `Strings.toFriendlyName()`, since this is displayed by the browser.
  Careful, this title is currently used for extracting the name of the resource; that approach is probably flawed.
- [x] The HTML that `WireframeTool` generates must create structures that correspond to what's in the `Wireframe`.
  For instance, an affordance needs a button, input fields need corresponding HTML controls, etc.
- [~] The HTML that `WireframeTool` generates must use CSS classes that correspond to what's in the generated CSS.
  It must also include the CSS file as stylesheet.
- [ ] The HTML that `WireframeTool` generates for affordances must invoke the endpoint for the `Command` that the
  `Wireframe` implements.
