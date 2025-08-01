package org.setms.sew.core.inbound.tool;

import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.sew.core.inbound.tool.Inputs.commands;
import static org.setms.sew.core.inbound.tool.Inputs.entities;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;
import org.setms.sew.core.inbound.format.sal.SalFormat;

public class CommandTool extends BaseTool {

  public static final String CREATE_PAYLOAD = "payload.create";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(commands(), entities());
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
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
  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    if (CREATE_PAYLOAD.equals(suggestionCode)) {
      createPayload(inputs.get(Command.class), location, resource, diagnostics);
    } else {
      super.apply(suggestionCode, inputs, location, resource, diagnostics);
    }
  }

  private void createPayload(
      List<Command> commands,
      Location location,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    commands.stream()
        .filter(command -> command.starts(location))
        .findFirst()
        .ifPresent(command -> createPayloadFor(command, resource, diagnostics));
  }

  private void createPayloadFor(
      Command command, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var designResource = toBase(resource).select(Inputs.PATH_DESIGN);
    try {
      var entity =
          new Entity(new FullyQualifiedName(command.getPackage(), command.getPayload().getId()));
      var entityResource = designResource.select(entity.getName() + ".entity");
      try (var output = entityResource.writeTo()) {
        new SalFormat().newBuilder().build(entity, output);
      }
      diagnostics.add(resourceCreated(entityResource));
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }
}
