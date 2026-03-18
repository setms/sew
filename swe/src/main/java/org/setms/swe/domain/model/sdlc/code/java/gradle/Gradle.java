package org.setms.swe.domain.model.sdlc.code.java.gradle;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
      mockito = "5.22.0"

      [libraries]
      assertj = { module = "org.assertj:assertj-core", version.ref = "assertj" }
      jqwik = { module = "net.jqwik:jqwik", version.ref = "jqwik" }
      junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
      junit-jupiter-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit-jupiter" }
      mockito = { module = "org.mockito:mockito-core", version.ref = "mockito" }
      mockito-junit = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }

      [bundles]
      test = ["assertj", "junit-jupiter", "jqwik", "mockito", "mockito-junit"]
      test-runtime = ["junit-jupiter-launcher"]

      [plugins]
      lombok = { id = "io.freefair.lombok", version.ref = "lombok-plugin" }
      """;
  private static final Map<String, String> LATEST_KNOWN_VERSIONS =
      Map.of("org.springframework.boot", "4.0.3", "com.github.akazver.mapstruct", "1.0.9");
  private static final String TASKS_BLOCK =
      """


      %s {
      %s
      }
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
              new Suggestion(GENERATE_BUILD_CONFIG, "Initialize Gradle project")));
    }
  }

  private boolean isInitialized(Resource<?> resource) {
    return resource.select("/build.gradle").exists()
        && resource.select("/settings.gradle").exists();
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
    var projectDir = resource.toFile();
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
    } catch (BuildException ignored) {
      // Ignore, since already caught by progress listener
    } catch (Exception e) {
      throw new IllegalStateException("gradle %s failed".formatted(String.join(", ", tasks)), e);
    }
  }

  @Override
  public void build(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    if (isInitialized(resource)) {
      runGradle(resource, diagnostics, this::parseCompilationErrors, "compileTestJava", "assemble");
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
  public void addBuildPlugin(String pluginId, Resource<?> resource) {
    initializeIn(resource);
    var key = pluginKey(pluginId);
    var version = latestVersionOfPlugin(pluginId);
    addPluginToVersionCatalog(pluginId, key, version, resource);
    addPluginToBuildGradle(key, resource);
  }

  private String pluginKey(String pluginId) {
    var parts = pluginId.split("\\.", 2);
    return parts.length > 1 ? parts[1].replace('.', '-') : pluginId;
  }

  private String latestVersionOfPlugin(String pluginId) {
    var groupPath = pluginId.replace('.', '/');
    var url =
        "https://plugins.gradle.org/m2/%s/%s.gradle.plugin/maven-metadata.xml"
            .formatted(groupPath, pluginId);
    try (var stream = URI.create(url).toURL().openStream()) {
      var content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      return Pattern.compile("<version>(\\d+(?:\\.\\d+)*)</version>")
          .matcher(content)
          .results()
          .map(m -> m.group(1))
          .reduce((ignored, b) -> b)
          .orElseThrow(
              () -> new IllegalStateException("No stable version found at %s".formatted(url)));
    } catch (UnknownHostException ignored) {
      // Probably offline, which means we're probably testing on an airplane or something,
      // so just return recent
      return LATEST_KNOWN_VERSIONS.getOrDefault(pluginId, "0.1.2");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch plugin version from %s".formatted(url), e);
    }
  }

  private void addPluginToVersionCatalog(
      String pluginId, String key, String version, Resource<?> resource) {
    var catalog = resource.select("gradle/libs.versions.toml");
    var content = catalog.readAsString();
    content =
        content.replace("\n\n[libraries]", "\n%s = \"%s\"\n\n[libraries]".formatted(key, version));
    content =
        "%s\n%s = { id = \"%s\", version.ref = \"%s\" }\n"
            .formatted(content.stripTrailing(), key, pluginId, key);
    try {
      catalog.writeAsString(content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update version catalog", e);
    }
  }

  private void addPluginToBuildGradle(String key, Resource<?> resource) {
    var buildGradle = resource.select("build.gradle");
    var content = buildGradle.readAsString();
    var alias = "    alias libs.plugins.%s".formatted(key.replace('-', '.'));
    content = content.replace("}\n\nrepositories", "%s\n}\n\nrepositories".formatted(alias));
    try {
      buildGradle.writeAsString(content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update build.gradle", e);
    }
  }

  @Override
  public void enableBuildPlugin(String plugin, Resource<?> resource) {
    var buildGradle = resource.select("build.gradle");
    var content = buildGradle.readAsString();
    var apply = "apply plugin: '%s'".formatted(plugin);
    content = content.replace("dependencies {", "%s\n\ndependencies {".formatted(apply));
    try {
      buildGradle.writeAsString(content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update build.gradle", e);
    }
  }

  @Override
  public void addDependency(String dependency, Resource<?> resource) {
    addScopedDependency(dependency, "implementation", resource);
  }

  @Override
  public void addRuntimeDependency(String dependency, Resource<?> resource) {
    addScopedDependency(dependency, "runtimeOnly", resource);
  }

  private void addScopedDependency(String dependency, String scope, Resource<?> resource) {
    initializeIn(resource);
    var parts = dependency.split(":");
    addDependencyToVersionCatalog(resource, parts[0], parts[1], parts.length < 3 ? null : parts[2]);
    addDependencyToBuildGradle(resource, parts[1], scope);
  }

  private void addDependencyToVersionCatalog(
      Resource<?> resource, String module, String artifact, String version) {
    var versionCatalogResource = resource.select("gradle/libs.versions.toml");
    var versionCatalog = new VersionCatalog(versionCatalogResource.readAsString());
    versionCatalog.addDependency(module, artifact, version);
    try {
      versionCatalogResource.writeAsString(versionCatalog.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update version catalog", e);
    }
  }

  private void addDependencyToBuildGradle(Resource<?> resource, String artifact, String scope) {
    try {
      var buildFileResource = resource.select("build.gradle");
      var buildFile = new BuildFile(buildFileResource.readAsString());
      buildFile.addDependency(scope, "libs.%s".formatted(artifact.replace('-', '.')));
      buildFileResource.writeAsString(buildFile.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update Gradle dependencies", e);
    }
  }

  @Override
  public void configureTask(String task, List<String> configuration, Resource<?> resource) {
    try {
      var buildFileResource = resource.select("build.gradle");
      var content = buildFileResource.readAsString();
      if (content.contains("%s {".formatted(task))) {
        return;
      }
      var properties = configuration.stream().map("    %s"::formatted).collect(joining("\n"));
      buildFileResource.writeAsString(
          content.stripTrailing() + TASKS_BLOCK.formatted(task, properties));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to configure task %s".formatted(task), e);
    }
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (!GENERATE_BUILD_CONFIG.equals(suggestionCode)) {
      return AppliedSuggestion.unknown(suggestionCode);
    }
    try {
      initializeIn(resource);
      return buildConfigResources(resource);
    } catch (Exception e) {
      return AppliedSuggestion.failedWith(e);
    }
  }

  private synchronized void initializeIn(Resource<?> root) {
    if (root.select("build.gradle").exists()) {
      return;
    }
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    try {
      try (var connection =
          GradleConnector.newConnector()
              .forProjectDirectory(root.toFile())
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
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "gradle init failed%nstdout: %s%nstderr: %s".formatted(stdout, stderr), e);
    }
  }

  private List<Path> cleanUpFiles(Resource<?> resource) throws IOException {
    var result = new ArrayList<Path>();
    cleanUpBuild(resource, result);
    cleanUpSettings(resource);
    cleanUpVersionCatalog(resource);
    return result;
  }

  private void cleanUpBuild(Resource<?> resource, Collection<Path> changed) throws IOException {
    resource.select("build.gradle").writeAsString(BUILD_GRADLE);
    var lib = resource.select("lib");
    lib.delete();
    changed.add(lib.toFile().toPath().toAbsolutePath());
  }

  private void cleanUpSettings(Resource<?> resource) throws IOException {
    var settings = resource.select("settings.gradle");
    String rootProject;
    try (var reader = new BufferedReader(new InputStreamReader(settings.readFrom()))) {
      rootProject =
          reader
              .lines()
              .filter(line -> line.contains("rootProject.name"))
              .findFirst()
              .orElseThrow();
    }
    settings.writeAsString(rootProject);
  }

  private void cleanUpVersionCatalog(Resource<?> resource) throws IOException {
    resource.select("gradle/libs.versions.toml").writeAsString(VERSION_CATALOG);
  }

  private AppliedSuggestion buildConfigResources(Resource<?> resource) {
    var root = resource.select("/");
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
            (acc, path) -> acc.with(root.select(path)),
            (a, ignored) -> a);
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
