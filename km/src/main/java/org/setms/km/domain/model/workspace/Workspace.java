package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.Artifact;

@Slf4j
public abstract class Workspace {

  private final Collection<ArtifactDefinition> artifactDefinitions = new HashSet<>();
  private final Collection<ArtifactChangedHandler> artifactChangedHandlers = new ArrayList<>();

  private InputSource input;
  private OutputSink output;
  private Resource root;

  public void registerArtifactDefinition(ArtifactDefinition definition) {
    artifactDefinitions.add(definition);
  }

  protected Optional<? extends Artifact> parse(String path) {
    return artifactDefinitions.stream()
        .filter(type -> type.glob().matches(path))
        .map(type -> parse(path, type))
        .filter(Objects::nonNull)
        .findFirst();
  }

  private Artifact parse(String path, ArtifactDefinition definition) {
    try (var stream = input().select(path).open()) {
      return definition.parser().parse(stream, definition.type(), false);
    } catch (IOException e) {
      return null;
    }
  }

  public void registerArtifactChangedHandler(ArtifactChangedHandler handler) {
    artifactChangedHandlers.add(handler);
  }

  protected void onChanged(Artifact artifact) {
    artifactChangedHandlers.forEach(handler -> handler.changed(artifact));
  }

  public InputSource input() {
    if (input == null) {
      input = newInputSource();
    }
    return input;
  }

  protected abstract InputSource newInputSource();

  public OutputSink output() {
    if (output == null) {
      output = newOutputSink();
    }
    return output;
  }

  protected abstract OutputSink newOutputSink();

  public Resource root() {
    if (root == null) {
      root = newRoot();
    }
    return root;
  }

  protected abstract Resource newRoot();

  public void close() throws IOException {}
}
