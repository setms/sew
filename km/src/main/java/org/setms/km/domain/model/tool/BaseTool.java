package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.ensureSuffix;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.INFO;

import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.*;

/**
 * Something that validates input, builds output from input, and provides and applies suggestions
 * based on the input.
 */
public abstract class BaseTool {

  protected static final String NL = System.lineSeparator();

  /**
   * The inputs this tool consumes.
   *
   * @return the inputs
   */
  public abstract List<Input<?>> getInputs();

  /**
   * The outputs this tool produces.
   *
   * @return the outputs
   */
  public abstract Optional<Output> getOutputs();

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
    var validate = new AtomicBoolean(true);
    getInputs()
        .forEach(
            input ->
                result.put(
                    input.name(), parse(resource, input, validate.getAndSet(false), diagnostics)));
    return result;
  }

  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {}

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

  public Optional<Output> htmlIn(String path) {
    return Optional.of(new Output(new Glob(path, "**/*.html")));
  }

  protected Resource<?> resourceFor(Artifact object, Resource<?> base) {
    var path =
        getInputs().stream()
            .filter(input -> input.type().equals(object.getClass()))
            .map(Input::glob)
            .map(Glob::path)
            .findFirst()
            .orElseThrow();
    return base.path().contains(path)
        ? base.select("../%s.%s".formatted(object.getName(), object.type()))
        : toBase(base).select("../%s/%s.%s".formatted(path, object.getName(), object.type()));
  }

  protected Resource<?> toBase(Resource<?> resource) {
    var glob = getInputs().getFirst().glob();
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

  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param workspace where to load input and store output
   * @param location where in the input to apply the suggestion
   * @return diagnostics about the applying the suggestion
   */
  public final SequencedSet<Diagnostic> apply(
      String suggestionCode, Workspace<?> workspace, Location location) {
    var result = new LinkedHashSet<Diagnostic>();
    var inputs = resolveInputs(workspace.root(), result);
    apply(suggestionCode, inputs, location, workspace.root(), result);
    return result;
  }

  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    diagnostics.add(new Diagnostic(ERROR, "Unknown suggestion: " + suggestionCode));
  }

  protected final Diagnostic resourceCreated(Resource<?> resource) {
    return new Diagnostic(INFO, "Created " + resource.toUri());
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
