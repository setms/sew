# TDD

## Test list

### DB schema for entity  

- [x] Introduce a new `DatabaseTopicProvider` implementation of `TopicProvider` that adds `Database` as a topic and
  `PostgreSql` as valid choice for that topic.
- [x] Add `Optional<Database> TechnologyResover.database()`, where `Database` is a new interface.
  `TechnologyResolverImpl.database()` returns an instance of a new `PostgreSql` class when the decision for the
  topic `Database` is `PostgreSql` and `empty()` otherwise.
- [x] Add a new method `DatabaseSchema Database.schemaFor(Entity)`, where `DatabaseSchema` is a new `CodeArtifact`.
  The `PostgreSql` implementation maps the `Entity` and its fields to a SQL table and its columns.
  The SQL script to create that table is the `DatabaseSchema`s `code`.
- [x] `DatabaseTechnology` is a new interface like `Packager` but with `databaseSchemas()` instead of
  `packagingDescriptions()`.
  `SqlDatabase` is an implementation of `DatabaseTechnology` that delivers a `Glob` for `*.sql` files.
- [x] `SqlDatabase.extractName()` assumes the code is a SQL `CREATE TABLE` script and extracts the table name.
  It must rely on ANSI SQL only and assume as little as possible about the SQL script.
- [x] `Inputs.databaseSchemas()` returns `Input`s for `DatabaseSchemas`.
  It uses `ServiceLoader` to get `DatabaseTechnology`s, similar to `Inputs.packageDescriptions()`.
- [x] `EntityTools.validationContext()` includes `Inputs.databaseSchemas()`.
- [x] `EntityTool` needs a `TechnologyResolver` field, just like `AggregateTool`.
- [ ] `EntityTool.validate()` checks if there is a corresponding `DatabaseSchema` for the `Entity`.
  If not, it calls `TechnologyResover.database()`.
  If that returns `empty()`, then it should stop.
- [ ] `TechnologyResolverImpl.database()` must check that a decision exists for a new decision topic `Database` and if
  not, add a diagnostic `Missing database` with a suggestion to create it.
- [ ] If `EntityTool.validate()` finds no `DatabaseSchema` for the `Entity`, and `TechnologyResolver.database()`
  returns something, then it should add a diagnostic about the missing database schema for the entity with a suggestion
  to create it using `Database.schemaFor()`.
