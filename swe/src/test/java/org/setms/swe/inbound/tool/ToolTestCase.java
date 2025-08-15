package org.setms.swe.inbound.tool;

import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.format.Strings.initLower;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.swe.inbound.format.sal.SalFormat;

@RequiredArgsConstructor
abstract class ToolTestCase<T extends Artifact> {

  @Getter(PROTECTED)
  private final Tool<T> tool;

  private final Class<? extends Format> formatType;
  private final String sourceLocation;
  private final String extension;
  private final File baseDir;

  protected ToolTestCase(Tool<T> tool, Class<T> type, String sourceLocation) {
    this(tool, SalFormat.class, sourceLocation, type);
  }

  protected ToolTestCase(
      Tool<T> tool, Class<? extends Format> formatType, String sourceLocation, Class<T> type) {
    this(tool, formatType, sourceLocation, initLower(type.getSimpleName()));
  }

  protected ToolTestCase(
      Tool<T> tool, Class<? extends Format> formatType, String sourceLocation, String extension) {
    this(tool, formatType, sourceLocation, extension, new File("src/test/resources/" + extension));
  }

  @Test
  void shouldDefineInputs() {
    assertMainInput();
    assertAdditionalInputs();
  }

  private void assertMainInput() {
    var actual = tool.mainInput();
    if (actual.isPresent()) {
      var input = actual.get();
      assertThat(input.glob()).hasToString("src/%s/**/*.%s".formatted(sourceLocation, extension));
      assertThat(input.format()).isInstanceOf(formatType);
    }
  }

  private void assertAdditionalInputs() {
    assertInputs(tool.additionalInputs());
  }

  protected void assertInputs(Set<Input<?>> inputs) {
    // For descendants to override, if needed
  }

  @Test
  void shouldParseObject() throws IOException {
    var workspace = workspaceFor("valid");
    var maybeInput = tool.mainInput();
    if (maybeInput.isEmpty()) {
      return;
    }
    var input = maybeInput.get();
    var matchingObjects = workspace.root().matching(input.glob());
    assertThat(matchingObjects).as("Missing objects at").isNotEmpty();
    for (var source : matchingObjects) {
      try (var sutStream = source.readFrom()) {
        var parsed = input.format().newParser().parse(sutStream, input.type(), true);

        assertThat(parsed).isNotNull();
        assertThatParsedObjectMatchesExpectations(parsed);
      }
    }
  }

  private File getTestDir(String name) {
    return new File(baseDir, name);
  }

  protected void assertThatParsedObjectMatchesExpectations(T parsed) {
    // Override to add assertions
  }

  protected Workspace<?> workspaceFor(String path) {
    return new DirectoryWorkspace(getTestDir(path));
  }

  protected SequencedSet<Diagnostic> validateAgainst(Workspace<?> workspace) {
    var result = new LinkedHashSet<Diagnostic>();
    tool.validate(resolveInputs(workspace.root(), result), result);
    return result;
  }

  private ResolvedInputs resolveInputs(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    tool.mainInput().ifPresent(input -> resolveInput(input, resource, true, diagnostics, result));
    tool.additionalInputs()
        .forEach(input -> resolveInput(input, resource, false, diagnostics, result));
    return result;
  }

  private void resolveInput(
      Input<?> input,
      Resource<?> resource,
      boolean validate,
      Collection<Diagnostic> diagnostics,
      ResolvedInputs inputs) {
    inputs.put(input.name(), parse(resource, input, validate, diagnostics));
  }

  private <A extends Artifact> List<A> parse(
      Resource<?> resource, Input<A> input, boolean validate, Collection<Diagnostic> diagnostics) {
    return input
        .format()
        .newParser()
        .parseMatching(resource, input.glob(), input.type(), validate, diagnostics)
        .toList();
  }

  @Test
  void shouldBuild() {
    var workspace = workspaceFor("valid");
    var resource = workspace.root();
    var diagnostics = build(workspace);
    assertThat(diagnostics).as("Diagnostics").isEmpty();
    assertBuild(resource);
  }

  protected void assertBuild(Resource<?> resource) {
    // Override to add assertions
  }

  protected File toFile(Resource<?> resource) {
    return Files.get(resource.toUri().toString());
  }

  protected List<Diagnostic> build(Workspace<?> workspace) {
    var result = new ArrayList<Diagnostic>();
    tool.build(resolveInputs(workspace.root(), result), workspace.root().select("build"), result);
    return result;
  }

  protected AppliedSuggestion apply(
      Suggestion suggestion, Diagnostic diagnostic, Workspace<?> workspace) {
    return apply(suggestion.code(), diagnostic.location(), workspace);
  }

  protected AppliedSuggestion apply(String code, Location location, Workspace<?> workspace) {
    var inputs = resolveInputs(workspace.root(), new LinkedHashSet<>());
    return tool.apply(code, inputs, location, workspace.root());
  }
}
