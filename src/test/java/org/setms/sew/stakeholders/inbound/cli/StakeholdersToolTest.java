package org.setms.sew.stakeholders.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.tool.Level.ERROR;
import static org.setms.sew.tool.Level.INFO;
import static org.setms.sew.tool.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.tool.Diagnostic;
import org.setms.sew.tool.Input;
import org.setms.sew.tool.Suggestion;
import org.setms.sew.tool.Tool;

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
    assertThat(input.glob().getPath()).isEqualTo("src/main/stakeholders");
    assertThat(input.glob().getPattern()).isEqualTo("**/*." + type.getSimpleName().toLowerCase());
    assertThat(input.type()).isEqualTo(type);
  }

  private void assertUseCase(Input<?> input) {
    assertThat(input.glob().getPath()).isEqualTo("src/main/requirements");
    assertThat(input.glob().getPattern()).isEqualTo("**/*.useCase");
    assertThat(input.type()).isEqualTo(UseCase.class);
  }

  @Test
  void shouldRejectMissingOwner() throws IOException {
    var dir = new File(baseDir, "invalid/owner");

    var actual = tool.run(dir);

    var suggestion = assertThatToolReportsErrorWithSuggestionToFix(actual);
    assertThatApplyingTheSuggestionCreatesAnOwner(suggestion, dir);
  }

  private Suggestion assertThatToolReportsErrorWithSuggestionToFix(List<Diagnostic> actual) {
    assertThat(actual).hasSize(1);
    var diagnostic = actual.getFirst();
    assertThat(diagnostic.level()).isEqualTo(ERROR);
    assertThat(diagnostic.message()).isEqualTo("Missing owner");
    var suggestions = diagnostic.suggestions();
    assertThat(suggestions).hasSize(1);
    var suggestion = suggestions.getFirst();
    assertThat(suggestion.message()).isEqualTo("Create owner");
    return suggestion;
  }

  private void assertThatApplyingTheSuggestionCreatesAnOwner(Suggestion suggestion, File dir)
      throws IOException {
    var ownerFile = new File(dir, "src/main/stakeholders/Some.owner");

    var actual = tool.apply(suggestion.code(), dir);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(INFO, "Created file: " + ownerFile.getPath()));
    try {
      assertThat(ownerFile).isFile().hasContent(OWNER_SKELETON);
    } finally {
      Files.delete(ownerFile.toPath());
    }
  }

  @Test
  void shouldRejectUnknowSuggestion() {
    var dir = new File(baseDir, "invalid/suggestion");

    var actual = tool.apply("unknown.suggestion", dir);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(WARN, "Unknown suggestion: unknown.suggestion"));
  }

  @Test
  void shouldRejectMultipleOwners() {
    var dir = new File(baseDir, "invalid/owners");

    var actual = tool.run(dir);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "There can be only one owner, but found First, Second"));
  }

  @Test
  void shouldRejectNonUserInUserCase() {
    var dir = new File(baseDir, "invalid/nonuser");

    var actual = tool.run(dir);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(ERROR, "Only users can appear in use case scenarios, found owner Duck"));
  }

  @Test
  void shouldRejectUnknownUserInUserCase() {
    var dir = new File(baseDir, "invalid/missing");

    var actual = tool.run(dir);

    assertThat(actual).hasSize(1).contains(new Diagnostic(ERROR, "Unknown user Micky"));
  }

  @Test
  void shouldAcceptUserInUserCase() {
    var dir = new File(baseDir, "valid");

    var actual = tool.run(dir);

    assertThat(actual).isEmpty();
  }
}
