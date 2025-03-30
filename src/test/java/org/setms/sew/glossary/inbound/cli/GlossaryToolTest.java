package org.setms.sew.glossary.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.tool.Tool;

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
    var input = actual.iterator().next();
    assertThat(input.getGlob()).hasToString("src/main/glossary/**/*.term");
    assertThat(input.getFormat()).isInstanceOf(SewFormat.class);
  }

  @Test
  void shouldBuildReport() {
    tool.run(new File(baseDir, "report"));

    var output = new File(baseDir, "report/build/reports/glossary/report.html");
    assertThat(output).isFile().content().isEqualTo(GLOSSARY);
  }

  @Test
  void shouldRejectInvalidName() {
    var dir = new File(baseDir, "invalid/name");

    assertThatThrownBy(() -> tool.run(dir))
        .hasMessage("Object name 'WrongName' doesn't match file name 'InvalidName.term'");
  }

  @Test
  void shouldRejectMissingDisplay() {
    var dir = new File(baseDir, "invalid/display");

    assertThatThrownBy(() -> tool.run(dir)).hasMessage("MissingDisplay: display must not be empty");
  }

  @Test
  void shouldRejectInvalidSeeAlso() {
    var dir = new File(baseDir, "invalid/see");

    assertThatThrownBy(() -> tool.run(dir))
        .hasMessage("Term 'InvalidSeeAlso' refers to unknown term 'NonExistent'");
  }
}
