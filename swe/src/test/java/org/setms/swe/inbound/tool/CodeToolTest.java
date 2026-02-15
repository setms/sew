package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_BUILD_TOOL;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.BuildTool;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;

class CodeToolTest extends ToolTestCase<CodeArtifact> {

  public CodeToolTest() {
    super(new CodeTool(), CodeArtifact.class, "src");
  }

  @Override
  @Test
  void shouldDefineInputs() {
    // Skip - CodeTool has multiple validation targets (one per language)
  }

  @Override
  @Test
  void shouldValidate() {
    // Skip - CodeTool validation is tested in specific tests below
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
          decision(BuildTool.TOPIC, "Gradle")
        };
    var inputs = new ResolvedInputs().put("decisions", List.of(decisions));
    var tool = (CodeTool) getTool();

    tool.validate(codeArtifact, inputs, diagnostics);

    assertThat(diagnostics).isEmpty();
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
}
