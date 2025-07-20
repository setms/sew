package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;

import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.outbound.workspace.file.FileOutputSink;
import org.setms.sew.core.domain.model.sdlc.ddd.Term;

class GlossaryToolTest extends ToolTestCase<Term> {

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

  public GlossaryToolTest() {
    super(new GlossaryTool(), Term.class, "main/glossary");
  }

  @Test
  void shouldDefineOutputs() {
    var actual = getTool().getOutputs();

    assertThat(actual).hasSize(1);
    var output = actual.getFirst();
    assertThat(output.glob()).hasToString("build/reports/glossary/*.html");
  }

  @Test
  void shouldBuildReport() {
    var source = inputSourceFor("valid");
    var sink = new FileOutputSink(getTestDir("build"));

    var actual = getTool().build(source, sink);

    assertThat(actual).isEmpty();
    var output = sink.select("reports/glossary/report.html").getFile();
    assertThat(output).isFile().content().isEqualTo(GLOSSARY);
  }

  @Test
  void shouldRejectMissingDisplay() {
    var source = inputSourceFor("invalid/display");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR,
                "MissingDisplay: display must not be empty",
                new Location("term", "MissingDisplay")));
  }

  @Test
  void shouldRejectInvalidSeeAlso() {
    var source = inputSourceFor("invalid/see");

    var actual = getTool().validate(source);

    assertThat(actual)
        .hasSize(1)
        .contains(
            new Diagnostic(
                ERROR,
                "Term 'InvalidSeeAlso' refers to unknown term 'NonExistent'",
                new Location("invalid", "term", "InvalidSeeAlso")));
  }
}
