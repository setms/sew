package org.setms.sew.core.inboud.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.tool.FileInputSource;
import org.setms.sew.core.domain.model.tool.FileOutputSink;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;
import org.setms.sew.core.inbound.tool.UseCaseTool;

class UseCaseToolTest {

  private static final String USE_CASE_HTML =
      """
    <html>
      <body>
        <h1>Just do it</h1>
        <p>A sample use case for demonstration purposes.</p>
        <h2>All's well that ends well</h2>
        <p>This is the happy path scenario, where everything goes according to plan.</p>
        <img src="HappyPath.png"/>
      </body>
    </html>
    """;
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
                .map(Glob::pattern)
                .map(p -> p.substring(1 + p.lastIndexOf(".")))
                .toList())
        .containsExactlyInAnyOrder("html", "png");
  }

  @Test
  void shouldBuildReport() {
    var testDir = new File(baseDir, "valid");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = tool.build(source, sink);

    assertThat(actual).isEmpty();
    var output = sink.select("reports/useCases/HappyPath.png").getFile();
    assertThat((output)).isFile();
    output = sink.select("reports/useCases/JustDoIt.html").getFile();
    assertThat((output)).isFile().hasContent(USE_CASE_HTML);
  }
}
