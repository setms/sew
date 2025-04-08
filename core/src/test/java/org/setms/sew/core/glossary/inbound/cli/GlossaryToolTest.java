package org.setms.sew.core.glossary.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.tool.Level.ERROR;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.format.sew.SewFormat;
import org.setms.sew.core.tool.Diagnostic;
import org.setms.sew.core.tool.Tool;

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
    var testDir = new File(baseDir, "report");
    var buildDir = new File(testDir, "build");

    var actual = tool.build(testDir, buildDir);

    assertThat(actual).isEmpty();
    var output = new File(buildDir, "reports/glossary/report.html");
    assertThat(output).isFile().content().isEqualTo(GLOSSARY);
  }

  @Test
  void shouldRejectInvalidName() {
    var dir = new File(baseDir, "invalid/name");

    var actual = tool.validate(dir);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR, "Object name 'WrongName' doesn't match file name 'InvalidName.term'"));
  }

  @Test
  void shouldRejectMissingDisplay() {
    var dir = new File(baseDir, "invalid/display");

    var actual = tool.validate(dir);

    assertThat(actual)
        .hasSize(1)
        .contains(new Diagnostic(ERROR, "MissingDisplay: display must not be empty"));
  }

  @Test
  void shouldRejectInvalidSeeAlso() {
    var dir = new File(baseDir, "invalid/see");

    var actual = tool.validate(dir);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(ERROR, "Term 'InvalidSeeAlso' refers to unknown term 'NonExistent'"));
  }
}
