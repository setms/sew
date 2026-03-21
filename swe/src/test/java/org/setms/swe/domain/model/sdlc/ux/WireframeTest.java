package org.setms.swe.domain.model.sdlc.ux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.ux.Direction.LEFT_TO_RIGHT;
import static org.setms.swe.domain.model.sdlc.ux.Direction.TOP_TO_BOTTOM;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

class WireframeTest {

  @Test
  void shouldContainContainersWithDirection() {
    var container =
        new Container(new FullyQualifiedName("ux", "Header")).setDirection(LEFT_TO_RIGHT);
    var wireframe =
        new Wireframe(new FullyQualifiedName("ux", "HomePage")).setContainers(List.of(container));

    var actual = wireframe.getContainers();

    assertThat(actual)
        .as(
            "Wireframe 'HomePage' should contain a container named 'Header' with direction LEFT_TO_RIGHT")
        .satisfiesExactly(
            c -> assertThat(c.getDirection()).as("Container direction").isEqualTo(LEFT_TO_RIGHT));
  }

  @Test
  void shouldAllowContainerToHaveChildrenOfDifferentTypes() {
    var inner = new Container(new FullyQualifiedName("ux", "Inner")).setDirection(TOP_TO_BOTTOM);
    var affordance = new Affordance(new FullyQualifiedName("ux", "Submit"));
    var container =
        new Container(new FullyQualifiedName("ux", "Header"))
            .setDirection(LEFT_TO_RIGHT)
            .setChildren(List.of(inner, affordance));

    var actual = container.getChildren();

    assertThat(actual)
        .as("Container 'Header' should have a nested Container and an Affordance as children")
        .satisfiesExactly(
            c -> assertThat(c).as("First child type").isInstanceOf(Container.class),
            c -> assertThat(c).as("Second child type").isInstanceOf(Affordance.class));
  }
}
