package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Map;
import java.util.Set;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

public class JavaLanguage implements TopicProvider, ProgrammingLanguageConventions {

  @Override
  public Map<String, Set<String>> validChoices() {
    return Map.of(ProgrammingLanguage.TOPIC, Set.of("Java"));
  }

  @Override
  public String extension() {
    return "java";
  }

  @Override
  public String unitTestPath() {
    return "src/test/java";
  }
}
