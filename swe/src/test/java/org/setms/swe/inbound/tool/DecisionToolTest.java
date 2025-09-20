package org.setms.swe.inbound.tool;

import org.setms.swe.domain.model.sdlc.architecture.Decision;

class DecisionToolTest extends ToolTestCase<Decision> {

  DecisionToolTest() {
    super(new DecisionTool(), Decision.class, "main/architecture");
  }
}
