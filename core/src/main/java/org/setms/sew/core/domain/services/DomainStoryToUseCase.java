package org.setms.sew.core.domain.services;

import jakarta.validation.constraints.NotEmpty;
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
        .setTitle("TODO: title")
        .setDescription("TODO: description")
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
            var step =
                switch (part.getType()) {
                  case "person", "people" -> new Pointer("user", part.getId());
                  case "computerSystem" -> new Pointer("externalSystem", part.getId());
                  case "activity" -> convertActivity(part.getId(), sentence.getParts(), i);
                  case "workObject" -> convertWorkObject(part.getId(), result);
                  default -> null;
                };
            if (step != null) {
              result.add(step);
            }
          }
        });
    complete(result);
    return result;
  }

  private Pointer convertActivity(String activity, List<Pointer> parts, int index) {
    var previous = parts.get(index - 1);
    if (previous.isType("person") || previous.isType("people")) {
      var next = parts.get(index + 1);
      var command = "%s%s".formatted(language.base(activity), next.getId());
      return new Pointer("command", command);
    }
    return null;
  }

  private Pointer convertWorkObject(@NotEmpty String workObject, List<Pointer> steps) {
    if (steps.getLast().isType("command")) {
      return new Pointer("aggregate", language.plural(workObject));
    }
    return null;
  }

  private void complete(List<Pointer> steps) {
    if (steps.getLast().isType("aggregate")) {
      var noun = language.singular(steps.getLast().getId());
      var verb = steps.get(steps.size() - 2).getId();
      if (verb.endsWith(noun)) {
        verb = verb.substring(0, verb.lastIndexOf(noun));
      }
      verb = language.past(language.base(verb));
      steps.add(new Pointer("event", noun + verb));
    }
  }
}
