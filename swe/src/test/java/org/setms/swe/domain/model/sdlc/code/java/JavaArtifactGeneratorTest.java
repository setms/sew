package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;

class JavaArtifactGeneratorTest {

  @Test
  void shouldEmitMissingInitiativeWhenNonePresent() {
    var inputs = new ResolvedInputs();
    var diagnostics = new ArrayList<Diagnostic>();

    var actual = JavaArtifactGenerator.topLevelPackage(inputs, diagnostics);

    assertThat(actual).as("Top-level package").isEmpty();
    assertThat(diagnostics).hasSize(1);
    assertThatInitiativeDiagnosticIsMissing(diagnostics.getFirst());
  }

  private void assertThatInitiativeDiagnosticIsMissing(Diagnostic diagnostic) {
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Missing initiative");
    assertThat(diagnostic.suggestions())
        .hasSize(1)
        .allSatisfy(s -> assertThat(s.message()).as("Suggestion").isEqualTo("Create initiative"));
  }
}
