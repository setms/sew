package org.setms.sew.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class RegistryTest {

  @Mock Tool tool;

  @Test
  void shouldRegisterTool() {
    Registry.register(tool);

    assertThat(Registry.getTools()).contains(tool);
  }
}
