package org.setms.km.domain.model.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.format.Strings.toKebabCase;
import static org.setms.km.domain.model.format.Strings.toPascalCase;
import static org.setms.km.domain.model.format.Strings.toSnakeCase;
import static org.setms.km.domain.model.format.Strings.wrap;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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
        Arguments.of("dingoelephant", "dingo\neleph\nant"));
  }

  @ParameterizedTest
  @CsvSource({"Ape,Ape", "BearCheetah,Bear cheetah", "DINGO,DINGO", "ELEPHANTFox,ELEPHANT fox"})
  void shouldMakeHumanReadable(String input, String expected) {
    assertThat(toFriendlyName(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"Ape,ape", "BearCheetah,bear_cheetah", "DINGO,dingo"})
  void shouldConvertToSnakeCase(String input, String expected) {
    assertThat(toSnakeCase(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"ape,Ape", "bear_cheetah,BearCheetah"})
  void shouldConvertToPascalCase(String input, String expected) {
    assertThat(toPascalCase(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"Ape,ape", "BearCheetah,bear-cheetah"})
  void shouldConvertToKebabCase(String input, String expected) {
    assertThat(toKebabCase(input)).isEqualTo(expected);
  }
}
