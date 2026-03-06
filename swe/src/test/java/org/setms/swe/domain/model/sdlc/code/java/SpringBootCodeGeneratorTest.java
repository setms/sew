package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;

@ExtendWith(MockitoExtension.class)
class SpringBootCodeGeneratorTest {

  @Mock CodeBuilder codeBuilder;

  private InMemoryWorkspace workspace;
  private SpringBootCodeGenerator generator;
  private Aggregate aggregate;
  private Command command;
  private Event event;

  @BeforeEach
  void setUp() {
    workspace = new InMemoryWorkspace();
    generator = new SpringBootCodeGenerator("com.example.todo", codeBuilder);
    aggregate = new Aggregate(new FullyQualifiedName("todo", "TodoItems"));
    command = new Command(new FullyQualifiedName("todo", "AddTodoItem"));
    event = new Event(new FullyQualifiedName("todo", "TodoItemAdded"));
  }

  @Test
  void shouldGenerateController() {
    var actual = generator.generateControllerFor(workspace.root(), aggregate, command, null, event);

    assertThat(actual)
        .as("Generated controller artifacts")
        .singleElement()
        .satisfies(this::assertThatGeneratedCodeIsController);
  }

  private void assertThatGeneratedCodeIsController(CodeArtifact actual) {
    assertThat(actual.getName()).as("Name").isEqualTo("TodoItemsController");
    assertThat(actual.getPackage()).as("Package").isEqualTo("com.example.todo.inbound.http");
    assertThat(actual.getCode())
        .contains("@RestController")
        .contains("@RequiredArgsConstructor")
        .contains("public class TodoItemsController")
        .contains("import lombok.RequiredArgsConstructor;")
        .contains("import com.example.todo.domain.model.AddTodoItem;")
        .contains("import com.example.todo.domain.model.TodoItemAdded;")
        .contains("import com.example.todo.domain.services.TodoItemsService;")
        .contains("import org.springframework.web.bind.annotation.PostMapping;")
        .contains("import org.springframework.web.bind.annotation.RequestBody;")
        .contains("import org.springframework.web.bind.annotation.RestController;")
        .contains("private final TodoItemsService todoItemsService;")
        .contains("@PostMapping(\"/todoItems\")")
        .contains("public TodoItemAdded addTodoItem(@RequestBody AddTodoItem addTodoItem)")
        .contains("return todoItemsService.accept(addTodoItem);");
  }

  @Test
  void shouldAddSpringBootPluginWhenGeneratingController() {
    generator.generateControllerFor(workspace.root(), aggregate, command, null, event);

    verify(codeBuilder).addBuildPlugin("org.springframework.boot", workspace.root());
    verify(codeBuilder)
        .addDependency("org.springframework.boot:spring-boot-starter-web", workspace.root());
  }
}
