# TDD

## Test list

- [x] `TechnologyResolverImpl.frameworkCodeGenerator()` should emit a diagnostic about a missing
  Framework decision when programming language and top-level package decisions are present.
  Add a test to `TechnologyResolverImplTest`, following the pattern of
  `shouldNeedBuildSystemForCodeTester`.
- [-] `TechnologyResolverImpl.frameworkCodeGenerator()` should return a `SpringBootCodeGenerator`
  when the Framework decision is "Spring Boot".
  Add a test to `TechnologyResolverImplTest`, following the pattern of
  `shouldReturnJavaCodeGeneratorWhenProgrammingLanguageIsJava`.
- [ ] `SpringBootCodeGenerator.generateControllerFor(Aggregate, Command, Entity commandPayload, Event event)`
  should generate a Spring Boot REST controller that injects the aggregate's domain service and
  maps the command to a POST endpoint returning the event.
  Add a new `SpringBootCodeGeneratorTest` class.
  The generated controller should look like
  `swe/src/test/resources/e2e/07/outputs/TodoItemsServiceImpl.java` in spirit, but for the
  inbound layer, e.g.:
  ```java
  package com.example.todo.inbound.http;

  import com.example.todo.domain.model.AddTodoItem;
  import com.example.todo.domain.model.TodoItemAdded;
  import com.example.todo.domain.services.TodoItemsService;
  import org.springframework.web.bind.annotation.PostMapping;
  import org.springframework.web.bind.annotation.RequestBody;
  import org.springframework.web.bind.annotation.RestController;

  @RestController
  @RequiredArgsConstructor
  public class TodoItemsController {

    private final TodoItemsService todoItemsService;

    @PostMapping("/todoItems")
    public TodoItemAdded addTodoItem(@RequestBody AddTodoItem addTodoItem) {
      return todoItemsService.accept(addTodoItem);
    }
  }
  ```
- [ ] `AggregateTool.validate` should emit a diagnostic for creating a controller when the code for its domain service
  is there but there is no code for the controller yet.
- [ ] `AggregateTool` should generate the controller via `TechnologyResolver.frameworkCodeGenerator().generateControllerFor()`
  when asked to apply the suggestion for the above diagnostic.
