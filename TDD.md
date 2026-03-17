# TDD

- [x] `JavaCodeGenerator.generate(Aggregate)` should contain the fields in the payload, like the ones for command
  and event do.
- [x] `SpringBootCodeGenerator.generateEntityFor()` should add MapStruct as a build plugin.
- [x] `SpringBootCodeGenerator.generateEntityFor()` should generate a MapStruct `@Mapping` for converting between
  aggregate domain objects and JPA entities.
  => Mapper doesn't show up in e2e test.
- [ ] `JavaCodeGenerator.generate(Aggregate, Command, Entity, Event, Entity)` should generate a domain repository
  interface (public) and implementation (package protected).
  This domain repository should have methods `Collection<AggregateDomainObject> loadAll()`,
  `void insert(AggregateDomainObject)`, and `void update(AggregateDomainObject)`, where AggregateDomainObject is the
  domain object generated for the `Aggregate`.
- [ ] `SpringBootCodeGenerator.generateEndpointFor()` should ensure the domain repository implementation is a
  Spring Bean, just like the domain service.
- [ ] The domain repository implementation for an `Aggregate` should implement its methods using the JPA repository
  and the MapStruct `@Mapping`.
- [ ] The unit test that `AcceptanceTestTool` generates should use the domain repository implementation to check the
  initial and resulting states of the acceptance test by mocking `loadAll()` and verify `insert()` and `update()`.
- [ ] `Gradle.configureTask()` must be idempotent
