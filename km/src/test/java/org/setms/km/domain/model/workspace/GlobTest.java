package org.setms.km.domain.model.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlobTest {

  @ParameterizedTest
  @CsvSource({
    "ape,bear,cheetah,false",
    "ape,**/*.bear,ape/cheetah.bear,true",
    "ape/bear,**/.cheetah,/dingo/ape/bear/elephant.cheetah,true",
    "ape/bear,**/.cheetah,/dingo/ape/bear/elephant/fox.cheetah,true"
  })
  void shouldMatch(String path, String pattern, String candidate, boolean expected) {
    var actual = new Glob(path, pattern).matches(candidate);

    assertThat(actual).isEqualTo(expected);
  }
}
