package org.setms.km.domain.model.tool;


public class BarTool extends BaseTool<Bar> {

  @Override
  public Input<Bar> getMainInput() {
    return new Input<>("bar", null, Bar.class);
  }
}
