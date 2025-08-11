package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.format.Strings.ensureSuffix;
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
public abstract class BaseTool<A extends Artifact> {

  protected static final String NL = System.lineSeparator();

  /**
   * The primary input this tool consumes, if any. In other words, the artifacts it validates.
   *
   * @return the main input
   */
  public Optional<Input<A>> mainInput() {
    return Optional.ofNullable(getMainInput());
  }

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

  public boolean matchesMainInput(String path) {
    return mainInput().map(Input::glob).filter(glob -> glob.matches(path)).isPresent();
  }

  /**
   * Validate the inputs.
   *
   * @deprecated Shouldn't need to call this anymore, KmSystem takes care of that
   * @param workspace the workspace in which to find inputs and store outputs
   * @return any validation issues
   */
  @Deprecated
  public SequencedSet<Diagnostic> validate(Workspace<?> workspace) {
    var result = new LinkedHashSet<Diagnostic>();
    validate(resolveInputs(workspace.root(), result), result);
    return result;
  }

  private ResolvedInputs resolveInputs(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    mainInput().ifPresent(input -> resolveInput(input, resource, true, diagnostics, result));
    additionalInputs().forEach(input -> resolveInput(input, resource, false, diagnostics, result));
    return result;
  }

  private void resolveInput(
      Input<?> input,
      Resource<?> resource,
      boolean validate,
      Collection<Diagnostic> diagnostics,
      ResolvedInputs inputs) {
    inputs.put(input.name(), parse(resource, input, validate, diagnostics));
  }

  @SuppressWarnings("unused")
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    // For descendants to override if that have additional validation logic
  }

  private <T extends Artifact> List<T> parse(
      Resource<?> resource, Input<T> input, boolean validate, Collection<Diagnostic> diagnostics) {
    return input
        .format()
        .newParser()
        .parseMatching(resource, input.glob(), input.type(), validate, diagnostics)
        .toList();
  }

  /**
   * Build the output from the input
   *
   * @deprecated Shouldn't need to call this anymore, the KmSystem should take care of this
   * @param workspace where to retrieve input and store output
   * @return diagnostics about building the output
   */
  @Deprecated
  public List<Diagnostic> build(Workspace<?> workspace) {
    var result = new ArrayList<Diagnostic>();
    build(resolveInputs(workspace.root(), result), workspace.root().select("build"), result);
    return result;
  }

  protected void addError(Collection<Diagnostic> diagnostics, String message, Object... args) {
    diagnostics.add(new Diagnostic(ERROR, message.formatted(args)));
  }

  protected Resource<?> resourceFor(Artifact object, Resource<?> base) {
    var path =
        allInputs().stream()
            .filter(input -> input.type().equals(object.getClass()))
            .map(Input::glob)
            .map(Glob::path)
            .findFirst()
            .orElseThrow();
    return base.path().contains(path)
        ? base.select("%s.%s".formatted(object.getName(), object.type()))
        : toBase(base).select("%s/%s.%s".formatted(path, object.getName(), object.type()));
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

  @SuppressWarnings("unused")
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param workspace where to load input and store output
   * @param location where in the input to apply the suggestion
   * @return artifacts created/changed and diagnostics
   */
  public final AppliedSuggestion apply(
      String suggestionCode, Workspace<?> workspace, Location location) {
    var result = new AppliedSuggestion();
    var inputs = resolveInputs(workspace.root(), new LinkedHashSet<>());
    return apply(suggestionCode, inputs, location, workspace.root(), result);
  }

  @SuppressWarnings("unused")
  protected AppliedSuggestion apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      Resource<?> resource,
      AppliedSuggestion appliedSuggestion) {
    return appliedSuggestion.with(new Diagnostic(ERROR, "Unknown suggestion: " + suggestionCode));
  }

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
}
