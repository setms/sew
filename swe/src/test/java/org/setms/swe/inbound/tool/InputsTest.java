package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InputsTest {

  @Test
  void shouldGetBuildConfigurationInputsFromProgrammingLanguageConventions() {
    var actual = Inputs.buildConfiguration();

    assertThat(actual)
        .anyMatch(input -> input.matches("build.gradle"))
        .anyMatch(input -> input.matches("settings.gradle"));
  }

  @Test
  void shouldGetUnitTestInputsFromProgrammingLanguageConventions() {
    var actual = Inputs.unitTests();

    assertThat(actual)
        .hasSize(1)
        .allSatisfy(
            input -> {
              assertThat(input.path()).as("Path").isEqualTo("src/test/java");
              assertThat(input.extension()).as("Extension").isEqualTo("java");
            });
  }
}
