package org.setms.km.domain.model.tool;

import static lombok.AccessLevel.PRIVATE;

import java.util.*;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;

@NoArgsConstructor(access = PRIVATE)
public class Tools {

  private static final Collection<Tool<?>> tools = new HashSet<>();

  static {
    reload();
  }

  public static void reload() {
    clear();
    var classLoader = Tool.class.getClassLoader();
    for (var tool : ServiceLoader.load(Tool.class, classLoader)) {
      add(tool);
    }
  }

  public static void clear() {
    tools.clear();
  }

  public static void add(Tool<?> tool) {
    tools.add(tool);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Artifact> Optional<Tool<T>> targeting(Class<T> type) {
    return tools.stream()
        .filter(tool -> tool.mainInput().map(Input::type).filter(type::equals).isPresent())
        .map(tool -> (Tool<T>) tool)
        .findFirst();
  }

  public static Collection<Tool<?>> dependingOn(Class<? extends Artifact> type) {
    return tools.stream()
        .filter(tool -> tool.additionalInputs().stream().map(Input::type).anyMatch(type::equals))
        .toList();
  }

  public static Stream<Tool<?>> all() {
    return tools.stream();
  }

  public static <T extends Artifact> Builder builderFor(T artifact) {
    return all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .filter(input -> input.type().equals(artifact.getClass()))
        .findFirst()
        .map(Input::format)
        .map(Format::newBuilder)
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Can't find builder for %s".formatted(artifact.getClass().getName())));
  }
}
