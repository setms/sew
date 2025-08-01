package org.setms.km.domain.model.tool;

import static lombok.AccessLevel.PRIVATE;

import java.util.*;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;

@NoArgsConstructor(access = PRIVATE)
public class Tools {

  private static final Collection<BaseTool> tools = new HashSet<>();

  static {
    reload();
  }

  public static void reload() {
    clear();
    var classLoader = BaseTool.class.getClassLoader();
    for (var tool : ServiceLoader.load(BaseTool.class, classLoader)) {
      add(tool);
    }
  }

  public static void clear() {
    tools.clear();
  }

  public static void add(BaseTool tool) {
    tools.add(tool);
  }

  public static <T extends Artifact> Optional<BaseTool> targeting(Class<T> type) {
    return tools.stream()
        .filter(tool -> tool.getInputs().stream().limit(1).map(Input::type).anyMatch(type::equals))
        .findFirst();
  }

  public static Collection<BaseTool> dependingOn(Class<? extends Artifact> type) {
    return tools.stream()
        .filter(tool -> tool.getInputs().stream().map(Input::type).anyMatch(type::equals))
        .toList();
  }

  public static Stream<BaseTool> all() {
    return tools.stream();
  }
}
