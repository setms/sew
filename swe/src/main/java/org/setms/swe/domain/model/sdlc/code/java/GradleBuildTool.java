package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.File;
import java.io.OutputStream;
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
      return buildConfigResources(resource);
    } catch (Exception e) {
      return AppliedSuggestion.failedWith(e);
    }
  }

  private void initializeGradleProject(Resource<?> resource) {
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
              "--dsl",
              "groovy",
              "--java-version",
              "25",
              "--project-name",
              projectName,
              "--no-split-project",
              "--use-defaults")
          .setStandardOutput(OutputStream.nullOutputStream())
          .setStandardError(OutputStream.nullOutputStream())
          .run();
    }
  }

  private AppliedSuggestion buildConfigResources(Resource<?> resource) {
    return Stream.of(
            "build.gradle",
            "settings.gradle",
            "gradlew",
            "gradlew.bat",
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties")
        .reduce(
            AppliedSuggestion.none(),
            (acc, path) -> acc.with(resource.select("/" + path)),
            (a, b) -> a);
  }
}
