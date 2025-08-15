package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.format.Strings.ensureSuffix;
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

  protected static final String NL = System.lineSeparator();

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

  protected Resource<?> resourceFor(Artifact artifact, Resource<?> base) {
    var path =
        allInputs().stream()
            .filter(input -> input.type().equals(artifact.getClass()))
            .map(Input::glob)
            .map(Glob::path)
            .findFirst()
            .orElseThrow();
    return base.path().contains(path)
        ? base.select("%s.%s".formatted(artifact.getName(), artifact.type()))
        : toBase(base).select("%s/%s.%s".formatted(path, artifact.getName(), artifact.type()));
  }

  protected Resource<?> toBase(Resource<?> resource) {
    var input = mainInput();
    if (input.isEmpty()) {
      return null;
    }
    var glob = input.get().glob();
    if (resource.name().endsWith(glob.extension())) {
      var path = ensureSuffix(glob.path(), "/");
      var current = resource;
      while (!current.path().endsWith(path)) {
        current = current.parent().orElseThrow();
      }
      return current.select(path.replaceAll("[^/]+", ".."));
    }
    return resource;
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

  protected String wrap(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }
    var index = maxLength - 1;
    while (index >= 0 && !Character.isUpperCase(text.charAt(index))) {
      index--;
    }
    if (index <= 0) {
      index = maxLength;
    }
    return text.substring(0, index) + NL + wrap(text.substring(index), maxLength);
  }

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param inputs inputs to use
   * @param location where in the input to apply the suggestion
   * @param output where to store outputs
   * @return artifacts created/changed and diagnostics
   */
  public AppliedSuggestion apply(
      String suggestionCode, ResolvedInputs inputs, Location location, Resource<?> output) {
    try {
      return doApply(suggestionCode, inputs, location, output);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  protected AppliedSuggestion doApply(
      String suggestionCode, ResolvedInputs inputs, Location location, Resource<?> output)
      throws Exception {
    return AppliedSuggestion.unknown(suggestionCode);
  }
}
