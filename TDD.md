# TDD

## Test list

### `Gradle` implements `CodeTester`

- [x] `GradleTest`: `shouldProduceNoDiagnosticsWhenThereIsNothingToTest` — when build files are
  absent, `test()` produces no diagnostics (mirrors `shouldProduceNoDiagnosticsWhenThereIsNothingToBuild`)
- [x] `GradleTest`: `shouldProduceNoDiagnosticsWhenTestsPass` — given an initialized Gradle project
  with a passing test, `test()` produces no diagnostics
- [~] `GradleTest`: `shouldEmitDiagnosticWhenTestFails` — given an initialized Gradle project with a
  failing test, `test()` emits an ERROR diagnostic containing the test method name and failure
  message

### `TechnologyResolver.codeTester()`

- [ ] `TechnologyResolverImplTest`: `shouldNeedProgrammingLanguageForCodeTester` — when no
  programming language decision is present, `codeTester()` returns empty and adds a WARN diagnostic
  "Missing decision on programming language" with suggestion "Decide on programming language"
- [ ] `TechnologyResolverImplTest`: `shouldNeedInitiativeForCodeTester` — when Java is decided but
  no initiative is present, `codeTester()` returns empty and adds a WARN diagnostic
  "Missing initiative" with suggestion "Create initiative"
- [ ] `TechnologyResolverImplTest`: `shouldReturnCodeTesterWhenAllDecisionsPresent` — when Java,
  Gradle, and an initiative are all present, `codeTester()` returns a non-empty Optional and adds
  no diagnostics

### `UnitTestTool`

- [ ] `UnitTestToolTest` (extending `ToolTestCase<UnitTest>`): `shouldDefineInputs` — validation
  targets are the unit test source paths (e.g. `src/test/java`) with `CodeFormat` and extension
  `java`; validation context includes decisions and initiatives
- [ ] `UnitTestToolTest`: `shouldRequireCodeTesterWhenUnitTestExists` — when a `UnitTest` artifact
  is validated without a build system decision, a WARN diagnostic "Missing decision on build system"
  is emitted with suggestion "Decide on build system"
- [ ] `UnitTestToolTest`: `shouldNotRequireCodeTesterWhenBuildSystemAlreadyDecided` — when a
  `UnitTest` artifact is validated with all required decisions present, no diagnostics are emitted
  for the missing build system
- [ ] `UnitTestToolTest`: `shouldCallTestAfterCodeTesterIsInitialized` — when validating a resource
  with all decisions present (Java + Gradle + initiative), `CodeTester.test()` is invoked
