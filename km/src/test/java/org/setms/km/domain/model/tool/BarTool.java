package org.setms.km.domain.model.tool;

import org.setms.km.domain.model.artifact.Artifact;

public class BarTool extends ArtifactTool {

  @Override
  public Input<? extends Artifact> validationTarget() {
    return new GlobInput<>("bar", null, Bar.class);
  }
}
