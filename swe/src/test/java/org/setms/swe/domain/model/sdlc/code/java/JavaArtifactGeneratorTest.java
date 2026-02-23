package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.overview.Initiative;

class JavaArtifactGeneratorTest {

  @Test
  void shouldEmitMissingInitiativeWhenNonePresent() {
    var inputs = new ResolvedInputs();
    var diagnostics = new ArrayList<Diagnostic>();

    var actual = JavaArtifactGenerator.topLevelPackage(inputs, diagnostics);

    assertThat(actual).as("Top-level package").isEmpty();
    assertThat(diagnostics).as("# Diagnostics").hasSize(1);
    assertThatInitiativeDiagnosticIsMissing(diagnostics.getFirst());
  }

  @Test
  void shouldEmitMissingTopLevelPackageWhenInitiativePresent() {
    var inputs = givenInputsWithInitiative();
    var diagnostics = new ArrayList<Diagnostic>();

    var actual = JavaArtifactGenerator.topLevelPackage(inputs, diagnostics);

    assertThat(actual).as("Top-level package").isEmpty();
    assertThat(diagnostics).as("# Diagnostics").hasSize(1);
    assertThatTopLevelPackageDiagnosticIsMissing(diagnostics.getFirst());
  }

  private ResolvedInputs givenInputsWithInitiative() {
    var initiative =
        new Initiative(new FullyQualifiedName("overview.Todo"))
            .setOrganization("Softure")
            .setTitle("Todo");
    return new ResolvedInputs().put("initiatives", List.of(initiative));
  }

  private void assertThatTopLevelPackageDiagnosticIsMissing(Diagnostic diagnostic) {
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message())
        .as("Message")
        .isEqualTo("Missing decision on top-level package");
    assertThat(diagnostic.suggestions())
        .hasSize(1)
        .allSatisfy(
            s -> assertThat(s.message()).as("Suggestion").isEqualTo("Decide on top-level package"));
  }

  private void assertThatInitiativeDiagnosticIsMissing(Diagnostic diagnostic) {
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Missing initiative");
    assertThat(diagnostic.suggestions())
        .hasSize(1)
        .allSatisfy(s -> assertThat(s.message()).as("Suggestion").isEqualTo("Create initiative"));
  }
}
