package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.databaseSchemas;

import java.util.HashSet;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;

public class DatabaseSchemaTool extends ArtifactTool<DatabaseSchema> {

  @Override
  public Set<Input<? extends DatabaseSchema>> validationTargets() {
    return databaseSchemas();
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return new HashSet<>(code());
  }
}
