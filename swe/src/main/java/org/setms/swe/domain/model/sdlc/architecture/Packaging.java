package org.setms.swe.domain.model.sdlc.architecture;

import java.util.Set;

public class Packaging implements TopicProvider {

  public static final String TOPIC = "Packaging";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }

  @Override
  public Set<String> dependsOn() {
    return Set.of();
  }
}
