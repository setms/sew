package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.validation.Level.ERROR;

import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
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
    return mainInput().map(Input::glob).filter(glob -> glob.matches(path)).isPresent();
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

  protected Resource<?> resourceFor(Artifact target, Artifact source, Resource<?> sourceResource) {
    var sourceContainerPath = containerPathFor(source);
    var targetContainerPath = containerPathFor(target);
    var sourcePath = sourceResource.path();
    var index = sourcePath.indexOf(sourceContainerPath);
    var prefix = sourcePath.substring(0, index);
    var suffix = sourcePath.substring(index + sourceContainerPath.length());
    suffix = suffix.substring(0, suffix.lastIndexOf(sourceResource.name()));
    var targetPath =
        prefix + targetContainerPath + suffix + target.getName() + extensionFor(target);
    return sourceResource.select(targetPath);
  }

  private String containerPathFor(Artifact artifact) {
    return globFor(artifact).map(Glob::path).orElseThrow();
  }

  private Optional<Glob> globFor(Artifact artifact) {
    return allInputs().stream()
        .filter(input -> input.type().equals(artifact.getClass()))
        .map(Input::glob)
        .findFirst();
  }

  private String extensionFor(Artifact artifact) {
    return globFor(artifact).map(Glob::extension).orElseThrow();
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

  protected Optional<Resource<?>> build(
      Artifact object, mxGraph graph, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    try {
      var image = renderGraph(graph);
      if (image == null) {
        return Optional.empty();
      }
      var result = resource.select(object.getName() + ".png");
      try (var output = result.writeTo()) {
        ImageIO.write(image, "PNG", output);
      }
      return Optional.of(result);
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
    return Optional.empty();
  }

  private BufferedImage renderGraph(mxGraph graph) {
    var image = mxCellRenderer.createBufferedImage(graph, null, 1, null, true, null);
    if (image == null) {
      return null;
    }
    clear(graph);
    var result =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    var graphics = result.getGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return result;
  }

  private void clear(mxGraph graph) {
    graph.getModel().beginUpdate();
    try {
      Object[] cells = graph.getChildCells(graph.getDefaultParent(), true, true);
      graph.removeCells(cells);
    } finally {
      graph.getModel().endUpdate();
    }
    graph.clearSelection();
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
}
