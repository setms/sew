package org.setms.swe.domain.model.sdlc.code.docker;

import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.Collection;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.CodePackager;

/** Packages code into a Docker image. */
public class Docker implements CodePackager {

  @FunctionalInterface
  interface CommandRunner {
    void run(String... command) throws Exception;
  }

  private static final CommandRunner SYSTEM =
      command -> new ProcessBuilder(command).inheritIO().start().waitFor();

  private final String projectName;
  private final CommandRunner commandRunner;

  public Docker(String projectName) {
    this(projectName, SYSTEM);
  }

  Docker(String projectName, CommandRunner commandRunner) {
    this.projectName = projectName;
    this.commandRunner = commandRunner;
  }

  @Override
  public void packageCode(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    try {
      commandRunner.run("docker", "build", "-t", projectName, ".");
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, "docker build failed", null));
    }
  }
}
