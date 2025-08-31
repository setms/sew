package org.setms.swe.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.setms.km.domain.model.file.Files.childrenOf;
import static org.setms.km.domain.model.format.Strings.initLower;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.tool.Tools;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.yaml.snakeyaml.Yaml;

class EndToEndTest {

  private static final String NL = System.lineSeparator();

  /*
   * Software engineering happens in iterations:
   * 1. Human creates initial artifact.
   * 2. SEW validates artifact and writes diagnostics.
   * 3. If there are no diagnostics, end.
   * 4. Else, human reads diagnostics and applies suggestions and/or edits artifacts.
   * 5. Go to step 2 to start new iteration.
   *
   * This end-to-end test plays the role of the human software engineer:
   * 1. Create initial artifact.
   * 2. Wait for expected diagnostics, fail if that takes too long or the wrong diagnostics appear.
   * 3. Potentially edit an artifact.
   * 4. Apply all suggestions for all diagnostics, if any. Otherwise, end.
   * 5. Go to step 2 to start a new iteration.
   */

  private final File root = new File("build/e2e");
  private Workspace<?> workspace;
  private KmSystem kmSystem;

  @BeforeEach
  void init() {
    Files.delete(root);
    workspace = new DirectoryWorkspace(root);
    kmSystem = new KmSystem(workspace);
  }

  @Test
  void shouldGuideSoftwareEngineering() throws IOException {
    for (var iteration : loadIterations()) {
      System.out.printf("Iteration %s%n", iteration);
      assertThatIterationIsCorrect(iteration);
    }
  }

  private List<Iteration> loadIterations() {
    return childrenOf(new File("src/test/resources/e2e"))
        .sorted()
        .map(this::parseIteration)
        .toList();
  }

  private Iteration parseIteration(File directory) {
    try (var input = new FileInputStream(new File(directory, "iteration.yaml"))) {
      var result = new Yaml().loadAs(input, Iteration.class);
      result.setDirectory(directory);
      return result;
    } catch (IOException e) {
      throw new AssertionError(e.getMessage());
    }
  }

  private void assertThatIterationIsCorrect(Iteration iteration) throws IOException {
    if (iteration.getOutputs() != null) {
      assertThatOutputsWereCreated(iteration);
    }
    if (iteration.getInputs() != null) {
      copyInputs(iteration);
    }
    if (iteration.getDiagnostics() != null) {
      assertThatDiagnosticsMatch(iteration);
    }
  }

  private void assertThatOutputsWereCreated(Iteration iteration) throws IOException {
    for (var output : iteration.getOutputs()) {
      var actual = readText(workspace.root().select(output), Resource::readFrom);
      var expected =
          readText(
              new File(
                  iteration.getDirectory(),
                  "outputs/%s".formatted(output.substring(1 + output.lastIndexOf("/")))),
              FileInputStream::new);
      assertThat(actual).as(output).isEqualTo(expected);
      System.out.printf("- Verified output %s%n", output);
    }
  }

  private <T> String readText(T source, InputStreamProvider<T> toInputStream) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(toInputStream.apply(source)))) {
      return reader.lines().collect(joining(NL));
    }
  }

  private void copyInputs(Iteration iteration) throws IOException {
    for (var input : iteration.getInputs()) {
      try (var source =
          new FileInputStream(
              new File(iteration.getDirectory(), "inputs/%s".formatted(input.getFile())))) {
        try (var target =
            workspace.root().select(input.getLocation()).select(input.getFile()).writeTo()) {
          source.transferTo(target);
        }
      }
      System.out.printf("- Created input %s%n", input.getFile());
    }
  }

  private void assertThatDiagnosticsMatch(Iteration iteration) {
    var expected = iteration.getDiagnostics();
    await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                assertThat(kmSystem.diagnosticsWithSuggestions())
                    .as(
                        "# diagnostics for iteration %s"
                            .formatted(iteration.getDirectory().getName()))
                    .hasSize(expected.size()));

    var diagnostics = kmSystem.diagnosticsWithSuggestions();
    var actual = diagnostics.stream().map(Diagnostic::message).sorted().toList();
    assertThat(actual)
        .as("Diagnostics for iteration %s".formatted(iteration.getDirectory().getName()))
        .isEqualTo(expected);

    diagnostics.forEach(
        diagnostic -> {
          var resource = workspace.root().select(toPath(diagnostic.location()));
          diagnostic
              .suggestions()
              .forEach(
                  suggestion -> {
                    var applied =
                        kmSystem.applySuggestion(
                            resource, suggestion.code(), diagnostic.location());
                    assertThat(applied.diagnostics())
                        .as(
                            "Diagnostics for applying suggestion %s at %s"
                                .formatted(suggestion.code(), diagnostic.location()))
                        .isEmpty();
                    System.out.printf(
                        "- Applied suggestion %s at %s%n",
                        suggestion.code(), diagnostic.location());
                  });
        });
  }

  private String toPath(Location location) {
    var segments = location.segments();
    if (segments.size() < 3) {
      System.err.printf("Unknown path for location %s%n", location);
      return "/";
    }
    var type = segments.get(1);
    var name = segments.get(2);
    return Tools.all()
        .map(Tool::mainInput)
        .flatMap(Optional::stream)
        .filter(input -> type.equals(initLower(input.type().getSimpleName())))
        .map(Input::glob)
        .findFirst()
        .map(glob -> "%s/%s%s".formatted(glob.path(), name, glob.extension()))
        .orElse("/");
  }
}
