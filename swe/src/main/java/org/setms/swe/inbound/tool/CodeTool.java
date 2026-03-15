package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.buildConfiguration;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.initiatives;
import static org.setms.swe.inbound.tool.Inputs.packageDescriptions;

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
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
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
    return new HashSet<Input<? extends CodeArtifact>>(Inputs.code());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>();
    result.add(decisions());
    result.add(initiatives());
    result.addAll(buildConfiguration());
    result.addAll(packageDescriptions());
    return result;
  }

  @Override
  public boolean validates(String path) {
    // Handle issues that aren't related to a specific artifact
    if ("/".equals(path)) {
      return true;
    }
    return super.validates(path);
  }

  @Override
  public void validate(
      Resource<?> resource,
      CodeArtifact codeArtifact,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    var decisions = Decisions.from(inputs);
    if (decisions.existFor(BuildSystem.TOPIC)) {
      validateBuild(resource, inputs, diagnostics);
    } else {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing decision on build system",
              null,
              new Suggestion(TechnologyResolverImpl.PICK_BUILD_SYSTEM, "Decide on build system")));
    }
  }

  private void validateBuild(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var root = resource.select("/");
    technologyResolver
        .codeBuilder(root, inputs, diagnostics)
        .filter(ignored -> diagnostics.isEmpty())
        .ifPresent(
            codeBuilder -> {
              var numDiagnostics = diagnostics.size();
              codeBuilder.build(root, diagnostics);
              if (numDiagnostics == diagnostics.size()) {
                technologyResolver
                    .codePackager(inputs, diagnostics)
                    .ifPresent(codePackager -> codePackager.packageCode(root, diagnostics));
              }
            });
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
