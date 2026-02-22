package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
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
    try (var reader =
        new BufferedReader(new InputStreamReader(resource.select("lib/build.gradle").readFrom()))) {
      try (var writer = new PrintWriter(resource.select("build.gradle").writeTo())) {
        reader
            .lines()
            .filter(line -> unwantedExampleDependencies().noneMatch(line::contains))
            .map(line -> line.replace("java-library", "java"))
            .forEach(writer::println);
      }
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
    List<String> lines;
    var versionCatalog = resource.select("gradle/libs.versions.toml");
    try (var reader = new BufferedReader(new InputStreamReader(versionCatalog.readFrom()))) {
      lines =
          reader
              .lines()
              .filter(line -> unwantedExampleDependencies().noneMatch(line::contains))
              .toList();
    }
    try (var writer = new PrintWriter(versionCatalog.writeTo())) {
      lines.forEach(writer::println);
    }
  }

  private Stream<String> unwantedExampleDependencies() {
    return Stream.of("math3", "guava");
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
