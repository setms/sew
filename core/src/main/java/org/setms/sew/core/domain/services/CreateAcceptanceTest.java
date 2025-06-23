package org.setms.sew.core.domain.services;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.PointerResolver;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.acceptance.ElementVariable;
import org.setms.sew.core.domain.model.sdlc.acceptance.FieldAssignment;
import org.setms.sew.core.domain.model.sdlc.acceptance.FieldVariable;
import org.setms.sew.core.domain.model.sdlc.acceptance.Scenario;
import org.setms.sew.core.domain.model.sdlc.acceptance.Variable;
import org.setms.sew.core.domain.model.sdlc.ddd.EventStorm;
import org.setms.sew.core.domain.model.sdlc.ddd.ResolvedSequence;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.design.Field;
import org.setms.sew.core.domain.model.sdlc.design.FieldConstraint;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Event;
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

  private void addAggregateScenarioTo(ResolvedSequence sequence, AcceptanceTest test) {
    var commandAggregateEvent = sequence.items();
    if (!test.getSut().pointsTo(commandAggregateEvent.get(1))) {
      return;
    }
    var scenario =
        new Scenario(scenarioName(sequence, test.getPackage()))
            .setCommand(
                pointerToVariable(
                    ensureVariableFor(test.getVariables(), commandAggregateEvent.getFirst())))
            .setEmitted(
                pointerToVariable(
                    ensureVariableFor(test.getVariables(), commandAggregateEvent.getLast())));
    test.getScenarios().add(scenario);
  }

  private Pointer pointerToVariable(Variable<?, ?> variable) {
    return new Pointer("variable", variable.getName());
  }

  private FullyQualifiedName scenarioName(ResolvedSequence sequence, String packageName) {
    var names = sequence.items().stream().map(NamedObject::getName).toList();
    var name = "%sAccepts%sAndEmits%s".formatted(names.get(1), names.get(0), names.get(2));
    return new FullyQualifiedName(packageName, name);
  }

  private Variable<?, ?> ensureVariableFor(
      @NotEmpty List<Variable<?, ?>> variables, NamedObject object) {
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
    variable.setType(new Pointer(command));
    if (resolver.resolve(command.getPayload(), "entity") instanceof Entity entity
        && !entity.getFields().isEmpty()) {
      setDefinition(variable, command, entity, variables);
    }
    return variable;
  }

  private void setDefinition(
      ElementVariable variable,
      NamedObject entityContainer,
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
                        .setValue(pointerToVariable(ensureVariableFor(variables, field)))));
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
          case NONEMPTY -> "nonempty";
          case UNIQUE -> null;
        });
  }

  private ElementVariable finalizeEventVariable(
      ElementVariable variable, Event event, @NotEmpty List<Variable<?, ?>> variables) {
    variable.setType(new Pointer(event));
    if (resolver.resolve(event.getPayload(), "entity") instanceof Entity entity
        && !entity.getFields().isEmpty()) {
      setDefinition(variable, event, entity, variables);
    }
    return variable;
  }

  private void addPolicyScenarioTo(ResolvedSequence eventPolicyCommand, AcceptanceTest test) {}

  private void addReadModelScenarioTo(ResolvedSequence eventReadModel, AcceptanceTest test) {}
}
