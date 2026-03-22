package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.ui.Properties;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Direction;
import org.setms.swe.domain.model.sdlc.ux.InputField;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

class WireframeToolTest {

  private static final String WIREFRAME_HTML =
      """
    <html>
      <body>
        <h1>Login screen</h1>
        <img src="LoginScreen.png" width="100%">
      </body>
    </html>
    """;

  private final WireframeTool tool = new WireframeTool();

  @Test
  void shouldIncludeDesignSystemsInValidationContext() {
    var actual = tool.validationContext();

    assertThat(actual.stream().map(Input::path).toList())
        .as(
            "WireframeTool validation context should include design systems input at 'src/main/ux/designSystems'")
        .contains("src/main/ux/designSystems");
  }

  @Test
  void shouldIncludeUiCodeInValidationContext() {
    var actual = tool.validationContext();

    assertThat(actual)
        .as("WireframeTool validation context should include UI code input for HTML templates")
        .anyMatch(input -> input.matches("src/main/resources/templates/home.html"));
  }

  @Test
  void shouldRequireDesignSystem() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "LoginScreen"));
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(wireframe, new ResolvedInputs(), diagnostics);

    assertThat(diagnostics)
        .as("Diagnostics when wireframe has no design system")
        .singleElement()
        .satisfies(this::assertThatDiagnosticSuggestsCreatingDesignSystem);
  }

  private void assertThatDiagnosticSuggestsCreatingDesignSystem(Diagnostic diagnostic) {
    assertThat(diagnostic.message())
        .as("Diagnostic message for missing design system")
        .isEqualTo("Missing design system");
    assertThat(diagnostic.suggestions().stream().map(Suggestion::message).toList())
        .as("Suggestion for missing design system")
        .containsExactly("Create design system");
  }

  @Test
  void shouldCreateDesignSystemWithDefaults() {
    var wireframe = new Wireframe(new FullyQualifiedName("ux", "LoginScreen"));
    var workspace = new InMemoryWorkspace();

    var actual =
        tool.applySuggestion(
            wireframe,
            WireframeTool.CREATE_DESIGN_SYSTEM,
            null,
            new ResolvedInputs(),
            workspace.root());

    var designSystemFile =
        workspace.root().select("src/main/ux/designSystems/Default.designSystem");
    assertThat(actual.createdOrChanged())
        .as(
            "'Create design system' suggestion should create file at 'src/main/ux/designSystems/Default.designSystem'")
        .containsExactly(designSystemFile);
    assertThatDesignSystemContainsAllDefaults(designSystemFile);
  }

  private void assertThatDesignSystemContainsAllDefaults(Resource<?> designSystem) {
    var content = designSystem.readAsString();
    Properties.names()
        .forEach(
            name ->
                assertThat(content)
                    .as(
                        "Design system file should contain property '%s' with default value '%s'"
                            .formatted(name, Properties.defaultFor(name)))
                    .contains(name)
                    .contains(Properties.defaultFor(name)));
  }

  @Test
  void shouldRenderWireframeAsLowFidelityMockup() {
    var wireframe = givenWireframeWithAffordanceAndInputField();
    var workspace = new InMemoryWorkspace();
    var diagnostics = new ArrayList<Diagnostic>();

    tool.buildReportsFor(wireframe, new ResolvedInputs(), workspace.root(), diagnostics);

    assertThat(diagnostics).as("Diagnostics when rendering wireframe").isEmpty();
    var html = workspace.root().select("LoginScreen/LoginScreen.html");
    assertThat(html.readAsString())
        .as("Generated wireframe HTML wrapping the image for 'Login Screen'")
        .isEqualTo(WIREFRAME_HTML);
    var png = workspace.root().select("LoginScreen/LoginScreen.png");
    assertThat(png.exists())
        .as("Rendered low-fidelity wireframe image for 'Login Screen'")
        .isTrue();
  }

  @Test
  void shouldStripLeadingVerbFromAffordanceLabel() {
    var affordance = new Affordance(new FullyQualifiedName("todo", "InitiateAddTodoItem"));

    var actual = tool.affordanceLabel(affordance);

    assertThat(actual).as("Affordance label with leading verb stripped").isEqualTo("Add todo item");
  }

  @Test
  void shouldRenderWireframeAsPortraitScreenMockup() throws IOException {
    var wireframe = givenWireframeWithAffordanceContainingInputFields();
    var workspace = new InMemoryWorkspace();
    var diagnostics = new ArrayList<Diagnostic>();

    tool.buildReportsFor(wireframe, new ResolvedInputs(), workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
    var actual =
        ImageIO.read(
            workspace.root().select("InitiateAddTodoItem/InitiateAddTodoItem.png").readFrom());
    assertThat(actual.getHeight())
        .as("Wireframe renders as portrait screen-like image")
        .isGreaterThan(actual.getWidth());
  }

  private Wireframe givenWireframeWithAffordanceContainingInputFields() {
    var affordance =
        new Affordance(new FullyQualifiedName("todo", "Submit"))
            .setInputFields(
                List.of(
                    new InputField(new FullyQualifiedName("todo", "Task")).setType(FieldType.TEXT),
                    new InputField(new FullyQualifiedName("todo", "DueDate"))
                        .setType(FieldType.DATETIME)));
    var container =
        new Container(new FullyQualifiedName("todo", "Form"))
            .setDirection(Direction.TOP_TO_BOTTOM)
            .setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("todo", "InitiateAddTodoItem"))
        .setContainers(List.of(container));
  }

  private Wireframe givenWireframeWithAffordanceAndInputField() {
    var affordance =
        new Affordance(new FullyQualifiedName("ux", "Login"))
            .setInputFields(
                List.of(
                    new InputField(new FullyQualifiedName("ux", "Password"))
                        .setType(FieldType.TEXT)));
    var container =
        new Container(new FullyQualifiedName("ux", "Header"))
            .setDirection(Direction.LEFT_TO_RIGHT)
            .setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("ux", "LoginScreen"))
        .setContainers(List.of(container));
  }
}
