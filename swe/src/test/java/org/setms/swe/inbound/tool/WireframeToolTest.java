package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.design.FieldType;
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
    var container =
        new Container(new FullyQualifiedName("ux", "Header"))
            .setDirection(Direction.LEFT_TO_RIGHT)
            .setChildren(
                List.of(
                    new Affordance(new FullyQualifiedName("ux", "Login")),
                    new InputField(new FullyQualifiedName("ux", "Password"))
                        .setType(FieldType.TEXT)));
    return new Wireframe(new FullyQualifiedName("ux", "LoginScreen"))
        .setContainers(List.of(container));
  }
}
