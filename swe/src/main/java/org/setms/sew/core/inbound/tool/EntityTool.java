package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.entities;

import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.sew.core.domain.model.sdlc.design.Entity;

public class EntityTool extends BaseTool<Entity> {

  @Override
  public Input<Entity> getMainInput() {
    return entities();
  }
}
