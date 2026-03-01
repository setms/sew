package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.code.CodeFormat;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
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

    assertThatSingleWarnDiagnosticWith(
        diagnostics, "Missing decision on build system", "Decide on build system");
  }

  private UnitTest newUnitTest() {
    return new UnitTest(new FullyQualifiedName("com.example", "ExampleTest"));
  }

  private void assertThatSingleWarnDiagnosticWith(
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
}
