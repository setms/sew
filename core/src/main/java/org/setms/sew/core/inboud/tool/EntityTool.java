package org.setms.sew.core.inboud.tool;

import java.util.List;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.Tool;

public class EntityTool extends Tool {
  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("src/main/design", Entity.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }
}
