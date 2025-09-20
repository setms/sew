package org.setms.swe.domain.model.sdlc.architecture;

import static java.util.Collections.emptySet;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.setms.swe.domain.model.sdlc.lang.ProgrammingLanguage;

@RequiredArgsConstructor(access = PRIVATE)
public class Topics {

  private static final Map<String, Set<String>> TOPICS = new TreeMap<>();

  static {
    reload();
  }

  public static void reload() {
    clear();
    var classLoader = ProgrammingLanguage.class.getClassLoader();
    var programmingLanguages = new TreeSet<String>();
    for (var programmingLanguage : ServiceLoader.load(ProgrammingLanguage.class, classLoader)) {
      programmingLanguages.add(programmingLanguage.getName());
      TOPICS.putAll(programmingLanguage.getTopics());
    }
    TOPICS.put(ProgrammingLanguage.TOPIC, programmingLanguages);
  }

  public static void clear() {
    TOPICS.clear();
  }

  public static Collection<String> topics() {
    return TOPICS.keySet();
  }

  public static Collection<String> choicesFor(String topic) {
    return TOPICS.getOrDefault(topic, emptySet());
  }
}
