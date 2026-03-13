package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
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
  void shouldGenerateEndpoint() {
    var actual = generator.generateEndpointFor(workspace.root(), aggregate, command, null, event);

    assertThat(actual)
        .as("Generated endpoint")
        .anySatisfy(this::assertThatGeneratedCodeImplementsEndpoint)
        .anySatisfy(this::assertThatGeneratedCodeIsMainClass);
  }

  private void assertThatGeneratedCodeImplementsEndpoint(CodeArtifact actual) {
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

  private void assertThatGeneratedCodeIsMainClass(CodeArtifact codeArtifact) {
    assertThat(codeArtifact.getName()).as("Name").endsWith("Application");
    assertThat(codeArtifact.getCode())
        .as("Code")
        .contains("@SpringBootApplication")
        .contains("SpringApplication.run");
  }

  @Test
  void shouldGenerateEntity() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));

    var actual = generator.generateEntityFor(schema, workspace.root());

    assertThat(actual)
        .as("SpringBootCodeGenerator should generate a JPA entity and repository for TodoItem")
        .extracting(CodeArtifact::getName)
        .contains("TodoItemEntity", "TodoItemRepository");
    assertThatEntityArtifactHasJpaAnnotations(actual);
    assertThatRepositoryArtifactExtendsJpaRepository(actual);
  }

  private void assertThatEntityArtifactHasJpaAnnotations(Collection<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .filteredOn(a -> "TodoItemEntity".equals(a.getName()))
        .singleElement()
        .satisfies(
            entity ->
                assertThat(entity.getCode())
                    .as("TodoItemEntity code should have JPA annotations")
                    .contains("@Entity", "@Id"));
  }

  private void assertThatRepositoryArtifactExtendsJpaRepository(
      Collection<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .filteredOn(a -> "TodoItemRepository".equals(a.getName()))
        .singleElement()
        .satisfies(
            repository ->
                assertThat(repository.getCode())
                    .as("TodoItemRepository code should extend JpaRepository")
                    .contains("JpaRepository"));
  }

  @Test
  void shouldAddJpaDependencyWhenGeneratingEntity() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));

    generator.generateEntityFor(schema, workspace.root());

    verify(codeBuilder)
        .addDependency("org.springframework.boot:spring-boot-starter-data-jpa", workspace.root());
  }

  @Test
  void shouldAddSpringBootPluginWhenGeneratingEndpoint() {
    generator.generateEndpointFor(workspace.root(), aggregate, command, null, event);

    verify(codeBuilder).addBuildPlugin("org.springframework.boot", workspace.root());
    verify(codeBuilder).enableBuildPlugin("io.spring.dependency-management", workspace.root());
    verify(codeBuilder)
        .addDependency("org.springframework.boot:spring-boot-starter-web", workspace.root());
  }
}
