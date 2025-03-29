package org.setms.sew.glossary.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.tool.Tool;

class GlossaryToolTest {

  private final Tool tool = new GlossaryTool();

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).hasSize(1);
    var input = actual.iterator().next();
    assertThat(input.getGlob()).hasToString("src/main/glossary/**/*.term");
    assertThat(input.getFormat()).isInstanceOf(SewFormat.class);
  }
}
