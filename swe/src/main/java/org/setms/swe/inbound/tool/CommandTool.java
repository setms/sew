package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.entities;

import java.util.ArrayList;
import java.util.Collection;
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
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class CommandTool extends ArtifactTool<Command> {

  public static final String CREATE_PAYLOAD = "payload.create";
  public static final String GENERATE_CODE = "code.generate";

  private final TechnologyResolver resolver;

  public CommandTool() {
    this(new TechnologyResolverImpl());
  }

  CommandTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

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
    var entities = inputs.get(Entity.class);
    validatePayload(command, entities, diagnostics);
    validateCode(command, entities, inputs.get(CodeArtifact.class), diagnostics);
  }

  private void validatePayload(
      Command command, Collection<Entity> entities, Collection<Diagnostic> diagnostics) {
    var payload = command.getPayload();
    if (payload != null) {
      if (payload.resolveFrom(entities).isEmpty()) {
        diagnostics.add(
            new Diagnostic(
                WARN,
                "Missing entity %s".formatted(payload.getId()),
                command.toLocation(),
                new Suggestion(CREATE_PAYLOAD, "Create entity")));
      }
    }
  }

  private void validateCode(
      Command command,
      Collection<Entity> entities,
      Collection<CodeArtifact> codeArtifacts,
      Collection<Diagnostic> diagnostics) {
    var payload = command.getPayload();
    if (payload != null
        && payload.resolveFrom(entities).isPresent()
        && codeArtifacts.stream()
            .noneMatch(
                ca ->
                    ca.getName().equals("%sCommand".formatted(command.getName()))
                        && ca.getPackage().endsWith(".domain.model"))) {
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
      case GENERATE_CODE -> generateCodeFor(commandResource, command, inputs);
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

  private AppliedSuggestion generateCodeFor(
      Resource<?> commandResource, Command command, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return resolver
        .codeGenerator(inputs, diagnostics)
        .map(generator -> writeCode(generator.generate(command), commandResource))
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
