package org.setms.km.domain.model.kmsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.ArtifactChangedHandler;
import org.setms.km.domain.model.workspace.Workspace;

@ExtendWith(MockitoExtension.class)
class KmSystemTest {

  @Mock Workspace workspace;
  @InjectMocks KmSystem kmSystem;
  @Captor ArgumentCaptor<ArtifactChangedHandler> artifactChangedCaptor;
  private final MainTool mainTool = new MainTool();
  private final OtherTool otherTool = new OtherTool();
  private final MainArtifact artifact = new MainArtifact(new FullyQualifiedName("ape.Bear"));

  @BeforeEach
  void init() {
    ToolRegistry.reload();
    ToolRegistry.add(mainTool);
    ToolRegistry.add(otherTool);
  }

  @Test
  void shouldValidateChangedArtifact() {
    verify(workspace).registerChangeHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();
    mainTool.validations.add(new Diagnostic(ERROR, "message"));

    handler.changed(artifact);

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isFalse();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isFalse();
  }

  @Test
  void shouldBuildValidChangedArtifact() {
    verify(workspace).registerChangeHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();

    handler.changed(artifact);

    assertThat(mainTool.validated).as("main validated").isTrue();
    assertThat(mainTool.built).as("main built").isTrue();
    assertThat(otherTool.validated).as("other validated").isFalse();
    assertThat(otherTool.built).as("other built").isTrue();
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
      return List.of(new Input<>("fake", null, MainArtifact.class));
    }

    @Override
    public Optional<Output> getOutputs() {
      return Optional.empty();
    }

    @Override
    public SequencedSet<Diagnostic> validate(Workspace workspace) {
      validated = true;
      return validations;
    }

    @Override
    public List<Diagnostic> build(Workspace workspace) {
      built = true;
      return new ArrayList<>();
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
          new Input<>("foo", null, OtherArtifact.class),
          new Input<>("bar", null, MainArtifact.class));
    }

    @Override
    public Optional<Output> getOutputs() {
      return Optional.empty();
    }

    @Override
    public SequencedSet<Diagnostic> validate(Workspace workspace) {
      validated = true;
      return new LinkedHashSet<>();
    }

    @Override
    public List<Diagnostic> build(Workspace workspace) {
      built = true;
      return new ArrayList<>();
    }
  }
}
