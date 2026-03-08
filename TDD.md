# TDD

## Root cause

`ProcessOrchestrator.registerArtifactDefinitions()` reconstructs globs via
`Glob.of(input.path(), input.extension())` instead of using the input's actual glob.
For inputs like `Glob("/", "Dockerfile")` (pattern without a dot), `extension()` returns `null`,
producing `Glob("/", "**/*.null")` — a bogus pattern that never matches `/Dockerfile`.
When the Dockerfile is created, the workspace doesn't recognize it as an artifact,
`artifactChanged` never fires, and stale "Missing Dockerfile" diagnostics persist.

## Fix

Add `Glob glob()` to `Input` interface (default: `Glob.of(path(), extension())`).
`GlobInput.glob()` already exists as a record accessor and naturally overrides.
`registerArtifactDefinitions()` uses `input.glob()` instead of reconstructing.

## Test list

- [ ] Add `ProcessOrchestratorTest.shouldRevalidateWhenContextFileWithoutExtensionChanges()` — a standalone tool whose `validationContext()` includes an input with a non-standard glob (`Glob("/", "SomeFile")`, no dot-extension) should be re-validated when a matching file is created, clearing its stale diagnostics
