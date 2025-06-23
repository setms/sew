package org.setms.sew.core.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Validation.validate;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.PointerResolver;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.design.Field;
import org.setms.sew.core.domain.model.sdlc.design.FieldType;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Event;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.model.tool.UnresolvedObject;

class CreateAcceptanceTestTest implements PointerResolver {

  private static final String PACKAGE_NAME = "ape";

  private final Entity commandPayload =
      new Entity(fqn("Bear")).setFields(List.of(new Field(fqn("Cheetah")).setType(FieldType.TEXT)));
  private final Command command =
      new Command(fqn("Dingo")).setDisplay("Elephant").setPayload(new Pointer(commandPayload));
  private final Aggregate aggregate =
      new Aggregate(fqn("Fox")).setDisplay("Giraffe").setRoot(new Pointer("entity", "Hyena"));
  private final Entity eventPayload =
      new Entity(fqn("Iguana"))
          .setFields(List.of(new Field(fqn("Jaguar")).setType(FieldType.BOOLEAN)));
  private final Event event = new Event(fqn("Koala")).setPayload(new Pointer(eventPayload));
  private final UseCase useCase =
      new UseCase(fqn("Leopard"))
          .setScenarios(
              List.of(
                  new Scenario(fqn("Mule"))
                      .setTitle("Nightingale")
                      .setSteps(
                          List.of(
                              new Pointer(command), new Pointer(aggregate), new Pointer(event)))));
  private final CreateAcceptanceTest creator =
      new CreateAcceptanceTest(PACKAGE_NAME, this, List.of(useCase));

  private static FullyQualifiedName fqn(String name) {
    return new FullyQualifiedName(PACKAGE_NAME, name);
  }

  @Override
  public NamedObject resolve(Pointer pointer, String defaultType) {
    var name = pointer.getId();
    return switch ( Optional.ofNullable(pointer.getType()).orElse(defaultType)) {
      case "aggregate" -> aggregate;
      case "command" -> command;
      case "event" -> event;
      case "entity" -> {
        if (commandPayload.getName().equals(name)) {
          yield commandPayload;
        }
        if (eventPayload.getName().equals(name)) {
          yield eventPayload;
        }
        yield unknown(pointer);
      }
      default -> unknown(pointer);
    };
  }

  private NamedObject unknown(Pointer pointer) {
    return new UnresolvedObject(fqn(pointer.getId()), pointer.getType());
  }

  @Test
  void shouldCreateScenarioForCommandAggregateEventSequence() {
    var actual = creator.apply(new Pointer(aggregate));

    assertThatNoException().isThrownBy(() -> validate(actual));
    assertThat(actual.getScenarios())
        .hasSize(1)
        .allSatisfy(
            scenario -> {
              assertThat(scenario.getInit()).isNull();
              assertThat(scenario.getCommand()).isEqualTo(pointerToVariableFor(command));
              assertThat(scenario.getState()).isNull();
              assertThat(scenario.getEmitted()).isEqualTo(pointerToVariableFor(event));
            });
    assertThat(actual.getVariables()).hasSize(4); // 2 objects, each with an entity with 1 field
  }

  private Pointer pointerToVariableFor(NamedObject object) {
    return new Pointer("variable", initLower(object.getName()));
  }
}
