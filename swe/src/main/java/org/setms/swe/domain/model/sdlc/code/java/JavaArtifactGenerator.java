package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.overview.Initiative;

public abstract class JavaArtifactGenerator {

  public static final String CREATE_INITIATIVE = "initiative.create";
  public static final String PICK_TOP_LEVEL_PACKAGE = "top-level-package.decide";

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
