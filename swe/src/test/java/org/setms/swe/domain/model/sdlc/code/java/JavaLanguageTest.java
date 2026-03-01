package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JavaLanguageTest {

  private final JavaLanguage language = new JavaLanguage();

  @ParameterizedTest
  @ValueSource(strings = {"class", "interface", "record"})
  void shouldExtractClassNameFromJavaCode(String type) {
    var code =
        """
        package com.example;

        %s Foo {
        }
        """
            .formatted(type);

    var name = language.extractName(code);

    assertThat(name.getPackage()).isEqualTo("com.example");
    assertThat(name.getName()).isEqualTo("Foo");
  }

  @Test
  void shouldHandleMissingPackage() {
    var code =
        """
        class Bar {
        }
        """;

    var name = language.extractName(code);

    assertThat(name.getPackage()).isEmpty();
    assertThat(name.getName()).isEqualTo("Bar");
  }

  @Test
  void shouldMatchUnitTestHelpers() {
    var glob = language.unitTestHelpersGlob();

    var actual = glob.matches("src/test/java/com/example/SomeHelper.java");

    assertThat(actual).isTrue();
  }

  @Test
  void shouldNotMatchUnitTestsAsHelpers() {
    var glob = language.unitTestHelpersGlob();

    var actual = glob.matches("src/test/java/com/example/SomeTest.java");

    assertThat(actual).isFalse();
  }

  @Test
  void shouldReturnBuildConfigurationFiles() {
    var actual = language.buildConfigurationFiles();

    assertThat(actual)
        .anySatisfy(glob -> glob.matches("/build.gradle"))
        .anySatisfy(glob -> glob.matches("/settings.gradle"));
  }

  @Test
  void shouldHandleMissingClass() {
    var code =
        """
        package com.example;
        """;

    var name = language.extractName(code);

    assertThat(name.getPackage()).isEqualTo("com.example");
    assertThat(name.getName()).isEmpty();
  }
}
