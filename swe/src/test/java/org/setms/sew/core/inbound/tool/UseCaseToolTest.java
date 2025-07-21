package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

class UseCaseToolTest extends ToolTestCase<UseCase> {

  private static final String USE_CASE_HTML =
      """
    <html>
      <body>
        <h1>Just do it</h1>
        <p>A sample use case for demonstration purposes.</p>
        <h2>Just do it</h2>
        <p>This is the happy path scenario, where everything goes according to plan.</p>
        <img src="HappyPath.png"/>
        <ol>
          <li>A donald, looking at the info, does it.</li>
          <li>The system does it again.</li>
          <li>The system updates the info.</li>
        </ol>
      </body>
    </html>
    """;
  private static final String DOMAIN_STORY =
      """
    package missing

    domainStory HappyPath {
      description = "TODO: Add description and sentences."
      granularity = fine
      pointInTime = tobe
    }

    sentence {
      parts = [ ]
    }
    """;
  public static final String CREATED = "Created ";

  public UseCaseToolTest() {
    super(new UseCaseTool(), UseCase.class, "main/requirements");
  }

  @Test
  void shouldDefineOutputs() {
    var actual = getTool().getOutputs();

    assertThat(actual)
        .isPresent()
        .hasValueSatisfying(
            output -> assertThat(output.glob().toString()).contains("reports/useCases/**/*.html"));
  }

  @Test
  void shouldWarnAboutMissingElementsAndCreateThem() throws IOException {
    var workspace = workspaceFor("missing");

    var diagnostics = getTool().validate(workspace);

    assertThat(diagnostics)
        .hasSizeGreaterThanOrEqualTo(7)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.suggestions()).as("Suggestions").isNotEmpty();
            });
    var diagnostic = diagnostics.getFirst();
    diagnostics =
        getTool()
            .apply(diagnostic.suggestions().getFirst().code(), workspace, diagnostic.location());
    assertThat(diagnostics).hasSize(1).allSatisfy(d -> assertThat(d.message()).contains("Created"));
    var file = toFile(workspace.output().select("../src/main/requirements/HappyPath.domainStory"));
    assertThat(file).isFile();
    try {
      assertThat(file).hasContent(DOMAIN_STORY);
    } finally {
      Files.delete(file.toPath());
    }
  }

  @Test
  void shouldRejectGrammarViolation() {
    var source = workspaceFor("grammar");

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

  @Override
  protected void assertBuild(OutputSink sink) {
    var output = toFile(sink.select("reports/useCases/HappyPath.png"));
    assertThat((output)).isFile();
    output = toFile(sink.select("reports/useCases/JustDoIt.html"));
    assertThat((output)).isFile().hasContent(USE_CASE_HTML);
  }

  @Test
  void shouldBuildComplexUseCaseWithoutProblems() {
    var workspace = workspaceFor("../domains/gdpr");

    var actual = getTool().build(workspace);

    assertThat(actual).isEmpty();
  }

  @Test
  void shouldCreateDomain() {
    var workspace = workspaceFor("valid");

    var actual = getTool().validate(workspace);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().equals("Missing subdomains")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing subdomains").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).isEqualTo("Discover subdomains");
    actual = getTool().apply(suggestion.code(), workspace, diagnostic.location());
    assertThat(actual).hasSize(1);
    diagnostic = actual.getFirst();
    assertThat(diagnostic.message()).startsWith(CREATED);
    workspace
        .output()
        .select("..")
        .matching(Inputs.domains().glob())
        .forEach(
            outputSink -> {
              try {
                outputSink.delete();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void shouldCreateAcceptanceTest() throws IOException {
    var workspace = workspaceFor("valid");

    var actual = getTool().validate(workspace);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().startsWith("Missing acceptance test")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing acceptance test").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).startsWith("Create acceptance test");
    actual = getTool().apply(suggestion.code(), workspace, diagnostic.location());
    assertThat(actual).as("Created artifacts").hasSize(1);
    diagnostic = actual.getFirst();
    assertThat(diagnostic.message()).startsWith(CREATED);
    workspace
        .output()
        .select("..")
        .matching(Inputs.acceptanceTests().glob())
        .forEach(
            outputSink -> {
              try {
                outputSink.delete();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
