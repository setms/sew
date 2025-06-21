package org.setms.sew.core.domain.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.PointerResolver;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.ddd.EventStorm;
import org.setms.sew.core.domain.model.sdlc.ddd.ResolvedSequence;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

@RequiredArgsConstructor
public class CreateAcceptanceTest implements Function<Pointer, AcceptanceTest> {

  private final EventStorm eventStorm;
  private final PointerResolver resolver;
  private final String packageName;

  public CreateAcceptanceTest(
      String packageName, PointerResolver resolver, Collection<UseCase> useCases) {
    this(new EventStorm(useCases), resolver, packageName);
  }

  @Override
  public AcceptanceTest apply(Pointer element) {
    var result =
        new AcceptanceTest(new FullyQualifiedName("%s.%s".formatted(packageName, element.getId())))
            .setSut(element)
            .setVariables(new ArrayList<>())
            .setScenarios(new ArrayList<>());
    switch (element.getType()) {
      case "aggregate" ->
          addScenariosTo(result, this::addAggregateScenarioTo, "command", "aggregate", "event");
      case "policy" -> {
        addScenariosTo(result, this::addPolicyScenarioTo, "event", "policy", "command");
        addScenariosTo(result, this::addPolicyScenarioTo, "clockEvent", "policy", "command");
      }
      case "readModel" ->
          addScenariosTo(result, this::addReadModelScenarioTo, "event", "readModel");
    }
    return result;
  }

  private void addScenariosTo(
      AcceptanceTest test,
      BiConsumer<ResolvedSequence, AcceptanceTest> addScenario,
      String... types) {
    findSequences(types).forEach(sequence -> addScenario.accept(sequence, test));
  }

  public Stream<ResolvedSequence> findSequences(String... types) {
    return eventStorm.findSequences(types).map(s -> s.resolve(resolver));
  }

  private void addAggregateScenarioTo(
      ResolvedSequence commandAggregateEvent, AcceptanceTest test) {}

  private void addPolicyScenarioTo(ResolvedSequence eventPolicyCommand, AcceptanceTest test) {}

  private void addReadModelScenarioTo(ResolvedSequence eventReadModel, AcceptanceTest test) {}
}
