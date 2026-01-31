package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.setms.swe.inbound.tool.AcceptanceTestTool.SUGGESTION_CREATE_UNIT_TEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;
import org.setms.swe.inbound.format.acceptance.AcceptanceFormat;

class AcceptanceTestToolTest extends ToolTestCase<AcceptanceTest> {

  private static final String AGGREGATE_ACCEPTANCE_TEST_HTML =
      """
    <html>
      <body>
        <h1>Acceptance tests for aggregate Notifications</h1>
        <h2>Accept NotifyUser and emit UserNotified</h2>
        <strong>Given</strong> <code>Notifications</code> is empty<br/>
        <strong>When</strong> <code>Notifications</code> accepts <code>NotifyUser{ $Message }</code><br/>
        <strong>Then</strong> <code>Notifications</code> is empty<br/>
        <strong>And</strong> <code>Notifications</code> emits <code>UserNotified{ $Message }</code><br/>
      </body>
    </html>
    """;
  private static final String REPORT_PATH = "build/Notifications-aggregate.html";

  private static final TechnologyResolver technologyResolver = mock(TechnologyResolver.class);
  private static final UnitTestGenerator generator = mock(UnitTestGenerator.class);

  protected AcceptanceTestToolTest() {
    super(
        new AcceptanceTestTool(technologyResolver),
        AcceptanceFormat.class,
        "test/acceptance",
        "acceptance");
  }

  @Override
  protected void assertBuild(Resource<?> resource) {
    var output = toFile(resource.select(REPORT_PATH));
    assertThat((output))
        .as("%s exists".formatted(REPORT_PATH))
        .isFile()
        .as("Contents of %s".formatted(REPORT_PATH))
        .hasContent(AGGREGATE_ACCEPTANCE_TEST_HTML);
  }

  @Test
  void shouldReportDiagnosticsFromTechnologyResolver() {
    var diagnostic = new Diagnostic(Level.WARN, "Something's not right");
    when(technologyResolver.unitTestGenerator(
            anyCollection(), any(Location.class), anyCollection()))
        .thenAnswer(
            invocation -> {
              Collection<Diagnostic> diagnostics = invocation.getArgument(2);
              diagnostics.add(diagnostic);
              return Optional.empty();
            });
    var tool = (AcceptanceTestTool) getTool();
    var diagnostics = new ArrayList<Diagnostic>();
    var acceptanceTest = new AcceptanceTest(new FullyQualifiedName("package.Name"));

    tool.validate(acceptanceTest, new ResolvedInputs(), diagnostics);

    assertThat(diagnostics).containsExactly(diagnostic);
  }

  @Test
  void shouldLetTechnologyResolverApplySuggestion() {
    var tool = (AcceptanceTestTool) getTool();
    var suggestionCode = "do.something.about.it";
    var resource = new InMemoryWorkspace().root();
    var expected = AppliedSuggestion.failedWith("It's out of my hands");
    when(technologyResolver.applySuggestion(suggestionCode, resource)).thenReturn(expected);

    var actual = tool.applySuggestion(null, suggestionCode, null, null, resource);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void shouldReportMissingUnitTest() {
    var tool = (AcceptanceTestTool) getTool();
    var diagnostics = new ArrayList<Diagnostic>();
    var acceptanceTest = new AcceptanceTest(new FullyQualifiedName("package.Name"));
    var inputs = new ResolvedInputs();
    when(technologyResolver.unitTestGenerator(
            anyCollection(), any(Location.class), anyCollection()))
        .thenReturn(Optional.of(generator));

    tool.validate(acceptanceTest, inputs, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .first()
        .satisfies(
            diagnostic -> {
              assertThat(diagnostic.level()).isEqualTo(Level.WARN);
              assertThat(diagnostic.message()).isEqualTo("Missing unit test");
            });
  }

  @Test
  void shouldCreateUnitTest() {
    var tool = (AcceptanceTestTool) getTool();
    var acceptanceTest = new AcceptanceTest(new FullyQualifiedName("package.Name"));
    var inputs = new ResolvedInputs();
    var workspace = new InMemoryWorkspace();
    when(technologyResolver.unitTestGenerator(
            anyCollection(), any(Location.class), anyCollection()))
        .thenReturn(Optional.of(generator));
    UnitTest unitTest = new UnitTest(new FullyQualifiedName("package.Name"));
    when(generator.generate(acceptanceTest)).thenReturn(unitTest);

    var actual =
        tool.applySuggestion(
            acceptanceTest,
            SUGGESTION_CREATE_UNIT_TEST,
            acceptanceTest.toLocation(),
            inputs,
            workspace.root());

    assertThat(actual).as("Applied suggestion").isNotNull();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .isNotNull()
        .hasSize(1)
        .first()
        .satisfies(resource -> assertThat(resource.path()).isEqualTo("/src/test/java/Name.java"));
  }
}
