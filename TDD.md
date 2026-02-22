# TDD

## Test list

- [x] `BuildTool` has a `build()` method that compiles the project and collects diagnostics.
- [x] When Gradle is initialized and sources compile cleanly, `build()` emits no diagnostics.
- [x] When Gradle is initialized and sources have a compilation error, `build()` emits a diagnostic with the compiler's error message and the line number in the file where the error occurred.
- [-] When Gradle is initialized and test sources have a compilation error, `build()` emits a diagnostic with the compiler's error message and the line number in the test file where the error occurred.
- [ ] `CodeTool` calls `buildTool.build()` after confirming the build tool is initialized.
