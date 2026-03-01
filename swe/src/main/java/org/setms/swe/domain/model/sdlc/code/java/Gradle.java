package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.Failure;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.events.FailureResult;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;
import org.gradle.tooling.events.test.TestFailureResult;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.CodeTester;

@RequiredArgsConstructor
public class Gradle implements CodeBuilder, CodeTester {

  public static final String GENERATE_BUILD_CONFIG = "gradle.generate.build.config";
  private static final String GRADLE_VERSION = "9.3.1";
  private static final Pattern COMPILATION_ERROR = Pattern.compile("^(/.+):(\\d+): error: (.+)$");
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
    if (!isInitialized(resource)) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Gradle project isn't initialized",
              null,
              new Suggestion(GENERATE_BUILD_CONFIG, "Generate build configuration files")));
    }
  }

  private boolean isInitialized(Resource<?> resource) {
    return resource.select("/build.gradle").exists()
        && resource.select("/settings.gradle").exists();
  }

  @Override
  public void build(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (isInitialized(resource)) {
      runCompile(resource, diagnostics);
    }
  }

  private void runCompile(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    runGradle(
        resource, diagnostics, this::parseCompilationErrors, "compileJava", "compileTestJava");
  }

  private Collection<Diagnostic> parseCompilationErrors(File projectDir, FinishEvent event) {
    return Optional.of(event.getResult()).map(FailureResult.class::cast).stream()
        .map(FailureResult::getFailures)
        .flatMap(Collection::stream)
        .map(Failure::getDescription)
        .flatMap(description -> Arrays.stream(description.split(System.lineSeparator())))
        .map(COMPILATION_ERROR::matcher)
        .filter(Matcher::matches)
        .map(matcher -> toCompilationDiagnostic(projectDir.getAbsolutePath(), matcher))
        .toList();
  }

  private Diagnostic toCompilationDiagnostic(String projectDir, Matcher matcher) {
    var filePath =
        matcher.group(1).replace(projectDir + File.separator, "").replace(File.separator, "/");
    var lineNumber = matcher.group(2);
    var message = matcher.group(3);
    return new Diagnostic(ERROR, message, new Location(filePath, lineNumber));
  }

  private void runGradle(
      Resource<?> resource,
      Collection<Diagnostic> diagnostics,
      BiFunction<File, FinishEvent, Collection<Diagnostic>> failureToDiagnostics,
      String... tasks) {
    var projectDir = toFile(resource);
    var output = new ByteArrayOutputStream();
    try (var connection =
        GradleConnector.newConnector()
            .forProjectDirectory(projectDir)
            .useGradleVersion(GRADLE_VERSION)
            .connect()) {
      connection
          .newBuild()
          .forTasks(tasks)
          .setStandardOutput(output)
          .setStandardError(output)
          .addProgressListener(new FailureListener(projectDir, failureToDiagnostics, diagnostics))
          .run();
    } catch (BuildException _) {
      // Ignore, since already caught by progress listener
    } catch (Exception e) {
      throw new IllegalStateException("gradle %s failed".formatted(String.join(", ", tasks)), e);
    }
  }

  @Override
  public void test(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (isInitialized(resource)) {
      runTests(resource, diagnostics);
    }
  }

  private void runTests(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    runGradle(resource, diagnostics, this::parseTestFailures, "test");
  }

  private Collection<Diagnostic> parseTestFailures(File ignored, FinishEvent event) {
    if (event.getDescriptor() instanceof JvmTestOperationDescriptor testDescriptor
        && testDescriptor.getMethodName() != null
        && event.getResult() instanceof TestFailureResult testFailure) {
      return testFailure.getFailures().stream()
          .map(Failure::getMessage)
          .map(
              message ->
                  new Diagnostic(
                      ERROR,
                      message,
                      new Location(testDescriptor.getClassName(), testDescriptor.getMethodName())))
          .toList();
    }
    return emptyList();
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (!GENERATE_BUILD_CONFIG.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }
    try {
      initializeIn(resource);
      return buildConfigResources(resource);
    } catch (Exception e) {
      return AppliedSuggestion.failedWith(e);
    }
  }

  private void initializeIn(Resource<?> root) {
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    try (var connection =
        GradleConnector.newConnector()
            .forProjectDirectory(toFile(root))
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
      connection.notifyDaemonsAboutChangedPaths(cleanUpFiles(root));
    } catch (Exception e) {
      throw new IllegalStateException(
          "gradle init failed%nstdout: %s%nstderr: %s".formatted(stdout, stderr), e);
    }
  }

  private File toFile(Resource<?> resource) {
    return new File(resource.toUri());
  }

  private List<Path> cleanUpFiles(Resource<?> resource) throws IOException {
    var result = new ArrayList<Path>();
    cleanUpBuild(resource, result);
    cleanUpSettings(resource);
    cleanUpVersionCatalog(resource);
    return result;
  }

  private void cleanUpBuild(Resource<?> resource, Collection<Path> changed) throws IOException {
    try (var output = resource.select("build.gradle").writeTo()) {
      output.write(BUILD_GRADLE.getBytes(StandardCharsets.UTF_8));
    }
    var lib = resource.select("lib");
    lib.delete();
    changed.add(toFile(lib).toPath().toAbsolutePath());
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

  @RequiredArgsConstructor
  private static class FailureListener implements ProgressListener {

    private final File projectDir;
    private final BiFunction<File, FinishEvent, Collection<Diagnostic>> failureToDiagnostics;
    private final Collection<Diagnostic> diagnostics;

    @Override
    public void statusChanged(ProgressEvent event) {
      if (event instanceof FinishEvent finishEvent) {
        if (finishEvent.getResult() instanceof FailureResult) {
          diagnostics.addAll(failureToDiagnostics.apply(projectDir, finishEvent));
        }
      }
    }
  }
}
