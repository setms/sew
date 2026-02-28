package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;

class AggregateToolTest extends ResolverToolTestCase<Aggregate> {

  AggregateToolTest() {
    super(new AggregateTool(), Aggregate.class, "main/design");
  }

  @Test
  void shouldWarnAboutMissingDomainService() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var inputs = givenInputsWithAggregateScenario(aggregate);
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingDomainService(diagnostics);
  }

  private ResolvedInputs givenInputsWithAggregateScenario(Aggregate aggregate) {
    var scenario = new AggregateScenario(new FullyQualifiedName("test", "CreateProject"));
    var acceptanceTest =
        new AcceptanceTest(new FullyQualifiedName("test", "Projects"))
            .setSut(new Link("aggregate", aggregate.getName()))
            .setVariables(List.of())
            .setScenarios(List.of(scenario));
    return givenInputsWithAllPrerequisites().put("acceptanceTests", List.of(acceptanceTest));
  }

  private void assertThatDiagnosticsWarnAboutMissingDomainService(
      Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            d -> {
              assertThat(d.level()).as("Level").isEqualTo(WARN);
              assertThat(d.message()).as("Message").isEqualTo("Missing domain service");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      s ->
                          assertThat(s.message())
                              .as("Suggestion")
                              .isEqualTo("Generate domain service"));
            });
  }
}
