package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.overview.Initiative;

public abstract class JavaArtifactGenerator {

  public static final String CREATE_INITIATIVE = "initiative.create";
  public static final String PICK_TOP_LEVEL_PACKAGE = "top-level-package.decide";

  public static AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case CREATE_INITIATIVE -> createInitiative(resource);
      case PICK_TOP_LEVEL_PACKAGE -> pickTopLevelPackageDecision(resource, inputs);
      default -> AppliedSuggestion.none();
    };
  }

  private static AppliedSuggestion createInitiative(Resource<?> resource) {
    try {
      var initiativeResource =
          resource.select("/").select("src/main/overview").select("Project.initiative");
      var content =
          """
          package overview

          initiative Project {
            organization = "Organization"
            title        = "Project"
          }
          """;
      try (var output = initiativeResource.writeTo()) {
        output.write(content.getBytes());
      }
      return created(initiativeResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private static AppliedSuggestion pickTopLevelPackageDecision(
      Resource<?> resource, ResolvedInputs inputs) {
    try {
      var choice =
          inputs.get(Initiative.class).stream()
              .findFirst()
              .map(
                  i ->
                      "com.%s.%s"
                          .formatted(i.getOrganization().toLowerCase(), i.getTitle().toLowerCase()))
              .orElse("com.example");
      var decisionResource =
          resource.select("/").select("src/main/architecture").select("TopLevelPackage.decision");
      var content =
          """
          package technology

          decision TopLevelPackage {
            choice = "%s"
            topic  = "TopLevelPackage"
          }
          """
              .formatted(choice);
      try (var output = decisionResource.writeTo()) {
        output.write(content.getBytes());
      }
      return created(decisionResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  public static Optional<String> topLevelPackage(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var initiative = inputs.get(Initiative.class).stream().findFirst();
    if (initiative.isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing initiative",
              null,
              new Suggestion(CREATE_INITIATIVE, "Create initiative")));
      return Optional.empty();
    }
    var topLevelPackage = Decisions.from(inputs).about(TopLevelPackage.TOPIC);
    if (topLevelPackage == null) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing decision on top-level package",
              null,
              new Suggestion(PICK_TOP_LEVEL_PACKAGE, "Decide on top-level package")));
      return Optional.empty();
    }
    return Optional.of(topLevelPackage);
  }
}
