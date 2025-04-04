package org.setms.sew.tool;

import static org.setms.sew.tool.Level.ERROR;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.setms.sew.schema.NamedObject;

public interface Tool {

  default List<Diagnostic> run(File dir) {
    var result = new ArrayList<Diagnostic>();
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.getName(), parse(dir, input, result)));
    run(dir, inputs, result);
    return result;
  }

  List<Input<?>> getInputs();

  private <T extends NamedObject> List<T> parse(
      File dir, Input<T> input, Collection<Diagnostic> diagnostics) {
    var parser = input.getFormat().newParser();
    return input.getGlob().matchingIn(dir).stream()
        .map(
            file -> {
              try {
                return parser.parse(file, input.getType());
              } catch (Exception e) {
                diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  void run(File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics);
}
