package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.util.SequencedCollection;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.swe.domain.model.sdlc.stakeholders.Owner;
import org.setms.swe.inbound.format.sal.SalFormat;

class ProjectToolTest extends ToolTestCase<Owner> {

  private static final String OWNER_SKELETON =
      """
      package missing

      owner Some {
        display = "<Some role>"
      }
      """;

  public ProjectToolTest() {
    super(new ProjectTool(), Owner.class, "main/stakeholders");
  }

  @Override
  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    assertThat(inputs)
        .hasSize(2)
        .allSatisfy(
            input -> {
              assertThat(input.format()).isInstanceOf(SalFormat.class);
              assertThat(input.path()).isEqualTo("src/main/stakeholders");
            });
  }

  @Test
  void shouldRejectMissingOwner() throws IOException {
    var workspace = workspaceFor("invalid/missing");

    var actual = validateAgainst(workspace);

    var suggestion = assertThatToolReportsDiagnosticWithSuggestionToFix(actual);
    assertThatApplyingTheSuggestionCreatesAnOwner(suggestion, workspace);
  }

  private Suggestion assertThatToolReportsDiagnosticWithSuggestionToFix(
      SequencedCollection<Diagnostic> actual) {
    assertThat(actual).hasSize(1);
    var diagnostic = actual.getFirst();
    assertThat(diagnostic.level()).isEqualTo(WARN);
    assertThat(diagnostic.message()).isEqualTo("Missing owner");
    var suggestions = diagnostic.suggestions();
    assertThat(suggestions).hasSize(1);
    var suggestion = suggestions.getFirst();
    assertThat(suggestion.message()).isEqualTo("Create owner");
    return suggestion;
  }

  private void assertThatApplyingTheSuggestionCreatesAnOwner(
      Suggestion suggestion, Workspace<?> workspace) throws IOException {
    var owner = workspace.root().select("src/main/stakeholders/Some.owner");

    var actual = apply(suggestion.code(), null, workspace);

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(1).contains(owner);
    try {
      assertThat(owner.readFrom()).hasContent(OWNER_SKELETON);
    } finally {
      owner.delete();
    }
  }

  @Test
  void shouldRejectUnknownSuggestion() {
    var workspace = workspaceFor("invalid/suggestion");

    var actual = apply("unknown.suggestion", null, workspace).diagnostics();

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "Unknown suggestion: unknown.suggestion"));
  }

  @Test
  void shouldRejectMultipleOwners() throws IOException {
    var workspace = workspaceFor("invalid/multiple");

    var actual = validateAgainst(workspace);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "There can be only one owner, but found First, Second"));
  }
}
