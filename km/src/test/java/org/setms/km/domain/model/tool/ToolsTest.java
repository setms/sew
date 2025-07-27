package org.setms.km.domain.model.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolsTest {

  @BeforeEach
  void init() {
    Tools.reload();
  }

  @Test
  void shouldRegisterTool() {
    assertThat(Tools.targeting(Foo.class)).isEmpty();

    var tool = new FooTool();
    Tools.add(tool);
    assertThat(Tools.targeting(Foo.class)).isPresent().hasValue(tool);
  }

  @Test
  void shouldRegisterToolsViaServiceLoader() {
    assertThat(Tools.targeting(Bar.class)).isPresent().containsInstanceOf(BarTool.class);
  }
}
