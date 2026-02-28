package org.setms.swe.inbound.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;

import java.util.Collection;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;

@NoArgsConstructor(access = PRIVATE)
class CodeWriter {

  static AppliedSuggestion writeCode(Collection<CodeArtifact> artifacts, Resource<?> resource) {
    return artifacts.stream()
        .map(artifact -> writeCodeArtifact(artifact, resource))
        .flatMap(applied -> applied.createdOrChanged().stream())
        .reduce(AppliedSuggestion.none(), AppliedSuggestion::with, (a, _) -> a);
  }

  private static AppliedSuggestion writeCodeArtifact(CodeArtifact artifact, Resource<?> resource) {
    try {
      var path = artifact.getPackage().replace('.', '/');
      var target =
          resource.select("/src/main/java").select(path).select(artifact.getName() + ".java");
      try (var output = target.writeTo()) {
        output.write(artifact.getCode().getBytes(UTF_8));
      }
      return created(target);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
