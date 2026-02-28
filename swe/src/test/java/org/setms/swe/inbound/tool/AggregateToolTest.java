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
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

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

  @Test
  void shouldNotWarnWhenDomainServiceCodeExists() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var serviceCode =
        new CodeArtifact(new FullyQualifiedName("com.example.domain.services", "ProjectsService"))
            .setCode("// existing");
    var inputs =
        givenInputsWithAggregateScenario(aggregate).put("codeArtifacts", List.of(serviceCode));
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldGenerateServiceCode() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var command =
        new Command(new FullyQualifiedName("design", "CreateProject")).setDisplay("Create project");
    var event = new Event(new FullyQualifiedName("design", "ProjectCreated"));
    var inputs = givenInputsWithFullAggregateScenario(aggregate, command, event);
    var workspace = new InMemoryWorkspace();

    var actual =
        ((AggregateTool) getTool())
            .applySuggestion(
                aggregate, AggregateTool.GENERATE_SERVICE, null, inputs, workspace.root());

    assertThat(actual.createdOrChanged()).hasSize(2);
  }

  private ResolvedInputs givenInputsWithFullAggregateScenario(
      Aggregate aggregate, Command command, Event event) {
    var commandVar =
        new ElementVariable(new FullyQualifiedName("test", "createProject"))
            .setType(new Link("command", command.getName()));
    var eventVar =
        new ElementVariable(new FullyQualifiedName("test", "projectCreated"))
            .setType(new Link("event", event.getName()));
    var scenario =
        new AggregateScenario(new FullyQualifiedName("test", "CreateProject"))
            .setAccepts(new Link("variable", commandVar.getName()))
            .setEmitted(new Link("variable", eventVar.getName()));
    var acceptanceTest =
        new AcceptanceTest(new FullyQualifiedName("test", "Projects"))
            .setSut(new Link("aggregate", aggregate.getName()))
            .setVariables(List.of(commandVar, eventVar))
            .setScenarios(List.of(scenario));
    return givenInputsWithAllPrerequisites()
        .put("acceptanceTests", List.of(acceptanceTest))
        .put("commands", List.of(command))
        .put("events", List.of(event));
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
