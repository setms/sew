package org.setms.swe.domain.model.sdlc.packaging.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.DatabaseTopicProvider;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;

class DockerTest {

  private static final String PROJECT_NAME = "my-project";

  private Workspace<?> workspace;

  @Test
  void shouldRunDockerBuildWhenPackagingCode(@TempDir File tempDir) throws IOException {
    var capturedCommands = new ArrayList<List<String>>();
    workspace = new DirectoryWorkspace(tempDir);
    givenDockerfileIn(workspace.root());
    var docker =
        new Docker(
            PROJECT_NAME,
            (ignored, command) -> {
              capturedCommands.add(Arrays.asList(command));
              return new Docker.Result(0, "");
            });

    docker.packageCode(workspace.root(), new ArrayList<>());

    assertThat(capturedCommands)
        .as("Expected docker build command with project name 'my-project'")
        .hasSize(1)
        .first()
        .isEqualTo(List.of("docker", "build", "-t", PROJECT_NAME, "."));
  }

  private void givenDockerfileIn(Resource<?> root) throws IOException {
    root.select("Dockerfile").writeAsString("FROM ubuntu:latest\n");
  }

  @Test
  void shouldReportDockerBuildErrorsAsDiagnostics(@TempDir File tempDir) throws IOException {
    workspace = new DirectoryWorkspace(tempDir);
    givenDockerfileIn(workspace.root());
    var diagnostics = new ArrayList<Diagnostic>();
    var errorOutput = "failed to solve: Dockerfile not found";
    var docker = new Docker(PROJECT_NAME, (dir, commands) -> new Docker.Result(1, errorOutput));

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

  @Test
  void shouldReportMissingDockerfileAsDiagnosticWithSuggestion(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);
    var diagnostics = new ArrayList<Diagnostic>();
    var docker =
        new Docker(
            PROJECT_NAME,
            (dir, commands) ->
                new Docker.Result(
                    1,
                    "ERROR: failed to build: failed to solve: failed to read dockerfile: open Dockerfile: no such file or directory"));

    docker.packageCode(workspace.root(), diagnostics);

    assertThat(diagnostics)
        .as("Missing Dockerfile should produce a warning with a suggestion to create it")
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.message()).as("Message").isEqualTo("Missing Dockerfile");
              assertThat(diagnostic.suggestions())
                  .as("Suggestion to create Dockerfile")
                  .hasSize(1)
                  .first()
                  .satisfies(
                      suggestion ->
                          assertThat(suggestion.code())
                              .as("Suggestion code")
                              .isEqualTo(Docker.CREATE_DOCKERFILE));
            });
  }

  @Test
  void shouldCreateNeutralDockerfile(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);
    var inputs = new ResolvedInputs();

    assertThatDockerFileMatches(inputs, this::isNeutralDockerFile);
  }

  private void assertThatDockerFileMatches(
      ResolvedInputs inputs,
      Consumer<AbstractStringAssert<? extends AbstractStringAssert<?>>> dockerFileChecker) {
    var docker = new Docker(PROJECT_NAME);

    var applied = docker.applySuggestion(Docker.CREATE_DOCKERFILE, workspace.root(), inputs);

    assertThat(applied.createdOrChanged())
        .as("Created")
        .isNotEmpty()
        .anySatisfy(
            created ->
                dockerFileChecker.accept(assertThat(created.readAsString()).as("Dockerfile")));
  }

  private void isNeutralDockerFile(
      AbstractStringAssert<? extends AbstractStringAssert<?>> assertThatDockerfile) {
    assertThatDockerfile.isEqualTo(("FROM ubuntu:latest\n"));
  }

  @Test
  void shouldCreateJavaDockerfile(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);
    var inputs = new ResolvedInputs().put("decisions", List.of(java()));

    assertThatDockerFileMatches(inputs, this::isJavaDockerFile);
  }

  private Decision java() {
    return new Decision(new FullyQualifiedName("technology.ProgrammingLanguage"))
        .setTopic(ProgrammingLanguage.TOPIC)
        .setChoice("Java");
  }

  private void isJavaDockerFile(
      AbstractStringAssert<? extends AbstractStringAssert<?>> assertThatDockerfile) {
    assertThatDockerfile
        .startsWith("FROM eclipse-temurin:25")
        .endsWith(
            """
            ENTRYPOINT ["java", "-jar", "/app/app.jar"]
            """);
  }

  @Test
  void shouldCreateGradleDockerfile(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);
    var inputs = new ResolvedInputs().put("decisions", List.of(java(), gradle()));

    assertThatDockerFileMatches(inputs, this::isGradleDockerFile);
  }

  private Decision gradle() {
    return new Decision(new FullyQualifiedName("technology.BuildSystem"))
        .setTopic(BuildSystem.TOPIC)
        .setChoice("Gradle");
  }

  private void isGradleDockerFile(
      AbstractStringAssert<? extends AbstractStringAssert<?>> assertThatDockerfile) {
    isJavaDockerFile(assertThatDockerfile);
    assertThatDockerfile.contains("COPY build/libs/%s.jar /app/app.jar".formatted(PROJECT_NAME));
  }

  @Test
  void shouldCreateDockerComposeFile(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);

    var actual =
        new Docker(PROJECT_NAME)
            .applySuggestion(Docker.CREATE_DOCKERFILE, workspace.root(), new ResolvedInputs());

    assertThat(actual.createdOrChanged())
        .as("Should create docker-compose.yml alongside Dockerfile")
        .anySatisfy(this::assertThatDockerComposeHasAppBuildInstruction);
  }

  private void assertThatDockerComposeHasAppBuildInstruction(Resource<?> resource) {
    assertThat(resource.name()).as("Resource name").isEqualTo("docker-compose.yml");
    assertThat(resource.readAsString())
        .as(
            "docker-compose.yml should include build instruction under include-app profile, active 'local' Spring profile, and no depends_on without a database")
        .contains("build: .")
        .contains("include-app")
        .contains("SPRING_PROFILES_ACTIVE: local")
        .doesNotContain("depends_on:");
  }

  @Test
  void shouldAddPostgreSqlContainerToDockerComposeFile(@TempDir File tempDir) {
    workspace = new DirectoryWorkspace(tempDir);
    var inputs = new ResolvedInputs().put("decisions", List.of(postgresql()));

    var actual =
        new Docker(PROJECT_NAME)
            .applySuggestion(Docker.CREATE_DOCKERFILE, workspace.root(), inputs);

    assertThat(actual.createdOrChanged())
        .as("Should include PostgreSQL container in docker-compose.yml")
        .anySatisfy(this::assertThatDockerComposeHasPostgreSqlContainer);
  }

  private Decision postgresql() {
    return new Decision(new FullyQualifiedName("technology.Database"))
        .setTopic(DatabaseTopicProvider.TOPIC)
        .setChoice("PostgreSql");
  }

  private void assertThatDockerComposeHasPostgreSqlContainer(Resource<?> resource) {
    assertThat(resource.name()).as("Resource name").isEqualTo("docker-compose.yml");
    assertThat(resource.readAsString())
        .as(
            "docker-compose.yml should include a 'db' service with postgres image, POSTGRES_PASSWORD, health check, and app depends_on")
        .contains("db:")
        .doesNotContain("postgres:")
        .contains("image: postgres")
        .contains("POSTGRES_PASSWORD:")
        .contains("pg_isready")
        .contains("depends_on:");
  }
}
