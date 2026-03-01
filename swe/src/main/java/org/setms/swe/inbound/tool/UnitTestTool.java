package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;
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
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

@RequiredArgsConstructor
public class UnitTestTool extends ArtifactTool<UnitTest> {

  private final TechnologyResolver technologyResolver;

  public UnitTestTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Set<Input<? extends UnitTest>> validationTargets() {
    return new HashSet<>(Inputs.unitTests());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(decisions(), initiatives());
  }

  @Override
  public UnitTest validate(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var result = super.validate(resource, inputs, diagnostics);
    technologyResolver
        .codeTester(inputs, new ArrayList<>())
        .ifPresent(codeTester -> codeTester.test(resource, diagnostics));
    return result;
  }

  @Override
  public void validate(
      UnitTest unitTest, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (Decisions.from(inputs).about(BuildSystem.TOPIC) == null) {
      diagnostics.add(missingBuildSystemDecision());
    }
  }

  private Diagnostic missingBuildSystemDecision() {
    return new Diagnostic(
        WARN,
        "Missing decision on build system",
        null,
        new Suggestion(TechnologyResolverImpl.PICK_BUILD_SYSTEM, "Decide on build system"));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      UnitTest artifact,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return technologyResolver.applySuggestion(suggestionCode, resource, inputs);
  }
}
