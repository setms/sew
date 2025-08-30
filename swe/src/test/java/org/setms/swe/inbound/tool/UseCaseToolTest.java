package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;

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
  private static final String USER =
      """
    package missing

    user Duck {
      display = "Duck"
    }
    """;

  public UseCaseToolTest() {
    super(new UseCaseTool(), UseCase.class, "main/requirements/use-cases");
  }

  @Test
  void shouldCreateDomainStory() throws IOException {
    var workspace = workspaceFor("missing");

    var diagnostics = validateAgainst(workspace);

    assertThat(diagnostics)
        .hasSizeGreaterThanOrEqualTo(6)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.suggestions()).as("Suggestions").isNotEmpty();
            });
    var diagnostic = diagnostics.getFirst();
    var created =
        apply(diagnostic.suggestions().getFirst(), diagnostic, workspace).createdOrChanged();
    var domainStory =
        workspace.root().select("src/main/requirements/domain-stories/HappyPath.domainStory");
    assertThat(created).hasSize(1).contains(domainStory);
    var file = toFile(domainStory);
    assertThat(file).isFile();
    try {
      assertThat(file).hasContent(DOMAIN_STORY);
    } finally {
      Files.delete(file.toPath());
    }
  }

  @Test
  void shouldCreateMissingReference() throws IOException {
    var workspace = workspaceFor("missing");

    var diagnostics = validateAgainst(workspace);

    assertThat(diagnostics)
        .hasSizeGreaterThanOrEqualTo(6)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.suggestions()).as("Suggestions").isNotEmpty();
            });
    var diagnostic =
        diagnostics.stream().filter(d -> d.message().contains("user")).findFirst().orElseThrow();
    var created = apply(diagnostic.suggestions().getFirst(), diagnostic, workspace);
    assertThat(created.diagnostics()).isEmpty();
    var user = workspace.root().select("src/main/stakeholders/Duck.user");
    assertThat(created.createdOrChanged()).hasSize(1).contains(user);
    var file = toFile(user);
    assertThat(file).isFile();
    try {
      assertThat(file).hasContent(USER);
    } finally {
      Files.delete(file.toPath());
    }
  }

  @Test
  void shouldRejectGrammarViolation() {
    var source = workspaceFor("grammar");

    var diagnostics = validateAgainst(source);

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
  protected void assertBuild(Resource<?> resource) {
    var output = toFile(resource.select("build/JustDoIt/HappyPath.png"));
    assertThat((output)).isFile();
    output = toFile(resource.select("build/JustDoIt/JustDoIt.html"));
    assertThat((output)).isFile().hasContent(USE_CASE_HTML);
  }

  @Test
  void shouldBuildComplexUseCaseWithoutProblems() {
    var workspace = workspaceFor("../domains/gdpr");

    var actual = build(workspace);

    assertThat(actual).isEmpty();
  }

  @Test
  void shouldCreateDomain() {
    var workspace = workspaceFor("valid");

    var actual = validateAgainst(workspace);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().equals("Missing subdomains")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing subdomains").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).isEqualTo("Discover subdomains");
    var created = apply(suggestion, diagnostic, workspace).createdOrChanged();
    assertThat(created).hasSize(1);
    workspace
        .root()
        .matching(Inputs.domains().glob())
        .forEach(
            resource -> {
              try {
                resource.delete();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void shouldCreateAcceptanceTest() {
    var workspace = workspaceFor("valid");

    var actual = validateAgainst(workspace);

    assertThat(actual.size()).isGreaterThanOrEqualTo(1);
    var maybeDiagnostic =
        actual.stream().filter(d -> d.message().startsWith("Missing acceptance test")).findFirst();
    assertThat(maybeDiagnostic).as("Warning about missing acceptance test").isPresent();
    var diagnostic = maybeDiagnostic.get();
    assertThat(diagnostic.suggestions()).hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).startsWith("Create acceptance test");
    var created = apply(suggestion, diagnostic, workspace).createdOrChanged();
    assertThat(created).as("Created artifacts").hasSize(1);
    workspace
        .root()
        .matching(Inputs.acceptanceTests().glob())
        .forEach(
            resource -> {
              try {
                resource.delete();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
