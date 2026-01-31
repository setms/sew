package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

  private static final String TECHNOLOGY_DECISIONS_PACKAGE = "technology";
  private static final String PROGRAMMING_LANGUAGE_DECISION = "ProgrammingLanguage";
  static final String PICK_PROGRAMMING_LANGUAGE = "programming-language.decide";

  @Override
  public Optional<UnitTestGenerator> unitTestGenerator(
      Collection<Decision> decisions, Location location, Collection<Diagnostic> diagnostics) {
    var programmingLanguage = groupByTopic(decisions).get(ProgrammingLanguage.TOPIC);
    return Optional.ofNullable(unitTestGeneratorFor(programmingLanguage, location, diagnostics));
  }

  private Map<String, String> groupByTopic(Collection<Decision> decisions) {
    var result = new HashMap<String, String>();
    decisions.forEach(decision -> result.put(decision.getTopic(), decision.getChoice()));
    return result;
  }

  private UnitTestGenerator unitTestGeneratorFor(
      String programmingLanguage, Location location, Collection<Diagnostic> diagnostics) {
    return switch (programmingLanguage) {
      case "Java" -> new JavaUnitGenerator();
      case null ->
          nothing(
              new Diagnostic(
                  WARN,
                  "Missing decision on programming language",
                  location,
                  new Suggestion(PICK_PROGRAMMING_LANGUAGE, "Decide on programming language")),
              diagnostics);
      default ->
          nothing(
              new Diagnostic(ERROR, "Decided on unknown programming language", location),
              diagnostics);
    };
  }

  private <T> T nothing(Diagnostic diagnostic, Collection<Diagnostic> diagnostics) {
    diagnostics.add(diagnostic);
    return null;
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
