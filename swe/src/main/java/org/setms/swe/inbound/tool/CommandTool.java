package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.entities;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class CommandTool extends DtoCodeTool<Command> {

  public CommandTool() {}

  CommandTool(TechnologyResolver resolver) {
    super(resolver);
  }

  @Override
  public Set<Input<? extends Command>> validationTargets() {
    return Set.of(commands());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Stream.of(Stream.of(entities(), decisions(), initiatives()), code().stream())
        .flatMap(s -> s)
        .collect(toSet());
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
      case GENERATE_CODE -> generateCodeFor(commandResource, command, inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateCodeFor(
      Resource<?> commandResource, Command command, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return resolver
        .codeGenerator(inputs, diagnostics)
        .flatMap(
            generator ->
                Optional.ofNullable(command.getPayload())
                    .flatMap(link -> link.resolveFrom(inputs.get(Entity.class)))
                    .map(entity -> writeCode(generator.generate(command, entity), commandResource)))
        .orElseGet(AppliedSuggestion::none);
  }

  private AppliedSuggestion writeCode(
      Collection<CodeArtifact> artifacts, Resource<?> commandResource) {
    return artifacts.stream()
        .map(artifact -> writeCodeArtifact(artifact, commandResource))
        .flatMap(applied -> applied.createdOrChanged().stream())
        .reduce(AppliedSuggestion.none(), AppliedSuggestion::with, (a, _) -> a);
  }

  private AppliedSuggestion writeCodeArtifact(CodeArtifact artifact, Resource<?> commandResource) {
    try {
      var path = artifact.getPackage().replace('.', '/');
      var resource =
          commandResource
              .select("/src/main/java")
              .select(path)
              .select(artifact.getName() + ".java");
      try (var output = resource.writeTo()) {
        output.write(artifact.getCode().getBytes(UTF_8));
      }
      return created(resource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
