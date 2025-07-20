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
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.km.outbound.workspace.file.FileInputSource;
import org.setms.km.outbound.workspace.file.FileOutputSink;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Owner;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Stakeholder;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.inbound.format.sal.SalFormat;

class StakeholdersToolTest extends ToolTestCase<Stakeholder> {

  private static final String OWNER_SKELETON =
      """
      package noowner

      owner Some {
        display = "<Some role>"
      }
      """;

  public StakeholdersToolTest() {
    super(new StakeholdersTool(), Stakeholder.class, "main/stakeholders");
  }

  @Override
  protected void assertInputs(List<Input<?>> actual) {
    assertThat(actual).hasSize(3);
    assertThat(actual)
        .allSatisfy(input -> assertThat(input.format()).isInstanceOf(SalFormat.class));
    assertStakeholder(actual.get(0), User.class);
    assertStakeholder(actual.get(1), Owner.class);
    assertUseCase(actual.get(2));
  }

  private void assertStakeholder(Input<?> input, Class<? extends Stakeholder> type) {
    assertThat(input.glob().path()).isEqualTo("src/main/stakeholders");
    assertThat(input.glob().pattern()).isEqualTo("**/*." + type.getSimpleName().toLowerCase());
    assertThat(input.type()).isEqualTo(type);
  }

  private void assertUseCase(Input<?> input) {
    assertThat(input.glob().path()).isEqualTo("src/main/requirements");
    assertThat(input.glob().pattern()).isEqualTo("**/*.useCase");
    assertThat(input.type()).isEqualTo(UseCase.class);
  }

  @Test
  void shouldRejectMissingOwner() throws IOException {
    var testDir = getTestDir("invalid/noowner");
    var source = new FileInputSource(testDir);

    var actual = getTool().validate(source);

    var suggestion = assertThatToolReportsDiagnosticWithSuggestionToFix(actual);
    assertThatApplyingTheSuggestionCreatesAnOwner(suggestion, source, new FileOutputSink(testDir));
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
      Suggestion suggestion, InputSource source, OutputSink sink) throws IOException {
    var owner = sink.select("src/main/stakeholders/Some.owner");

    var actual = getTool().apply(suggestion.code(), source, null, sink);

    assertThat(actual).hasSize(1).contains(new Diagnostic(INFO, "Created " + owner.toUri()));
    try {
      assertThat(owner.toInput().open()).hasContent(OWNER_SKELETON);
    } finally {
      owner.delete();
    }
  }

  @Test
  void shouldRejectUnknownSuggestion() {
    var source = inputSourceFor("invalid/suggestion");

    var actual = getTool().apply("unknown.suggestion", source, null, null);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "Unknown suggestion: unknown.suggestion"));
  }

  @Test
  void shouldRejectMultipleOwners() {
    var source = inputSourceFor("invalid/owners");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "There can be only one owner, but found First, Second"));
  }

  @Test
  void shouldRejectNonUserInUserCase() {
    var source = inputSourceFor("invalid/nonuser");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR,
                "Only users can appear in use case scenarios, found owner Duck",
                new Location(
                    "nonuser", "useCase", "JustDoIt", "scenario", "HappyPath", "steps[0]")));
  }

  @Test
  void shouldRejectUnknownUserInUserCase() {
    var source = inputSourceFor("invalid/missing");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR,
                "Unknown user Micky",
                new Location(
                    "missing", "useCase", "JustDoIt", "scenario", "HappyPath", "steps[0]")));
  }
}
