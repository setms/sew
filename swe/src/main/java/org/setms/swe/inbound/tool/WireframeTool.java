package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.wireframes;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.diagram.BaseDiagramTool;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.Orientation;
import org.setms.km.domain.model.diagram.Shape;
import org.setms.km.domain.model.diagram.ShapeBox;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Direction;
import org.setms.swe.domain.model.sdlc.ux.Feedback;
import org.setms.swe.domain.model.sdlc.ux.InputField;
import org.setms.swe.domain.model.sdlc.ux.View;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;
import org.setms.swe.domain.model.sdlc.ux.WireframeElement;

public class WireframeTool extends BaseDiagramTool<Wireframe> {

  @Override
  public Set<Input<? extends Wireframe>> validationTargets() {
    return Set.of(wireframes());
  }

  @Override
  protected Input<? extends Artifact> reportingTargetInput() {
    return wireframes();
  }

  @Override
  public void buildReportsFor(
      Wireframe wireframe,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    buildHtml(
        wireframe, null, toDiagram(wireframe), resource.select(wireframe.getName()), diagnostics);
  }

  Diagram toDiagram(Wireframe wireframe) {
    var result = new Diagram();
    Optional.ofNullable(wireframe.getContainers())
        .ifPresent(containers -> containers.forEach(container -> addContainer(container, result)));
    return result;
  }

  private void addContainer(Container container, Diagram diagram) {
    diagram.setOrientation(toOrientation(container.getDirection()));
    Optional.ofNullable(container.getChildren())
        .ifPresent(children -> children.forEach(child -> addElement(child, diagram)));
  }

  private Orientation toOrientation(Direction direction) {
    return switch (direction) {
      case LEFT_TO_RIGHT, RIGHT_TO_LEFT -> Orientation.LEFT_TO_RIGHT;
      case TOP_TO_BOTTOM, BOTTOM_TO_TOP -> Orientation.TOP_TO_BOTTOM;
    };
  }

  private void addElement(WireframeElement element, Diagram diagram) {
    switch (element) {
      case Container container -> addContainer(container, diagram);
      case Affordance affordance -> diagram.add(new ShapeBox(affordance.getName(), Shape.ELLIPSE));
      case InputField inputField ->
          diagram.add(new ShapeBox(inputField.getName(), Shape.RECTANGLE));
      case View view -> diagram.add(new ShapeBox(view.getName(), Shape.RECTANGLE));
      case Feedback feedback -> diagram.add(new ShapeBox(feedback.getName(), Shape.ELLIPSE));
    }
  }
}
