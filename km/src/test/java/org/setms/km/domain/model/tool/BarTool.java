package org.setms.km.domain.model.tool;


public class BarTool extends ArtifactTool<Bar> {

  @Override
  public Input<Bar> validationTarget() {
    return new GlobInput<>("bar", null, Bar.class);
  }
}
