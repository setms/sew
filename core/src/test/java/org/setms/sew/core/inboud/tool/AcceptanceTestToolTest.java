package org.setms.sew.core.inboud.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;
import org.setms.sew.core.inbound.tool.AcceptanceTestTool;

class AcceptanceTestToolTest extends ToolTestCase<AcceptanceTest> {

  protected AcceptanceTestToolTest() {
    super(new AcceptanceTestTool(), AcceptanceTest.class, "test/acceptance");
  }

  @Override
  protected void assertInputs(List<Input<?>> actual) {
    var input = actual.getFirst();
    assertThat(input.glob()).hasToString("src/test/acceptance/**/*.acceptance");
    assertThat(input.format()).isInstanceOf(AcceptanceFormat.class);
  }
}
