package org.setms.swe.domain.model.sdlc.architecture;

import static java.util.Collections.emptySet;

import java.util.Set;

public interface TopicProvider {

  default Set<String> dependsOn() {
    return emptySet();
  }

  default Set<String> topics() {
    return emptySet();
  }

  default boolean isValidChoice(String topic, String choice) {
    return false;
  }
}
