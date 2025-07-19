package org.setms.sew.core.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Pointer;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.domainstory.Sentence;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

class DomainStoryToUseCaseTest {

  private final DomainStoryToUseCase converter = new DomainStoryToUseCase();

  @Test
  void shouldConvertSingleSentence() {
    var domainStory =
        new DomainStory(new FullyQualifiedName("ape.Bear"))
            .setDescription("cheetah")
            .setSentences(
                List.of(
                    new Sentence(new FullyQualifiedName("ape.Sentence1"))
                        .setParts(
                            List.of(
                                new Pointer("person", "Dingo"),
                                new Pointer("activity", "Elephants"),
                                new Pointer("workObject", "Fox")))));

    var actual = converter.createUseCaseFrom(domainStory);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("ape.Bear"))
                .setTitle("TODO")
                .setDescription("TODO")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("ape.Bear"))
                            .setElaborates(domainStory.pointerTo())
                            .setSteps(
                                List.of(
                                    new Pointer("user", "Dingo"),
                                    new Pointer("command", "ElephantFox"),
                                    new Pointer("aggregate", "Foxes"),
                                    new Pointer("event", "FoxElephanted"))))));
  }

  @Test
  void shouldConvertMultipleSentences() {
    var domainStory =
        new DomainStory(new FullyQualifiedName("giraffe.Hyena"))
            .setSentences(
                List.of(
                    new Sentence(new FullyQualifiedName("giraffe.Sentence1"))
                        .setParts(
                            List.of(
                                new Pointer("person", "Sam"),
                                new Pointer("activity", "Buys"),
                                new Pointer("workObject", "Ticket"))),
                    new Sentence(new FullyQualifiedName("giraffe.Sentence2"))
                        .setParts(
                            List.of(
                                new Pointer("person", "Dean"),
                                new Pointer("activity", "Sells"),
                                new Pointer("workObject", "Ticket")))));

    var actual = converter.createUseCaseFrom(domainStory);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("giraffe.Hyena"))
                .setTitle("TODO")
                .setDescription("TODO")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("giraffe.Hyena"))
                            .setElaborates(domainStory.pointerTo())
                            .setSteps(
                                List.of(
                                    new Pointer("user", "Sam"),
                                    new Pointer("command", "BuyTicket"),
                                    new Pointer("aggregate", "Tickets"),
                                    new Pointer("event", "TicketBought"),
                                    new Pointer("readModel", "Tickets"),
                                    new Pointer("user", "Dean"),
                                    new Pointer("command", "SellTicket"),
                                    new Pointer("aggregate", "Tickets"),
                                    new Pointer("event", "TicketSold"))))));
  }

  @Test
  void shouldAddScenarioToUseCase() {
    var useCase =
        new UseCase(new FullyQualifiedName("iguana.Jaguar"))
            .setTitle("Koala")
            .setDescription("Leopard")
            .setScenarios(
                List.of(
                    new Scenario(new FullyQualifiedName("iguana.Mule"))
                        .setSteps(List.of(new Pointer("hotspot", "Nightingale")))));
    var domainStory =
        new DomainStory(new FullyQualifiedName("iguana.Opossum"))
            .setDescription("parrot")
            .setSentences(
                List.of(
                    new Sentence(new FullyQualifiedName("iguana.Sentence1"))
                        .setParts(
                            List.of(
                                new Pointer("person", "Quetzal"),
                                new Pointer("activity", "Rhino"),
                                new Pointer("workObject", "Snake")))));

    var actual = converter.addScenarioFrom(domainStory, useCase);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("iguana.Jaguar"))
                .setTitle("Koala")
                .setDescription("Leopard")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("iguana.Mule"))
                            .setSteps(List.of(new Pointer("hotspot", "Nightingale"))),
                        new Scenario(new FullyQualifiedName("iguana.Opossum"))
                            .setElaborates(domainStory.pointerTo())
                            .setSteps(
                                List.of(
                                    new Pointer("user", "Quetzal"),
                                    new Pointer("command", "RhinoSnake"),
                                    new Pointer("aggregate", "Snakes"),
                                    new Pointer("event", "SnakeRhinoed"))))));
  }
}
