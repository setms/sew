package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;
import org.setms.sew.core.outbound.tool.file.FileInputSource;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;

class AcceptanceTestToolTest extends ToolTestCase<AcceptanceTest> {

  private static final String ACCEPTANCE_TEST_HTML =
      """
    <html>
      <body>
        <h1>Acceptance tests for aggregate Notifications</h1>
        <h2>Accept NotifyUser and emit UserNotified</h2>
        <strong>Given</strong> <code>Notifications</code> is empty<br/>
        <strong>When</strong> <code>Notifications</code> accepts <code>NotifyUser{ $Message }</code><br/>
        <strong>Then</strong> <code>Notifications</code> is empty<br/>
        <strong>And</strong> <code>Notifications</code> emits <code>UserNotified{ $Message }</code><br/>
      </body>
    </html>
    """;

  protected AcceptanceTestToolTest() {
    super(new AcceptanceTestTool(), AcceptanceTest.class, "test/acceptance");
  }

  @Override
  protected void assertInputs(List<Input<?>> actual) {
    var input = actual.getFirst();
    assertThat(input.glob()).hasToString("src/test/acceptance/**/*.acceptance");
    assertThat(input.format()).isInstanceOf(AcceptanceFormat.class);
  }

  @Test
  void shouldBuild() {
    var testDir = getTestDir("valid");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = getTool().build(source, sink);

    assertThat(actual).isEmpty();
    var output = sink.select("reports/acceptanceTests/Notifications-aggregate.html").getFile();
    assertThat((output)).isFile().hasContent(ACCEPTANCE_TEST_HTML);
  }
}
