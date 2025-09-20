package org.setms.km.domain.model.tool;

public class BarTool extends Tool<Bar> {

  @Override
  public Input<Bar> getMainInput() {
    return new GlobInput<>("bar", null, Bar.class);
  }
}
