package org.setms.sew.core.domain.model.tool;

import static org.setms.sew.core.domain.model.validation.Level.ERROR;
import static org.setms.sew.core.domain.model.validation.Level.INFO;

import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.setms.sew.core.domain.model.format.Strings;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.process.Todo;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;
import org.setms.sew.core.inbound.format.sal.SalFormat;

/**
 * Something that validates input, builds output from input, and provides and applies suggestions
 * based on the input.
 */
public abstract class Tool {

  protected static final String NL = "\n";

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
  public abstract List<Output> getOutputs();

  /**
   * Validate the inputs.
   *
   * @param source the directory in which to find inputs
   * @return any validation issues
   */
  public final SequencedSet<Diagnostic> validate(InputSource source) {
    var result = new LinkedHashSet<Diagnostic>();
    validate(resolveInputs(source, result), result);
    createTodosFor(source.toSink(), result);
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
  }

  private <T extends NamedObject> List<T> parse(
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
   * @param source where to load input
   * @param sink where to store output
   * @return diagnostics about building the output
   */
  public final List<Diagnostic> build(InputSource source, OutputSink sink) {
    var result = new ArrayList<Diagnostic>();
    build(resolveInputs(source, result), sink, result);
    return result;
  }

  protected void addError(Collection<Diagnostic> diagnostics, String message, Object... args) {
    diagnostics.add(new Diagnostic(ERROR, message.formatted(args)));
  }

  public List<Output> htmlWithImages(String path) {
    return List.of(new Output(new Glob(path, "*.html")), new Output(new Glob(path, "*.png")));
  }

  protected OutputSink toBase(OutputSink sink) {
    var glob = getInputs().getFirst().glob();
    if (sink.toUri().toString().endsWith(glob.extension())) {
      return sink.select(glob.path().replaceAll("[^/]+", "..")).select("..");
    }
    return sink;
  }

  protected void build(
      ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param source where to load input
   * @param location where in the input to apply the suggestion
   * @param sink where to store input
   * @return diagnostics about the applying the suggestion
   */
  public final SequencedSet<Diagnostic> apply(
      String suggestionCode, InputSource source, Location location, OutputSink sink) {
    var result = new LinkedHashSet<Diagnostic>();
    var inputs = resolveInputs(source, result);
    apply(suggestionCode, inputs, location, sink, result);
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
      NamedObject object, mxGraph graph, OutputSink sink, Collection<Diagnostic> diagnostics) {
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
