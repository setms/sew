package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.databaseSchemas;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

@RequiredArgsConstructor
public class DatabaseSchemaTool extends ArtifactTool<DatabaseSchema> {

  static final String CREATE_ENTITY = "entity.object.create";

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
    var result = new HashSet<Input<? extends Artifact>>(code());
    result.add(decisions());
    result.add(initiatives());
    return result;
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
    return inputs.get(CodeArtifact.class).stream()
        .anyMatch(code -> code.getName().equals(entityName));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      DatabaseSchema schema,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case CREATE_ENTITY -> generateEntityFor(resource, schema, inputs);
      default -> AppliedSuggestion.unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateEntityFor(
      Resource<?> resource, DatabaseSchema schema, ResolvedInputs inputs) {
    return resolver
        .frameworkCodeGenerator(inputs, new ArrayList<>())
        .map(
            generator ->
                CodeWriter.writeCode(
                    generator.generateEntityFor(
                        null,
                        schema,
                        resolver.database(inputs, new HashSet<>()).orElseThrow(),
                        resource),
                    resource))
        .orElseGet(AppliedSuggestion::none);
  }
}
