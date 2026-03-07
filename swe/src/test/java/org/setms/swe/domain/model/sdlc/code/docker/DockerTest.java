package org.setms.swe.domain.model.sdlc.code.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;

class DockerTest {

  private final InMemoryWorkspace workspace = new InMemoryWorkspace();

  @Test
  void shouldRunDockerBuildWhenPackagingCode() {
    var capturedCommands = new ArrayList<List<String>>();
    var docker =
        new Docker(
            "my-project",
            command -> {
              capturedCommands.add(Arrays.asList(command));
              return new Docker.Result(0, "");
            });

    docker.packageCode(workspace.root(), new ArrayList<>());

    assertThat(capturedCommands)
        .as("Expected docker build command with project name 'my-project'")
        .hasSize(1)
        .first()
        .isEqualTo(List.of("docker", "build", "-t", "my-project", "."));
  }

  @Test
  void shouldReportDockerBuildErrorsAsDiagnostics() {
    var diagnostics = new ArrayList<Diagnostic>();
    var errorOutput = "failed to solve: Dockerfile not found";
    var docker = new Docker("my-project", command -> new Docker.Result(1, errorOutput));

    docker.packageCode(workspace.root(), diagnostics);

    assertThat(diagnostics)
        .as("Diagnostic for docker build failure should contain the error output")
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(ERROR);
              assertThat(diagnostic.message()).as("Message").isEqualTo(errorOutput);
            });
  }
}
