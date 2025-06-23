package org.setms.sew.core.inboud.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.core.outbound.tool.file.FileInputSource;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;

class UseCaseToolTest extends ToolTestCase<UseCase> {

  private static final String USE_CASE_HTML =
      """
    <html>
      <body>
        <h1>Just do it</h1>
        <p>A sample use case for demonstration purposes.</p>
        <h2>All's well that ends well</h2>
        <p>This is the happy path scenario, where everything goes according to plan.</p>
        <img src="HappyPath-structure.png"/>
        <ol>
          <li>A donald, looking at the info, does it.</li>
          <li>The system does it again.</li>
          <li>The system updates the info.</li>
        </ol>
      </body>
    </html>
    """;
  private static final String DUCK_USER =
      """
    package missing

    user Duck {
      display = "Duck"
    }
    """;

  public UseCaseToolTest() {
    super(new UseCaseTool(), UseCase.class, "main/requirements");
  }

  @Test
  void shouldDefineOutputs() {
    var actual = getTool().getOutputs();

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
  void shouldWarnAboutMissingElementsAndCreateThem() throws IOException {
    var testDir = getTestDir("missing");
    var source = new FileInputSource(testDir);

    var diagnostics = getTool().validate(source);

    assertThat(diagnostics)
        .hasSizeGreaterThanOrEqualTo(7)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.suggestions()).as("Suggestions").isNotEmpty();
            });
    var sink = new FileOutputSink(testDir);
    var diagnostic = diagnostics.getFirst();
    diagnostics =
        getTool()
            .apply(diagnostic.suggestions().getFirst().code(), source, diagnostic.location(), sink);
    assertThat(diagnostics).hasSize(1).allSatisfy(d -> assertThat(d.message()).contains("Created"));
    var file = sink.select("src/main/stakeholders/Duck.user").getFile();
    assertThat(file).isFile();
    try {
      assertThat(file).hasContent(DUCK_USER);
    } finally {
      Files.delete(file.toPath());
    }
  }

  @Test
  void shouldRejectGrammarViolation() {
    var source = inputSourceFor("grammar");

    var diagnostics = getTool().validate(source);

    assertThat(diagnostics)
        .filteredOn(diagnostic -> diagnostic.level() == ERROR)
        .map(Diagnostic::message)
        .containsExactlyInAnyOrder(
            "Users can't emit events",
            "Events can't precede aggregates",
            "Aggregates can't issue commands",
            "Commands can't precede policies");
  }

  @Test
  void shouldBuildReport() {
    var testDir = getTestDir("valid");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = getTool().build(source, sink);

    assertThat(actual).isEmpty();
    var output = sink.select("reports/useCases/HappyPath-structure.png").getFile();
    assertThat((output)).isFile();
    output = sink.select("reports/useCases/JustDoIt.html").getFile();
    assertThat((output)).isFile().hasContent(USE_CASE_HTML);
  }

  @Test
  void shouldBuildComplexUseCaseWithoutProblems() {
    var testDir = getTestDir("../domains/gdpr");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = getTool().build(source, sink);

    assertThat(actual).isEmpty();
  }

  @Test
  void shouldCreateDomain() {
    var testDir = getTestDir( "valid");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = getTool().validate(source);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().equals("Missing domain")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing domain").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).isEqualTo("Discover subdomains");
    actual = getTool().apply(suggestion.code(), source, diagnostic.location(), sink);
    assertThat(actual).hasSize(1);
    diagnostic = actual.getFirst();
    assertThat(diagnostic.message()).startsWith("Created ");
  }

  @Test
  void shouldCreateAcceptanceTest() {
    var testDir = getTestDir( "valid");
    var source = new FileInputSource(testDir);
    var sink = new FileOutputSink(testDir).select("build");

    var actual = getTool().validate(source);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().startsWith("Missing acceptance test")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing acceptance test").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).startsWith("Create acceptance test");
    actual = getTool().apply(suggestion.code(), source, diagnostic.location(), sink);
    assertThat(actual).as("Created artifacts").hasSize(1);
    diagnostic = actual.getFirst();
    assertThat(diagnostic.message()).startsWith("Created ");
  }
}
