package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.CREATE_INITIATIVE;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_BUILD_SYSTEM;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_PROGRAMMING_LANGUAGE;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_TOP_LEVEL_PACKAGE;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.java.Gradle;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class TechnologyResolverImplTest {

  private final TechnologyResolver resolver = new TechnologyResolverImpl();

  @Test
  void shouldNeedProgrammingLanguageForUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();

    resolver.unitTestGenerator(Decisions.none(), emptyList(), diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
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
  void shouldRequireProjectForUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    var decisions = Decisions.of(decision(ProgrammingLanguage.TOPIC, "Java"));

    var actual = resolver.unitTestGenerator(decisions, emptyList(), diagnostics);

    assertThat(actual).as("Generator").isEmpty();
    assertThat(diagnostics)
        .as("Diagnostics")
        .extracting(Diagnostic::message)
        .containsExactly("Missing initiative");
    assertThat(diagnostics)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.location()).as("Location").isNull();
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.code())
                              .as("Suggestion code")
                              .isEqualTo(CREATE_INITIATIVE));
            });
  }

  @Test
  void shouldNeedTopLevelPackageAfterProject() {
    var diagnostics = new ArrayList<Diagnostic>();
    var decisions = Decisions.of(decision(ProgrammingLanguage.TOPIC, "Java"));
    var projects = List.of(initiative());

    resolver.unitTestGenerator(decisions, projects, diagnostics);

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

  private Initiative initiative() {
    return new Initiative(new FullyQualifiedName("overview.Todo"))
        .setOrganization("Softure")
        .setTitle("Todo");
  }

  @Test
  void shouldReturnGeneratorWhenAllDecisionsAndProjectPresent() {
    var diagnostics = new ArrayList<Diagnostic>();
    var decisions =
        Decisions.of(
            decision(ProgrammingLanguage.TOPIC, "Java"),
            decision(TopLevelPackage.TOPIC, "com.example"));
    var projects = List.of(initiative());

    var actual = resolver.unitTestGenerator(decisions, projects, diagnostics);

    assertThat(actual).as("Generator").isPresent();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  @Test
  void shouldCreateProgrammingLanguageDecision() {
    var workspace = new InMemoryWorkspace();

    var actual =
        resolver.applySuggestion(PICK_PROGRAMMING_LANGUAGE, workspace.root(), new ResolvedInputs());

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

    var actual =
        resolver.applySuggestion(PICK_TOP_LEVEL_PACKAGE, workspace.root(), new ResolvedInputs());

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
  void shouldDeriveDefaultTopLevelPackageChoiceFromInitiative() {
    var workspace = new InMemoryWorkspace();
    var inputs = new ResolvedInputs().put("initiatives", List.of(initiative()));

    var actual = resolver.applySuggestion(PICK_TOP_LEVEL_PACKAGE, workspace.root(), inputs);

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .hasSize(1)
        .allSatisfy(
            resource -> assertThat(contentOf(resource)).contains("choice = \"com.softure.todo\""));
  }

  private String contentOf(Resource<?> resource) {
    try (var input = resource.readFrom()) {
      return new String(input.readAllBytes(), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void shouldCreateCodeBuilderDecision() {
    var workspace = new InMemoryWorkspace();

    var actual =
        resolver.applySuggestion(PICK_BUILD_SYSTEM, workspace.root(), new ResolvedInputs());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/architecture/BuildSystem.decision"));
  }

  @Test
  void shouldRequireProjectForCodeBuilder() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = new ResolvedInputs();
    var workspace = new InMemoryWorkspace();

    resolver.codeBuilder(workspace.root(), inputs, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message()).as("Message").isEqualTo("Missing initiative");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Create initiative"));
            });
  }

  @Test
  void shouldCreateProjectArtifact() {
    var workspace = new InMemoryWorkspace();

    var actual =
        resolver.applySuggestion(
            TechnologyResolverImpl.CREATE_INITIATIVE, workspace.root(), new ResolvedInputs());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource ->
                assertThat(resource.path())
                    .as("Path")
                    .isEqualTo("/src/main/overview/Project.initiative"));
  }

  @Test
  void shouldGenerateBuildConfigWhenApplyingSuggestion(@TempDir File projectDir) {
    var workspace = new DirectoryWorkspace(projectDir);
    var inputs = givenInputsForJavaGradleProject();

    var actual = resolver.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root(), inputs);

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(10);
  }

  private ResolvedInputs givenInputsForJavaGradleProject() {
    return new ResolvedInputs()
        .put("initiatives", List.of(initiative()))
        .put(
            "decisions",
            List.of(
                decision(ProgrammingLanguage.TOPIC, "Java"),
                decision(BuildSystem.TOPIC, "Gradle")));
  }
}
