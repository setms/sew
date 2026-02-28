# TDD

## Test list

- [ ] Update e2e iteration 07 expected output: `TodoItemsAggregateTest.java` should call `task()` and `dueDate()` (record accessors) instead of `getTask()` and `getDueDate()` (JavaBean getters)
- [ ] Update `JavaUnitTestGeneratorTest`: the generated constructor args for an aggregate scenario should call `task()` not `getTask()` when the command is a record
