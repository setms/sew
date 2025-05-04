package org.setms.sew.core.glossary.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.FileInputSource;
import org.setms.sew.core.domain.model.tool.FileOutputSink;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;
import org.setms.sew.core.inbound.tool.GlossaryTool;

class GlossaryToolTest {

  private static final String GLOSSARY =
      """
    <html>
      <head>
        <title>Glossary: Report</title>
      </head>
      <body>
        <h1>Glossary: Report</h1>
        <dl>
          <dt id="Main">Maine</dt>
          <dd>The most important term.<br/>
            See also: <a href="#Other">Otherz</a>.
          </dd>
          <dt id="Other">Otherz</dt>
          <dd>A much less important term.</dd>
        </dl>
      </body>
    </html>
    """;
  private final Tool tool = new GlossaryTool();
  private final File baseDir = new File("src/test/resources/glossary");

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).hasSize(1);
    var input = actual.getFirst();
    assertThat(input.glob()).hasToString("src/main/glossary/**/*.term");
    assertThat(input.format()).isInstanceOf(SewFormat.class);
  }

  @Test
  void shouldDefineOutputs() {
    var actual = tool.getOutputs();

    assertThat(actual).hasSize(1);
    var output = actual.getFirst();
    assertThat(output.glob()).hasToString("build/reports/glossary/*.html");
  }

  @Test
  void shouldBuildReport() {
    var source = inputSourceFor("report");
    var sink = new FileOutputSink(new File(baseDir, "build"));

    var actual = tool.build(source, sink);

    assertThat(actual).isEmpty();
    var output = sink.select("reports/glossary/report.html").getFile();
    assertThat(output).isFile().content().isEqualTo(GLOSSARY);
  }

  private InputSource inputSourceFor(String path) {
    return new FileInputSource(new File(baseDir, path));
  }

  @Test
  void shouldRejectMissingDisplay() {
    var source = inputSourceFor("invalid/display");

    var actual = tool.validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "MissingDisplay: display must not be empty"));
  }

  @Test
  void shouldRejectInvalidSeeAlso() {
    var source = inputSourceFor("invalid/see");

    var actual = tool.validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR,
                "Term 'InvalidSeeAlso' refers to unknown term 'NonExistent'",
                new Location("term", "InvalidSeeAlso")));
  }
}
