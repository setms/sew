package org.setms.swe.domain.model.sdlc.architecture;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import java.util.Map;
import java.util.Set;

public interface TopicProvider {

  default Set<String> dependsOn() {
    return emptySet();
  }

  default Set<String> topics() {
    return emptySet();
  }

  default Map<String, Set<String>> validChoices() {
    return emptyMap();
  }
}
