package org.setms.swe.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.validation.Validation.validate;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.artifact.LinkResolver;
import org.setms.km.domain.model.artifact.UnresolvedArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;

class CreateAcceptanceTestTest implements LinkResolver {

  private static final String PACKAGE_NAME = "ape";

  private final Entity commandPayload =
      new Entity(fqn("Bear")).setFields(List.of(new Field(fqn("Cheetah")).setType(FieldType.TEXT)));
  private final Command command =
      new Command(fqn("Dingo")).setDisplay("Elephant").setPayload(new Link(commandPayload));
  private final Aggregate aggregate =
      new Aggregate(fqn("Fox")).setDisplay("Giraffe").setRoot(new Link("entity", "Hyena"));
  private final Entity eventPayload =
      new Entity(fqn("Iguana"))
          .setFields(List.of(new Field(fqn("Jaguar")).setType(FieldType.BOOLEAN)));
  private final Event event = new Event(fqn("Koala")).setPayload(new Link(eventPayload));
  private final UseCase useCase =
      new UseCase(fqn("Leopard"))
          .setScenarios(
              List.of(
                  new Scenario(fqn("Mule"))
                      .setSteps(List.of(new Link(command), new Link(aggregate), new Link(event)))));
  private final CreateAcceptanceTest creator =
      new CreateAcceptanceTest(PACKAGE_NAME, this, List.of(useCase));

  private static FullyQualifiedName fqn(String name) {
    return new FullyQualifiedName(PACKAGE_NAME, name);
  }

  @Override
  public Artifact resolve(Link link, String defaultType) {
    var name = link.getId();
    return switch (Optional.ofNullable(link.getType()).orElse(defaultType)) {
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
        yield unknown(link);
      }
      default -> unknown(link);
    };
  }

  private Artifact unknown(Link link) {
    return new UnresolvedArtifact(fqn(link.getId()), link.getType());
  }

  @Test
  void shouldCreateScenarioForCommandAggregateEventSequence() {
    var actual = creator.apply(new Link(aggregate));

    assertThatNoException().isThrownBy(() -> validate(actual));
    assertThat(actual.getScenarios())
        .hasSize(1)
        .allSatisfy(
            scenario -> {
              assertThat(scenario.getInit()).isNull();
              assertThat(scenario.getCommand()).isEqualTo(linkToVariableFor(command));
              assertThat(scenario.getState()).isNull();
              assertThat(scenario.getEmitted()).isEqualTo(linkToVariableFor(event));
            });
    assertThat(actual.getVariables()).hasSize(4); // 2 objects, each with an entity with 1 field
  }

  private Link linkToVariableFor(Artifact object) {
    return new Link("variable", initLower(object.getName()));
  }
}
