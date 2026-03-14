# TDD

## Test list

- [ ] `Gradle.addBuildPlugin()` and `addDependency()` should be idempotent.
  Parse the sections to add to and sort them.
- [ ] `PostgreSql.schemaFor()` should add a primary key clause.
- [ ] `Database` should have a method `Collection<Field> extractFields(DatabaseSchema)`
- [ ] `SpringBootCodeGenerator.generateEntityFor()` should add all fields of the `DataSchema` to the entity object.
  It should use `Database.extractFields()`.
