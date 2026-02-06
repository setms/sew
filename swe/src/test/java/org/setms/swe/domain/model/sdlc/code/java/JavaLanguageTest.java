package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JavaLanguageTest {

  private final JavaLanguage language = new JavaLanguage();

  @Test
  void shouldExtractNameFromJavaCode() {
    var code =
        """
        package com.example;

        class Foo {
        }
        """;

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
