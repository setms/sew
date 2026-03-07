package org.setms.swe.domain.model.sdlc.code.docker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;

class DockerTest {

  private final InMemoryWorkspace workspace = new InMemoryWorkspace();

  @Test
  void shouldRunDockerBuildWhenPackagingCode() {
    var capturedCommands = new ArrayList<List<String>>();
    var docker = new Docker("my-project", command -> capturedCommands.add(Arrays.asList(command)));

    docker.packageCode(workspace.root(), new ArrayList<>());

    assertThat(capturedCommands)
        .as("Expected docker build command with project name 'my-project'")
        .hasSize(1)
        .first()
        .isEqualTo(List.of("docker", "build", "-t", "my-project", "."));
  }
}
