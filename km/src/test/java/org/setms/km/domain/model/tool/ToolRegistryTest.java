package org.setms.km.domain.model.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

  @BeforeEach
  void init() {
    ToolRegistry.reload();
  }

  @Test
  void shouldRegisterTool() {
    assertThat(ToolRegistry.handling(Foo.class)).isEmpty();

    var tool = new FooTool();
    ToolRegistry.add(tool);
    assertThat(ToolRegistry.handling(Foo.class)).isPresent().hasValue(tool);
  }

  @Test
  void shouldRegisterToolsViaServiceLoader() {
    assertThat(ToolRegistry.handling(Bar.class)).isPresent().containsInstanceOf(BarTool.class);
  }
}
