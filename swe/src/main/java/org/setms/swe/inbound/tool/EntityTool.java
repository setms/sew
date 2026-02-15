package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.entities;

import java.util.Set;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.design.Entity;

public class EntityTool extends ArtifactTool<Entity> {

  @Override
  public Set<Input<? extends Entity>> validationTargets() {
    return Set.of(entities());
  }
}
