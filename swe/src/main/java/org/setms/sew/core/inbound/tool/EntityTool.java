package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.entities;

import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.Tool;

public class EntityTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(entities());
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }
}
