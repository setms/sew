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
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.StandaloneTool;
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
  private final Tool tool;

  private final Class<? extends Format> formatType;
  private final String sourceLocation;
  private final String extension;
  private final File baseDir;

  protected ToolTestCase(Tool tool, Class<T> type, String sourceLocation) {
    this(tool, SalFormat.class, sourceLocation, type);
  }

  protected ToolTestCase(
      Tool tool, Class<? extends Format> formatType, String sourceLocation, Class<T> type) {
    this(tool, formatType, sourceLocation, initLower(type.getSimpleName()));
  }

  protected ToolTestCase(
      Tool tool, Class<? extends Format> formatType, String sourceLocation, String extension) {
    this(tool, formatType, sourceLocation, extension, new File("src/test/resources/" + extension));
  }

  @Test
  void shouldDefineInputs() {
    if (tool instanceof ArtifactTool artifactTool) {
      assertValidationTarget(artifactTool);
    }
    assertValidationContext(tool.validationContext());
    if (tool instanceof ArtifactTool artifactTool) {
      assertReportingTarget(artifactTool);
    }
    assertReportingContext(tool.reportingContext());
  }

  private void assertValidationTarget(ArtifactTool tool) {
    var input = tool.validationTarget();
    assertThat(input.path()).isEqualTo("src/%s".formatted(sourceLocation));
    assertThat(input.extension()).isEqualTo(extension);
    assertThat(input.format()).isInstanceOf(formatType);
  }

  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    // For descendants to override, if needed
  }

  private void assertReportingTarget(ArtifactTool tool) {
    tool.reportingTarget().ifPresent(this::assertReportingTarget);
  }

  protected void assertReportingTarget(Input<? extends Artifact> input) {
    // For descendants to override, if needed
  }

  protected void assertReportingContext(Set<Input<? extends Artifact>> inputs) {
    // For descendants to override, if needed
  }

  @Test
  void shouldValidate() throws IOException {
    var diagnostics = validateAgainst(workspaceFor("valid"));
    assertThat(diagnostics).isEmpty();
  }

  protected Workspace<?> workspaceFor(String path) {
    return new DirectoryWorkspace(getTestDir(path));
  }

  private File getTestDir(String name) {
    return new File(baseDir, name);
  }

  protected SequencedSet<Diagnostic> validateAgainst(Workspace<?> workspace) throws IOException {
    return switch (tool) {
      case ArtifactTool artifactTool -> validate(workspace, artifactTool);
      case StandaloneTool standaloneTool -> validate(workspace, standaloneTool);
    };
  }

  private SequencedSet<Diagnostic> validate(Workspace<?> workspace, ArtifactTool artifactTool)
      throws IOException {
    var input = artifactTool.validationTarget();
    var matchingObjects = workspace.root().matching(input.path(), input.extension());
    assertThat(matchingObjects).as("Missing objects at").isNotEmpty();
    var result = new LinkedHashSet<Diagnostic>();
    for (var source : matchingObjects) {
      var artifact = parse(source, input, true);
      assertThat(artifact).isNotNull();
      assertThatParsedObjectMatchesExpectations(artifact);

      artifactTool.validate(
          artifact, resolveValidationInputs(artifactTool, workspace.root(), result), result);
    }
    return result;
  }

  private Artifact parse(Resource<? extends Resource<?>> source, Input<?> input, boolean validate) {
    Artifact artifact;
    try (var sutStream = source.readFrom()) {
      artifact = input.format().newParser().parse(sutStream, input.type(), validate);
    } catch (IOException e) {
      return null;
    }
    return artifact;
  }

  protected void assertThatParsedObjectMatchesExpectations(Artifact artifact) {
    // Override to add assertions
  }

  private ResolvedInputs resolveValidationInputs(
      Tool tool, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    if (tool instanceof ArtifactTool artifactTool) {
      resolveInput(artifactTool.validationTarget(), resource, true, diagnostics, result);
    }
    tool.validationContext()
        .forEach(input -> resolveInput(input, resource, false, diagnostics, result));
    return result;
  }

  private void resolveInput(
      Input<? extends Artifact> input,
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
        .parseMatching(
            resource, input.path(), input.extension(), input.type(), validate, diagnostics)
        .toList();
  }

  private SequencedSet<Diagnostic> validate(Workspace<?> workspace, StandaloneTool tool) {
    var result = new LinkedHashSet<Diagnostic>();
    tool.validate(resolveValidationInputs(tool, workspace.root(), result), result);
    return result;
  }

  protected AppliedSuggestion apply(
      Suggestion suggestion, Diagnostic diagnostic, Workspace<?> workspace) {
    return apply(suggestion.code(), diagnostic.location(), workspace);
  }

  protected AppliedSuggestion apply(String code, Location location, Workspace<?> workspace) {
    var inputs = resolveValidationInputs(tool, workspace.root(), new LinkedHashSet<>());
    return switch (tool) {
      case ArtifactTool artifactTool ->
          artifactTool.applySuggestion(
              toArtifact(workspace, location, artifactTool.validationTarget()),
              code,
              location,
              inputs,
              workspace.root());
      case StandaloneTool standaloneTool ->
          standaloneTool.applySuggestion(code, location, inputs, workspace.root());
    };
  }

  private Artifact toArtifact(
      Workspace<?> workspace, Location location, Input<? extends Artifact> input) {
    return workspace.root().matching(input.path(), input.extension()).stream()
        .map(resource -> parse(resource, input, false))
        .filter(Objects::nonNull)
        .filter(artifact -> artifact.starts(location))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void shouldBuild() throws IOException {
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

  protected List<Diagnostic> build(Workspace<?> workspace) throws IOException {
    return switch (tool) {
      case ArtifactTool artifactTool -> build(workspace, artifactTool);
      case StandaloneTool standaloneTool -> build(workspace, standaloneTool);
    };
  }

  private List<Diagnostic> build(Workspace<?> workspace, ArtifactTool tool) throws IOException {
    var result = new ArrayList<Diagnostic>();
    var target = tool.reportingTarget();
    if (target.isEmpty()) {
      return result;
    }
    var resolvedInputs = resolveBuildInputs(workspace.root(), result);
    var output = workspace.root().select("build");
    var input = target.get();
    for (var resource : workspace.root().matching(input.path(), input.extension())) {
      var artifact = parse(resource, input, false);
      assertThat(artifact).isNotNull();
      tool.buildReportsFor(artifact, resolvedInputs, output, result);
    }
    return result;
  }

  private ResolvedInputs resolveBuildInputs(
      Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    tool.reportingContext()
        .forEach(input -> resolveInput(input, resource, false, diagnostics, result));
    return result;
  }

  private List<Diagnostic> build(Workspace<?> workspace, StandaloneTool tool) {
    var result = new ArrayList<Diagnostic>();
    tool.buildReports(
        resolveBuildInputs(workspace.root(), result), workspace.root().select("build"), result);
    return result;
  }
}
