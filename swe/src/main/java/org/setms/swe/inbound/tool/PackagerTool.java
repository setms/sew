package org.setms.swe.inbound.tool;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class PackagerTool extends ArtifactTool<CodeArtifact> {

  private final TechnologyResolver resolver;

  @SuppressWarnings("unused") // Called by ServiceLoader
  public PackagerTool() {
    this(new TechnologyResolverImpl());
  }

  PackagerTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public Set<Input<? extends CodeArtifact>> validationTargets() {
    return new LinkedHashSet<>(Inputs.code());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new LinkedHashSet<Input<? extends Artifact>>(Inputs.packageDescriptions());
    result.add(Inputs.decisions());
    result.add(Inputs.initiatives());
    result.addAll(Inputs.buildConfiguration());
    return result;
  }

  @Override
  public void validate(
      Resource<?> resource,
      CodeArtifact artifact,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    resolver
        .codePackager(inputs, diagnostics)
        .ifPresent(packager -> packager.packageCode(resource.select("/"), diagnostics));
  }
}
