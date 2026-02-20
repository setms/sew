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
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.java.GradleBuildTool;
import org.setms.swe.domain.model.sdlc.code.java.JavaUnitTestGenerator;
import org.setms.swe.domain.model.sdlc.project.Project;
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
  static final String CREATE_PROJECT = "project.create";

  @Override
  public Optional<UnitTestGenerator> unitTestGenerator(
      Collection<Decision> decisions,
      Collection<Project> projects,
      Location location,
      Collection<Diagnostic> diagnostics) {
    var topics = groupByTopic(decisions);
    var programmingLanguage = topics.get(ProgrammingLanguage.TOPIC);
    var topLevelPackage = topics.get(TopLevelPackage.TOPIC);
    return Optional.ofNullable(
        unitTestGeneratorFor(
            programmingLanguage, projects, topLevelPackage, location, diagnostics));
  }

  private Map<String, String> groupByTopic(Collection<Decision> decisions) {
    var result = new HashMap<String, String>();
    decisions.forEach(decision -> result.put(decision.getTopic(), decision.getChoice()));
    return result;
  }

  private UnitTestGenerator unitTestGeneratorFor(
      String programmingLanguage,
      Collection<Project> projects,
      String topLevelPackage,
      Location location,
      Collection<Diagnostic> diagnostics) {
    return switch (programmingLanguage) {
      case "Java" -> javaUnitGenerator(projects, topLevelPackage, location, diagnostics);
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
      Collection<Project> projects,
      String topLevelPackage,
      Location location,
      Collection<Diagnostic> diagnostics) {
    if (projects.isEmpty()) {
      return null;
    }
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
  public Optional<org.setms.swe.domain.model.sdlc.technology.BuildTool> buildTool(
      Resource<?> resource,
      ResolvedInputs inputs,
      Location location,
      Collection<Diagnostic> diagnostics) {
    var project = inputs.get(Project.class).stream().findFirst();
    if (project.isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN, "Missing project", location, new Suggestion(CREATE_PROJECT, "Create project")));
      return Optional.empty();
    }

    var projectName = project.get().getTitle();
    if (projectName == null || projectName.isBlank()) {
      diagnostics.add(new Diagnostic(WARN, "Missing project title", location));
      return Optional.empty();
    }

    var topics = groupByTopic(inputs.get(Decision.class));
    var programmingLanguage = topics.get(ProgrammingLanguage.TOPIC);
    var buildToolChoice = topics.get(org.setms.swe.domain.model.sdlc.architecture.BuildTool.TOPIC);

    var result =
        buildToolFor(programmingLanguage, buildToolChoice, projectName, location, diagnostics);
    if (resource != null) {
      result.ifPresent(bt -> bt.validate(resource, diagnostics));
    }
    return result;
  }

  private Optional<org.setms.swe.domain.model.sdlc.technology.BuildTool> buildToolFor(
      String programmingLanguage,
      String buildToolChoice,
      String projectName,
      Location location,
      Collection<Diagnostic> diagnostics) {
    if (programmingLanguage == null) {
      return Optional.ofNullable(
          nothing(
              new Diagnostic(
                  WARN,
                  "Missing decision on programming language",
                  location,
                  new Suggestion(PICK_PROGRAMMING_LANGUAGE, "Decide on programming language")),
              diagnostics));
    }
    if (buildToolChoice == null) {
      return Optional.ofNullable(
          nothing(
              new Diagnostic(
                  WARN,
                  "Missing decision on build tool",
                  location,
                  new Suggestion(PICK_BUILD_TOOL, "Decide on build tool")),
              diagnostics));
    }

    return programmingLanguage.equals("Java")
        ? javaBuildTool(buildToolChoice, projectName, location, diagnostics)
        : Optional.ofNullable(
            nothing(
                new Diagnostic(ERROR, "Decided on unsupported programming language", location),
                diagnostics));
  }

  private Optional<org.setms.swe.domain.model.sdlc.technology.BuildTool> javaBuildTool(
      String buildToolChoice,
      String projectName,
      Location location,
      Collection<Diagnostic> diagnostics) {
    return buildToolChoice.equals("Gradle")
        ? Optional.of(new GradleBuildTool(projectName))
        : Optional.ofNullable(
            nothing(
                new Diagnostic(ERROR, "Decided on unsupported build tool", location), diagnostics));
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    return switch (suggestionCode) {
      case PICK_PROGRAMMING_LANGUAGE ->
          pickDecision(resource, PROGRAMMING_LANGUAGE_DECISION, ProgrammingLanguage.TOPIC);
      case PICK_TOP_LEVEL_PACKAGE ->
          pickDecision(resource, TOP_LEVEL_PACKAGE_DECISION, TopLevelPackage.TOPIC);
      case PICK_BUILD_TOOL ->
          pickDecision(
              resource,
              BUILD_TOOL_DECISION,
              org.setms.swe.domain.model.sdlc.architecture.BuildTool.TOPIC);
      case CREATE_PROJECT -> createProject(resource);
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

  private AppliedSuggestion createProject(Resource<?> resource) {
    try {
      var projectName = "ProjectName";
      var projectInput = Inputs.projects();
      var projectResource =
          resource
              .select("/")
              .select(projectInput.path())
              .select("Project.%s".formatted(projectInput.extension()));
      var content =
          """
          package overview

          project %s {
            title = "%s"
          }
          """;
      try (var output = projectResource.writeTo()) {
        output.write(content.formatted(projectName, projectName).getBytes());
      }
      return created(projectResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
