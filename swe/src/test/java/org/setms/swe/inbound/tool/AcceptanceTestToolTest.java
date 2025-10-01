package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.swe.inbound.format.acceptance.AcceptanceFormat;

class AcceptanceTestToolTest extends ToolTestCase<AcceptanceTest> {

  private static final String AGGREGATE_ACCEPTANCE_TEST_HTML =
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
  private static final String REPORT_PATH = "build/Notifications-aggregate.html";

  protected AcceptanceTestToolTest() {
    super(new AcceptanceTestTool(), AcceptanceFormat.class, "test/acceptance", "acceptance");
  }

  @Override
  protected void assertBuild(Resource<?> resource) {
    var output = toFile(resource.select(REPORT_PATH));
    assertThat((output))
        .as("%s exists".formatted(REPORT_PATH))
        .isFile()
        .as("Contents of %s".formatted(REPORT_PATH))
        .hasContent(AGGREGATE_ACCEPTANCE_TEST_HTML);
  }
}
