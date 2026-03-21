package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.entities;
import static org.setms.swe.inbound.tool.Inputs.initiatives;
import static org.setms.swe.inbound.tool.Inputs.useCases;
import static org.setms.swe.inbound.tool.Inputs.wireframes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Direction;
import org.setms.swe.domain.model.sdlc.ux.InputField;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

public class CommandTool extends DtoCodeTool<Command> {

  private static final String CREATE_WIREFRAME = "wireframe.create";

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
    return Stream.of(
            Stream.of(entities(), useCases(), wireframes(), decisions(), initiatives()),
            code().stream())
        .flatMap(s -> s)
        .collect(toSet());
  }

  @Override
  public void validate(Command command, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    super.validate(command, inputs, diagnostics);
    if (isIssuedByUser(command, inputs) && !isInitiatedByWireframe(command, inputs)) {
      diagnostics.add(
          new Diagnostic(
              Level.WARN,
              "Not initiated by wireframe",
              command.toLocation(),
              new Suggestion(CREATE_WIREFRAME, "Create wireframe")));
    }
  }

  private boolean isIssuedByUser(Command command, ResolvedInputs inputs) {
    return inputs.get(UseCase.class).stream()
        .anyMatch(
            useCase -> useCase.predecessorsOf(command).anyMatch(link -> link.hasType("user")));
  }

  private boolean isInitiatedByWireframe(Command command, ResolvedInputs inputs) {
    return inputs.get(Wireframe.class).stream()
        .anyMatch(wireframe -> initiates(command, wireframe));
  }

  private boolean initiates(Command command, Wireframe wireframe) {
    return wireframe.initiates(command)
        || wireframe.getName().equals("Initiate" + command.getName());
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
      case CREATE_WIREFRAME -> generateWireframeFor(commandResource, command, inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateCodeFor(
      Resource<?> commandResource, Command command, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return getResolver()
        .codeGenerator(inputs, diagnostics)
        .flatMap(
            generator ->
                Optional.ofNullable(command.getPayload())
                    .flatMap(link -> link.resolveFrom(inputs.get(Entity.class)))
                    .map(entity -> writeCode(generator.generate(command, entity), commandResource)))
        .orElseGet(AppliedSuggestion::none);
  }

  private AppliedSuggestion generateWireframeFor(
      Resource<?> commandResource, Command command, ResolvedInputs inputs) {
    var wireframe = toWireframe(command, inputs);
    var wireframeResource = resourceFor(wireframe, command, commandResource);
    try (var output = wireframeResource.writeTo()) {
      builderFor(wireframe).build(wireframe, output);
    } catch (IOException e) {
      return failedWith(e);
    }
    return created(wireframeResource);
  }

  private Wireframe toWireframe(Command command, ResolvedInputs inputs) {
    var fqn =
        new FullyQualifiedName("%s.Initiate%s".formatted(command.getPackage(), command.getName()));
    var affordance =
        new Affordance(fqn)
            .setInputFields(
                Optional.ofNullable(command.getPayload())
                    .flatMap(payload -> payload.resolveFrom(inputs.get(Entity.class)))
                    .stream()
                    .map(Entity::getFields)
                    .flatMap(Collection::stream)
                    .map(
                        field ->
                            new InputField(field.getFullyQualifiedName()).setType(field.getType()))
                    .toList());
    var container =
        new Container(fqn).setDirection(Direction.TOP_TO_BOTTOM).setChildren(List.of(affordance));
    return new Wireframe(fqn).setContainers(List.of(container));
  }
}
