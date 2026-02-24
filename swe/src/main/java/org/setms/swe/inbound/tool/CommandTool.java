package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.entities;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

public class CommandTool extends ArtifactTool<Command> {

  public static final String CREATE_PAYLOAD = "payload.create";
  public static final String GENERATE_CODE = "code.generate";

  @Override
  public Set<Input<? extends Command>> validationTargets() {
    return Set.of(commands());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Stream.concat(Stream.of(entities()), code().stream()).collect(toSet());
  }

  @Override
  public void validate(Command command, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    validate(command, inputs.get(Entity.class), diagnostics);
    validateCode(command, inputs.get(CodeArtifact.class), diagnostics);
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

  private void validateCode(
      Command command, List<CodeArtifact> codeArtifacts, Collection<Diagnostic> diagnostics) {
    if (codeArtifacts.stream().noneMatch(ca -> ca.getName().equals(command.getName()))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing code",
              command.toLocation(),
              new Suggestion(GENERATE_CODE, "Generate code")));
    }
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> commandResource,
      Command command,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case CREATE_PAYLOAD -> createPayloadFor(commandResource, command);
      case GENERATE_CODE -> generateCodeFor(command, inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion createPayloadFor(Resource<?> commandResource, Command command) {
    try {
      var entity =
          new Entity(new FullyQualifiedName(command.getPackage(), command.getPayload().getId()));
      var entityResource = resourceFor(entity, command, commandResource);
      try (var output = entityResource.writeTo()) {
        builderFor(entity).build(entity, output);
      }
      return created(entityResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private AppliedSuggestion generateCodeFor(Command command, ResolvedInputs inputs) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
