package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.HasPayload;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

abstract class DtoCodeTool<A extends Artifact & HasPayload> extends ArtifactTool<A> {

  public static final String CREATE_PAYLOAD = "payload.create";
  public static final String GENERATE_CODE = "code.generate";

  final TechnologyResolver resolver;

  DtoCodeTool() {
    this(new TechnologyResolverImpl());
  }

  DtoCodeTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void validate(A artifact, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var before = diagnostics.size();
    validatePayload(artifact, inputs.get(Entity.class), diagnostics);
    if (diagnostics.size() == before) {
      validateCode(artifact, inputs, diagnostics);
    }
  }

  private void validatePayload(
      A artifact, Collection<Entity> entities, Collection<Diagnostic> diagnostics) {
    Optional.ofNullable(artifact.getPayload())
        .filter(payload -> payload.resolveFrom(entities).isEmpty())
        .ifPresent(
            payload ->
                diagnostics.add(
                    new Diagnostic(
                        WARN,
                        "Unknown entity '%s'".formatted(payload.getId()),
                        artifact.toLocation(),
                        new Suggestion(CREATE_PAYLOAD, "Create entity"))));
  }

  protected void validateCode(
      A artifact, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (artifact.getPayload() == null || resolver.codeGenerator(inputs, diagnostics).isEmpty()) {
      return;
    }
    if (codeFor(artifact, inputs).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing domain object",
              artifact.toLocation(),
              new Suggestion(GENERATE_CODE, "Generate domain object")));
    }
  }

  protected AppliedSuggestion createPayloadFor(Resource<?> artifactResource, A artifact) {
    try {
      var entity =
          new Entity(new FullyQualifiedName(artifact.getPackage(), artifact.getPayload().getId()));
      var entityResource = resourceFor(entity, artifact, artifactResource);
      try (var output = entityResource.writeTo()) {
        builderFor(entity).build(entity, output);
      }
      return created(entityResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  protected Optional<CodeArtifact> codeFor(A artifact, ResolvedInputs inputs) {
    return inputs.get(CodeArtifact.class).stream()
        .filter(
            ca ->
                ca.getName().equals(artifact.getName())
                    && ca.getPackage().endsWith(".domain.model"))
        .findFirst();
  }

  protected AppliedSuggestion writeCode(
      Collection<CodeArtifact> artifacts, Resource<?> eventResource) {
    return artifacts.stream()
        .map(artifact -> writeCodeArtifact(artifact, eventResource))
        .flatMap(applied -> applied.createdOrChanged().stream())
        .reduce(AppliedSuggestion.none(), AppliedSuggestion::with, (a, _) -> a);
  }

  private AppliedSuggestion writeCodeArtifact(CodeArtifact artifact, Resource<?> eventResource) {
    try {
      var path = artifact.getPackage().replace('.', '/');
      var resource =
          eventResource.select("/src/main/java").select(path).select(artifact.getName() + ".java");
      try (var output = resource.writeTo()) {
        output.write(artifact.getCode().getBytes(UTF_8));
      }
      return created(resource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
