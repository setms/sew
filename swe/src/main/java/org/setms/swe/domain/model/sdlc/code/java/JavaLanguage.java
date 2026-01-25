package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Map;
import java.util.Set;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;

public class JavaLanguage implements TopicProvider {

  @Override
  public Map<String, Set<String>> validChoices() {
    return Map.of(ProgrammingLanguage.TOPIC, Set.of("Java"));
  }
}
