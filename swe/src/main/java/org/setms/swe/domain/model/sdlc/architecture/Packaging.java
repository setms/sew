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
    return Set.of(ProgrammingLanguage.TOPIC);
  }

  @Override
  public boolean isValidChoice(String topic, String choice) {
    return TOPIC.equals(topic) && choice.equals("None");
  }
}
