package org.setms.swe.domain.model.sdlc.architecture;

import java.util.Set;

public class ProgrammingLanguage implements TopicProvider {

  public static final String TOPIC = "ProgrammingLanguage";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }
}
