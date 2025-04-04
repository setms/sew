package org.setms.sew.stakeholders.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.setms.sew.tool.Level.ERROR;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.tool.Diagnostic;
import org.setms.sew.tool.Input;
import org.setms.sew.tool.Tool;

class StakeholdersToolTest {

  private final Tool tool = new StakeholdersTool();
  private final File baseDir = new File("src/test/resources/stakeholders");

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).hasSize(3);
    assertThat(actual)
        .allSatisfy(input -> assertThat(input.getFormat()).isInstanceOf(SewFormat.class));
    assertStakeholder(actual.get(0), User.class);
    assertStakeholder(actual.get(1), Owner.class);
    assertUseCase(actual.get(2));
  }

  private void assertStakeholder(Input<?> input, Class<? extends Stakeholder> type) {
    assertThat(input.getGlob().getPath()).isEqualTo("src/main/stakeholders");
    assertThat(input.getGlob().getPattern())
        .isEqualTo("**/*." + type.getSimpleName().toLowerCase());
    assertThat(input.getType()).isEqualTo(type);
  }

  private void assertUseCase(Input<?> input) {
    assertThat(input.getGlob().getPath()).isEqualTo("src/main/requirements");
    assertThat(input.getGlob().getPattern()).isEqualTo("**/*.useCase");
    assertThat(input.getType()).isEqualTo(UseCase.class);
  }

  @Test
  void shouldRejectMissingOwner() {
    var dir = new File(baseDir, "invalid/owner");

    var actual = tool.run(dir);

    assertThat(actual).hasSize(1).contains(new Diagnostic(ERROR, "Missing owner"));
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
