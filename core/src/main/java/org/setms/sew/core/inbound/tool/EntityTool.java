package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.entities;

import java.util.List;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.Tool;

public class EntityTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(entities());
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }
}
