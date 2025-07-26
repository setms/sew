package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.INFO;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.util.List;
import java.util.SequencedCollection;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Owner;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Stakeholder;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.inbound.format.sal.SalFormat;

class ProjectToolTest extends ToolTestCase<Stakeholder> {

  private static final String OWNER_SKELETON =
      """
      package missing

      owner Some {
        display = "<Some role>"
      }
      """;

  public ProjectToolTest() {
    super(new ProjectTool(), Stakeholder.class, "main/stakeholders");
  }

  @Override
  protected void assertInputs(List<Input<?>> actual) {
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .allSatisfy(input -> assertThat(input.format()).isInstanceOf(SalFormat.class));
    assertStakeholder(actual.getFirst(), Owner.class);
    assertStakeholder(actual.get(1), User.class);
  }

  private void assertStakeholder(Input<?> input, Class<? extends Stakeholder> type) {
    assertThat(input.glob().path()).isEqualTo("src/main/stakeholders");
    assertThat(input.glob().pattern()).isEqualTo("**/*." + type.getSimpleName().toLowerCase());
    assertThat(input.type()).isEqualTo(type);
  }

  @Test
  void shouldRejectMissingOwner() throws IOException {
    var workspace = workspaceFor("invalid/missing");

    var actual = getTool().validate(workspace);

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
      Suggestion suggestion, Workspace workspace) throws IOException {
    var owner = workspace.root().select("src/main/stakeholders/Some.owner");

    var actual = getTool().apply(suggestion.code(), workspace, null);

    assertThat(actual).hasSize(1).contains(new Diagnostic(INFO, "Created " + owner.toUri()));
    try {
      assertThat(owner.readFrom()).hasContent(OWNER_SKELETON);
    } finally {
      owner.delete();
    }
  }

  @Test
  void shouldRejectUnknownSuggestion() {
    var workspace = workspaceFor("invalid/suggestion");

    var actual = getTool().apply("unknown.suggestion", workspace, null);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "Unknown suggestion: unknown.suggestion"));
  }

  @Test
  void shouldRejectMultipleOwners() {
    var source = workspaceFor("invalid/multiple");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "There can be only one owner, but found First, Second"));
  }
}
