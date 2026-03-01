package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.code.CodeFormat;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

class UnitTestToolTest extends ToolTestCase<UnitTest> {

  UnitTestToolTest() {
    super(new UnitTestTool(), CodeFormat.class, "test/java", "java");
  }

  @Override
  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    assertThat(inputs)
        .anySatisfy(input -> assertThat(input.type()).isEqualTo(Decision.class))
        .anySatisfy(input -> assertThat(input.type()).isEqualTo(Initiative.class));
  }
}
