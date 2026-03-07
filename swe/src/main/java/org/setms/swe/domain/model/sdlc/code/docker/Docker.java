package org.setms.swe.domain.model.sdlc.code.docker;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.CodePackager;

/** Packages code into a Docker image. */
public class Docker implements CodePackager {

  public static final String CREATE_DOCKERFILE = "dockerfile.create";
  private static final String DEFAULT_DOCKERFILE =
      """
      FROM eclipse-temurin:25
      COPY . .
      RUN ./gradlew assemble
      ENTRYPOINT ["java", "-jar", "build/libs/app.jar"]
      """;

  record Result(int exitCode, String output) {}

  @FunctionalInterface
  interface CommandRunner {
    Result run(String... command) throws Exception;
  }

  private static final CommandRunner SYSTEM =
      command -> {
        var output = new ByteArrayOutputStream();
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().transferTo(output);
        return new Result(process.waitFor(), output.toString(StandardCharsets.UTF_8));
      };

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
      var result = commandRunner.run("docker", "build", "-t", projectName, ".");
      if (result.exitCode() != 0) {
        diagnostics.add(buildFailureDiagnostic(result.output()));
      }
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage(), null));
    }
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (!CREATE_DOCKERFILE.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }
    try {
      var dockerfile = resource.select("/Dockerfile");
      dockerfile.writeAsString(DEFAULT_DOCKERFILE);
      return created(dockerfile);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private Diagnostic buildFailureDiagnostic(String output) {
    if (output.contains("open Dockerfile: no such file or directory")) {
      return new Diagnostic(
          WARN, "Missing Dockerfile", null, new Suggestion(CREATE_DOCKERFILE, "Create Dockerfile"));
    }
    return new Diagnostic(ERROR, output, null);
  }
}
