package org.setms.swe.domain.model.sdlc.code;

import java.util.Set;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;

public class TopLevelPackage implements TopicProvider {

  public static final String TOPIC = "TopLevelPackage";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }

  @Override
  public Set<String> dependsOn() {
    return Set.of(ProgrammingLanguage.TOPIC);
  }
}
