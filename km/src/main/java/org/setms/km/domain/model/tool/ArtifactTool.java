package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.*;
import java.util.regex.Pattern;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.*;

/** {@linkplain Tool} that operates on a specific {@linkplain Artifact artifact}. */
public abstract non-sealed class ArtifactTool<A extends Artifact> extends Tool {

  private static final Pattern VALIDATION_ERROR =
      Pattern.compile("[A-Z]+\\sat\\s[^:]+:\\s(?<message>.+)");

  /**
   * The input that this tool validates.
   *
   * @return the validation targets
   */
  public abstract Input<A> validationTarget();

  /**
   * @param path the path to an artifact
   * @return whether this tool can validate the artifact at the given path
   */
  public boolean validates(String path) {
    return validationTarget().matches(path);
  }

  /**
   * Validate an artifact stored at a resource.
   *
   * @param resource the input to validate
   * @param context additional inputs required for validation
   * @param diagnostics where to store any validation issues
   */
  public A validate(
      Resource<?> resource, ResolvedInputs context, Collection<Diagnostic> diagnostics) {
    var input = validationTarget();
    try (var sutStream = resource.readFrom()) {
      var result = input.format().newParser().parse(sutStream, input.type(), true);
      validate(result, context, diagnostics);
      return result;
    } catch (Exception e) {
      Arrays.stream(e.getMessage().split("\n"))
          .map(this::normalize)
          .forEach(
              message ->
                  diagnostics.add(
                      new Diagnostic(
                          ERROR,
                          message,
                          new Location(
                              resource.parent().map(Resource::name).orElse(null),
                              resource.name().substring(0, resource.name().lastIndexOf('.'))))));
    }
    return null;
  }

  private String normalize(String message) {
    var matcher = VALIDATION_ERROR.matcher(message);
    return matcher.matches() ? matcher.group("message") : message;
  }

  /**
   * Validate an artifact.
   *
   * @param artifact the input to validate
   * @param context additional inputs required for validation
   * @param diagnostics where to any validation issues
   */
  public void validate(A artifact, ResolvedInputs context, Collection<Diagnostic> diagnostics) {
    // For descendants to override
  }

  /**
   * Apply a suggestion.
   *
   * @param artifact the artifact to apply the suggestion to
   * @param suggestionCode the suggestion to apply
   * @param location where in the input to apply the suggestion
   * @param inputs inputs to use
   * @param resource where to store outputs
   * @return artifacts created/changed and diagnostics
   */
  public AppliedSuggestion applySuggestion(
      A artifact,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs,
      Resource<?> resource) {
    try {
      return doApply(resource, artifact, suggestionCode, location, inputs);
    } catch (Exception e) {
      return failedWith(e);
    }
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
    var prefix = index < 0 ? "" : sourcePath.substring(0, index);
    var suffix = index < 0 ? "" : sourcePath.substring(index + sourceContainerPath.length());
    suffix =
        suffix.isEmpty() ? "/" : suffix.substring(0, suffix.lastIndexOf(sourceResource.name()));
    var targetPath =
        prefix + targetContainerPath + suffix + target.getName() + "." + extensionFor(target);
    return sourceResource.select(targetPath);
  }

  private String containerPathFor(Artifact artifact) {
    return inputFor(artifact).map(Input::path).orElseThrow();
  }

  private Optional<Input<? extends Artifact>> inputFor(Artifact artifact) {
    if (validationTarget().targets(artifact)) {
      return Optional.of(validationTarget());
    }
    return validationContext().stream().filter(input -> input.targets(artifact)).findFirst();
  }

  private String extensionFor(Artifact artifact) {
    return inputFor(artifact).map(Input::extension).orElseThrow();
  }

  /**
   * The input that this tool build reports for, if any.
   *
   * @return the reporting targets
   */
  public Optional<Input<? extends Artifact>> reportingTarget() {
    return Optional.ofNullable(reportingTargetInput());
  }

  protected Input<? extends Artifact> reportingTargetInput() {
    return null;
  }

  public boolean reportsOn(String path) {
    return reportingTarget().map(input -> input.matches(path)).orElse(false);
  }

  /**
   * Build reports for an artifact.
   *
   * @param artifact the artifact to build reports for
   * @param inputs additional inputs needed to build reports
   * @param output where to store output
   * @param diagnostics where to any issues
   */
  public void buildReportsFor(
      A artifact, ResolvedInputs inputs, Resource<?> output, Collection<Diagnostic> diagnostics) {
    // For descendants to override
  }

  @Override
  public Set<Input<? extends Artifact>> allInputs() {
    var result = new LinkedHashSet<>(super.allInputs());
    result.add(validationTarget());
    reportingTarget().ifPresent(result::add);
    return result;
  }
}
