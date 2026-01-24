package org.setms.km.domain.model.tool;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

class ResolvedInputsTest {

  private final ResolvedInputs inputs = new ResolvedInputs();

  @Test
  void shouldMergeInputs() {
    inputs.put("foos", List.of(newFoo()));

    inputs.put("foos", List.of(newFoo(), newFoo()));

    assertThat(inputs.get(Foo.class)).hasSize(3);
  }

  private Foo newFoo() {
    return new Foo(new FullyQualifiedName("package", randomUUID().toString().replace("-", "")));
  }
}
