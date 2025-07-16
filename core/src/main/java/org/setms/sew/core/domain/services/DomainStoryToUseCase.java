package org.setms.sew.core.domain.services;

import static java.util.Collections.emptyList;
import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.setms.sew.core.domain.model.nlp.English;
import org.setms.sew.core.domain.model.nlp.NaturalLanguage;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.domainstory.Sentence;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

public class DomainStoryToUseCase {

  private final NaturalLanguage language = new English();

  public UseCase createUseCaseFrom(DomainStory domainStory) {
    return new UseCase(new FullyQualifiedName(domainStory.getPackage(), domainStory.getName()))
        .setTitle("TODO")
        .setDescription("TODO")
        .setScenarios(List.of(toScenario(domainStory)));
  }

  private Scenario toScenario(DomainStory domainStory) {
    return new Scenario(new FullyQualifiedName(domainStory.getPackage(), domainStory.getName()))
        .setElaborates(domainStory.pointerTo())
        .setSteps(toSteps(domainStory.getSentences()));
  }

  private List<Pointer> toSteps(Collection<Sentence> sentences) {
    var result = new ArrayList<Pointer>();
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

  private List<Pointer> convertPerson(Pointer part, List<Pointer> steps) {
    var result = new ArrayList<Pointer>();
    if (!steps.isEmpty() && steps.getLast().isType("event")) {
      var aggregate = steps.get(steps.size() - 2);
      result.add(new Pointer("readModel", aggregate.getId()));
    }
    result.add(new Pointer("user", part.getId()));
    return result;
  }

  private List<Pointer> convertComputerSystem(Pointer part) {
    return List.of(new Pointer("externalSystem", part.getId()));
  }

  private List<Pointer> convertActivity(String activity, List<Pointer> parts, int index) {
    var result = new ArrayList<Pointer>();
    var previous = parts.get(index - 1);
    if (previous.isType("person") || previous.isType("people")) {
      var next = parts.get(index + 1);
      var command = "%s%s".formatted(language.base(activity), next.getId());
      result.add(new Pointer("command", command));
    }
    return result;
  }

  private List<Pointer> convertWorkObject(String workObject, List<Pointer> steps) {
    var result = new ArrayList<Pointer>();
    if (steps.getLast().isType("command")) {
      result.add(new Pointer("aggregate", language.plural(workObject)));

      var verb = initLower(steps.getLast().getId());
      if (verb.endsWith(workObject)) {
        verb = verb.substring(0, verb.lastIndexOf(workObject));
      }
      verb = language.past(language.base(verb));
      result.add(new Pointer("event", initUpper(workObject) + initUpper(verb)));
    }
    return result;
  }
}
