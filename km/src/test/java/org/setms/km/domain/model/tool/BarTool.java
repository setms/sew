package org.setms.km.domain.model.tool;

import java.util.List;

public class BarTool extends BaseTool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("bar", null, Bar.class));
  }
}
