package org.setms.km.domain.model.kmsystem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.io.InputStream;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.*;

@ExtendWith(MockitoExtension.class)
class KmSystemTest {

  private static final Parser PARSER = new TestParser();

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private KmSystem kmSystem;

  @Mock
  @SuppressWarnings("rawtypes")
  Resource resource;

  @Mock Workspace workspace;
  @Captor ArgumentCaptor<ArtifactChangedHandler> artifactChangedCaptor;
  @Captor ArgumentCaptor<ArtifactDefinition> artifactDefinitionCaptor;

  private final MainTool mainTool = new MainTool();
  private final OtherTool otherTool = new OtherTool();
  private final MainArtifact artifact = new MainArtifact(new FullyQualifiedName("ape.Bear"));

  @BeforeEach
  void init() {
    Tools.reload();
    Tools.add(mainTool);
    Tools.add(otherTool);
  }

  @Test
  void shouldValidateChangedArtifact() {
    givenRootResource();
    createKmSystem();
    verify(workspace).registerArtifactChangedHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();
    mainTool.validations.add(new Diagnostic(ERROR, "message"));

    handler.changed(artifact);

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isFalse();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isFalse();
  }

  @SuppressWarnings("unchecked")
  private void givenRootResource() {
    when(resource.matching(any(Glob.class))).thenReturn(emptyList());
    when(workspace.root()).thenReturn(resource);
  }

  private void createKmSystem() {
    kmSystem = new KmSystem((workspace));
  }

  @Test
  void shouldBuildValidChangedArtifact() {
    givenRootResource();
    createKmSystem();
    verify(workspace).registerArtifactChangedHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();

    handler.changed(artifact);

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isTrue();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isTrue();
  }

  @Test
  void shouldRegisterArtifactTypesInWorkspace() {
    createKmSystem();

    verify(workspace, atLeastOnce()).registerArtifactDefinition(artifactDefinitionCaptor.capture());
    assertThat(artifactDefinitionCaptor.getAllValues())
        .containsExactlyInAnyOrder(
            new ArtifactDefinition(
                MainArtifact.class, new Glob("main", "**/*.mainArtifact"), PARSER),
            new ArtifactDefinition(
                OtherArtifact.class, new Glob("other", "**/*.otherArtifact"), PARSER),
            new ArtifactDefinition(Bar.class, new Glob("bar", "**/*.bar"), null));
  }

  @Test
  void shouldUpdateCachedGlobsOnStartup() {
    // TODO: Fill workspace with artifacts
    createKmSystem();
  }

  private static class MainArtifact extends Artifact {

    public MainArtifact(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }

  private static class MainTool extends BaseTool {

    private boolean validated;
    private final SequencedSet<Diagnostic> validations = new LinkedHashSet<>();
    private boolean built;

    @Override
    public List<Input<?>> getInputs() {
      return List.of(new Input<>("main", new TestFormat(), MainArtifact.class));
    }

    @Override
    public Optional<Output> getOutputs() {
      return Optional.empty();
    }

    @Override
    public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
      diagnostics.addAll(validations);
      validated = true;
    }

    @Override
    public void build(
        ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
      built = true;
    }
  }

  private static class OtherArtifact extends Artifact {

    public OtherArtifact(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }

  private static class OtherTool extends BaseTool {

    private boolean validated;
    private boolean built;

    @Override
    public List<Input<?>> getInputs() {
      return List.of(
          new Input<>("other", new TestFormat(), OtherArtifact.class),
          new Input<>("main", new TestFormat(), MainArtifact.class));
    }

    @Override
    public Optional<Output> getOutputs() {
      return Optional.empty();
    }

    @Override
    public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
      validated = true;
    }

    @Override
    public void build(
        ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
      built = true;
    }
  }

  private static class TestFormat implements Format {

    @Override
    public Parser newParser() {
      return PARSER;
    }

    @Override
    public Builder newBuilder() {
      throw new UnsupportedOperationException("TestFormat.newBuilder");
    }
  }

  private static class TestParser implements Parser {

    @Override
    public RootObject parse(InputStream input) {
      return new RootObject("ape", MainArtifact.class.getSimpleName(), "Bear");
    }
  }
}
