package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.acceptanceTests;
import static org.setms.swe.inbound.tool.Inputs.aggregates;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.databaseSchemas;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.entities;
import static org.setms.swe.inbound.tool.Inputs.events;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.eventstorming.HasPayload;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class AggregateTool extends DtoCodeTool<Aggregate> {

  static final String GENERATE_DOMAIN_OBJECT = "domainObject.generate";
  static final String GENERATE_SERVICE = "service.generate";
  static final String GENERATE_ENDPOINT = "endpoint.generate";
  static final String GENERATE_SCHEMA = "schema.generate";

  public AggregateTool() {}

  @SuppressWarnings("unused") // Used by ServiceLoader
  AggregateTool(TechnologyResolver resolver) {
    super(resolver);
  }

  @Override
  public Set<Input<? extends Aggregate>> validationTargets() {
    return Set.of(aggregates());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>();
    result.add(acceptanceTests());
    result.add(commands());
    result.addAll(databaseSchemas());
    result.add(decisions());
    result.add(entities());
    result.add(events());
    result.add(initiatives());
    result.addAll(code());
    return result;
  }

  @Override
  public void validate(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (hasAggregateScenario(aggregate, inputs)
        && getResolver().codeGenerator(inputs, diagnostics).isPresent()) {
      validateDomainObject(aggregate, inputs, diagnostics);
      validateDomainService(aggregate, inputs, diagnostics);
      validateController(aggregate, inputs, diagnostics);
    }
    validateRootEntity(aggregate, inputs, diagnostics);
  }

  private void validateDomainObject(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (missesCode(aggregate, "", inputs)) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing domain object",
              aggregate.toLocation(),
              new Suggestion(GENERATE_DOMAIN_OBJECT, "Generate domain object")));
    }
  }

  private void validateDomainService(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (missesCode(aggregate, "Service", inputs)) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing domain service",
              aggregate.toLocation(),
              new Suggestion(GENERATE_SERVICE, "Generate domain service")));
    }
  }

  private boolean hasAggregateScenario(Aggregate aggregate, ResolvedInputs inputs) {
    return inputs.get(AcceptanceTest.class).stream()
        .filter(at -> referencesAggregate(at, aggregate))
        .anyMatch(at -> at.getScenarios().stream().anyMatch(AggregateScenario.class::isInstance));
  }

  private boolean referencesAggregate(AcceptanceTest acceptanceTest, Aggregate aggregate) {
    return Optional.ofNullable(acceptanceTest.getSut())
        .filter(sut -> sut.hasType("aggregate"))
        .map(Link::getId)
        .filter(aggregate.getName()::equals)
        .isPresent();
  }

  private boolean missesCode(Aggregate aggregate, String suffix, ResolvedInputs inputs) {
    var name = aggregate.getName() + suffix;
    return inputs.get(CodeArtifact.class).stream().noneMatch(ca -> ca.getName().equals(name));
  }

  private void validateController(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (getResolver().frameworkCodeGenerator(inputs, diagnostics).isPresent()
        && missesCode(aggregate, "Controller", inputs)) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing endpoint",
              aggregate.toLocation(),
              new Suggestion(GENERATE_ENDPOINT, "Generate endpoint")));
    }
  }

  private void validateRootEntity(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    Optional.ofNullable(aggregate.getRoot())
        .flatMap(link -> link.resolveFrom(inputs.get(Entity.class)))
        .ifPresentOrElse(
            root -> validateDatabaseSchema(aggregate, root, inputs, diagnostics),
            () ->
                diagnostics.add(
                    new Diagnostic(
                        WARN, "Missing on unknown root entity", aggregate.toLocation())));
  }

  private void validateDatabaseSchema(
      Aggregate aggregate, Entity root, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (inputs.get(DatabaseSchema.class).stream()
        .noneMatch(schema -> schema.getName().equalsIgnoreCase(root.getName()))) {
      getResolver()
          .database(inputs, diagnostics)
          .ifPresent(
              database ->
                  diagnostics.add(
                      new Diagnostic(
                          Level.WARN,
                          "Missing database schema",
                          aggregate.toLocation(),
                          new Suggestion(GENERATE_SCHEMA, "Generate database schema"))));
    }
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      Aggregate aggregate,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case GENERATE_SERVICE -> generateServiceFor(resource, aggregate, inputs);
      case GENERATE_ENDPOINT -> generateEndpointFor(resource, aggregate, inputs);
      case GENERATE_SCHEMA ->
          generateSchemaFor(
              resource,
              aggregate.getRoot().resolveFrom(inputs.get(Entity.class)).orElseThrow(),
              inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateServiceFor(
      Resource<?> aggregateResource, Aggregate aggregate, ResolvedInputs inputs) {
    return generateCode(
        aggregateResource,
        aggregate,
        inputs,
        getResolver().codeGenerator(inputs, new ArrayList<>()),
        (generator, pair) ->
            generator.generate(
                aggregate,
                pair.command(),
                resolvePayload(pair.command(), inputs),
                pair.event(),
                resolvePayload(pair.event(), inputs)));
  }

  private <G> AppliedSuggestion generateCode(
      Resource<?> aggregateResource,
      Aggregate aggregate,
      ResolvedInputs inputs,
      Optional<G> maybeGenerator,
      BiFunction<G, CommandAndEvent, List<CodeArtifact>> codeFunction) {
    return maybeGenerator
        .flatMap(
            generator ->
                findCommandAndEvent(aggregate, inputs)
                    .map(
                        pair ->
                            CodeWriter.writeCode(
                                codeFunction.apply(generator, pair), aggregateResource)))
        .orElseGet(AppliedSuggestion::none);
  }

  private Optional<CommandAndEvent> findCommandAndEvent(
      Aggregate aggregate, ResolvedInputs inputs) {
    return inputs.get(AcceptanceTest.class).stream()
        .filter(at -> referencesAggregate(at, aggregate))
        .flatMap(
            at ->
                at.getScenarios().stream()
                    .filter(AggregateScenario.class::isInstance)
                    .map(AggregateScenario.class::cast)
                    .flatMap(scenario -> toCommandAndEvent(scenario, at, inputs).stream()))
        .findFirst();
  }

  private Optional<CommandAndEvent> toCommandAndEvent(
      AggregateScenario scenario, AcceptanceTest acceptanceTest, ResolvedInputs inputs) {
    var commandOpt =
        resolveArtifact(scenario.getAccepts(), acceptanceTest, inputs.get(Command.class));
    var eventOpt = resolveArtifact(scenario.getEmitted(), acceptanceTest, inputs.get(Event.class));
    return commandOpt.flatMap(
        command -> eventOpt.map(event -> new CommandAndEvent(command, event)));
  }

  private <T extends Artifact> Optional<T> resolveArtifact(
      Link variableLink, AcceptanceTest acceptanceTest, List<T> candidates) {
    return acceptanceTest
        .findVariable(variableLink)
        .filter(ElementVariable.class::isInstance)
        .map(ElementVariable.class::cast)
        .map(ElementVariable::getType)
        .flatMap(typeLink -> typeLink.resolveFrom(candidates));
  }

  private Entity resolvePayload(HasPayload artifact, ResolvedInputs inputs) {
    return Optional.ofNullable(artifact.getPayload())
        .flatMap(link -> link.resolveFrom(inputs.get(Entity.class)))
        .orElse(null);
  }

  private AppliedSuggestion generateEndpointFor(
      Resource<?> aggregateResource, Aggregate aggregate, ResolvedInputs inputs) {
    return generateCode(
        aggregateResource,
        aggregate,
        inputs,
        getResolver().frameworkCodeGenerator(inputs, new ArrayList<>()),
        (generator, pair) ->
            generator.generateEndpointFor(
                aggregateResource.select("/"),
                aggregate,
                pair.command(),
                resolvePayload(pair.command(), inputs),
                pair.event()));
  }

  private AppliedSuggestion generateSchemaFor(
      Resource<?> resource, Entity entity, ResolvedInputs inputs) {
    return getResolver()
        .database(inputs, new ArrayList<>())
        .map(database -> writeSchema(database.schemaFor(entity), resource))
        .orElseGet(AppliedSuggestion::none);
  }

  private AppliedSuggestion writeSchema(DatabaseSchema schema, Resource<?> resource) {
    try {
      var target =
          resource
              .select("/")
              .select(Inputs.PATH_PHYSICAL_DESIGN)
              .select(schema.getName() + ".sql");
      target.writeAsString(schema.getCode());
      return created(target);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private record CommandAndEvent(Command command, Event event) {}
}
