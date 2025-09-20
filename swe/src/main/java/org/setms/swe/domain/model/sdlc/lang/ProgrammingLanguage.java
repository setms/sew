package org.setms.swe.domain.model.sdlc.lang;

import java.util.Map;
import java.util.Set;

public interface ProgrammingLanguage {

  String TOPIC = "urn:setms:swe:decision:topic:programming-language";

  String getName();

  Map<String, Set<String>> getTopics();
}
