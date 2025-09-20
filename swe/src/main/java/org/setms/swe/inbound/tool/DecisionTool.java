package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;

import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Tool;
import org.setms.swe.domain.model.sdlc.architecture.Decision;

public class DecisionTool extends Tool<Decision> {

  @Override
  public Input<Decision> getMainInput() {
    return decisions();
  }

}
