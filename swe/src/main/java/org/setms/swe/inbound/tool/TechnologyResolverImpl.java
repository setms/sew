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
import org.setms.swe.domain.model.sdlc.architecture.BuildTool;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.java.JavaUnitTestGenerator;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;

public class TechnologyResolverImpl implements TechnologyResolver {

  private static final String TECHNOLOGY_DECISIONS_PACKAGE = "technology";
  private static final String PROGRAMMING_LANGUAGE_DECISION = "ProgrammingLanguage";
  private static final String TOP_LEVEL_PACKAGE_DECISION = "TopLevelPackage";
  private static final String BUILD_TOOL_DECISION = "BuildTool";
  static final String PICK_PROGRAMMING_LANGUAGE = "programming-language.decide";
  static final String PICK_TOP_LEVEL_PACKAGE = "top-level-package.decide";
  static final String PICK_BUILD_TOOL = "build-tool.decide";

  @Override
  public Optional<UnitTestGenerator> unitTestGenerator(
      Collection<Decision> decisions, Location location, Collection<Diagnostic> diagnostics) {
    var topics = groupByTopic(decisions);
    var programmingLanguage = topics.get(ProgrammingLanguage.TOPIC);
    var topLevelPackage = topics.get(TopLevelPackage.TOPIC);
    return Optional.ofNullable(
        unitTestGeneratorFor(programmingLanguage, topLevelPackage, location, diagnostics));
  }

  private Map<String, String> groupByTopic(Collection<Decision> decisions) {
    var result = new HashMap<String, String>();
    decisions.forEach(decision -> result.put(decision.getTopic(), decision.getChoice()));
    return result;
  }

  private UnitTestGenerator unitTestGeneratorFor(
      String programmingLanguage,
      String topLevelPackage,
      Location location,
      Collection<Diagnostic> diagnostics) {
    return switch (programmingLanguage) {
      case "Java" -> javaUnitGenerator(topLevelPackage, location, diagnostics);
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
              new Diagnostic(ERROR, "Decided on unsupported programming language", location),
              diagnostics);
    };
  }

  private UnitTestGenerator javaUnitGenerator(
      String topLevelPackage, Location location, Collection<Diagnostic> diagnostics) {
    if (topLevelPackage == null) {
      return nothing(
          new Diagnostic(
              WARN,
              "Missing decision on top-level package",
              location,
              new Suggestion(PICK_TOP_LEVEL_PACKAGE, "Decide on top-level package")),
          diagnostics);
    }
    return new JavaUnitTestGenerator(topLevelPackage);
  }

  private <T> T nothing(Diagnostic diagnostic, Collection<Diagnostic> diagnostics) {
    diagnostics.add(diagnostic);
    return null;
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    return switch (suggestionCode) {
      case PICK_PROGRAMMING_LANGUAGE ->
          pickDecision(resource, PROGRAMMING_LANGUAGE_DECISION, ProgrammingLanguage.TOPIC);
      case PICK_TOP_LEVEL_PACKAGE ->
          pickDecision(resource, TOP_LEVEL_PACKAGE_DECISION, TopLevelPackage.TOPIC);
      case PICK_BUILD_TOOL -> pickDecision(resource, BUILD_TOOL_DECISION, BuildTool.TOPIC);
      default -> AppliedSuggestion.none();
    };
  }

  private AppliedSuggestion pickDecision(Resource<?> resource, String decisionName, String topic) {
    try {
      var decision =
          new Decision(new FullyQualifiedName(TECHNOLOGY_DECISIONS_PACKAGE, decisionName))
              .setTopic(topic);
      var decisionInput = Inputs.decisions();
      var decisionResource =
          resource
              .select("/")
              .select(decisionInput.path())
              .select("%s.%s".formatted(decisionName, decisionInput.extension()));
      try (var output = decisionResource.writeTo()) {
        builderFor(decision).build(decision, output);
      }
      return created(decisionResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
