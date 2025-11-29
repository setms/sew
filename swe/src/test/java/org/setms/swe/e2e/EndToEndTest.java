package org.setms.swe.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.setms.km.domain.model.file.Files.childrenOf;
import static org.setms.km.domain.model.format.Strings.NL;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.validation.Validation.validate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.orchestration.ProcessOrchestrator;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Tools;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.yaml.snakeyaml.Yaml;

class EndToEndTest {

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

  private final Collection<String> created = new HashSet<>();
  private final File root = new File("build/e2e");
  private Workspace<?> workspace;
  private ProcessOrchestrator kmSystem;
  private final Chat chat = new Chat("Human", "SEW");

  @BeforeEach
  void init() {
    Files.delete(root);
    workspace = new DirectoryWorkspace(root);
    kmSystem = new ProcessOrchestrator(workspace);

    // Force initialization of Hibernate, to keep its output from interfering with our chat output
    validate(new Object());
  }

  @Test
  void shouldGuideSoftwareEngineering() throws IOException {
    for (var iteration : loadIterations()) {
      chat.topic(iteration.getDirectory().getName());
      assertThatIterationIsCorrect(iteration);
    }
    chat.topic("The End");
    kmSystem
        .diagnosticsWithSuggestions()
        .forEach(diagnostic -> System.out.printf("%s%n", diagnostic));
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
    assertThatOutputsWereCreatedFor(iteration);
    copyInputsFrom(iteration);
    assertThatDiagnosticsMatch(iteration);
  }

  private void assertThatOutputsWereCreatedFor(Iteration iteration) {
    for (var output : listOf(iteration.getOutputs())) {
      var actual = readText(workspace.root().select(output), Resource::readFrom);
      assertThat(actual).as("Missing " + output).isNotNull();
      var expected =
          readText(
              new File(
                  iteration.getDirectory(),
                  "outputs/%s".formatted(output.substring(1 + output.lastIndexOf("/")))),
              FileInputStream::new);
      assertThat(actual).as(output).isEqualTo(expected);
      chat.add(false, "Created " + output);
    }
  }

  private <T> List<T> listOf(List<T> values) {
    return Optional.ofNullable(values).orElseGet(Collections::emptyList);
  }

  private <T> String readText(T source, InputStreamProvider<T> toInputStream) {
    try (var reader = new BufferedReader(new InputStreamReader(toInputStream.apply(source)))) {
      return reader.lines().collect(joining(NL));
    } catch (Exception e) {
      return null;
    }
  }

  private void copyInputsFrom(Iteration iteration) throws IOException {
    for (var input : listOf(iteration.getInputs())) {
      try (var source =
          new FileInputStream(
              new File(iteration.getDirectory(), "inputs/%s".formatted(input.getFile())))) {
        try (var target =
            workspace.root().select(input.getLocation()).select(input.getFile()).writeTo()) {
          source.transferTo(target);
        }
      }
      chat.add(
          true,
          "%s %s/%s"
              .formatted(
                  created.contains(input.getFile()) ? "Updated" : "Created",
                  input.getLocation(),
                  input.getFile()));
    }
  }

  private void assertThatDiagnosticsMatch(Iteration iteration) {
    var expected = listOf(iteration.getDiagnostics());
    await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                assertThat(kmSystem.diagnosticsWithSuggestions())
                    .as(
                        "Diagnostics for iteration %s"
                            .formatted(iteration.getDirectory().getName()))
                    .map(Diagnostic::message)
                    .containsExactlyInAnyOrderElementsOf(expected));

    var diagnostics = kmSystem.diagnosticsWithSuggestions();
    var allDiagnostics = kmSystem.diagnostics();
    allDiagnostics.removeAll(diagnostics);
    assertThat(allDiagnostics).as("Diagnostics without suggestions").isEmpty();
    diagnostics.forEach(
        diagnostic ->
            chat.add(
                false,
                "Found issue `%s` in %s".formatted(diagnostic.message(), diagnostic.location())));
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
                    applied.createdOrChanged().stream().map(Resource::name).forEach(created::add);
                    chat.add(
                        true,
                        "Applied suggestion `%s` to %s"
                            .formatted(suggestion.message(), diagnostic.location()));
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
        .filter(ArtifactTool.class::isInstance)
        .map(ArtifactTool.class::cast)
        .map(ArtifactTool::validationTarget)
        .filter(input -> type.equals(initLower(input.type().getSimpleName())))
        .findFirst()
        .map(input -> "%s/%s.%s".formatted(input.path(), name, input.extension()))
        .orElse("/");
  }
}
