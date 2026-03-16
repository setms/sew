package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

class AggregateToolTest extends ResolverToolTestCase<Aggregate> {

  AggregateToolTest() {
    super(new AggregateTool(), Aggregate.class, "main/design/logical");
  }

  @Test
  void shouldExtendDtoCodeTool() {
    var actual = new AggregateTool();

    assertThat(actual)
        .as("AggregateTool should extend DtoCodeTool")
        .isInstanceOf(DtoCodeTool.class);
  }

  @Test
  void shouldWarnAboutMissingDomainObject() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var inputs = givenInputsWithAggregateScenario(aggregate);
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingDomainObject(diagnostics);
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

  private void assertThatDiagnosticsWarnAboutMissingDomainObject(
      Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .as("Should warn about missing domain object with a suggestion to generate it")
        .anySatisfy(
            d -> {
              assertThat(d.level()).as("Level").isEqualTo(WARN);
              assertThat(d.message()).as("Message").isEqualTo("Missing domain object");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      s ->
                          assertThat(s.message())
                              .as("Suggestion")
                              .isEqualTo("Generate domain object"));
            });
  }

  @Test
  void shouldGenerateDomainObjectCode() {
    var entity = newEntityWithTaskAndDueDate();
    var aggregate =
        new Aggregate(new FullyQualifiedName("design", "Projects"))
            .setRoot(new Link("entity", entity.getName()));
    var inputs = givenInputsWithAllPrerequisites().put("entities", List.of(entity));
    var workspace = new InMemoryWorkspace();

    var actual =
        ((AggregateTool) getTool())
            .applySuggestion(
                aggregate, AggregateTool.GENERATE_DOMAIN_OBJECT, null, inputs, workspace.root());

    assertThat(actual.createdOrChanged())
        .as("Should generate a domain object record for the aggregate")
        .anySatisfy(
            resource ->
                assertThat(resource.readAsString())
                    .as("Generated code should declare the Projects domain record with its fields")
                    .contains("record Projects")
                    .contains("String task")
                    .contains("LocalDate dueDate"));
  }

  @Test
  void shouldWarnAboutMissingDomainService() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var inputs = givenInputsWithAggregateScenario(aggregate);
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingDomainService(diagnostics);
  }

  private void assertThatDiagnosticsWarnAboutMissingDomainService(
      Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasAtLeastOneElementOfType(Diagnostic.class)
        .anySatisfy(
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

  @Test
  void shouldWarnAboutMissingFrameworkDecisionWhenDomainServiceCodeExists() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var serviceCode =
        new CodeArtifact(new FullyQualifiedName("com.example.domain.services", "ProjectsService"))
            .setCode("// existing");
    var inputs =
        givenInputsWithAggregateScenario(aggregate).put("codeArtifacts", List.of(serviceCode));
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingFrameworkDecision(diagnostics);
  }

  private void assertThatDiagnosticsWarnAboutMissingFrameworkDecision(
      Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .extracting(Diagnostic::level, Diagnostic::message)
        .contains(tuple(WARN, "Missing decision on framework"));
    assertThat(diagnostics)
        .flatExtracting(Diagnostic::suggestions)
        .extracting(Suggestion::message)
        .contains("Decide on framework");
  }

  @Test
  void shouldWarnAboutMissingControllerWhenServiceCodeExists() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var inputs = givenInputsForSpringBootWithServiceCode(aggregate);
    var diagnostics = new ArrayList<Diagnostic>();

    ((AggregateTool) getTool()).validate(aggregate, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingController(diagnostics);
  }

  private ResolvedInputs givenInputsForSpringBootWithServiceCode(Aggregate aggregate) {
    var serviceCode =
        new CodeArtifact(
                new FullyQualifiedName(
                    "com.example.domain.services", aggregate.getName() + "Service"))
            .setCode("// existing");
    return givenInputsWithAggregateScenario(aggregate)
        .put("codeArtifacts", List.of(serviceCode))
        .put("decisions", newSpringBootDecisions());
  }

  private void assertThatDiagnosticsWarnAboutMissingController(Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasAtLeastOneElementOfType(Diagnostic.class)
        .anySatisfy(
            d -> {
              assertThat(d.level()).as("Level").isEqualTo(WARN);
              assertThat(d.message()).as("Message").isEqualTo("Missing endpoint");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      s -> assertThat(s.message()).as("Suggestion").isEqualTo("Generate endpoint"));
            });
  }

  @Test
  void shouldGenerateServiceCode() {
    var entity = newEntityWithTaskAndDueDate();
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var command =
        new Command(new FullyQualifiedName("design", "CreateProject"))
            .setDisplay("Create project")
            .setPayload(new Link("entity", entity.getName()));
    var event =
        new Event(new FullyQualifiedName("design", "ProjectCreated"))
            .setPayload(new Link("entity", entity.getName()));
    var inputs = givenInputsWithFullAggregateScenario(aggregate, command, event, entity);
    var workspace = new InMemoryWorkspace();

    var actual =
        ((AggregateTool) getTool())
            .applySuggestion(
                aggregate, AggregateTool.GENERATE_SERVICE, null, inputs, workspace.root());

    assertThat(actual.createdOrChanged())
        .hasSize(2)
        .anySatisfy(
            resource ->
                assertThat(resource.readAsString())
                    .contains(
                        "return new ProjectCreated(createProject.task(), createProject.dueDate())"));
  }

  private Entity newEntityWithTaskAndDueDate() {
    return new Entity(new FullyQualifiedName("design", "ProjectData"))
        .setFields(
            List.of(
                new Field(new FullyQualifiedName("design", "task")).setType(FieldType.TEXT),
                new Field(new FullyQualifiedName("design", "dueDate")).setType(FieldType.DATE)));
  }

  private ResolvedInputs givenInputsWithFullAggregateScenario(
      Aggregate aggregate, Command command, Event event, Entity entity) {
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
        .put("events", List.of(event))
        .put("entities", List.of(entity));
  }

  @Test
  void shouldGenerateControllerCode(@TempDir File tempDir) {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "Projects"));
    var command =
        new Command(new FullyQualifiedName("design", "CreateProject")).setDisplay("Create project");
    var event = new Event(new FullyQualifiedName("design", "ProjectCreated"));
    var inputs =
        givenInputsForSpringBootWithFullAggregateScenario(
            aggregate, command, event, newEntityWithTaskAndDueDate());
    var workspace = new DirectoryWorkspace(tempDir);

    var actual =
        ((AggregateTool) getTool())
            .applySuggestion(
                aggregate, AggregateTool.GENERATE_ENDPOINT, null, inputs, workspace.root());

    assertThat(actual.createdOrChanged())
        .anySatisfy(
            resource ->
                assertThat(resource.readAsString())
                    .contains("@RestController")
                    .contains("public class ProjectsController"));
  }

  private ResolvedInputs givenInputsForSpringBootWithFullAggregateScenario(
      Aggregate aggregate, Command command, Event event, Entity entity) {
    return givenInputsWithFullAggregateScenario(aggregate, command, event, entity)
        .put("decisions", newSpringBootDecisions());
  }
}
