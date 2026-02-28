package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.acceptanceTests;
import static org.setms.swe.inbound.tool.Inputs.aggregates;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.commands;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.events;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class AggregateTool extends ArtifactTool<Aggregate> {

  static final String GENERATE_SERVICE = "service.generate";

  private final TechnologyResolver resolver;

  public AggregateTool() {
    this(new TechnologyResolverImpl());
  }

  AggregateTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public Set<Input<? extends Aggregate>> validationTargets() {
    return Set.of(aggregates());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Stream.of(
            Stream.of(acceptanceTests(), commands(), events(), decisions(), initiatives()),
            code().stream())
        .flatMap(s -> s)
        .collect(toSet());
  }

  @Override
  public void validate(
      Aggregate aggregate, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (!hasAggregateScenario(aggregate, inputs)) {
      return;
    }
    if (resolver.codeGenerator(inputs, diagnostics).isEmpty()) {
      return;
    }
    if (!hasServiceCode(aggregate, inputs)) {
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

  private boolean hasServiceCode(Aggregate aggregate, ResolvedInputs inputs) {
    var serviceName = aggregate.getName() + "Service";
    return inputs.get(CodeArtifact.class).stream().anyMatch(ca -> ca.getName().equals(serviceName));
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
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateServiceFor(
      Resource<?> aggregateResource, Aggregate aggregate, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return resolver
        .codeGenerator(inputs, diagnostics)
        .flatMap(
            generator ->
                findCommandAndEvent(aggregate, inputs)
                    .map(
                        pair ->
                            CodeWriter.writeCode(
                                generator.generate(aggregate, pair.command(), pair.event()),
                                aggregateResource)))
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

  private record CommandAndEvent(Command command, Event event) {}
}
