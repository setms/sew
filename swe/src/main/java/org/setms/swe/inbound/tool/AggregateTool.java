package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.acceptanceTests;
import static org.setms.swe.inbound.tool.Inputs.aggregates;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.Collection;
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
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
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
    return Stream.of(Stream.of(acceptanceTests(), decisions(), initiatives()), code().stream())
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
    return AppliedSuggestion.none();
  }
}
