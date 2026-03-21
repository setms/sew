package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.swe.inbound.format.xml.XmlFormat;

class InputsTest {

  @Test
  void shouldGetBuildConfigurationInputsFromProgrammingLanguageConventions() {
    var actual = Inputs.buildConfiguration();

    assertThat(actual)
        .anyMatch(input -> input.matches("/build.gradle"))
        .anyMatch(input -> input.matches("/settings.gradle"));
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

  @Test
  void shouldUseXmlFormatForWireframes() {
    var actual = Inputs.wireframes();

    assertThat(actual.format())
        .as("Wireframes input should use XmlFormat to support deeply nested structures")
        .isInstanceOf(XmlFormat.class);
  }

  @Test
  void shouldUseXmlFormatForDesignSystems() {
    var actual = Inputs.designSystems();

    assertThat(actual.format())
        .as("Design systems input should use XmlFormat")
        .isInstanceOf(XmlFormat.class);
    assertThat(actual.matches("src/main/ux/designSystems/mySystem.designSystem"))
        .as("Design systems input should match files in src/main/ux/designSystems")
        .isTrue();
  }

  @Test
  void shouldGetDatabaseSchemaInputsFromDatabaseTechnology() {
    var actual = Inputs.databaseSchemas();

    assertThat(actual)
        .as("Database schema inputs from database technology")
        .anyMatch(input -> input.matches("src/main/design/physical/shop/product.sql"));
  }
}
