package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.code.java.JavaUnitGenerator;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;

public class TechnologyResolverImpl implements TechnologyResolver {

  static final String PICK_PROGRAMMING_LANGUAGE = "programming-language.decide";
  public static final String TECHNOLOGY_DECISIONS_PACKAGE = "technology";
  public static final String PROGRAMMING_LANGUAGE_DECISION = "ProgrammingLanguage";

  @Override
  public UnitTestGenerator unitTestGenerator(
      Collection<Decision> decisions, Location location, Collection<Diagnostic> diagnostics) {
    var decisionsByTopic = groupByTopic(decisions);
    var programmingLanguage = decisionsByTopic.get(ProgrammingLanguage.TOPIC);
    UnitTestGenerator result = null;
    switch (programmingLanguage) {
      case "Java" -> new JavaUnitGenerator();
      case null ->
          diagnostics.add(
              new Diagnostic(
                  WARN,
                  "Missing decision on programming language",
                  location,
                  new Suggestion(PICK_PROGRAMMING_LANGUAGE, "Decide on programming language")));
      default ->
          diagnostics.add(
              new Diagnostic(ERROR, "Decided on unknown programming language", location));
    }
    return result;
  }

  private Map<String, String> groupByTopic(Collection<Decision> decisions) {
    var result = new HashMap<String, String>();
    decisions.forEach(decision -> result.put(decision.getTopic(), decision.getChoice()));
    return result;
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (suggestionCode.equals(PICK_PROGRAMMING_LANGUAGE)) {
      return pickProgrammingLanguage(resource);
    }
    return AppliedSuggestion.none();
  }

  private AppliedSuggestion pickProgrammingLanguage(Resource<?> resource) {
    try {
      var decision =
          new Decision(
                  new FullyQualifiedName(
                      TECHNOLOGY_DECISIONS_PACKAGE, PROGRAMMING_LANGUAGE_DECISION))
              .setTopic(ProgrammingLanguage.TOPIC);
      var decisionInput = Inputs.decisions();
      var decisionResource =
          resource
              .select("/")
              .select(decisionInput.path())
              .select("%s.%s".formatted(PROGRAMMING_LANGUAGE_DECISION, decisionInput.extension()));
      try (var output = decisionResource.writeTo()) {
        builderFor(decision).build(decision, output);
      }
      return created(decisionResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
