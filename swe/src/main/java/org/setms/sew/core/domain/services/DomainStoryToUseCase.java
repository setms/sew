package org.setms.sew.core.domain.services;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.domainstory.Sentence;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

public class DomainStoryToUseCase {

  private final NaturalLanguage language = new English();

  public UseCase createUseCaseFrom(DomainStory domainStory) {
    return addScenarioFrom(
        domainStory,
        new UseCase(new FullyQualifiedName(domainStory.getPackage(), domainStory.getName()))
            .setTitle("TODO")
            .setDescription("TODO")
            .setScenarios(emptyList()));
  }

  public UseCase addScenarioFrom(DomainStory domainStory, UseCase source) {
    var scenarios = new ArrayList<>(source.getScenarios());
    scenarios.add(toScenario(domainStory));
    source.setScenarios(scenarios);
    return source;
  }

  private Scenario toScenario(DomainStory domainStory) {
    return new Scenario(new FullyQualifiedName(domainStory.getPackage(), domainStory.getName()))
        .setElaborates(domainStory.linkTo())
        .setSteps(toSteps(domainStory.getSentences()));
  }

  private List<Link> toSteps(Collection<Sentence> sentences) {
    var result = new ArrayList<Link>();
    sentences.forEach(
        sentence -> {
          for (var i = 0; i < sentence.getParts().size(); i++) {
            var part = sentence.getParts().get(i);
            result.addAll(
                switch (part.getType()) {
                  case "person", "people" -> convertPerson(part, result);
                  case "computerSystem" -> convertComputerSystem(part);
                  case "activity" -> convertActivity(part.getId(), sentence.getParts(), i);
                  case "workObject" -> convertWorkObject(part.getId(), result);
                  default -> emptyList();
                });
          }
        });
    return result;
  }

  private List<Link> convertPerson(Link part, List<Link> steps) {
    var result = new ArrayList<Link>();
    if (!steps.isEmpty() && steps.getLast().hasType("event")) {
      var aggregate = steps.get(steps.size() - 2);
      result.add(new Link("readModel", aggregate.getId()));
    }
    result.add(new Link("user", part.getId()));
    return result;
  }

  private List<Link> convertComputerSystem(Link part) {
    return List.of(new Link("externalSystem", part.getId()));
  }

  private List<Link> convertActivity(String activity, List<Link> parts, int index) {
    var result = new ArrayList<Link>();
    var previous = parts.get(index - 1);
    if (previous.hasType("person") || previous.hasType("people")) {
      var next = parts.get(index + 1);
      var command = "%s%s".formatted(language.base(activity), next.getId());
      result.add(new Link("command", command));
    }
    return result;
  }

  private List<Link> convertWorkObject(String workObject, List<Link> steps) {
    var result = new ArrayList<Link>();
    if (steps.getLast().hasType("command")) {
      result.add(new Link("aggregate", language.plural(workObject)));

      var verb = initLower(steps.getLast().getId());
      if (verb.endsWith(workObject)) {
        verb = verb.substring(0, verb.lastIndexOf(workObject));
      }
      verb = language.past(language.base(verb));
      result.add(new Link("event", initUpper(workObject) + initUpper(verb)));
    }
    return result;
  }
}
