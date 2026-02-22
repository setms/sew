package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toMap;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.projects;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
import org.setms.swe.domain.model.sdlc.architecture.BuildTool;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

@RequiredArgsConstructor
public class CodeTool extends ArtifactTool<CodeArtifact> {

  private final TechnologyResolver technologyResolver;

  public CodeTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Set<Input<? extends CodeArtifact>> validationTargets() {
    var result = new HashSet<Input<? extends CodeArtifact>>();
    result.addAll(Inputs.code());
    result.addAll(Inputs.unitTests());
    result.addAll(Inputs.unitTestHelpers());
    return result;
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(decisions(), projects());
  }

  @Override
  public boolean validates(String path) {
    // Allow applying build tool suggestion from root, since it's not tied to any one specific file,
    // but all of them
    if ("/".equals(path)) {
      return true;
    }
    return super.validates(path);
  }

  @Override
  public CodeArtifact validate(
      Resource<?> resource, ResolvedInputs context, Collection<Diagnostic> diagnostics) {
    var result = super.validate(resource, context, diagnostics);
    validateBuildTool(resource.select("/"), context, diagnostics);
    return result;
  }

  private void validateBuildTool(
      Resource<?> root, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var topics = groupByTopic(inputs.get(Decision.class));
    if (topics.get(BuildTool.TOPIC) != null) {
      technologyResolver.buildTool(root, inputs, null, diagnostics);
    }
  }

  @Override
  public void validate(
      CodeArtifact codeArtifact, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var topics = groupByTopic(inputs.get(Decision.class));
    var buildToolChoice = topics.get(BuildTool.TOPIC);
    if (buildToolChoice == null) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing decision on build tool",
              null,
              new Suggestion(TechnologyResolverImpl.PICK_BUILD_TOOL, "Decide on build tool")));
    }
  }

  private Map<String, String> groupByTopic(Collection<Decision> decisions) {
    return decisions.stream()
        .filter(decision -> decision.getChoice() != null)
        .collect(toMap(Decision::getTopic, Decision::getChoice));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      CodeArtifact artifact,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return technologyResolver.applySuggestion(suggestionCode, resource, inputs);
  }
}
