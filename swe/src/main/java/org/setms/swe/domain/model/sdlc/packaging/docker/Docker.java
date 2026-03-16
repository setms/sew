package org.setms.swe.domain.model.sdlc.packaging.docker;

import static lombok.AccessLevel.PACKAGE;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.DatabaseTopicProvider;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.technology.CodePackager;

/** Packages code into a Docker image. */
@RequiredArgsConstructor(access = PACKAGE)
public class Docker implements CodePackager {

  public static final String CREATE_DOCKERFILE = "dockerfile.create";
  private static final String DOCKER_COMPOSE_APP_SERVICE =
      """
      services:
        %s:
          build: .
          profiles:
            - include-app
          environment:
            SPRING_PROFILES_ACTIVE: local
      """;
  private static final String DOCKER_COMPOSE_APP_DATASOURCE_URL =
      "      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/%s\n";
  private static final String DOCKER_COMPOSE_APP_DEPENDS_ON =
      """
          depends_on:
            db:
              condition: service_healthy
      """;
  private static final String DOCKER_COMPOSE_DB_SERVICE =
      """
        db:
          image: postgres
          environment:
            POSTGRES_PASSWORD: password
            POSTGRES_DB: %s
          ports:
            - "5432:5432"
          healthcheck:
            test: ["CMD-SHELL", "pg_isready -U postgres"]
            interval: 10s
            timeout: 5s
            retries: 5
      """;
  private static final String NEUTRAL_DOCKERFILE =
      """
      FROM ubuntu:latest
      """;
  private static final String JAVA_DOCKERFILE =
      """
      FROM eclipse-temurin:25
      %s
      ENTRYPOINT ["java", "-jar", "/app/app.jar"]
      """;

  record Result(int exitCode, String output) {}

  @FunctionalInterface
  interface CommandRunner {
    Result run(File directory, String... command) throws Exception;
  }

  private static final CommandRunner SYSTEM =
      (workingDirectory, command) -> {
        var output = new ByteArrayOutputStream();
        var process =
            new ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start();
        process.getInputStream().transferTo(output);
        return new Result(process.waitFor(), output.toString(StandardCharsets.UTF_8));
      };

  private final String applicationName;
  private final CommandRunner commandRunner;

  public Docker(String applicationName) {
    this(applicationName, SYSTEM);
  }

  @Override
  public void packageCode(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (!resource.select("Dockerfile").exists()) {
      diagnostics.add(missingDockerFile());
      return;
    }
    try {
      var result =
          commandRunner.run(
              resource.toFile(), "docker", "build", "-t", applicationName.toLowerCase(), ".");
      if (result.exitCode() != 0) {
        diagnostics.add(buildFailureDiagnostic(result.output()));
      }
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage(), null));
    }
  }

  private Diagnostic buildFailureDiagnostic(String output) {
    if (output.contains("open Dockerfile: no such file or directory")) {
      return missingDockerFile();
    }
    return new Diagnostic(ERROR, output, null);
  }

  private Diagnostic missingDockerFile() {
    return new Diagnostic(
        WARN, "Missing Dockerfile", null, new Suggestion(CREATE_DOCKERFILE, "Create Dockerfile"));
  }

  @Override
  public AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs) {
    if (!CREATE_DOCKERFILE.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }
    try {
      var decisions = Decisions.from(inputs);
      var dockerfile = resource.select("/Dockerfile");
      dockerfile.writeAsString(dockerFileFor(decisions));
      var dockerCompose = resource.select("/docker-compose.yml");
      dockerCompose.writeAsString(dockerComposeFor(decisions));
      return created(dockerfile).with(dockerCompose);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private String dockerFileFor(Decisions decisions) {
    var programmingLanguage = decisions.about(ProgrammingLanguage.TOPIC);
    return switch (programmingLanguage) {
      case null -> NEUTRAL_DOCKERFILE;
      case "Java" -> javaDockerFileFor(decisions);
      default ->
          throw new IllegalStateException(
              "Don't know how to build Dockerfile for " + programmingLanguage);
    };
  }

  private String javaDockerFileFor(Decisions decisions) {
    return switch (decisions.about(BuildSystem.TOPIC)) {
      case null -> JAVA_DOCKERFILE.formatted("");
      case "Gradle" -> gradleDockerFileFor();
      default -> JAVA_DOCKERFILE.formatted("");
    };
  }

  private String gradleDockerFileFor() {
    return JAVA_DOCKERFILE.formatted(
        "COPY build/libs/%s.jar /app/app.jar".formatted(applicationName));
  }

  private String dockerComposeFor(Decisions decisions) {
    var hasPostgres = "PostgreSql".equals(decisions.about(DatabaseTopicProvider.TOPIC));
    var result = DOCKER_COMPOSE_APP_SERVICE.formatted(applicationName.toLowerCase());
    if (hasPostgres) {
      result +=
          DOCKER_COMPOSE_APP_DATASOURCE_URL.formatted(applicationName.toLowerCase())
              + DOCKER_COMPOSE_APP_DEPENDS_ON
              + DOCKER_COMPOSE_DB_SERVICE.formatted(applicationName.toLowerCase());
    }
    return result;
  }
}
