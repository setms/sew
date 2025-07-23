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
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.km.domain.model.workspace.Workspace;

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
   * @param workspace the workspace in which to find inputs and store outputs
   * @return any validation issues
   */
  public final SequencedSet<Diagnostic> validate(Workspace workspace) {
    var result = new LinkedHashSet<Diagnostic>();
    validate(resolveInputs(workspace.input(), result), result);
    createTodosFor(workspace.output(), result);
    return result;
  }

  private ResolvedInputs resolveInputs(InputSource source, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    var validate = new AtomicBoolean(true);
    getInputs()
        .forEach(
            input ->
                result.put(
                    input.name(), parse(source, input, validate.getAndSet(false), diagnostics)));
    return result;
  }

  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {}

  private void createTodosFor(OutputSink sink, Collection<Diagnostic> diagnostics) {
    diagnostics.stream()
        .filter(d -> d.level() != INFO)
        .filter(d -> !d.suggestions().isEmpty())
        .forEach(diagnostic -> createTodoFor(diagnostic, sink));
  }

  private void createTodoFor(Diagnostic diagnostic, OutputSink sink) {
    /* TODO: Decouple from SAL - needs to be part of the collaboration package
    var suggestion = diagnostic.suggestions().getFirst();
    var name = Strings.toObjectName(diagnostic.message());
    String packageName;
    String location;
    String path;
    if (diagnostic.location() == null) {
      packageName = "todos";
      location = null;
      path = "";
    } else {
      packageName = diagnostic.location().segments().getFirst();
      location = diagnostic.location().toString();
      path = location + "/";
    }
    var todo =
        new Todo(new FullyQualifiedName(packageName, name))
            .setTool(getClass().getName())
            .setLocation(location)
            .setMessage(diagnostic.message())
            .setCode(suggestion.code())
            .setAction(suggestion.message());
    var todoSink = sink.select("src/todo/%s%s.todo".formatted(path, name));
    try {
      new SalFormat().newBuilder().build(todo, todoSink);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
     */
  }

  private <T extends Artifact> List<T> parse(
      InputSource source, Input<T> input, boolean validate, Collection<Diagnostic> diagnostics) {
    return input
        .format()
        .newParser()
        .parseMatching(source, input.glob(), input.type(), validate, diagnostics)
        .toList();
  }

  /**
   * Build the output from the input
   *
   * @param workspace where to retrieve input and store output
   * @return diagnostics about building the output
   */
  public final List<Diagnostic> build(Workspace workspace) {
    var result = new ArrayList<Diagnostic>();
    build(resolveInputs(workspace.input(), result), workspace.output(), result);
    return result;
  }

  protected void addError(Collection<Diagnostic> diagnostics, String message, Object... args) {
    diagnostics.add(new Diagnostic(ERROR, message.formatted(args)));
  }

  public Optional<Output> htmlIn(String path) {
    return Optional.of(new Output(new Glob(path, "**/*.html")));
  }

  protected OutputSink sinkFor(Artifact object, OutputSink base) {
    var path =
        getInputs().stream()
            .filter(input -> input.type().equals(object.getClass()))
            .map(Input::glob)
            .map(Glob::path)
            .findFirst()
            .orElseThrow();
    return base.toUri().toString().contains(path)
        ? base.select("../%s.%s".formatted(object.getName(), object.type()))
        : toBase(base).select("../%s/%s.%s".formatted(path, object.getName(), object.type()));
  }

  protected OutputSink toBase(OutputSink sink) {
    var glob = getInputs().getFirst().glob();
    if (sink.toUri().toString().endsWith(glob.extension())) {
      var path = ensureSuffix(glob.path(), "/");
      var current = sink;
      while (!current.toUri().toString().endsWith(path)) {
        current = current.select("..");
      }
      return current.select(path.replaceAll("[^/]+", ".."));
    }
    return sink.select("..");
  }

  protected void build(
      ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param workspace where to load input and store output
   * @param location where in the input to apply the suggestion
   * @return diagnostics about the applying the suggestion
   */
  public final SequencedSet<Diagnostic> apply(
      String suggestionCode, Workspace workspace, Location location) {
    var result = new LinkedHashSet<Diagnostic>();
    var inputs = resolveInputs(workspace.input(), result);
    apply(suggestionCode, inputs, location, workspace.output(), result);
    return result;
  }

  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    diagnostics.add(new Diagnostic(ERROR, "Unknown suggestion: " + suggestionCode));
  }

  protected final Diagnostic sinkCreated(OutputSink sink) {
    return new Diagnostic(INFO, "Created " + sink.toUri());
  }

  protected Optional<OutputSink> build(
      Artifact object, mxGraph graph, OutputSink sink, Collection<Diagnostic> diagnostics) {
    try {
      var image = renderGraph(graph);
      if (image == null) {
        return Optional.empty();
      }
      var result = sink.select(object.getName() + ".png");
      try (var output = result.open()) {
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
