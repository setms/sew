package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Map;
import java.util.Set;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;

public class JavaLanguage implements TopicProvider {

  public static final String BUILD_TOOL = "JavaBuildTool";

  @Override
  public Set<String> topics() {
    return Set.of(BUILD_TOOL);
  }

  @Override
  public Map<String, Set<String>> validChoices() {
    return Map.of(ProgrammingLanguage.TOPIC, Set.of("Java"));
  }
}
