package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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

  protected AcceptanceTestToolTest() {
    super(new AcceptanceTestTool(), AcceptanceFormat.class, "test/acceptance", "acceptance");
  }

  @Test
  void shouldBuild() {
    var workspace = workspaceFor("valid");

    var actual = build(workspace);

    assertThat(actual).isEmpty();
    var output = toFile(workspace.root().select("build/Notifications-aggregate.html"));
    assertThat((output)).isFile().hasContent(AGGREGATE_ACCEPTANCE_TEST_HTML);
  }
}
