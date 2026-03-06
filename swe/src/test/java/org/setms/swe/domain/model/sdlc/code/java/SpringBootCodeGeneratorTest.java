package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;

@ExtendWith(MockitoExtension.class)
class SpringBootCodeGeneratorTest {

  @Mock CodeBuilder codeBuilder;

  @Test
  void shouldGenerateController() {
    var generator = new SpringBootCodeGenerator("com.example.todo", codeBuilder);
    var aggregate = new Aggregate(new FullyQualifiedName("todo", "TodoItems"));
    var command = new Command(new FullyQualifiedName("todo", "AddTodoItem"));
    var event = new Event(new FullyQualifiedName("todo", "TodoItemAdded"));

    var actual = generator.generateControllerFor(aggregate, command, null, event);

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
}
