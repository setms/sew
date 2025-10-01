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
    assertThat(Tools.validating(Foo.class)).isEmpty();

    var tool = new FooTool();
    Tools.add(tool);
    assertThat(Tools.validating(Foo.class)).map(ArtifactTool.class::cast).containsExactly(tool);
  }

  @Test
  void shouldRegisterToolsViaServiceLoader() {
    assertThat(Tools.validating(Bar.class))
        .anySatisfy(tool -> assertThat(tool).isInstanceOf(BarTool.class));
  }
}
