package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.entities;

import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;

public class EntityTool extends BaseTool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(entities());
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }
}
