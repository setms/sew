package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;

import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.architecture.Decision;

public class DecisionTool extends ArtifactTool<Decision> {

  @Override
  public Input<Decision> validationTarget() {
    return decisions();
  }
}
