package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.database.postgresql.PostgreSql;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.Database;

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
    var database = mock(Database.class);
    when(database.extractFieldsFrom(schema))
        .thenReturn(List.of(newField("Name"), newField("Description")));

    var actual = generator.generateEntityFor(schema, database, workspace.root());

    assertThat(actual)
        .as("SpringBootCodeGenerator should generate a JPA entity and repository for TodoItem")
        .extracting(CodeArtifact::getName)
        .contains("TodoItemEntity", "TodoItemRepository");
    assertThatJpaEntityIsValid(actual);
    assertThatRepositoryArtifactExtendsJpaRepository(actual);
  }

  private Field newField(String name) {
    return new Field(new FullyQualifiedName(name)).setType(FieldType.TEXT);
  }

  private void assertThatJpaEntityIsValid(Collection<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .filteredOn(a -> "TodoItemEntity".equals(a.getName()))
        .singleElement()
        .satisfies(
            entity ->
                assertThat(entity.getCode())
                    .as("TodoItemEntity code should have JPA annotations")
                    .contains("@Entity", "@Id", "name", "description"));
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
  void shouldCreateApplicationLocalYmlWithDataSourceUrlForPostgreSql() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));
    schema.setCode("CREATE TABLE todo_item (id UUID PRIMARY KEY);");

    generator.generateEntityFor(schema, new PostgreSql(), workspace.root());

    var actual = workspace.root().select("src/main/resources/application-local.yml");
    assertThat(actual.exists())
        .as("application-local.yml should be created when generating entity for PostgreSQL")
        .isTrue();
    assertThat(actual.readAsString())
        .as(
            "application-local.yml should configure Spring datasource URL to connect to local PostgreSQL database 'todo'")
        .contains("jdbc:postgresql://localhost:5432/todo");
  }

  @Test
  void shouldAddJpaDependencyWhenGeneratingEntity() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));
    var database = mock(Database.class);

    generator.generateEntityFor(schema, database, workspace.root());

    verify(codeBuilder)
        .addDependency("org.springframework.boot:spring-boot-starter-data-jpa", workspace.root());
  }

  @Test
  void shouldConfigureBootRunWithLocalProfileWhenGeneratingEntity() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));
    var database = mock(Database.class);

    generator.generateEntityFor(schema, database, workspace.root());

    verify(codeBuilder)
        .configureTask("bootRun", Map.of("spring.profiles.active", "local"), workspace.root());
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
