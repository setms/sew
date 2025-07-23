package org.setms.km.domain.model.tool;

import static java.util.function.Predicate.not;
import static lombok.AccessLevel.PRIVATE;

import java.util.*;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;

@NoArgsConstructor(access = PRIVATE)
public class ToolRegistry {

  private static final Map<Class<? extends Artifact>, BaseTool> toolsByArtifactType =
      new HashMap<>();

  static {
    for (var tool : ServiceLoader.load(BaseTool.class)) {
      add(tool);
    }
  }

  public static void add(BaseTool tool) {
    Optional.ofNullable(tool)
        .map(BaseTool::getInputs)
        .filter(not(Collection::isEmpty))
        .map(List::getFirst)
        .map(Input::type)
        .ifPresent(type -> toolsByArtifactType.put(type, tool));
  }

  public static <T extends Artifact> Optional<BaseTool> handling(Class<T> type) {
    return Optional.ofNullable(toolsByArtifactType.get(type));
  }
}
