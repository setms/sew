package org.setms.km.domain.model.tool;


import java.util.List;
import java.util.Optional;

public class BarTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("bar", null, Bar.class));
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }
}
