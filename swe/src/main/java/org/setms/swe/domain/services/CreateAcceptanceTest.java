package org.setms.swe.domain.services;

import static org.setms.km.domain.model.artifact.Link.testType;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.artifact.LinkResolver;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldAssignment;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Variable;
import org.setms.swe.domain.model.sdlc.ddd.EventStorm;
import org.setms.swe.domain.model.sdlc.ddd.ResolvedSequence;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldConstraint;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;

@RequiredArgsConstructor
public class CreateAcceptanceTest implements Function<Link, AcceptanceTest> {

  private final EventStorm eventStorm;
  private final LinkResolver resolver;
  private final String packageName;

  public CreateAcceptanceTest(
      String packageName, LinkResolver resolver, Collection<UseCase> useCases) {
    this(new EventStorm(useCases), resolver, packageName);
  }

  @Override
  public AcceptanceTest apply(Link element) {
    var result =
        new AcceptanceTest(
                new FullyQualifiedName(
                    "%s.%s%s"
                        .formatted(packageName, element.getId(), initUpper(element.getType()))))
            .setSut(element)
            .setVariables(new ArrayList<>())
            .setScenarios(new ArrayList<>());
    switch (element.getType()) {
      case "aggregate" ->
          addScenariosTo(
              result,
              this::addAggregateScenarioTo,
              List.of(testType("command"), element.testEqual(), testType("event")));
      case "policy" -> {
        addScenariosTo(
            result,
            this::addPolicyScenarioTo,
            List.of(testType("event"), element.testEqual(), testType("command")));
        addScenariosTo(
            result,
            this::addPolicyScenarioTo,
            List.of(testType("clockEvent"), element.testEqual(), testType("command")));
      }
      case "readModel" ->
          addScenariosTo(
              result,
              this::addReadModelScenarioTo,
              List.of(testType("event"), element.testEqual()));
    }
    return result;
  }

  private void addScenariosTo(
      AcceptanceTest test,
      BiConsumer<ResolvedSequence, AcceptanceTest> addScenario,
      List<Predicate<Link>> tests) {
    findSequences(tests).forEach(sequence -> addScenario.accept(sequence, test));
  }

  public Stream<ResolvedSequence> findSequences(List<Predicate<Link>> tests) {
    return eventStorm.findSequences(tests).map(s -> s.resolve(resolver));
  }

  private void addAggregateScenarioTo(ResolvedSequence sequence, AcceptanceTest test) {
    var commandAggregateEvent = sequence.items();
    var scenario =
        new AggregateScenario(scenarioName(sequence, test.getPackage()))
            .setAccepts(
                linkToVariable(
                    ensureVariableFor(test.getVariables(), commandAggregateEvent.getFirst())))
            .setEmitted(
                linkToVariable(
                    ensureVariableFor(test.getVariables(), commandAggregateEvent.getLast())));
    test.getScenarios().add(scenario);
  }

  private Link linkToVariable(Variable<?, ?> variable) {
    return new Link("variable", variable.getName());
  }

  private FullyQualifiedName scenarioName(ResolvedSequence sequence, String packageName) {
    var names = sequence.items().stream().map(Artifact::getName).toList();
    var name = "Accept %s and emit %s".formatted(names.getFirst(), names.getLast());
    return new FullyQualifiedName(packageName, name);
  }

  private Variable<?, ?> ensureVariableFor(
      @NotEmpty List<Variable<?, ?>> variables, Artifact object) {
    var name = initLower(object.getName());
    var result = variables.stream().filter(v -> v.getName().equals(name)).findFirst().orElse(null);
    if (result == null) {
      var fqn = new FullyQualifiedName(object.getPackage(), name);
      result =
          switch (object) {
            case Command command ->
                finalizeCommandVariable(new ElementVariable(fqn), command, variables);
            case Field field -> finalizeFieldVariable(new FieldVariable(fqn), field);
            case Event event -> finalizeEventVariable(new ElementVariable(fqn), event, variables);
            default ->
                throw new UnsupportedOperationException(
                    "Can't define variable for %s %s"
                        .formatted(object.getClass().getSimpleName(), object.getName()));
          };
      variables.add(result);
    }
    return result;
  }

  private Variable<?, ?> finalizeCommandVariable(
      ElementVariable variable, Command command, List<Variable<?, ?>> variables) {
    variable.setType(new Link(command));
    if (resolver.resolve(command.getPayload(), "entity") instanceof Entity entity
        && !entity.getFields().isEmpty()) {
      setDefinition(variable, command, entity, variables);
    }
    return variable;
  }

  private void setDefinition(
      ElementVariable variable,
      Artifact entityContainer,
      Entity entity,
      List<Variable<?, ?>> variables) {
    var definitions = new ArrayList<FieldAssignment>();
    entity
        .getFields()
        .forEach(
            field ->
                definitions.add(
                    new FieldAssignment(
                            new FullyQualifiedName(entityContainer.getPackage(), field.getName()))
                        .setFieldName(field.getName())
                        .setValue(linkToVariable(ensureVariableFor(variables, field)))));
    variable.setDefinitions(definitions);
  }

  private FieldVariable finalizeFieldVariable(FieldVariable variable, Field field) {
    variable.setType(field.getType());
    if (field.getConstraints() != null && !field.getConstraints().isEmpty()) {
      variable.setDefinitions(
          field.getConstraints().stream()
              .map(this::toDefinition)
              .flatMap(Optional::stream)
              .toList());
    }
    return variable;
  }

  private Optional<String> toDefinition(FieldConstraint fieldConstraint) {
    return Optional.ofNullable(
        switch (fieldConstraint) {
          case NONEMPTY, EMAIL -> fieldConstraint.name().toLowerCase();
          case UNIQUE -> null;
        });
  }

  private ElementVariable finalizeEventVariable(
      ElementVariable variable, Event event, @NotEmpty List<Variable<?, ?>> variables) {
    variable.setType(new Link(event));
    if (resolver.resolve(event.getPayload(), "entity") instanceof Entity entity
        && !entity.getFields().isEmpty()) {
      setDefinition(variable, event, entity, variables);
    }
    return variable;
  }

  private void addPolicyScenarioTo(ResolvedSequence sequence, AcceptanceTest test) {
    var eventPolicyCommand = sequence.items();
    var scenario =
        new PolicyScenario(scenarioName(sequence, test.getPackage()))
            .setHandles(
                linkToVariable(
                    ensureVariableFor(test.getVariables(), eventPolicyCommand.getFirst())))
            .setIssued(
                linkToVariable(
                    ensureVariableFor(test.getVariables(), eventPolicyCommand.getLast())));
    test.getScenarios().add(scenario);
  }

  private void addReadModelScenarioTo(ResolvedSequence sequence, AcceptanceTest test) {
    var eventReadModel = sequence.items();
    var scenario =
        new ReadModelScenario(scenarioName(sequence, test.getPackage()))
            .setHandles(
                linkToVariable(ensureVariableFor(test.getVariables(), eventReadModel.getFirst())));
    test.getScenarios().add(scenario);
  }
}
