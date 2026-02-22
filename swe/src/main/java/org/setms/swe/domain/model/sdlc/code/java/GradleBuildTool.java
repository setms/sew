package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.gradle.tooling.GradleConnector;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.BuildTool;

@RequiredArgsConstructor
public class GradleBuildTool implements BuildTool {

  public static final String GENERATE_BUILD_CONFIG = "gradle.generate.build.config";
  private static final String GRADLE_VERSION = "9.3.1";
  private static final String BUILD_GRADLE =
      """
      plugins {
          id 'java'
          alias libs.plugins.lombok
      }

      repositories {
          mavenCentral()
      }

      dependencies {
          testImplementation libs.bundles.test

          testRuntimeOnly libs.bundles.test.runtime
      }

      java {
          toolchain {
              languageVersion = JavaLanguageVersion.of(25)
          }
      }

      tasks.named('test') {
          useJUnitPlatform()
      }
      """;
  private static final String VERSION_CATALOG =
      """
      [versions]
      assertj = "3.27.7"
      jqwik = "1.9.3"
      junit-jupiter = "6.0.3"
      lombok-plugin = "9.2.0"

      [libraries]
      assertj = { module = "org.assertj:assertj-core", version.ref = "assertj" }
      jqwik = { module = "net.jqwik:jqwik", version.ref = "jqwik" }
      junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
      junit-jupiter-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit-jupiter" }

      [bundles]
      test = ["assertj", "junit-jupiter", "jqwik"]
      test-runtime = ["junit-jupiter-launcher"]

      [plugins]
      lombok = { id = "io.freefair.lombok", version.ref = "lombok-plugin" }
      """;

  private final String projectName;

  @Override
  public void validate(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (!resource.select("/build.gradle").exists()
        || !resource.select("/settings.gradle").exists()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Gradle project isn't initialized",
              null,
              new Suggestion(GENERATE_BUILD_CONFIG, "Generate build configuration files")));
    }
  }

  @Override
  public void build(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (!resource.select("/build.gradle").exists()
        || !resource.select("/settings.gradle").exists()) {
      return;
    }
    runCompile(resource);
  }

  private void runCompile(Resource<?> resource) {
    try (var connection =
        GradleConnector.newConnector()
            .forProjectDirectory(new File(resource.toUri()))
            .useGradleVersion(GRADLE_VERSION)
            .connect()) {
      connection.newBuild().forTasks("compileJava").run();
    } catch (Exception e) {
      throw new IllegalStateException("gradle compile failed", e);
    }
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (!GENERATE_BUILD_CONFIG.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }
    try {
      initializeGradleProject(resource);
      cleanUpFiles(resource);
      return buildConfigResources(resource);
    } catch (Exception e) {
      return AppliedSuggestion.failedWith(e);
    }
  }

  private void initializeGradleProject(Resource<?> resource) {
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    try (var connection =
        GradleConnector.newConnector()
            .forProjectDirectory(new File(resource.toUri()))
            .useGradleVersion(GRADLE_VERSION)
            .connect()) {
      connection
          .newBuild()
          .withArguments(
              "init",
              "--type",
              "java-library",
              "--java-version",
              Integer.toString(Runtime.version().feature()),
              "--dsl",
              "groovy",
              "--project-name",
              projectName,
              "--test-framework",
              "junit-jupiter",
              "--use-defaults",
              "--no-comments",
              "--no-split-project",
              "--overwrite")
          .setStandardOutput(stdout)
          .setStandardError(stderr)
          .run();
    } catch (Exception e) {
      throw new IllegalStateException(
          "gradle init failed%nstdout: %s%nstderr: %s".formatted(stdout, stderr), e);
    }
  }

  private void cleanUpFiles(Resource<?> resource) throws IOException {
    cleanUpBuild(resource);
    cleanUpSettings(resource);
    cleanUpVersionCatalog(resource);
  }

  private void cleanUpBuild(Resource<?> resource) throws IOException {
    try (var output = resource.select("build.gradle").writeTo()) {
      output.write(BUILD_GRADLE.getBytes(StandardCharsets.UTF_8));
    }
    resource.select("lib").delete();
  }

  private void cleanUpSettings(Resource<?> resource) throws IOException {
    String rootProject;
    try (var reader =
        new BufferedReader(new InputStreamReader(resource.select("settings.gradle").readFrom()))) {
      rootProject =
          reader
              .lines()
              .filter(line -> line.contains("rootProject.name"))
              .findFirst()
              .orElseThrow();
    }
    try (var writer = new PrintWriter(resource.select("settings.gradle").writeTo())) {
      writer.println(rootProject);
    }
  }

  private void cleanUpVersionCatalog(Resource<?> resource) throws IOException {
    try (var output = resource.select("gradle/libs.versions.toml").writeTo()) {
      output.write(VERSION_CATALOG.getBytes(StandardCharsets.UTF_8));
    }
  }

  private AppliedSuggestion buildConfigResources(Resource<?> resource) {
    return Stream.of(
            ".gitattributes",
            ".gitignore",
            "build.gradle",
            "gradle.properties",
            "gradlew",
            "gradlew.bat",
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties",
            "settings.gradle")
        .reduce(
            AppliedSuggestion.none(),
            (acc, path) -> acc.with(resource.select("/" + path)),
            (a, _) -> a);
  }
}
