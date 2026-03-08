package org.setms.swe.domain.model.sdlc.packaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.swe.domain.model.sdlc.packaging.docker.DockerPackager;

class DockerPackagerTest {

  @Test
  void shouldReturnPackagingDescriptions() {
    var packager = new DockerPackager();

    var actual = packager.packagingDescriptions();

    assertThat(actual)
        .as("Packaging descriptions")
        .isNotEmpty()
        .as("Packager should return the Dockerfile glob in packaging descriptions")
        .anyMatch(glob -> glob.matches("/Dockerfile"))
        .anyMatch(glob -> glob.matches("/docker-compose.yml"));
  }
}
