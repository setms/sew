package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.CodeTester;

class GradleTest {

  private static final String PROJECT_NAME = "MyProject";

  private final CodeBuilder codeBuilder = new Gradle(PROJECT_NAME);
  private final CodeTester codeTester = new Gradle(PROJECT_NAME);
  private final InMemoryWorkspace workspace = new InMemoryWorkspace();

  @Test
  void shouldEmitDiagnosticWhenBuildGradleMissing() throws IOException {
    createFile("/settings.gradle", "rootProject.name = 'test'");
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.validate(workspace.root(), diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .first()
        .satisfies(
            diagnostic -> {
              assertThat(diagnostic.level()).isEqualTo(WARN);
              assertThat(diagnostic.message()).isEqualTo("Gradle project isn't initialized");
              assertThat(diagnostic.suggestions()).hasSize(1);
              assertThat(diagnostic.suggestions().getFirst().message())
                  .isEqualTo("Generate build configuration files");
            });
  }

  @Test
  void shouldEmitDiagnosticWhenSettingsGradleMissing() throws IOException {
    createFile("/build.gradle", "plugins { id 'java' }");
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.validate(workspace.root(), diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .first()
        .satisfies(
            diagnostic -> {
              assertThat(diagnostic.level()).isEqualTo(WARN);
              assertThat(diagnostic.message()).isEqualTo("Gradle project isn't initialized");
            });
  }

  @Test
  void shouldNotEmitDiagnosticWhenConfigurationExists() throws IOException {
    createFile("/build.gradle", "plugins { id 'java' }");
    createFile("/settings.gradle", "rootProject.name = 'test'");
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.validate(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldGenerateBuildConfigViaGradleToolingApi(@TempDir File projectDir) {
    var workspace = new DirectoryWorkspace(projectDir);

    var actual = codeBuilder.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root());

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(10);
    assertThat(workspace.root().select("/gradle/libs.versions.toml").exists()).isTrue();
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void shouldGenerateBuildConfigEvenWithExistingSourceFiles(@TempDir File projectDir)
      throws IOException {
    var javaFile = new File(projectDir, "src/test/java/com/example/FooTest.java");
    javaFile.getParentFile().mkdirs();
    javaFile.createNewFile();
    var workspace = new DirectoryWorkspace(projectDir);

    var actual = codeBuilder.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root());

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(10);
  }

  @Test
  void shouldProduceNoDiagnosticsWhenSourcesCompileCleanly(@TempDir File projectDir)
      throws IOException {
    var workspace = new DirectoryWorkspace(projectDir);
    codeBuilder.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root());
    givenJavaSourceFile(workspace.root());
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.build(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
    assertThat(workspace.root().select("build/classes/java/main/com/example/Hello.class").exists())
        .isTrue();
  }

  @Test
  void shouldEmitDiagnosticWhenSourcesHaveCompilationError(@TempDir File projectDir)
      throws IOException {
    var workspace = new DirectoryWorkspace(projectDir);
    codeBuilder.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root());
    givenJavaSourceFileWithError(workspace.root());
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.build(workspace.root(), diagnostics);

    assertThat(diagnostics).hasSize(1);
    var actual = diagnostics.getFirst();
    assertThat(actual.level()).isEqualTo(ERROR);
    assertThat(actual.message()).isEqualTo("illegal start of expression");
    assertThat(actual.location())
        .isEqualTo(new Location("src/main/java/com/example/Hello.java", "3"));
  }

  @Test
  void shouldEmitDiagnosticWhenTestSourcesHaveCompilationError(@TempDir File projectDir)
      throws IOException {
    var workspace = new DirectoryWorkspace(projectDir);
    codeBuilder.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, workspace.root());
    givenJavaTestSourceFileWithError(workspace.root());
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.build(workspace.root(), diagnostics);

    assertThat(diagnostics).hasSize(1);
    var actual = diagnostics.getFirst();
    assertThat(actual.level()).isEqualTo(ERROR);
    assertThat(actual.message()).isEqualTo("illegal start of expression");
    assertThat(actual.location())
        .isEqualTo(new Location("src/test/java/com/example/HelloTest.java", "3"));
  }

  private void givenJavaTestSourceFileWithError(Resource<?> root) throws IOException {
    var resource = root.select("src/test/java/com/example/HelloTest.java");
    try (var output = resource.writeTo()) {
      output.write("package com.example;\npublic class HelloTest {\n    int x = ;\n}".getBytes());
    }
  }

  private void givenJavaSourceFileWithError(Resource<?> root) throws IOException {
    var resource = root.select("src/main/java/com/example/Hello.java");
    try (var output = resource.writeTo()) {
      output.write("package com.example;\npublic class Hello {\n    int x = ;\n}".getBytes());
    }
  }

  private void givenJavaSourceFile(Resource<?> root) throws IOException {
    var resource = root.select("src/main/java/com/example/Hello.java");
    try (var output = resource.writeTo()) {
      output.write("package com.example; public class Hello {}".getBytes());
    }
  }

  @Test
  void shouldProduceNoDiagnosticsWhenThereIsNothingToBuild() {
    var diagnostics = new ArrayList<Diagnostic>();

    codeBuilder.build(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldProduceNoDiagnosticsWhenThereIsNothingToTest() {
    var diagnostics = new ArrayList<Diagnostic>();

    codeTester.test(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldReturnNoneForUnknownSuggestion() {
    var actual = codeBuilder.applySuggestion("unknown.suggestion", workspace.root());

    assertThat(actual.createdOrChanged()).isEmpty();
    assertThat(actual.diagnostics()).isEmpty();
  }

  private void createFile(String path, String content) throws IOException {
    var resource = workspace.root().select(path);
    try (var output = resource.writeTo()) {
      output.write(content.getBytes());
    }
  }
}
