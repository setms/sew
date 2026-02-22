package org.setms.swe.domain.model.sdlc.architecture;

import static lombok.AccessLevel.PRIVATE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.tool.ResolvedInputs;

@RequiredArgsConstructor(access = PRIVATE)
public class Decisions {

  private final Map<String, String> choicesByTopic;

  public static Decisions none() {
    return from(Stream.empty());
  }

  public static Decisions from(ResolvedInputs inputs) {
    return Decisions.from(
        Optional.ofNullable(inputs).stream()
            .map(i -> i.get(Decision.class))
            .flatMap(Collection::stream));
  }

  private static Decisions from(Stream<Decision> decisions) {
    return new Decisions(
        decisions
            .filter(decision -> decision.getChoice() != null)
            .collect(Collectors.toMap(Decision::getTopic, Decision::getChoice)));
  }

  public static Decisions of(Decision... decisions) {
    return from(Arrays.stream(decisions));
  }

  public boolean existFor(String topic) {
    return choicesByTopic.containsKey(topic);
  }

  public String about(String topic) {
    return choicesByTopic.get(topic);
  }
}
