package org.setms.swe.e2e;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Iteration {

  private File directory;
  private List<Input> inputs;
  private List<String> diagnostics;
  private List<String> outputs;

  public Iteration validate() {
    validateInputsExist();
    validateOutputsExist();
    validateNoUndefinedInputsExist();
    validateNoUndefinedOutputsExist();
    return this;
  }

  private void validateInputsExist() {
    if (inputs == null) {
      return;
    }
    var missingInputs =
        inputs.stream()
            .filter(input -> !input.resolve(directory).isFile())
            .map(input -> "%s/inputs/%s".formatted(directory.getName(), input))
            .collect(joining(", "));
    if (!missingInputs.isEmpty()) {
      throw new IllegalStateException("Missing input files: " + missingInputs);
    }
  }

  private void validateOutputsExist() {
    if (outputs == null) {
      return;
    }
    var missingOutputs =
        outputs.stream()
            .map(output -> output.substring(output.lastIndexOf("/") + 1))
            .filter(
                output -> !directory.toPath().resolve("outputs").resolve(output).toFile().isFile())
            .map(output -> "%s/outputs/%s".formatted(directory.getName(), output))
            .collect(joining(", "));
    if (!missingOutputs.isEmpty()) {
      throw new IllegalStateException("Missing output files: " + missingOutputs);
    }
  }

  private void validateNoUndefinedInputsExist() {
    var expected = Optional.ofNullable(inputs).map(Collection::size).orElse(0);
    var actual =
        Optional.of(new File(directory, "inputs"))
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles().length)
            .orElse(0);
    if (!expected.equals(actual)) {
      throw new IllegalStateException(
          "Expected %d input files in %s, but got %d"
              .formatted(expected, directory.getName(), actual));
    }
  }

  private void validateNoUndefinedOutputsExist() {
    var expected = Optional.ofNullable(outputs).map(Collection::size).orElse(0);
    var actual =
        Optional.of(new File(directory, "outputs"))
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles().length)
            .orElse(0);
    if (!expected.equals(actual)) {
      throw new IllegalStateException(
          "Expected %d output files in %s, but got %d"
              .formatted(expected, directory.getName(), actual));
    }
  }

  @Override
  public String toString() {
    return Optional.ofNullable(directory).map(File::getName).orElse("???");
  }
}
