package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.domain.model.sdlc.architecture.BuildTool.TOPIC;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_BUILD_TOOL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.code.CodeFormat;
import org.setms.swe.domain.model.sdlc.project.Project;
import org.setms.swe.domain.model.sdlc.technology.BuildTool;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class CodeToolTest extends ToolTestCase<CodeArtifact> {

  public CodeToolTest() {
    super(new CodeTool(), CodeArtifact.class, "src");
  }

  @Override
  protected void assertValidationTarget(ArtifactTool<?> tool) {
    var targets = tool.validationTargets();
    assertThat(targets).hasSize(3);
    assertThat(targets).anySatisfy(input -> assertThat(input.path()).isEqualTo("src/main/java"));
    assertThat(targets).anySatisfy(input -> assertThat(input.path()).isEqualTo("src/test/java"));
    assertThat(targets)
        .allSatisfy(
            input -> {
              assertThat(input.format()).isInstanceOf(CodeFormat.class);
              assertThat(input.extension()).isEqualTo("java");
            });
  }

  @Test
  void shouldNeedBuildToolWhenUnitTestExists() {
    var diagnostics = new ArrayList<Diagnostic>();
    var codeArtifact = codeArtifact();
    var decisions =
        new Decision[] {
          decision(ProgrammingLanguage.TOPIC, "Java"),
          decision(TopLevelPackage.TOPIC, "com.example")
        };
    var inputs = new ResolvedInputs().put("decisions", List.of(decisions));
    var tool = (CodeTool) getTool();

    tool.validate(codeArtifact, inputs, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message())
                  .as("Message")
                  .isEqualTo("Missing decision on build tool");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Decide on build tool"));
            });
  }

  private CodeArtifact codeArtifact() {
    return new CodeArtifact(new FullyQualifiedName("com.example", "MyTest")).setCode("test code");
  }

  private Decision decision(String topic, String choice) {
    return new Decision(new FullyQualifiedName("technology", topic))
        .setTopic(topic)
        .setChoice(choice);
  }

  @Test
  void shouldNotNeedBuildToolWhenAlreadyDecided() {
    var diagnostics = new ArrayList<Diagnostic>();
    var codeArtifact = codeArtifact();
    Decision[] decisions =
        new Decision[] {
          decision(ProgrammingLanguage.TOPIC, "Java"),
          decision(TopLevelPackage.TOPIC, "com.example"),
          decision(TOPIC, "Gradle")
        };
    var inputs = new ResolvedInputs().put("decisions", List.of(decisions));
    var tool = (CodeTool) getTool();

    tool.validate(codeArtifact, inputs, diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldValidateGradleInitializationWhenGradleDecided() throws IOException {
    var workspace = new InMemoryWorkspace();
    createFile(
        workspace,
        "/src/test/java/com/example/MyTest.java",
        "package com.example;\npublic class MyTest {}");
    var decisions =
        List.of(
            decision(ProgrammingLanguage.TOPIC, "Java"),
            decision(TopLevelPackage.TOPIC, "com.example"),
            decision(TOPIC, "Gradle"));
    var inputs =
        new ResolvedInputs().put("decisions", decisions).put("projects", List.of(project()));
    var diagnostics = new ArrayList<Diagnostic>();
    var tool = (CodeTool) getTool();

    tool.validate(
        workspace.root().select("/src/test/java/com/example/MyTest.java"), inputs, diagnostics);

    assertThat(diagnostics)
        .map(Diagnostic::message)
        .containsExactly("Gradle project isn't initialized");
  }

  private void createFile(Workspace<?> workspace, String path, String content) throws IOException {
    var resource = workspace.root().select(path);
    try (var output = resource.writeTo()) {
      output.write(content.getBytes());
    }
  }

  private Project project() {
    return new Project(new FullyQualifiedName("overview", "MyProject")).setTitle("MyProject");
  }

  @Test
  void shouldCreateBuildToolDecision() {
    var workspace = new InMemoryWorkspace();
    var tool = (CodeTool) getTool();

    var actual = tool.applySuggestion(null, PICK_BUILD_TOOL, null, null, workspace.root());

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
  void shouldCallBuildAfterBuildToolIsInitialized() throws IOException {
    var buildTool = mock(BuildTool.class);
    var tool = givenToolWith(buildTool);
    var workspace = givenWorkspaceWithTestSource();
    var inputs = givenInputsWithBuildToolDecision();
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(
        workspace.root().select("/src/test/java/com/example/MyTest.java"), inputs, diagnostics);

    verify(buildTool).build(any(), any());
  }

  private ArtifactTool<CodeArtifact> givenToolWith(BuildTool buildTool) {
    var resolver = mock(TechnologyResolver.class);
    when(resolver.buildTool(any(), any(), any(), any())).thenReturn(Optional.of(buildTool));
    return new CodeTool(resolver);
  }

  private Workspace<?> givenWorkspaceWithTestSource() throws IOException {
    var workspace = new InMemoryWorkspace();
    createFile(
        workspace,
        "/src/test/java/com/example/MyTest.java",
        "package com.example;\npublic class MyTest {}");
    return workspace;
  }

  private ResolvedInputs givenInputsWithBuildToolDecision() {
    var decisions =
        List.of(
            decision(ProgrammingLanguage.TOPIC, "Java"),
            decision(TopLevelPackage.TOPIC, "com.example"),
            decision(TOPIC, "Gradle"));
    return new ResolvedInputs().put("decisions", decisions).put("projects", List.of(project()));
  }
}
