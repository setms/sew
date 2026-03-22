package org.setms.swe.domain.model.sdlc.code.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

class ServerSideHtmlGeneratorTest {

  private final ServerSideHtmlGenerator generator = new ServerSideHtmlGenerator();

  @Test
  void shouldGenerateCssForDesignSystem() {
    var designSystem = givenDesignSystemWithButtonStyle();
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "Checkout"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatCssArtifactContainsButtonStyle(actual);
  }

  private DesignSystem givenDesignSystemWithButtonStyle() {
    return new DesignSystem(new FullyQualifiedName("ui", "Styles"))
        .setStyles(
            List.of(
                new Style(new FullyQualifiedName("ui", "button"))
                    .setProperties(
                        List.of(
                            new Property(new FullyQualifiedName("", "color")).setValue("red")))));
  }

  private void assertThatCssArtifactContainsButtonStyle(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("CSS artifact for DesignSystem")
        .anySatisfy(
            artifact -> {
              assertThat(artifact.getPackage())
                  .as("CSS artifact should have package 'css' to indicate its extension")
                  .isEqualTo("css");
              assertThat(artifact.getCode())
                  .as("CSS code with '.button { color: red; }'")
                  .contains(".button")
                  .contains("color: red");
            });
  }

  @Test
  void shouldGenerateHtmlForWireframe() {
    var wireframe = givenWireframeWithMainContainer();
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlArtifactContainsMainContainer(actual);
  }

  private Wireframe givenWireframeWithMainContainer() {
    return new Wireframe(new FullyQualifiedName("ux", "Checkout"))
        .setContainers(List.of(new Container(new FullyQualifiedName("", "main"))));
  }

  private void assertThatHtmlArtifactContainsMainContainer(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact for Wireframe")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as("HTML code containing 'main' container")
                    .contains("<html>")
                    .contains("main"));
  }
}
