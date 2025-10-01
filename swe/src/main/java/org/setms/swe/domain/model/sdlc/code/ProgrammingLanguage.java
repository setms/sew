package org.setms.swe.domain.model.sdlc.code;

import java.util.Set;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;

public class ProgrammingLanguage implements TopicProvider {

  public static final String TOPIC = "ProgrammingLanguage";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }
}
