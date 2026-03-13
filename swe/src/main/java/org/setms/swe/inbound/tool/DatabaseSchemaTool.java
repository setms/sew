package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.databaseSchemas;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

@RequiredArgsConstructor
public class DatabaseSchemaTool extends ArtifactTool<DatabaseSchema> {

  static final String CREATE_ENTITY = "entity.create";

  private final TechnologyResolver resolver;

  public DatabaseSchemaTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Set<Input<? extends DatabaseSchema>> validationTargets() {
    return databaseSchemas();
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return new HashSet<>(code());
  }

  @Override
  public void validate(
      DatabaseSchema schema, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (hasEntityCode(schema, inputs)) {
      return;
    }
    resolver
        .frameworkCodeGenerator(inputs, diagnostics)
        .ifPresent(
            ignored ->
                diagnostics.add(
                    new Diagnostic(
                        WARN,
                        "Missing entity object",
                        schema.toLocation(),
                        new Suggestion(CREATE_ENTITY, "Create entity object"))));
  }

  private boolean hasEntityCode(DatabaseSchema schema, ResolvedInputs inputs) {
    var entityName = schema.getName() + "Entity";
    return inputs.get(CodeArtifact.class).stream().anyMatch(ca -> ca.getName().equals(entityName));
  }
}
