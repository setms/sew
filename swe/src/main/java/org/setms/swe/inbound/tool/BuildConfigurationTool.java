package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.projects;
import static org.setms.swe.inbound.tool.Inputs.unitTests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.StandaloneTool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

@RequiredArgsConstructor
public class BuildConfigurationTool extends StandaloneTool {

  private final TechnologyResolver technologyResolver;

  public BuildConfigurationTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>();
    result.add(projects());
    result.add(decisions());
    result.addAll(unitTests());
    return result;
  }

  @Override
  public void validate(
      ResolvedInputs inputs, Resource<?> root, Collection<Diagnostic> diagnostics) {
    if (inputs.get(UnitTest.class).isEmpty()) {
      return;
    }
    var buildTool = technologyResolver.buildTool(null, inputs, null, diagnostics);
    buildTool.ifPresent(bt -> bt.validate(root, diagnostics));
  }

  @Override
  protected AppliedSuggestion doApply(
      String suggestionCode, Location location, ResolvedInputs inputs, Resource<?> resource)
      throws Exception {
    // First try TechnologyResolver for decision-related suggestions
    var technologyResult = technologyResolver.applySuggestion(suggestionCode, resource);
    if (!technologyResult.createdOrChanged().isEmpty()
        || !technologyResult.diagnostics().isEmpty()) {
      return technologyResult;
    }
    // Then try BuildTool for build configuration suggestions
    var buildTool = technologyResolver.buildTool(resource, inputs, location, new ArrayList<>());
    if (buildTool.isPresent()) {
      var result = buildTool.get().applySuggestion(suggestionCode, resource);
      if (!result.createdOrChanged().isEmpty() || !result.diagnostics().isEmpty()) {
        return result;
      }
    }
    return super.doApply(suggestionCode, location, inputs, resource);
  }
}
