package org.setms.km.domain.model.tool;

import static java.util.function.Predicate.not;
import static lombok.AccessLevel.PRIVATE;

import java.util.*;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;

@NoArgsConstructor(access = PRIVATE)
public class Tools {

  private static final Map<Class<? extends Artifact>, Tool> toolsByArtifactType = new HashMap<>();

  static {
    for (var tool : ServiceLoader.load(Tool.class)) {
      add(tool);
    }
  }

  public static void add(Tool tool) {
    Optional.ofNullable(tool)
        .map(Tool::getInputs)
        .filter(not(Collection::isEmpty))
        .map(List::getFirst)
        .map(Input::type)
        .ifPresent(type -> toolsByArtifactType.put(type, tool));
  }

  public static <T extends Artifact> Optional<Tool> handling(Class<T> type) {
    return Optional.ofNullable(toolsByArtifactType.get(type));
  }
}
