package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.databaseSchemas;
import static org.setms.swe.inbound.tool.Inputs.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

@RequiredArgsConstructor
public class EntityTool extends ArtifactTool<Entity> {

  private final TechnologyResolver resolver;

  public EntityTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Set<Input<? extends Entity>> validationTargets() {
    return Set.of(entities());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return new HashSet<>(databaseSchemas());
  }

  @Override
  public void validate(Entity entity, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (hasDatabaseSchema(entity, inputs)) {
      return;
    }
    resolver.database(inputs, diagnostics);
  }

  private boolean hasDatabaseSchema(Entity entity, ResolvedInputs inputs) {
    return inputs.get(DatabaseSchema.class).stream()
        .anyMatch(schema -> schema.getName().equalsIgnoreCase(entity.getName()));
  }
}
