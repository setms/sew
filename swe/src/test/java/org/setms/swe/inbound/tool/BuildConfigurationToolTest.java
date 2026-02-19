package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.code.java.GradleBuildTool.GENERATE_BUILD_CONFIG;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.project.Project;

class BuildConfigurationToolTest {

  private final BuildConfigurationTool tool = new BuildConfigurationTool();

  @Test
  void shouldNotRequireBuildConfigurationWhenFilesExist() throws IOException {
    var workspace = new InMemoryWorkspace();
    createBuildFiles(workspace.root());
    var inputs = resolvedInputs(workspace);
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(inputs, workspace.root(), diagnostics);

    assertThat(diagnostics).as("Diagnostics").isEmpty();
  }

  private void createBuildFiles(org.setms.km.domain.model.workspace.Resource<?> root)
      throws IOException {
    try (var writer = new PrintWriter(root.select("/build.gradle").writeTo())) {
      writer.println("// build");
    }
    try (var writer = new PrintWriter(root.select("/settings.gradle").writeTo())) {
      writer.println("rootProject.name = 'Test'");
    }
  }

  private ResolvedInputs resolvedInputs(InMemoryWorkspace workspace) throws IOException {
    var inputs = new ResolvedInputs();
    var project =
        new Project(new FullyQualifiedName("overview", "MyProject")).setTitle("MyProject");
    inputs.put("projects", List.of(project));
    var langDecision =
        new Decision(new FullyQualifiedName("technology", "ProgrammingLanguage"))
            .setTopic(ProgrammingLanguage.TOPIC)
            .setChoice("Java");
    var buildDecision =
        new Decision(new FullyQualifiedName("technology", "BuildTool"))
            .setTopic(org.setms.swe.domain.model.sdlc.architecture.BuildTool.TOPIC)
            .setChoice("Gradle");
    inputs.put("decisions", List.of(langDecision, buildDecision));
    return inputs;
  }

  @Test
  void shouldRequireBuildConfigurationWhenFilesAbsent() throws IOException {
    var workspace = new InMemoryWorkspace();
    var inputs = resolvedInputs(workspace);
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(inputs, workspace.root(), diagnostics);

    assertThat(diagnostics)
        .as("Diagnostics")
        .hasSize(1)
        .allSatisfy(
            d -> {
              assertThat(d.message()).isEqualTo("Missing build configuration");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(s -> assertThat(s.code()).isEqualTo(GENERATE_BUILD_CONFIG));
            });
  }
}
