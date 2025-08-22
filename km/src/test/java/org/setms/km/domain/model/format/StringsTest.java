package org.setms.km.domain.model.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.format.Strings.wrap;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StringsTest {

  @ParameterizedTest
  @MethodSource("texts")
  void shouldWrap(String text, String expected) {
    assertThat(wrap(text, 5)).isEqualTo(expected);
  }

  static Stream<Arguments> texts() {
    return Stream.of(
        Arguments.of("ape", "ape"),
        Arguments.of("bear cobra", "bear\ncobra"),
        Arguments.of("dingoelephant", "dingo\neleph\nant")
    );
  }
}
