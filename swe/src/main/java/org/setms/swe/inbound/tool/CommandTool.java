package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.entities;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.inbound.format.sal.SalFormat;

public class CommandTool extends BaseTool<Command> {

  public static final String CREATE_PAYLOAD = "payload.create";

  @Override
  public Input<Command> getMainInput() {
    return commands();
  }

  @Override
  public Set<Input<?>> getAdditionalInputs() {
    return Set.of(entities());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var entities = inputs.get(Entity.class);
    inputs.get(Command.class).forEach(command -> validate(command, entities, diagnostics));
  }

  private void validate(
      Command command, List<Entity> entities, Collection<Diagnostic> diagnostics) {
    var payload = command.getPayload();
    if (payload != null) {
      if (payload.resolveFrom(entities).isEmpty()) {
        diagnostics.add(
            new Diagnostic(
                WARN,
                "Missing entity " + payload.getId(),
                command.toLocation(),
                new Suggestion(CREATE_PAYLOAD, "Create entity")));
      }
    }
  }

  @Override
  protected AppliedSuggestion apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      Resource<?> resource,
      AppliedSuggestion appliedSuggestion) {
    if (CREATE_PAYLOAD.equals(suggestionCode)) {
      return createPayload(inputs.get(Command.class), location, resource, appliedSuggestion);
    }
    return super.apply(suggestionCode, inputs, location, resource, appliedSuggestion);
  }

  private AppliedSuggestion createPayload(
      List<Command> commands,
      Location location,
      Resource<?> resource,
      AppliedSuggestion appliedSuggestion) {
    return commands.stream()
        .filter(command1 -> command1.starts(location))
        .findFirst()
        .map(command -> createPayloadFor(command, resource, appliedSuggestion))
        .orElse(appliedSuggestion);
  }

  private AppliedSuggestion createPayloadFor(
      Command command, Resource<?> resource, AppliedSuggestion appliedSuggestion) {
    var designResource = toBase(resource).select(Inputs.PATH_DESIGN);
    try {
      var entity =
          new Entity(new FullyQualifiedName(command.getPackage(), command.getPayload().getId()));
      var entityResource = designResource.select(entity.getName() + ".entity");
      try (var output = entityResource.writeTo()) {
        new SalFormat().newBuilder().build(entity, output);
      }
      return appliedSuggestion.with(entityResource);
    } catch (Exception e) {
      return appliedSuggestion.with(e);
    }
  }
}
