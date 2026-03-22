package org.setms.swe.inbound.tool;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;

import java.util.Collection;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

@NoArgsConstructor(access = PRIVATE)
class CodeWriter {

  static AppliedSuggestion writeCode(Collection<CodeArtifact> artifacts, Resource<?> resource) {
    return writeAll(
        artifacts, artifact -> writeCodeArtifact(artifact, javaTarget(artifact, resource)));
  }

  private static AppliedSuggestion writeAll(
      Collection<CodeArtifact> artifacts, Function<CodeArtifact, AppliedSuggestion> write) {
    return artifacts.stream()
        .map(write)
        .flatMap(applied -> applied.createdOrChanged().stream())
        .reduce(AppliedSuggestion.none(), AppliedSuggestion::with, (a, ignored) -> a);
  }

  private static AppliedSuggestion writeCodeArtifact(CodeArtifact artifact, Resource<?> target) {
    try {
      target.writeAsString(artifact.getCode());
      return created(target);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private static Resource<?> javaTarget(CodeArtifact artifact, Resource<?> resource) {
    var path = artifact.getPackage().replace('.', '/');
    return resource.select("/src/main/java").select(path).select(artifact.getName() + ".java");
  }

  static AppliedSuggestion writeCode(
      Collection<CodeArtifact> artifacts,
      Resource<?> resource,
      ProgrammingLanguageConventions conventions) {
    return writeAll(
        artifacts,
        artifact ->
            writeCodeArtifact(artifact, conventionsTarget(artifact, conventions, resource)));
  }

  private static Resource<?> conventionsTarget(
      CodeArtifact artifact, ProgrammingLanguageConventions conventions, Resource<?> resource) {
    return resource
        .select(conventions.codePath())
        .select(artifact.getName() + "." + conventions.extension());
  }
}
