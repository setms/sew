package org.setms.sew.core.stakeholders.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.tool.Level.ERROR;
import static org.setms.sew.core.tool.Level.INFO;
import static org.setms.sew.core.tool.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.format.sew.SewFormat;
import org.setms.sew.core.tool.Diagnostic;
import org.setms.sew.core.tool.FileInputSource;
import org.setms.sew.core.tool.FileOutputSink;
import org.setms.sew.core.tool.Input;
import org.setms.sew.core.tool.InputSource;
import org.setms.sew.core.tool.OutputSink;
import org.setms.sew.core.tool.Suggestion;
import org.setms.sew.core.tool.Tool;

class StakeholdersToolTest {

  private final Tool tool = new StakeholdersTool();
  private final File baseDir = new File("src/test/resources/stakeholders");
  private static final String OWNER_SKELETON =
      """
      package owner

      owner Some {
        display = "<Some role>"
      }
      """;

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).hasSize(3);
    assertThat(actual)
        .allSatisfy(input -> assertThat(input.format()).isInstanceOf(SewFormat.class));
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
    var testDir = new File(baseDir, "invalid/owner");
    var source = new FileInputSource(testDir);

    var actual = tool.validate(source);

    var suggestion = assertThatToolReportsDiagnosticWithSuggestionToFix(actual);
    assertThatApplyingTheSuggestionCreatesAnOwner(suggestion, source, new FileOutputSink(testDir));
  }

  private FileInputSource inputSourceFor(String path) {
    return new FileInputSource(new File(baseDir, path));
  }

  private Suggestion assertThatToolReportsDiagnosticWithSuggestionToFix(List<Diagnostic> actual) {
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

    var actual = tool.apply(suggestion.code(), source, sink);

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

    var actual = tool.apply("unknown.suggestion", source, null);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(WARN, "Unknown suggestion: unknown.suggestion"));
  }

  @Test
  void shouldRejectMultipleOwners() {
    var source = inputSourceFor("invalid/owners");

    var actual = tool.validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "There can be only one owner, but found First, Second"));
  }

  @Test
  void shouldRejectNonUserInUserCase() {
    var source = inputSourceFor("invalid/nonuser");

    var actual = tool.validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(ERROR, "Only users can appear in use case scenarios, found owner Duck"));
  }

  @Test
  void shouldRejectUnknownUserInUserCase() {
    var source = inputSourceFor("invalid/missing");

    var actual = tool.validate(source);

    assertThat(actual).hasSize(1).contains(new Diagnostic(ERROR, "Unknown user Micky"));
  }

  @Test
  void shouldAcceptUserInUserCase() {
    var source = inputSourceFor("valid");

    var actual = tool.validate(source);

    assertThat(actual).isEmpty();
  }
}
