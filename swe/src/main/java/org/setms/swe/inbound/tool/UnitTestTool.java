package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.buildConfiguration;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.initiatives;
import static org.setms.swe.inbound.tool.Inputs.unitTestHelpers;
import static org.setms.swe.inbound.tool.Inputs.unitTests;

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
import org.setms.km.domain.model.workspace.Resource;
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
    return new HashSet<>(unitTests());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>();
    result.add(decisions());
    result.add(initiatives());
    result.addAll(unitTestHelpers());
    result.addAll(code());
    result.addAll(buildConfiguration());
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
      UnitTest unitTest,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    technologyResolver
        .codeTester(inputs, diagnostics)
        .ifPresent(codeTester -> codeTester.test(resource.root(), diagnostics));
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
