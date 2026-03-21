package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.diagram.Orientation;
import org.setms.km.domain.model.diagram.Shape;
import org.setms.km.domain.model.diagram.ShapeBox;
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
  void shouldRenderAffordanceAsButtonAndInputFieldAsTextBox() {
    var wireframe = givenWireframeWithAffordanceAndInputField();

    var actual = tool.toDiagram(wireframe);

    assertThat(actual.getOrientation())
        .as("Diagram orientation should reflect the container's LEFT_TO_RIGHT direction")
        .isEqualTo(Orientation.LEFT_TO_RIGHT);
    assertThat(actual.getBoxes())
        .as(
            "Wireframe diagram should contain a button (ellipse) for the 'Login' affordance and a text box (rectangle) for the 'Password' input field")
        .satisfiesExactlyInAnyOrder(
            box ->
                assertThat(box)
                    .as("'Login' affordance rendered as ellipse (button)")
                    .isInstanceOfSatisfying(
                        ShapeBox.class,
                        shapeBox -> assertThat(shapeBox.getShape()).isEqualTo(Shape.ELLIPSE)),
            box ->
                assertThat(box)
                    .as("'Password' input field rendered as rectangle (text box)")
                    .isInstanceOfSatisfying(
                        ShapeBox.class,
                        shapeBox -> assertThat(shapeBox.getShape()).isEqualTo(Shape.RECTANGLE)));
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
