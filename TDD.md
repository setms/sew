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
- [x] The HTML that `WireframeTool` generates must use CSS classes that correspond to what's in the generated CSS.
  It must also include the CSS file as stylesheet.
- [x] The HTML that `WireframeTool` generates for affordances must invoke the endpoint for the `Command` that the
  `Wireframe` implements.
  Careful, this endpoint depends on the decision of what framework to use.
- [x] The XML format doesn't round-trip `Link` fields: `XmlFormatBuilder` silently ignores `Reference` values
  (doesn't write them as attributes), and `Parser.setProperty` can't convert a `String` back to a `Link`.
  Fix both so that a wireframe with an affordance linked to a command survives serialization/deserialization.
- [ ] `CommandTool.toWireframe()` doesn't set the command link on the generated affordance, so the HTML generator
  can't produce the correct form action. Fix it to call `.setCommand()` with a link to the command.
- [ ] The Spring Boot controller that `SpringBootCodeGenerator` generates uses the aggregate name as the endpoint
  (e.g. `/todoItems`), but it should use the command name in kebab-case (e.g. `/add-todo-item`).
  This aligns with the endpoint the HTML generator will use for form actions.
