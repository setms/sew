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
  private final FakeTool tool = new FakeTool();
  private final FakeArtifact artifact = new FakeArtifact(new FullyQualifiedName("ape.Bear"));

  @BeforeEach
  void init() {
    ToolRegistry.add(tool);
  }

  @Test
  void shouldValidateChangedArtifact() {
    verify(workspace).registerChangeHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();
    tool.validations.add(new Diagnostic(ERROR, "message"));

    handler.changed(artifact);

    assertThat(tool.validated).isTrue();
    assertThat(tool.built).isFalse();
  }

  @Test
  void shouldBuildValidChangedArtifact() {
    verify(workspace).registerChangeHandler(artifactChangedCaptor.capture());
    var handler = artifactChangedCaptor.getValue();

    handler.changed(artifact);

    assertThat(tool.validated).isTrue();
    assertThat(tool.built).isTrue();
  }

  private static class FakeArtifact extends Artifact {

    public FakeArtifact(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }

  private static class FakeTool extends BaseTool {

    private boolean validated;
    private final SequencedSet<Diagnostic> validations = new LinkedHashSet<>();
    private boolean built;

    @Override
    public List<Input<?>> getInputs() {
      return List.of(new Input<>("fake", null, FakeArtifact.class));
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
}
