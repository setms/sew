package org.setms.km.domain.model.tool;

import static java.util.Collections.emptyList;

import java.util.List;

public class BarTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("bar", null, Bar.class));
  }

  @Override
  public List<Output> getOutputs() {
    return emptyList();
  }
}
