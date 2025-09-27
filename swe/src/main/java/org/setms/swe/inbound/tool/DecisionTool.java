package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;

public class DecisionTool extends ArtifactTool {

  @Override
  public Input<? extends Artifact> validationTarget() {
    return decisions();
  }
}
