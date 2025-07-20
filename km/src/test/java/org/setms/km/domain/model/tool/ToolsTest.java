package org.setms.km.domain.model.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ToolsTest {

  @Test
  void shouldRegisterTool() {
    assertThat(Tools.handling(Foo.class)).isEmpty();

    var tool = new FooTool();
    Tools.add(tool);
    assertThat(Tools.handling(Foo.class)).isPresent().hasValue(tool);
  }

  @Test
  void shouldRegisterToolsViaServiceLoader() {
    assertThat(Tools.handling(Bar.class)).isPresent().containsInstanceOf(BarTool.class);
  }
}
