package org.setms.sew.core.domain.model.tool;

import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.INFO;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

/**
 * Something that validates input, builds output from input, and provides and applies suggestions
 * based on the input.
 */
public abstract class Tool {

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
    validate(source, resolveInputs(source, result), result);
    return result;
  }

  private ResolvedInputs resolveInputs(InputSource source, Collection<Diagnostic> diagnostics) {
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.name(), parse(source, input, diagnostics)));
    return inputs;
  }

  private <T extends NamedObject> List<T> parse(
      InputSource source, Input<T> input, Collection<Diagnostic> diagnostics) {
    var parser = input.format().newParser();
    return source.matching(input.glob()).stream()
        .map(
            inputSource -> {
              try (var inputStream = inputSource.open()) {
                return parser.parse(inputStream, input.type());
              } catch (Exception e) {
                diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  protected void validate(
      InputSource source, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {}

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

  protected void build(
      ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param source where to load input
   * @param sink where to store input
   * @return diagnostics about the applying the suggestion
   */
  public final List<Diagnostic> apply(String suggestionCode, InputSource source, OutputSink sink) {
    var result = new ArrayList<Diagnostic>();
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.name(), parse(source, input, result)));
    apply(suggestionCode, inputs, sink, result);
    return result;
  }

  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    diagnostics.add(new Diagnostic(WARN, "Unknown suggestion: " + suggestionCode));
  }

  protected final Diagnostic sinkCreated(OutputSink sink) {
    return new Diagnostic(INFO, "Created " + sink.toUri());
  }
}
