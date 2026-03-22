package org.setms.swe.domain.model.sdlc.code.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.InputField;
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
  void shouldTranslateSewPropertyNamesToCssPropertyNamesScopedToTheirElement() {
    var designSystem = givenDesignSystemWithButtonFontSizeAndInputFontSize();
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "Checkout"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatCssScopesFontSizeToButtonAndInput(actual);
  }

  private DesignSystem givenDesignSystemWithButtonFontSizeAndInputFontSize() {
    return new DesignSystem(new FullyQualifiedName("ui", "Styles"))
        .setStyles(
            List.of(
                new Style(new FullyQualifiedName("ui", "Default"))
                    .setProperties(
                        List.of(
                            new Property(new FullyQualifiedName("", "ButtonFontSize"))
                                .setValue("14px"),
                            new Property(new FullyQualifiedName("", "InputFontSize"))
                                .setValue("12px")))));
  }

  private void assertThatCssScopesFontSizeToButtonAndInput(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("CSS artifact should scope translated properties to their HTML element")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "CSS should have 'font-size: 14px' scoped to 'button' and"
                            + " 'font-size: 12px' scoped to 'input',"
                            + " not mixed in a single 'Default' rule")
                    .contains("button")
                    .contains("input")
                    .contains("font-size: 14px")
                    .contains("font-size: 12px")
                    .doesNotContain("ButtonFontSize")
                    .doesNotContain("InputFontSize"));
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
        .setContainers(List.of(new Container(new FullyQualifiedName("", "Main"))));
  }

  private void assertThatHtmlArtifactContainsMainContainer(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact for Wireframe")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as("HTML code containing 'main' container with an <h1> heading")
                    .contains("<!DOCTYPE html>")
                    .contains("id=\"main\"")
                    .contains("<h1>Main</h1>"));
  }

  @Test
  void shouldSetHtmlTitleToHumanReadableName() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "LoginScreen"));
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlHasFriendlyTitleAndArtifactName(actual);
  }

  private void assertThatHtmlHasFriendlyTitleAndArtifactName(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact should have a human-readable title and a machine-readable artifact name")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "HTML should have title 'Login screen' for browser display"
                            + " and data-artifact-name='LoginScreen' for name extraction")
                    .contains("<title>Login screen</title>")
                    .contains("data-artifact-name=\"LoginScreen\""));
  }

  @Test
  void shouldGenerateHtmlElementsForAffordanceWithInputField() {
    var wireframe = givenWireframeWithAffordanceContainingInputField();
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlContainsButtonForAffordanceAndInputForField(actual);
  }

  private Wireframe givenWireframeWithAffordanceContainingInputField() {
    var inputField =
        new InputField(new FullyQualifiedName("", "itemQuantity")).setType(FieldType.TEXT);
    var affordance =
        new Affordance(new FullyQualifiedName("", "PlaceOrder"))
            .setInputFields(List.of(inputField));
    var container =
        new Container(new FullyQualifiedName("", "mainForm")).setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("ux", "Checkout"))
        .setContainers(List.of(container));
  }

  private void assertThatHtmlContainsButtonForAffordanceAndInputForField(
      List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact should contain a button for the affordance and an input for the field")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "HTML should have id='main-form' (kebab-case), a <form> with a <button>"
                            + " for 'PlaceOrder', a <label> and <input name='item-quantity'>"
                            + " (kebab-case) for the text field")
                    .contains("id=\"main-form\"")
                    .contains("<form")
                    .contains("<label")
                    .contains("<button")
                    .contains("name=\"item-quantity\""));
  }

  @Test
  void shouldNotGenerateHtmlInputForIdField() {
    var wireframe = givenWireframeWithAffordanceContainingIdField();
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlContainsNoInputForIdField(actual);
  }

  private Wireframe givenWireframeWithAffordanceContainingIdField() {
    var idField = new InputField(new FullyQualifiedName("", "orderId")).setType(FieldType.ID);
    var affordance =
        new Affordance(new FullyQualifiedName("", "CancelOrder")).setInputFields(List.of(idField));
    var container =
        new Container(new FullyQualifiedName("", "main")).setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("ux", "Orders")).setContainers(List.of(container));
  }

  private void assertThatHtmlContainsNoInputForIdField(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact should not generate any <input> for an ID-type field")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "HTML should have a <button> for 'CancelOrder' but no <input> for the ID field")
                    .contains("<button")
                    .doesNotContain("<input"));
  }

  @Test
  void shouldSetFormActionToCommandEndpointWhenAffordanceHasLinkedCommand() {
    var wireframe = givenWireframeWithAffordanceLinkedToCommand();
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlFormPostsToCommandEndpoint(actual);
  }

  private Wireframe givenWireframeWithAffordanceLinkedToCommand() {
    var affordance =
        new Affordance(new FullyQualifiedName("todo", "AddTodoItem"))
            .setCommand(new Link("command", "AddTodoItem"));
    var container =
        new Container(new FullyQualifiedName("todo", "main")).setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("todo", "AddTodoItem"))
        .setContainers(List.of(container));
  }

  private void assertThatHtmlFormPostsToCommandEndpoint(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact for affordance with linked command")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "HTML form should POST to '/add-todo-item' endpoint"
                            + " when affordance is linked to 'AddTodoItem' command")
                    .contains("action=\"/add-todo-item\"")
                    .contains("method=\"post\""));
  }

  @Test
  void shouldGenerateCssWithFullWidthForInputsAndButton() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "Checkout"));
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatCssHasFullWidthForInputsAndButton(actual);
  }

  private void assertThatCssHasFullWidthForInputsAndButton(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("CSS artifact should include full-width styling for inputs and button")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "CSS should have 'width: 100%' for both 'input' and 'button'"
                            + " so they span the full form width")
                    .contains("input")
                    .contains("button")
                    .contains("width: 100%"));
  }

  @Test
  void shouldGenerateCssWithFlexColumnLayoutForForm() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "Checkout"));
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatCssHasFlexColumnLayoutForForm(actual);
  }

  private void assertThatCssHasFlexColumnLayoutForForm(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("CSS artifact should include flex column layout for form elements")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "CSS should have 'display: flex' and 'flex-direction: column' for 'form'"
                            + " so that labels and inputs stack vertically")
                    .contains("form")
                    .contains("display: flex")
                    .contains("flex-direction: column"));
  }

  @Test
  void shouldIncludeCssStylesheetInHtml() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "Checkout"));
    var designSystem = new DesignSystem(new FullyQualifiedName("ui", "Styles"));

    var actual = generator.generate(wireframe, designSystem);

    assertThatHtmlLinksToStylesheet(actual);
  }

  private void assertThatHtmlLinksToStylesheet(List<CodeArtifact> artifacts) {
    assertThat(artifacts)
        .as("HTML artifact should link to the generated CSS stylesheet")
        .anySatisfy(
            artifact ->
                assertThat(artifact.getCode())
                    .as(
                        "HTML head should include"
                            + " <link rel=\"stylesheet\" href=\"css/styles.css\">")
                    .contains("<link rel=\"stylesheet\" href=\"css/styles.css\">"));
  }
}
