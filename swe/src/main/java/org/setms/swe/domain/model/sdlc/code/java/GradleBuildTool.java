package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.BuildTool;

@RequiredArgsConstructor
public class GradleBuildTool implements BuildTool {

  public static final String GENERATE_BUILD_CONFIG = "gradle.generate.build.config";
  private static final String BUILD_GRADLE_CONTENT =
      """
      plugins {
          id 'java'
      }

      repositories {
          mavenCentral()
      }

      java {
          toolchain {
              languageVersion = JavaLanguageVersion.of(25)
          }
      }

      dependencies {
          testImplementation 'org.junit.jupiter:junit-jupiter-api:6.0.2'
          testImplementation 'org.assertj:assertj-core:3.27.7'
          testImplementation 'net.jqwik:jqwik:1.9.3'
          testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.0.2'
      }

      tasks.named('test') {
          useJUnitPlatform()
      }
      """;

  private final String projectName;

  @Override
  public void validate(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (!resource.select("/build.gradle").exists()
        || !resource.select("/settings.gradle").exists()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing build configuration",
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
      var buildGradleResource = resource.select("/build.gradle");
      try (var output = buildGradleResource.writeTo()) {
        output.write(BUILD_GRADLE_CONTENT.getBytes());
      }

      var settingsGradleResource = resource.select("/settings.gradle");
      try (var output = settingsGradleResource.writeTo()) {
        output.write(("rootProject.name = '" + projectName + "'\n").getBytes());
      }

      return AppliedSuggestion.created(buildGradleResource).with(settingsGradleResource);
    } catch (IOException e) {
      return AppliedSuggestion.failedWith(e);
    }
  }
}
