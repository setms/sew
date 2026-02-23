package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.java.Gradle;
import org.setms.swe.domain.model.sdlc.code.java.JavaArtifactGenerator;
import org.setms.swe.domain.model.sdlc.code.java.JavaCodeGenerator;
import org.setms.swe.domain.model.sdlc.code.java.JavaUnitTestGenerator;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;

public class TechnologyResolverImpl implements TechnologyResolver {

  static final String PICK_PROGRAMMING_LANGUAGE = "programming-language.decide";
  static final String PICK_TOP_LEVEL_PACKAGE = "top-level-package.decide";
  static final String PICK_BUILD_SYSTEM = "build-system.decide";
  static final String CREATE_INITIATIVE = JavaArtifactGenerator.CREATE_INITIATIVE;

  private static final String TECHNOLOGY_DECISIONS_PACKAGE = "technology";
  private static final String PROGRAMMING_LANGUAGE_DECISION = "ProgrammingLanguage";
  private static final String TOP_LEVEL_PACKAGE_DECISION = "TopLevelPackage";

  @Override
  public Optional<UnitTestGenerator> unitTestGenerator(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var decisions = Decisions.from(inputs);
    var programmingLanguage = decisions.about(ProgrammingLanguage.TOPIC);
    var topLevelPackage = decisions.about(TopLevelPackage.TOPIC);
    return Optional.ofNullable(
        unitTestGeneratorFor(programmingLanguage, topLevelPackage, diagnostics));
  }

  private UnitTestGenerator unitTestGeneratorFor(
      String programmingLanguage, String topLevelPackage, Collection<Diagnostic> diagnostics) {
    return switch (programmingLanguage) {
      case "Java" -> javaUnitGenerator(topLevelPackage, diagnostics);
      case null -> nothing(missingProgrammingLanguageDecision(), diagnostics);
      default -> nothing(unsupportedProgrammingLanguage(), diagnostics);
    };
  }

