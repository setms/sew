package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.code.CodeFormat;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.technology.CodeTester;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

class UnitTestToolTest extends ToolTestCase<UnitTest> {

  UnitTestToolTest() {
    super(new UnitTestTool(), CodeFormat.class, "test/java", "java");
  }

  @Override
  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    assertThat(inputs)
        .anySatisfy(input -> assertThat(input.type()).isEqualTo(Decision.class))
        .anySatisfy(input -> assertThat(input.type()).isEqualTo(Initiative.class));
  }

  @Test
  void shouldRequireCodeTesterWhenUnitTestExists() {
    var diagnostics = new ArrayList<Diagnostic>();
    var unitTest = newUnitTest();
    var tool = (UnitTestTool) getTool();

    tool.validate(unitTest, new ResolvedInputs(), diagnostics);

    assertThatMissingBuildSystemDiagnosticIsEmitted(diagnostics);
  }

  private void assertThatMissingBuildSystemDiagnosticIsEmitted(List<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message())
                  .as("Message")
                  .isEqualTo("Missing decision on build system");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Decide on build system"));
            });
  }

  @Test
  void shouldNotRequireCodeTesterWhenBuildSystemAlreadyDecided() {
    var diagnostics = new ArrayList<Diagnostic>();
    var unitTest = newUnitTest();
    var inputs = givenInputsWithBuildSystemDecision();
    var tool = (UnitTestTool) getTool();

    tool.validate(unitTest, inputs, diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  private ResolvedInputs givenInputsWithBuildSystemDecision() {
    return new ResolvedInputs().put("decisions", List.of(buildSystemDecision()));
  }

  private Decision buildSystemDecision() {
    return new Decision(new FullyQualifiedName("technology", BuildSystem.TOPIC))
        .setTopic(BuildSystem.TOPIC)
        .setChoice("Gradle");
  }

  private UnitTest newUnitTest() {
    return new UnitTest(new FullyQualifiedName("com.example", "ExampleTest"));
  }

  @Test
  void shouldCallTestAfterCodeTesterIsInitialized() {
    var codeTester = mock(CodeTester.class);
    var resolver = givenResolverWithCodeTester(codeTester);
    var tool = new UnitTestTool(resolver);
    var workspace = workspaceFor("valid");
    var resource =
        workspace.root().matching("src/test/java", "java").stream().findFirst().orElseThrow();
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(resource, givenInputsWithAllDecisions(), diagnostics);

    verify(codeTester).test(any(), same(diagnostics));
  }

  private TechnologyResolver givenResolverWithCodeTester(CodeTester codeTester) {
    var resolver = mock(TechnologyResolver.class);
    when(resolver.codeTester(any(), anyCollection())).thenReturn(Optional.of(codeTester));
    return resolver;
  }

  private ResolvedInputs givenInputsWithAllDecisions() {
    return new ResolvedInputs()
        .put(
            "initiatives",
            List.of(
                new Initiative(new FullyQualifiedName("overview", "Project"))
                    .setOrganization("Example")
                    .setTitle("Project")))
        .put(
            "decisions",
            List.of(
                new Decision(new FullyQualifiedName("technology", "ProgrammingLanguage"))
                    .setTopic(ProgrammingLanguage.TOPIC)
                    .setChoice("Java"),
                new Decision(new FullyQualifiedName("technology", BuildSystem.TOPIC))
                    .setTopic(BuildSystem.TOPIC)
                    .setChoice("Gradle")));
  }
}
