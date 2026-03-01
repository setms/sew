package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_BUILD_SYSTEM;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_PROGRAMMING_LANGUAGE;

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
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.java.Gradle;
import org.setms.swe.domain.model.sdlc.code.java.JavaArtifactGenerator;
import org.setms.swe.domain.model.sdlc.code.java.JavaCodeGenerator;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class TechnologyResolverImplTest {

  private final TechnologyResolver resolver = new TechnologyResolverImpl();

  @Test
  void shouldNeedProgrammingLanguageForUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();

    resolver.unitTestGenerator(new ResolvedInputs(), diagnostics);

    assertThatSingleWarnDiagnosticHas(
        diagnostics, "Missing decision on programming language", "Decide on programming language");
  }

  @Test
  void shouldNeedTopLevelPackageForJavaUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = givenInputsForJavaWithoutTopLevelPackage();

    resolver.unitTestGenerator(inputs, diagnostics);

    assertThatSingleWarnDiagnosticHas(
        diagnostics, "Missing decision on top-level package", "Decide on top-level package");
  }

  private ResolvedInputs givenInputsForJavaWithoutTopLevelPackage() {
    return new ResolvedInputs()
        .put("initiatives", List.of(initiative()))
        .put("decisions", List.of(decision(ProgrammingLanguage.TOPIC, "Java")));
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
  void shouldReturnUnitTestGeneratorWhenAllDecisionsPresent() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = givenInputsForJavaWithTopLevelPackage();

    var actual = resolver.unitTestGenerator(inputs, diagnostics);

    assertThat(actual).as("Generator").isPresent();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  private ResolvedInputs givenInputsForJavaWithTopLevelPackage() {
    return new ResolvedInputs()
        .put("initiatives", List.of(initiative()))
        .put(
            "decisions",
            List.of(
                decision(ProgrammingLanguage.TOPIC, "Java"),
                decision(TopLevelPackage.TOPIC, "com.example")));
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
        resolver.applySuggestion(
            JavaArtifactGenerator.PICK_TOP_LEVEL_PACKAGE, workspace.root(), new ResolvedInputs());

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
  void shouldUseDefaultChoiceForTopLevelPackageDecisionWhenNoInitiative() {
    var workspace = new InMemoryWorkspace();

    var actual =
        resolver.applySuggestion(
            JavaArtifactGenerator.PICK_TOP_LEVEL_PACKAGE, workspace.root(), new ResolvedInputs());

    assertThat(actual.createdOrChanged())
        .hasSize(1)
        .allSatisfy(
            resource -> assertThat(contentOf(resource)).contains("choice = \"com.example\""));
  }

  @Test
  void shouldDeriveDefaultTopLevelPackageChoiceFromInitiative() {
    var workspace = new InMemoryWorkspace();
    var inputs = new ResolvedInputs().put("initiatives", List.of(initiative()));

    var actual =
        resolver.applySuggestion(
            JavaArtifactGenerator.PICK_TOP_LEVEL_PACKAGE, workspace.root(), inputs);

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
  void shouldNeedInitiativeForJavaUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = givenInputsForJavaWithoutInitiative();

    resolver.unitTestGenerator(inputs, diagnostics);

    assertThatSingleWarnDiagnosticHas(diagnostics, "Missing initiative", "Create initiative");
  }

  @Test
  void shouldNeedProgrammingLanguageForCodeTester() {
    var diagnostics = new ArrayList<Diagnostic>();

    resolver.codeTester(new ResolvedInputs(), diagnostics);

    assertThatSingleWarnDiagnosticHas(
        diagnostics, "Missing decision on programming language", "Decide on programming language");
  }

  @Test
  void shouldNeedProgrammingLanguageForCodeGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();

    resolver.codeGenerator(new ResolvedInputs(), diagnostics);

    assertThatSingleWarnDiagnosticHas(
        diagnostics, "Missing decision on programming language", "Decide on programming language");
  }

  @Test
  void shouldNeedInitiativeForJavaCodeGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = givenInputsForJavaWithoutInitiative();

    resolver.codeGenerator(inputs, diagnostics);

    assertThatSingleWarnDiagnosticHas(diagnostics, "Missing initiative", "Create initiative");
  }

  private ResolvedInputs givenInputsForJavaWithoutInitiative() {
    return new ResolvedInputs()
        .put("decisions", List.of(decision(ProgrammingLanguage.TOPIC, "Java")));
  }

  @Test
  void shouldReturnJavaCodeGeneratorWhenProgrammingLanguageIsJava() {
    var diagnostics = new ArrayList<Diagnostic>();
    var inputs = givenInputsForJavaWithTopLevelPackage();

    var actual = resolver.codeGenerator(inputs, diagnostics);

    assertThat(actual).as("Generator").isPresent();
    assertThat(actual.get()).as("Generator type").isInstanceOf(JavaCodeGenerator.class);
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  @Test
  void shouldAcceptResolvedInputsForUnitTestAndCodeGenerators() {
    var inputs = givenInputsForJavaWithTopLevelPackage();
    var diagnostics = new ArrayList<Diagnostic>();

    var unitTestGen = resolver.unitTestGenerator(inputs, diagnostics);
    var codeGen = resolver.codeGenerator(inputs, diagnostics);

    assertThat(unitTestGen).as("Unit test generator").isPresent();
    assertThat(codeGen).as("Code generator").isPresent();
    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  @Test
  void shouldRequireProjectForCodeBuilder() {
    var diagnostics = new ArrayList<Diagnostic>();
    var workspace = new InMemoryWorkspace();

    resolver.codeBuilder(workspace.root(), new ResolvedInputs(), diagnostics);

    assertThatSingleWarnDiagnosticHas(diagnostics, "Missing initiative", "Create initiative");
  }

  private void assertThatSingleWarnDiagnosticHas(
      List<Diagnostic> diagnostics, String message, String suggestionMessage) {
    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message()).as("Message").isEqualTo(message);
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo(suggestionMessage));
            });
  }

  @Test
  void shouldCreateInitiative() {
    var workspace = new InMemoryWorkspace();

    var actual =
        resolver.applySuggestion(
            JavaArtifactGenerator.CREATE_INITIATIVE, workspace.root(), new ResolvedInputs());

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
