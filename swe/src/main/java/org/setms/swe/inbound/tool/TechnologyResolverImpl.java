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
import org.setms.swe.domain.model.sdlc.code.java.Gradle;
import org.setms.swe.domain.model.sdlc.code.java.JavaArtifactGenerator;
import org.setms.swe.domain.model.sdlc.code.java.JavaCodeGenerator;
import org.setms.swe.domain.model.sdlc.code.java.JavaUnitTestGenerator;
import org.setms.swe.domain.model.sdlc.overview.Initiative;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;
import org.setms.swe.domain.model.sdlc.technology.CodeTester;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;

public class TechnologyResolverImpl implements TechnologyResolver {

  static final String PICK_PROGRAMMING_LANGUAGE = "programming-language.decide";
  static final String PICK_BUILD_SYSTEM = "build-system.decide";

  private static final String TECHNOLOGY_DECISIONS_PACKAGE = "technology";
  private static final String PROGRAMMING_LANGUAGE_DECISION = "ProgrammingLanguage";

  @Override
  public Optional<UnitTestGenerator> unitTestGenerator(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var programmingLanguage = Decisions.from(inputs).about(ProgrammingLanguage.TOPIC);
    return switch (programmingLanguage) {
      case "Java" -> JavaUnitTestGenerator.from(inputs, diagnostics);
      case null -> empty(missingProgrammingLanguageDecision(), diagnostics);
      default -> empty(unsupportedProgrammingLanguage(), diagnostics);
    };
  }

  @Override
  public Optional<CodeGenerator> codeGenerator(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var programmingLanguage = Decisions.from(inputs).about(ProgrammingLanguage.TOPIC);
    return switch (programmingLanguage) {
      case "Java" -> JavaCodeGenerator.from(inputs, diagnostics);
      case null -> empty(missingProgrammingLanguageDecision(), diagnostics);
      default -> empty(unsupportedProgrammingLanguage(), diagnostics);
    };
  }

  private <T> Optional<T> empty(Diagnostic diagnostic, Collection<Diagnostic> diagnostics) {
    diagnostics.add(diagnostic);
    return Optional.empty();
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
        WARN,
        "Missing initiative",
        null,
        new Suggestion(JavaArtifactGenerator.CREATE_INITIATIVE, "Create initiative"));
  }

  private Optional<CodeBuilder> codeBuilderFor(
      String programmingLanguage,
      String selectedBuildSystem,
      String projectName,
      Collection<Diagnostic> diagnostics) {
    if (programmingLanguage == null) {
      return empty(missingProgrammingLanguageDecision(), diagnostics);
    }
    if (selectedBuildSystem == null) {
      return empty(missingBuildSystemDecision(), diagnostics);
    }

    return programmingLanguage.equals("Java")
        ? javaBuildSystem(selectedBuildSystem, projectName, diagnostics)
        : empty(unsupportedProgrammingLanguage(), diagnostics);
  }

  @Override
  public Optional<CodeTester> codeTester(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var programmingLanguage = Decisions.from(inputs).about(ProgrammingLanguage.TOPIC);
    return switch (programmingLanguage) {
      case "Java" -> javaCodeTester(inputs, diagnostics);
      case null -> empty(missingProgrammingLanguageDecision(), diagnostics);
      default -> empty(unsupportedProgrammingLanguage(), diagnostics);
    };
  }

  private Optional<CodeTester> javaCodeTester(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var initiative = inputs.get(Initiative.class).stream().findFirst();
    if (initiative.isEmpty()) {
      return empty(missingInitiative(), diagnostics);
    }
    var buildSystem = Decisions.from(inputs).about(BuildSystem.TOPIC);
    return switch (buildSystem) {
      case "Gradle" -> Optional.of(new Gradle(initiative.get().getTitle()));
      case null -> empty(missingBuildSystemDecision(), diagnostics);
      default ->
          empty(new Diagnostic(ERROR, "Decided on unsupported build system", null), diagnostics);
    };
  }

  private Diagnostic missingProgrammingLanguageDecision() {
    return new Diagnostic(
        WARN,
        "Missing decision on programming language",
        null,
        new Suggestion(PICK_PROGRAMMING_LANGUAGE, "Decide on programming language"));
  }

  private Diagnostic missingBuildSystemDecision() {
    return new Diagnostic(
        WARN,
        "Missing decision on build system",
        null,
        new Suggestion(PICK_BUILD_SYSTEM, "Decide on build system"));
  }

  private Optional<CodeBuilder> javaBuildSystem(
      String selectedBuildSystem, String projectName, Collection<Diagnostic> diagnostics) {
    return selectedBuildSystem.equals("Gradle")
        ? Optional.of(new Gradle(projectName))
        : empty(new Diagnostic(ERROR, "Decided on unsupported build system", null), diagnostics);
  }

  @Override
  public AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case PICK_PROGRAMMING_LANGUAGE ->
          pickDecision(resource, PROGRAMMING_LANGUAGE_DECISION, ProgrammingLanguage.TOPIC);
      case JavaArtifactGenerator.CREATE_INITIATIVE, JavaArtifactGenerator.PICK_TOP_LEVEL_PACKAGE ->
          JavaArtifactGenerator.applySuggestion(suggestionCode, resource, inputs);
      case PICK_BUILD_SYSTEM -> pickDecision(resource, BuildSystem.TOPIC, BuildSystem.TOPIC);
      case Gradle.GENERATE_BUILD_CONFIG -> generateBuildConfig(resource, inputs);
      default -> AppliedSuggestion.none();
    };
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
