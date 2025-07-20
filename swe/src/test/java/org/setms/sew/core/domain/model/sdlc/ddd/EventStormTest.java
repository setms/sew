package org.setms.sew.core.domain.model.sdlc.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.format.Parser;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.inbound.format.sal.SalFormat;

class EventStormTest {

  public static final String POLICY_PREFIX = "Policy";
  private final Parser parser = new SalFormat().newParser();

  @ParameterizedTest
  @MethodSource("scenarios")
  void shouldAddSequencesFromUseCase(
      String name, int numSequences, List<Integer> sizes, int numUniqueElements)
      throws IOException {
    var model = new EventStorm(List.of(parseUseCase(name)));

    assertThat(model.getSequences()).hasSize(numSequences);
    var index = 0;
    for (var sequence : model.getSequences()) {
      assertThat(sequence.items()).hasSize(sizes.get(index++));
    }
    assertThat(model.elements()).hasSize(numUniqueElements);
  }

  public static Stream<Arguments> scenarios() {
    return Stream.of(
        Arguments.of("simple", 1, List.of(4), 4),
        Arguments.of("split-policy-reads-from-readmodel", 2, List.of(5, 4), 5),
        Arguments.of("split-event-updates-readmodel", 2, List.of(6, 5), 7),
        Arguments.of("split-scenario-on-ending-event", 2, List.of(7, 8), 9),
        Arguments.of("split-scenario-on-intermediate-event", 3, List.of(5, 7, 8), 10));
  }

  private UseCase parseUseCase(String name) throws IOException {
    return parser.parse(
        new FileInputStream("src/test/resources/eventstorm/%s.useCase".formatted(name)),
        UseCase.class,
        false);
  }

  @Test
  void shouldFindSequences() throws IOException {
    var model = new EventStorm(List.of(parseUseCase("split-scenario-on-intermediate-event")));

    var actual =
        model
            .findSequences(
                List.of(
                    Link.testType("aggregate"),
                    new Link("event", "Done")::equals,
                    Link.testType("policy")))
            .toList();

    assertThat(actual)
        .hasSize(2)
        .map(Sequence::last)
        .map(Link::getId)
        .allSatisfy(id -> assertThat(id).startsWith(POLICY_PREFIX))
        .map(id -> id.substring(POLICY_PREFIX.length()))
        .containsExactlyInAnyOrder("1", "2");
  }
}
