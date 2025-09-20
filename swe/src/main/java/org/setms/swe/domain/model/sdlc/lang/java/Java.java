package org.setms.swe.domain.model.sdlc.lang.java;

import java.util.Map;
import java.util.Set;
import org.setms.swe.domain.model.sdlc.lang.ProgrammingLanguage;

public class Java implements ProgrammingLanguage {

  @Override
  public String getName() {
    return "Java";
  }

  @Override
  public Map<String, Set<String>> getTopics() {
    return Map.of();
  }
}