  private UnitTestGenerator javaUnitGenerator(
      String topLevelPackage, Collection<Diagnostic> diagnostics) {
    if (topLevelPackage == null) {
      return nothing(
          new Diagnostic(
              WARN,
              "Missing decision on top-level package",
              null,
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
  public Optional<CodeGenerator> codeGenerator(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var decisions = Decisions.from(inputs);
    var programmingLanguage = decisions.about(ProgrammingLanguage.TOPIC);
    var topLevelPackage = decisions.about(TopLevelPackage.TOPIC);
    return Optional.ofNullable(codeGeneratorFor(programmingLanguage, topLevelPackage, diagnostics));
  }

  private CodeGenerator codeGeneratorFor(
      String programmingLanguage, String topLevelPackage, Collection<Diagnostic> diagnostics) {
    return switch (programmingLanguage) {
      case "Java" -> new JavaCodeGenerator(topLevelPackage);
      case null -> nothing(missingProgrammingLanguageDecision(), diagnostics);
      default -> nothing(unsupportedProgrammingLanguage(), diagnostics);
    };
  }

  private Diagnostic unsupportedProgrammingLanguage() {
    return new Diagnostic(ERROR, "Decided on unsupported programming language", null);
  }

  @Override
  public Optional<CodeBuilder> codeBuilder(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var initiative = inputs.get(Initiative.class).stream().findFirst();
    if (initiative.isEmpty()) {
      diagnostics.add(missingInitiative());
      return Optional.empty();
    }

    var decisions = Decisions.from(inputs);
    var result =
        codeBuilderFor(
            decisions.about(ProgrammingLanguage.TOPIC),
            decisions.about(BuildSystem.TOPIC),
            initiative.get().getTitle(),
            diagnostics);
    result.ifPresent(codeBuilder -> codeBuilder.validate(resource, diagnostics));
    return result;
  }

  private Diagnostic missingInitiative() {
    return new Diagnostic(
        WARN, "Missing initiative", null, new Suggestion(CREATE_INITIATIVE, "Create initiative"));
  }

  private Optional<CodeBuilder> codeBuilderFor(
      String programmingLanguage,
      String selectedBuildSystem,
      String projectName,
      Collection<Diagnostic> diagnostics) {
    if (programmingLanguage == null) {
      return Optional.ofNullable(nothing(missingProgrammingLanguageDecision(), diagnostics));
    }
    if (selectedBuildSystem == null) {
      return Optional.ofNullable(
          nothing(
              new Diagnostic(
                  WARN,
                  "Missing decision on build system",
                  null,
                  new Suggestion(PICK_BUILD_SYSTEM, "Decide on build system")),
              diagnostics));
    }

    return programmingLanguage.equals("Java")
        ? javaBuildSystem(selectedBuildSystem, projectName, diagnostics)
        : Optional.ofNullable(nothing(unsupportedProgrammingLanguage(), diagnostics));
  }

  private Diagnostic missingProgrammingLanguageDecision() {
    return new Diagnostic(
        WARN,
        "Missing decision on programming language",
        null,
        new Suggestion(PICK_PROGRAMMING_LANGUAGE, "Decide on programming language"));
  }

  private Optional<CodeBuilder> javaBuildSystem(
      String selectedBuildSystem, String projectName, Collection<Diagnostic> diagnostics) {
    return selectedBuildSystem.equals("Gradle")
        ? Optional.of(new Gradle(projectName))
        : Optional.ofNullable(
            nothing(
                new Diagnostic(ERROR, "Decided on unsupported build system", null), diagnostics));
  }

  @Override
  public AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case PICK_PROGRAMMING_LANGUAGE ->
          pickDecision(resource, PROGRAMMING_LANGUAGE_DECISION, ProgrammingLanguage.TOPIC);
      case PICK_TOP_LEVEL_PACKAGE -> pickTopLevelPackageDecision(resource, inputs);
      case PICK_BUILD_SYSTEM -> pickDecision(resource, BuildSystem.TOPIC, BuildSystem.TOPIC);
      case CREATE_INITIATIVE -> createProject(resource);
      case Gradle.GENERATE_BUILD_CONFIG -> generateBuildConfig(resource, inputs);
      default -> AppliedSuggestion.none();
    };
  }

  private AppliedSuggestion pickTopLevelPackageDecision(
      Resource<?> resource, ResolvedInputs inputs) {
    var defaultChoice =
        inputs.get(Initiative.class).stream()
            .findFirst()
            .map(
                initiative ->
                    "com.%s.%s"
                        .formatted(
                            initiative.getOrganization().toLowerCase(),
                            initiative.getTitle().toLowerCase()))
            .orElse(null);
    return pickDecision(resource, TOP_LEVEL_PACKAGE_DECISION, TopLevelPackage.TOPIC, defaultChoice);
  }

  private AppliedSuggestion generateBuildConfig(Resource<?> resource, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return codeBuilder(resource, inputs, diagnostics)
        .map(bt -> bt.applySuggestion(Gradle.GENERATE_BUILD_CONFIG, resource))
        .orElseGet(
            () ->
                diagnostics.stream()
                    .reduce(
                        AppliedSuggestion.none(),
                        AppliedSuggestion::with,
                        (appliedSuggestion, _) -> appliedSuggestion));
  }

  private AppliedSuggestion pickDecision(Resource<?> resource, String decisionName, String topic) {
    return pickDecision(resource, decisionName, topic, null);
  }

  private AppliedSuggestion pickDecision(
      Resource<?> resource, String decisionName, String topic, String defaultChoice) {
    try {
      var decision =
          new Decision(new FullyQualifiedName(TECHNOLOGY_DECISIONS_PACKAGE, decisionName))
              .setTopic(topic);
      Optional.ofNullable(defaultChoice).ifPresent(decision::setChoice);
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
      var projectName = "Project";
      var input = Inputs.initiatives();
      var initiativeResource =
          resource
              .select("/")
              .select(input.path())
              .select("Project.%s".formatted(input.extension()));
      var content =
          """
          package overview

          initiative %s {
            organization = "Organization"
            title        = "%s"
          }
          """;
      try (var output = initiativeResource.writeTo()) {
        output.write(content.formatted(projectName, projectName).getBytes());
      }
      return created(initiativeResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
