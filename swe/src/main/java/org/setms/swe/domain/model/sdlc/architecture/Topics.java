package org.setms.swe.domain.model.sdlc.architecture;

import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = PRIVATE)
public class Topics {

  private static final Collection<TopicProvider> providers = new HashSet<>();

  static {
    reload();
  }

  public static void reload() {
    clear();
    var classLoader = TopicProvider.class.getClassLoader();
    for (var provider : ServiceLoader.load(TopicProvider.class, classLoader)) {
      add(provider);
    }
  }

  public static void clear() {
    providers.clear();
  }

  private static void add(TopicProvider provider) {
    providers.add(provider);
  }

  public static Stream<TopicProvider> providers() {
    return providers.stream();
  }

  public static Collection<String> names() {
    return providers.stream()
        .map(TopicProvider::topics)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  public static boolean isValidChoice(String topic, String choice) {
    return providers.stream().anyMatch(provider -> provider.isValidChoice(topic, choice));
  }
}
