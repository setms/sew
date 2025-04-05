package org.setms.sew.tool;

import static org.setms.sew.tool.Level.ERROR;
import static org.setms.sew.tool.Level.INFO;
import static org.setms.sew.tool.Level.WARN;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.setms.sew.schema.NamedObject;

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
   * @param dir the directory in which to find inputs
   * @return any validation issues
   */
  public final List<Diagnostic> validate(File dir) {
    var result = new ArrayList<Diagnostic>();
    validate(dir, resolveInputs(dir, result), result);
    return result;
  }

  private ResolvedInputs resolveInputs(File dir, Collection<Diagnostic> result) {
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.name(), parse(dir, input, result)));
    return inputs;
  }

  private <T extends NamedObject> List<T> parse(
      File dir, Input<T> input, Collection<Diagnostic> diagnostics) {
    var parser = input.format().newParser();
    return input.glob().matchingIn(dir).stream()
        .map(
            file -> {
              try {
                return parser.parse(file, input.type());
              } catch (Exception e) {
                diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  protected void validate(File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {}

  /**
   * Build the output from the input
   *
   * @param dir the directory in which to find input
   * @return diagnostics about building the output
   */
  public final List<Diagnostic> build(File dir) {
    var result = new ArrayList<Diagnostic>();
    build(dir, resolveInputs(dir, result), result);
    return result;
  }

  protected void build(File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {}

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param dir the directory in which to find input
   * @return diagnostics about the applying the suggestion
   */
  public final List<Diagnostic> apply(String suggestionCode, File dir) {
    var result = new ArrayList<Diagnostic>();
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.name(), parse(dir, input, result)));
    apply(suggestionCode, dir, inputs, result);
    return result;
  }

  protected void apply(
      String suggestionCode, File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.add(new Diagnostic(WARN, "Unknown suggestion: " + suggestionCode));
  }

  protected final Diagnostic fileCreated(File file) {
    return new Diagnostic(INFO, "Created file: " + file.getPath());
  }
}
