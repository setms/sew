# TDD

- [x] `init` and `state` in aggregate acceptance tests are entity type, so `JavaUnitTestGenerator`
      must generate init/state code for entity-typed variables (not aggregate-typed)
- [x] Update e2e expected output `07/outputs/TodoItemsAggregateTest.java` to include
      entity import, `expectedTodoItem` variable, and `verify(repository).insert(...)` call
- [ ] `JavaCodeGenerator.generate(Aggregate, Entity)` should name the domain record after the root
      entity, not the aggregate — in progress
- [ ] `JavaCodeGenerator.generate(Aggregate, Command, Entity, Event, Entity)` should use the root
      entity name in the repository interface (type in `loadAll`, `insert`, `update`)
- [ ] Update e2e expected outputs in `07/outputs/` to reflect renamed `TodoItem` record and
      updated `TodoItemsRepository` that stores `TodoItem` instead of `TodoItems`
