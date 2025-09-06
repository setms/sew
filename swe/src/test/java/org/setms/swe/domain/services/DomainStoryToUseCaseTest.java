package org.setms.swe.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;
import org.setms.swe.domain.model.sdlc.domainstory.Sentence;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;

class DomainStoryToUseCaseTest {

  private final DomainStoryToUseCase converter = new DomainStoryToUseCase(Optional.empty());

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
                                new Link("person", "Dingo"),
                                new Link("activity", "Elephants"),
                                new Link("workObject", "Fox")))));

    var actual = converter.createUseCaseFrom(domainStory);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("ape.Bear"))
                .setTitle("Bear")
                .setDescription("Bear")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("ape.Bear"))
                            .setElaborates(domainStory.linkTo())
                            .setSteps(
                                List.of(
                                    new Link("user", "Dingo"),
                                    new Link("command", "ElephantFox"),
                                    new Link("aggregate", "Foxes"),
                                    new Link("event", "FoxElephanted"))))));
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
                                new Link("person", "Sam"),
                                new Link("activity", "Buys"),
                                new Link("workObject", "Ticket"))),
                    new Sentence(new FullyQualifiedName("giraffe.Sentence2"))
                        .setParts(
                            List.of(
                                new Link("person", "Dean"),
                                new Link("activity", "Sells"),
                                new Link("workObject", "Ticket")))));

    var actual = converter.createUseCaseFrom(domainStory);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("giraffe.Hyena"))
                .setTitle("Hyena")
                .setDescription("Hyena")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("giraffe.Hyena"))
                            .setElaborates(domainStory.linkTo())
                            .setSteps(
                                List.of(
                                    new Link("user", "Sam"),
                                    new Link("command", "BuyTicket"),
                                    new Link("aggregate", "Tickets"),
                                    new Link("event", "TicketBought"),
                                    new Link("readModel", "Tickets"),
                                    new Link("user", "Dean"),
                                    new Link("command", "SellTicket"),
                                    new Link("aggregate", "Tickets"),
                                    new Link("event", "TicketSold"))))));
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
                        .setSteps(List.of(new Link("hotspot", "Nightingale")))));
    var domainStory =
        new DomainStory(new FullyQualifiedName("iguana.Opossum"))
            .setDescription("parrot")
            .setSentences(
                List.of(
                    new Sentence(new FullyQualifiedName("iguana.Sentence1"))
                        .setParts(
                            List.of(
                                new Link("person", "Quetzal"),
                                new Link("activity", "Rhino"),
                                new Link("workObject", "Snake")))));

    var actual = converter.addScenarioFrom(domainStory, useCase);

    assertThat(actual)
        .isEqualTo(
            new UseCase(new FullyQualifiedName("iguana.Jaguar"))
                .setTitle("Koala")
                .setDescription("Leopard")
                .setScenarios(
                    List.of(
                        new Scenario(new FullyQualifiedName("iguana.Mule"))
                            .setSteps(List.of(new Link("hotspot", "Nightingale"))),
                        new Scenario(new FullyQualifiedName("iguana.Opossum"))
                            .setElaborates(domainStory.linkTo())
                            .setSteps(
                                List.of(
                                    new Link("user", "Quetzal"),
                                    new Link("command", "RhinoSnake"),
                                    new Link("aggregate", "Snakes"),
                                    new Link("event", "SnakeRhinoed"))))));
  }
}
