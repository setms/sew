package org.setms.sew.stakeholders.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;
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

    assertThatThrownBy(() -> tool.run(dir)).hasMessage("Missing owner");
  }

  @Test
  void shouldRejectMultipleOwners() {
    var dir = new File(baseDir, "invalid/owners");

    assertThatThrownBy(() -> tool.run(dir))
        .hasMessage("There can be only one owner, but found First, Second");
  }

  @Test
  void shouldRejectNonUserInUserCase() {
    var dir = new File(baseDir, "invalid/nonuser");

    assertThatThrownBy(() -> tool.run(dir))
        .hasMessage("Only users can appear in use case scenarios, found owner Duck");
  }

  @Test
  void shouldRejectUnknownUserInUserCase() {
    var dir = new File(baseDir, "invalid/missing");

    assertThatThrownBy(() -> tool.run(dir)).hasMessage("Unknown user Micky");
  }

  @Test
  void shouldAcceptUserInUserCase() {
    var dir = new File(baseDir, "valid");

    assertThatNoException().isThrownBy(() -> tool.run(dir));
  }
}
