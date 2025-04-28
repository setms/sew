package org.setms.sew.core.usecase.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.format.sew.SewFormat;
import org.setms.sew.core.tool.Glob;
import org.setms.sew.core.tool.Output;
import org.setms.sew.core.tool.Tool;

class UseCaseToolTest {

  private final Tool tool = new UseCaseTool();
  private final File baseDir = new File("src/test/resources/use-cases");

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).hasSize(1);
    var input = actual.getFirst();
    assertThat(input.glob()).hasToString("src/main/requirements/**/*.useCase");
    assertThat(input.format()).isInstanceOf(SewFormat.class);
  }

  @Test
  void shouldDefineOutputs() {
    var actual = tool.getOutputs();

    assertThat(actual)
        .hasSize(2)
        .allSatisfy(
            output -> assertThat(output.glob().toString()).contains("build/reports/useCases/*."));
    assertThat(
            actual.stream()
                .map(Output::glob)
                .map(Glob::getPattern)
                .map(p -> p.substring(1 + p.lastIndexOf(".")))
                .toList())
        .containsExactlyInAnyOrder("html", "png");
  }

  @Test
  void shouldBuildScenarioImage() {
    var testDir = new File(baseDir, "valid");
    var buildDir = new File(testDir, "build");

    var actual = tool.build(testDir, buildDir);

    assertThat(actual).isEmpty();
    var output = new File(buildDir, "reports/useCases/HappyPath.png");
    assertThat((output)).isFile();
  }
}
