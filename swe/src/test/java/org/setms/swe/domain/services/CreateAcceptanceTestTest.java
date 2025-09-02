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
import org.setms.swe.domain.model.sdlc.acceptance.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptance.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptance.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.eventstorming.Policy;
import org.setms.swe.domain.model.sdlc.eventstorming.ReadModel;
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
  private final Policy policy = new Policy(fqn("Leopard"));
  private final ReadModel readModel = new ReadModel(fqn("Mule"));
  private final UseCase commandAggregateEventUseCase =
      new UseCase(fqn("Nightingale"))
          .setScenarios(
              List.of(
                  new Scenario(fqn("Opossum"))
                      .setSteps(List.of(new Link(command), new Link(aggregate), new Link(event)))));
  private final UseCase eventPolicyCommandUseCase =
      new UseCase(fqn("Parrot"))
          .setScenarios(
              List.of(
                  new Scenario(fqn("Quetzal"))
                      .setSteps(List.of(new Link(event), new Link(policy), new Link(command)))));
  private final UseCase eventReadModelUseCase =
      new UseCase(fqn("Rhino"))
          .setScenarios(
              List.of(
                  new Scenario(fqn("Snake"))
                      .setSteps(List.of(new Link(event), new Link(readModel)))));
  private CreateAcceptanceTest creator;

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
      case "policy" -> policy;
      case "readModel" -> readModel;
      default -> unknown(link);
    };
  }

  private Artifact unknown(Link link) {
    return new UnresolvedArtifact(fqn(link.getId()), link.getType());
  }

  @Test
  void shouldCreateScenarioForCommandAggregateEventSequence() {
    creator = new CreateAcceptanceTest(PACKAGE_NAME, this, List.of(commandAggregateEventUseCase));

    var actual = creator.apply(new Link(aggregate));

    assertThatNoException().isThrownBy(() -> validate(actual));
    assertThat(actual.getScenarios())
        .hasSize(1)
        .map(AggregateScenario.class::cast)
        .allSatisfy(
            scenario -> {
              assertThat(scenario.getInit()).isNull();
              assertThat(scenario.getAccepts()).isEqualTo(linkToVariableFor(command));
              assertThat(scenario.getState()).isNull();
              assertThat(scenario.getEmitted()).isEqualTo(linkToVariableFor(event));
            });
    assertThat(actual.getVariables()).hasSize(4); // 2 objects, each with an entity with 1 field
  }

  private Link linkToVariableFor(Artifact object) {
    return new Link("variable", initLower(object.getName()));
  }

  @Test
  void shouldCreateScenarioForEventPolicyCommandSequence() {
    creator = new CreateAcceptanceTest(PACKAGE_NAME, this, List.of(eventPolicyCommandUseCase));

    var actual = creator.apply(new Link(policy));

    assertThatNoException().isThrownBy(() -> validate(actual));
    assertThat(actual.getScenarios())
        .hasSize(1)
        .map(PolicyScenario.class::cast)
        .allSatisfy(
            scenario -> {
              assertThat(scenario.getInit()).isNull();
              assertThat(scenario.getHandles()).isEqualTo(linkToVariableFor(event));
              assertThat(scenario.getIssued()).isEqualTo(linkToVariableFor(command));
            });
    assertThat(actual.getVariables()).hasSize(4); // 2 objects, each with an entity with 1 field
  }

  @Test
  void shouldCreateScenarioForEventReadModelSequence() {
    creator = new CreateAcceptanceTest(PACKAGE_NAME, this, List.of(eventReadModelUseCase));

    var actual = creator.apply(new Link(readModel));

    assertThatNoException().isThrownBy(() -> validate(actual));
    assertThat(actual.getScenarios())
        .hasSize(1)
        .map(ReadModelScenario.class::cast)
        .allSatisfy(
            scenario -> {
              assertThat(scenario.getInit()).isNull();
              assertThat(scenario.getHandles()).isEqualTo(linkToVariableFor(event));
              assertThat(scenario.getState()).isNull();
            });
    assertThat(actual.getVariables()).hasSize(2); // 1 objects, each with an entity with 1 field
  }
}
