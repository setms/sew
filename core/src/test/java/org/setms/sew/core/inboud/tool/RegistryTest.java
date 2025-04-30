package org.setms.sew.core.inboud.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.setms.sew.core.domain.model.tool.Registry;
import org.setms.sew.core.domain.model.tool.Tool;

class RegistryTest {

  @Mock
  Tool tool;

  @Test
  void shouldRegisterTool() {
    Registry.register(tool);

    assertThat(Registry.getTools()).contains(tool);
  }
}
