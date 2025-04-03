package org.setms.sew.tool;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.setms.sew.glossary.inbound.cli.ResolvedInputs;
import org.setms.sew.schema.NamedObject;

public interface Tool {

  default void run(File dir) {
    var inputs = new ResolvedInputs();
    getInputs().forEach(input -> inputs.put(input.getName(), parse(dir, input)));
    run(dir, inputs);
  }

  Collection<Input<?>> getInputs();

  private <T extends NamedObject> List<T> parse(File dir, Input<T> input) {
    var parser = input.getFormat().newParser();
    return input.getGlob().matchingIn(dir).stream()
        .map(
            file -> {
              try {
                return parser.parse(file, input.getType());
              } catch (IOException e) {
                throw new IllegalArgumentException("Failed to parse " + file, e);
              }
            })
        .toList();
  }

  void run(File dir, ResolvedInputs inputs);
}
