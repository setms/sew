package org.setms.sew.core.domain.model.sdlc.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.sdlc.Pointer;

class EventStormTest {

  private final EventStorm model = new EventStorm();

  @Test
  void shouldFindSequence() {
    var policy1 = new Pointer("policy", "WheneverIFeelLikeIt1");
    var command1 = new Pointer("command", "DoIt1");
    var policy2 = new Pointer("policy", "WheneverIFeelLikeIt2");
    var command2 = new Pointer("command", "DoIt2");
    var aggregate1 = new Pointer("aggregate", "SpiderInTheWeb1");
    var policy3 = new Pointer("policy", "WheneverIFeelLikeIt3");
    var command3 = new Pointer("command", "DoIt3");
    var aggregate2 = new Pointer("aggregate", "SpiderInTheWeb2");
    model.add(policy1, command1);
    model.add(command1, aggregate1);
    model.add(policy2, command2);
    model.add(command2, aggregate1);
    model.add(policy3, command3);
    model.add(command3, aggregate2);

    var actual = model.findSequences("policy", "command", "aggregate");

    assertThat(actual)
        .containsExactlyInAnyOrder(
            new Sequence(policy1, aggregate1),
            new Sequence(policy2, aggregate1),
            new Sequence(policy3, aggregate2));
  }
}
