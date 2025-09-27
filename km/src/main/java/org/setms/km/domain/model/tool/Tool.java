package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.validation.Diagnostic;

/**
 * Something that validates input, provides and applies suggestions for validation issues, and
 * builds reports from input.
 */
public abstract sealed class Tool permits ArtifactTool, StandaloneTool {

  /**
   * The inputs that this tool needs for validation, if any.
   *
   * @return any validation inputs
   */
  public Set<Input<? extends Artifact>> validationContext() {
    return emptySet();
  }

  protected void addError(Collection<Diagnostic> diagnostics, String message, Object... args) {
    diagnostics.add(new Diagnostic(ERROR, message.formatted(args)));
  }

  /**
   * The inputs this tool needs for building reports, if any.
   *
   * @return any report inputs
   */
  public Set<Input<? extends Artifact>> reportingContext() {
    return emptySet();
  }

  /**
   * All inputs this tool needs for any purpose.
   *
   * @return all inputs
   */
  public Set<Input<? extends Artifact>> allInputs() {
    var result = new LinkedHashSet<Input<? extends Artifact>>();
    result.addAll(validationContext());
    result.addAll(reportingContext());
    return result;
  }
}
