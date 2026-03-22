package org.setms.swe.domain.model.sdlc.architecture;

import java.util.Set;

public class UserInterface implements TopicProvider {

  public static final String TOPIC = "UserInterface";

  @Override
  public Set<String> topics() {
    return Set.of(TOPIC);
  }
}
