package org.setms.swe;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;

class EndToEndTest {

  private static final String DOMAIN_STORY_NAME = "AddTodo";
  private static final String DOMAIN_STORY_CONTENT =
      """
      package todo

      domainStory %s {
        description = "Add a todo item"
      }

      sentence {
        parts = [
          person(User),
          activity(Adds),
          workObject(TodoItem)
        ]
      }
      """;
  private static final String CREATED_USE_CASE =
      """
      package todo

      useCase AddTodo {
        description = "Add todo"
        title = "Add todo"
      }

      scenario AddTodo {
        elaborates = domainStory(AddTodo)
        steps = [
          user(User),
          command(AddTodoItem),
          aggregate(TodoItems),
          event(TodoItemAdded)
        ]
      }
      """;
  private static final String CREATED_DOMAIN =
      """
      package todo

      domain Todo {
      }

      subdomain TodoItems {
        content = [
          aggregate(TodoItems),
          command(AddTodoItem),
          event(TodoItemAdded)
        ]
      }
      """;

  private final File root = new File("build/e2etest");
  private Workspace<?> workspace;
  private KmSystem kmSystem;

  @BeforeEach
  void init() {
    Files.delete(root);
    workspace = new DirectoryWorkspace(root);
  }

  @Test
  @SuppressWarnings("unused")
  void shouldGuideSoftwareEngineering() throws IOException {
    var domainStory = assertThatExistingDomainStoryIsValidated();
    var useCase = assertThatUseCaseIsCreatedFromDomainStory(domainStory);
    var domain = assertThatDomainIsCreatedFromUseCase(useCase);
  }

  private Resource<?> assertThatExistingDomainStoryIsValidated() throws IOException {
    var result =
        workspace
            .root()
            .select("src/main/requirements/%s.domainStory".formatted(DOMAIN_STORY_NAME));
    try (var writer = new PrintWriter(result.writeTo())) {
      writer.println(DOMAIN_STORY_CONTENT.formatted(DOMAIN_STORY_NAME));
    }
    kmSystem = new KmSystem(workspace);
    await()
        .atMost(5, SECONDS)
        .untilAsserted(() -> assertThat(kmSystem.diagnosticsFor(result.path())).isNotEmpty());
    return result;
  }

  private Resource<?> assertThatUseCaseIsCreatedFromDomainStory(Resource<?> domainStory) {
    var appliedSuggestion = applySuggestionFor("Not elaborated in use case scenario", domainStory);

    assertThat(appliedSuggestion.diagnostics()).isEmpty();
    var created = appliedSuggestion.createdOrChanged();
    assertThat(created).hasSize(1);
    var result = created.iterator().next();
    assertThat(result.contentAsString()).isEqualTo(CREATED_USE_CASE);
    return result;
  }

  private AppliedSuggestion applySuggestionFor(String message, Resource<?> domainStory) {
    var diagnostics = diagnosticsWithSuggestionsFor(domainStory);
    var diagnostic = findDiagnostic(message, diagnostics);

    return kmSystem.applySuggestion(
        domainStory, diagnostic.suggestions().getFirst().code(), diagnostic.location());
  }

  private List<Diagnostic> diagnosticsWithSuggestionsFor(Resource<?> resource) {
    return kmSystem.diagnosticsFor(resource.path()).stream()
        .filter(d -> !d.suggestions().isEmpty())
        .toList();
  }

  private Diagnostic findDiagnostic(String message, Collection<Diagnostic> diagnostics) {
    var diagnostic = diagnostics.stream().filter(d -> d.message().contains(message)).findFirst();
    assertThat(diagnostic).as("Diagnostic with message '%s'".formatted(message)).isPresent();
    return diagnostic.get();
  }

  private Resource<?> assertThatDomainIsCreatedFromUseCase(Resource<?> useCase) {
    var appliedSuggestion = applySuggestionFor("Missing subdomains", useCase);

    assertThat(appliedSuggestion.diagnostics()).isEmpty();
    var created = appliedSuggestion.createdOrChanged();
    assertThat(created).hasSize(1);
    var result = created.iterator().next();
    assertThat(result.contentAsString()).isEqualTo(CREATED_DOMAIN);
    return result;
  }
}
