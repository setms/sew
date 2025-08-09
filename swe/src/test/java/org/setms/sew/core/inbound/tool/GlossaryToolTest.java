package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;

import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
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
  void shouldBuildReport() {
    var workspace = workspaceFor("valid");

    var actual = getTool().build(workspace);

    assertThat(actual).isEmpty();
    var output = toFile(workspace.root().select("build/report.html"));
    assertThat(output).isFile().content().isEqualTo(GLOSSARY);
  }

  @Test
  void shouldRejectMissingDisplay() {
    var source = workspaceFor("invalid/display");

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
    var source = workspaceFor("invalid/see");

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
