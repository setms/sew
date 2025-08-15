package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.entities;

import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Tool;
import org.setms.swe.domain.model.sdlc.design.Entity;

public class EntityTool extends Tool<Entity> {

  @Override
  public Input<Entity> getMainInput() {
    return entities();
  }
}
