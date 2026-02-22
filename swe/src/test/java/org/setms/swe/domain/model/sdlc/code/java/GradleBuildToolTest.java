package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;

class GradleBuildToolTest {

  private static final String PROJECT_NAME = "MyProject";

  private final GradleBuildTool buildTool = new GradleBuildTool(PROJECT_NAME);
  private final InMemoryWorkspace workspace = new InMemoryWorkspace();

  @Test
  void shouldEmitDiagnosticWhenBuildGradleMissing() throws IOException {
    createFile("/settings.gradle", "rootProject.name = 'test'");
    var diagnostics = new ArrayList<Diagnostic>();

    buildTool.validate(workspace.root(), diagnostics);

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

    buildTool.validate(workspace.root(), diagnostics);

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

    buildTool.validate(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldGenerateBuildConfigViaGradleToolingApi(@TempDir File projectDir) {
    var workspace = new DirectoryWorkspace(projectDir);

    var actual = buildTool.applySuggestion(GradleBuildTool.GENERATE_BUILD_CONFIG, workspace.root());

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

    var actual = buildTool.applySuggestion(GradleBuildTool.GENERATE_BUILD_CONFIG, workspace.root());

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(10);
  }

  @Test
  void shouldProduceNoDiagnosticsWhenSourcesCompileCleanly(@TempDir File projectDir)
      throws IOException {
    var workspace = new DirectoryWorkspace(projectDir);
    buildTool.applySuggestion(GradleBuildTool.GENERATE_BUILD_CONFIG, workspace.root());
    givenJavaSourceFile(workspace.root());
    var diagnostics = new ArrayList<Diagnostic>();

    buildTool.build(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
    assertThat(workspace.root().select("build/classes/java/main/com/example/Hello.class").exists())
        .isTrue();
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

    buildTool.build(workspace.root(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldReturnNoneForUnknownSuggestion() {
    var actual = buildTool.applySuggestion("unknown.suggestion", workspace.root());

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
