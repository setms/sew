package org.setms.km.domain.model.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

class ToolsTest {

  @BeforeEach
  void init() {
    Tools.reload();
  }

  @Test
  void shouldRegisterTool() {
    var foo = new Foo(new FullyQualifiedName("foo.Baz"));
    assertThat(Tools.validating("/foo/Baz.foo", foo)).isEmpty();

    var tool = new FooTool();
    Tools.add(tool);
    assertThat(Tools.validating("/foo/Baz.foo", foo))
        .map(ArtifactTool.class::cast)
        .containsExactly(tool);
  }

  @Test
  void shouldRegisterToolsViaServiceLoader() {
    var bar = new Bar(new FullyQualifiedName("bar.Baz"));
    assertThat(Tools.validating("/bar/Baz.bar", bar))
        .anySatisfy(tool -> assertThat(tool).isInstanceOf(BarTool.class));
  }
}
