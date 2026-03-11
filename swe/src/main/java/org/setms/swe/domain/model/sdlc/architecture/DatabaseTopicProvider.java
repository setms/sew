package org.setms.swe.domain.model.sdlc.architecture;

import java.util.Set;

public class DatabaseTopicProvider implements TopicProvider {

  public static final String TOPIC = "Database";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }

  @Override
  public boolean isValidChoice(String topic, String choice) {
    return TOPIC.equals(topic) && "PostgreSql".equals(choice);
  }
}
