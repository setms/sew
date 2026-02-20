package org.setms.swe.inbound.tool;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_BUILD_TOOL;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_PROGRAMMING_LANGUAGE;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_TOP_LEVEL_PACKAGE;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.project.Project;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class TechnologyResolverImplTest {

  private final TechnologyResolver resolver = new TechnologyResolverImpl();

  @Test
  void shouldNeedProgrammingLanguageForUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    Location location = new Location("foo/bar");

    resolver.unitTestGenerator(emptyList(), emptyList(), location, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.location()).as("Location").isEqualTo(location);
              assertThat(diagnostic.message())
                  .as("Message")
                  .isEqualTo("Missing decision on programming language");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Decide on programming language"));
            });
  }

  @Test
  void shouldReturnEmptyWithoutDiagnosticWhenProjectMissing() {
    var diagnostics = new ArrayList<Diagnostic>();
    var location = new Location("foo/bar");
    var decisions = List.of(decision(ProgrammingLanguage.TOPIC, "Java"));

    var actual = resolver.unitTestGenerator(decisions, emptyList(), location, diagnostics);

    assertThat(actual).as("Generator").isEmpty();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  @Test
  void shouldNeedTopLevelPackageAfterProject() {
    var diagnostics = new ArrayList<Diagnostic>();
    var location = new Location("foo/bar");
    var decisions = List.of(decision(ProgrammingLanguage.TOPIC, "Java"));
    var projects = List.of(project());

    resolver.unitTestGenerator(decisions, projects, location, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message())
                  .as("Message")
                  .isEqualTo("Missing decision on top-level package");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Decide on top-level package"));
            });
  }

  private Decision decision(String topic, String choice) {
    return new Decision(new FullyQualifiedName("technology", topic))
        .setTopic(topic)
        .setChoice(choice);
  }

  private Project project() {
    return new Project(new FullyQualifiedName("overview.Todo")).setTitle("Todo");
  }

  @Test
  void shouldReturnGeneratorWhenAllDecisionsAndProjectPresent() {
    var diagnostics = new ArrayList<Diagnostic>();
    var location = new Location("foo/bar");
    var decisions =
        List.of(
            decision(ProgrammingLanguage.TOPIC, "Java"),
            decision(TopLevelPackage.TOPIC, "com.example"));
    var projects = List.of(project());

    var actual = resolver.unitTestGenerator(decisions, projects, location, diagnostics);

    assertThat(actual).as("Generator").isPresent();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  @Test
  void shouldCreateProgrammingLanguageDecision() {
    var workspace = new InMemoryWorkspace();

    var actual = resolver.applySuggestion(PICK_PROGRAMMING_LANGUAGE, workspace.root());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/architecture/ProgrammingLanguage.decision"));
  }

  @Test
  void shouldCreateTopLevelPackageDecision() {
    var workspace = new InMemoryWorkspace();

    var actual = resolver.applySuggestion(PICK_TOP_LEVEL_PACKAGE, workspace.root());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/architecture/TopLevelPackage.decision"));
  }

  @Test
  void shouldCreateBuildToolDecision() {
    var workspace = new InMemoryWorkspace();

    var actual = resolver.applySuggestion(PICK_BUILD_TOOL, workspace.root());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/architecture/BuildTool.decision"));
  }

  @Test
  void shouldRequireProjectForBuildTool() {
    var diagnostics = new ArrayList<Diagnostic>();
    var location = new Location("foo/bar");
    var inputs = new ResolvedInputs();
    var workspace = new InMemoryWorkspace();

    resolver.buildTool(workspace.root(), inputs, location, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message()).as("Message").isEqualTo("Missing project");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Create project"));
            });
  }

  @Test
  void shouldCreateProjectArtifact() {
    var workspace = new InMemoryWorkspace();

    var actual = resolver.applySuggestion(TechnologyResolverImpl.CREATE_PROJECT, workspace.root());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/overview/Project.project"));
  }
}
