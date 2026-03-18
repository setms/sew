# TDD

- [ ] The unit test that `JavaUnitTestGenerator.generate()` generates should use the domain repository implementation
  to check the `init` and `state` states of the acceptance test by mocking `loadAll()` and verifying `insert()` and
  `update()` calls.
- [ ] `Gradle.configureTask()` must be idempotent.
