package org.setms.swe.domain.model.sdlc.ux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.ux.Direction.LEFT_TO_RIGHT;

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
}
