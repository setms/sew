package org.setms.swe.inbound.tool;

import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

abstract class DtoCodeTool<A extends Artifact> extends ArtifactTool<A> {

  public static final String CREATE_PAYLOAD = "payload.create";
  public static final String GENERATE_CODE = "code.generate";

  final TechnologyResolver resolver;

  DtoCodeTool() {
    this(new TechnologyResolverImpl());
  }

  DtoCodeTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  protected Optional<CodeArtifact> codeFor(A artifact, ResolvedInputs inputs) {
    return inputs.get(CodeArtifact.class).stream()
        .filter(
            ca ->
                ca.getName().equals(artifact.getName())
                    && ca.getPackage().endsWith(".domain.model"))
        .findFirst();
  }
}
