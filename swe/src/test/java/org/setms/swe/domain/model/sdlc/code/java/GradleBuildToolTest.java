package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
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
  void shouldGenerateBuildGradleAndSettings() throws IOException {
    var suggestionCode = GradleBuildTool.GENERATE_BUILD_CONFIG;

    var actual = buildTool.applySuggestion(suggestionCode, workspace.root());

    assertThat(actual.diagnostics()).isEmpty();
    assertThat(actual.createdOrChanged()).hasSize(2);
    var buildGradle = workspace.root().select("/build.gradle");
    assertThat(buildGradle.exists()).isTrue();
    var buildContent = readFile(buildGradle);
    assertThat(buildContent).contains("plugins", "id 'java'", "junit-jupiter", "assertj", "jqwik");
    var settingsGradle = workspace.root().select("/settings.gradle");
    assertThat(settingsGradle.exists()).isTrue();
    var settingsContent = readFile(settingsGradle);
    assertThat(settingsContent).contains("rootProject.name = '" + PROJECT_NAME + "'");
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

  private String readFile(org.setms.km.domain.model.workspace.Resource<?> resource)
      throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(resource.readFrom()))) {
      return reader.lines().reduce((a, b) -> a + "\n" + b).orElse("");
    }
  }
}
