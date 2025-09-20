package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.*;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.*;

/**
 * Something that validates input, builds output from input, and provides and applies suggestions
 * based on the input.
 */
public abstract class Tool<A extends Artifact> {

  /**
   * The primary input this tool consumes, if any. In other words, the artifacts it validates.
   *
   * @return the main input
   */
  public Optional<Input<A>> mainInput() {
    return Optional.ofNullable(getMainInput());
  }

  /**
   * Returns the main input; the artifacts to be validated. Override when the tool validates some
   * artifacts.
   *
   * @return the main input
   */
  protected Input<A> getMainInput() {
    return null;
  }

  /**
   * Any additional inputs this tool consumes, if any.
   *
   * @return any additional inputs
   */
  public Set<Input<?>> additionalInputs() {
    return emptySet();
  }

  /**
   * All inputs that this tool consumes. That's the main input plus any additional inputs.
   *
   * @return all inputs that this tool consumes
   */
  public Set<Input<?>> allInputs() {
    var result = new LinkedHashSet<Input<?>>();
    mainInput().ifPresent(result::add);
    result.addAll(additionalInputs());
    return result;
  }

  /**
   * Whether a given path matches this tool's main input.
   *
   * @param path the path to match against the main input
   * @return whether the path matches the main input
   */
  public boolean matchesMainInput(String path) {
    return mainInput().filter(input -> input.matches(path)).isPresent();
  }

  /**
   * Validate the inputs.
   *
   * @param inputs the inputs to validate
   * @param diagnostics any validation issues found
   */
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    // For descendants to override if they have additional validation logic
  }

  protected void addError(Collection<Diagnostic> diagnostics, String message, Object... args) {
    diagnostics.add(new Diagnostic(ERROR, message.formatted(args)));
  }

  /**
   * Apply a suggestion.
   *
   * @param resource where to store outputs
   * @param suggestionCode the suggestion to apply
   * @param location where in the input to apply the suggestion
   * @param inputs inputs to use
   * @return artifacts created/changed and diagnostics
   */
  public AppliedSuggestion apply(
      Resource<?> resource, String suggestionCode, Location location, ResolvedInputs inputs) {
    try {
      return doApply(resource, artiFactFor(location, inputs), suggestionCode, location, inputs);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private A artiFactFor(Location location, ResolvedInputs inputs) {
    return inputs.get(getMainInput().type()).stream()
        .filter(artifact -> artifact.starts(location))
        .findFirst()
        .orElse(null);
  }

  protected AppliedSuggestion doApply(
      Resource<?> resource,
      A artifact,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws Exception {
    return AppliedSuggestion.unknown(suggestionCode);
  }

  protected Resource<?> resourceFor(Artifact target, Artifact source, Resource<?> sourceResource) {
    var sourceContainerPath = containerPathFor(source);
    var targetContainerPath = containerPathFor(target);
    var sourcePath = sourceResource.path();
    var index = sourcePath.indexOf(sourceContainerPath);
    var prefix = sourcePath.substring(0, index);
    var suffix = sourcePath.substring(index + sourceContainerPath.length());
    suffix = suffix.substring(0, suffix.lastIndexOf(sourceResource.name()));
    var targetPath =
        prefix + targetContainerPath + suffix + target.getName() + "." + extensionFor(target);
    return sourceResource.select(targetPath);
  }

  private String containerPathFor(Artifact artifact) {
    return inputFor(artifact).map(Input::path).orElseThrow();
  }

  private Optional<Input<?>> inputFor(Artifact artifact) {
    return allInputs().stream().filter(input -> input.targets(artifact)).findFirst();
  }

  private String extensionFor(Artifact artifact) {
    return inputFor(artifact).map(Input::extension).orElseThrow();
  }

  /**
   * Build the output from the input.
   *
   * @param inputs inputs to build from
   * @param resource where to store output
   * @param diagnostics any issues encountered during building
   */
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {}
}
