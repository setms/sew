package org.setms.swe.inbound.tool;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.TechnologyResolverImpl.PICK_PROGRAMMING_LANGUAGE;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class TechnologyResolverImplTest {

  private final TechnologyResolver resolver = new TechnologyResolverImpl();

  @Test
  void shouldNeedProgrammingLanguageForUnitTestGenerator() {
    var diagnostics = new ArrayList<Diagnostic>();
    Location location = new Location("foo/bar");

    resolver.unitTestGenerator(emptyList(), location, diagnostics);

    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            diagnostic -> {
              assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
              assertThat(diagnostic.location()).as("Location").isEqualTo(location);
              assertThat(diagnostic.message())
                  .as("Message")
                  .isEqualTo("Missing decision on programming language");
              assertThat(diagnostic.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      suggestion ->
                          assertThat(suggestion.message())
                              .as("Suggestion")
                              .isEqualTo("Decide on programming language"));
            });
  }

  @Test
  void shouldCreateProgrammingLanguageDecision() {
    var workspace = new InMemoryWorkspace();

    var actual = resolver.applySuggestion(PICK_PROGRAMMING_LANGUAGE, workspace.root());

    assertThat(actual.diagnostics()).as("Diagnostics").isEmpty();
    assertThat(actual.createdOrChanged())
        .as("Created")
        .hasSize(1)
        .allSatisfy(
            resource -> {
              assertThat(resource.path())
                  .as("Path")
                  .isEqualTo("/src/main/architecture/ProgrammingLanguage.decision");
            });
  }
}
