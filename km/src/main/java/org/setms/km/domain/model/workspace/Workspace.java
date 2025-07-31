package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.Artifact;

@Slf4j
public abstract class Workspace<T> {

  private final Collection<ArtifactDefinition> artifactDefinitions = new HashSet<>();
  private final Collection<ArtifactChangedHandler> artifactChangedHandlers = new ArrayList<>();
  private final Collection<ArtifactDeletedHandler> artifactDeletedHandlers = new ArrayList<>();

  private Resource<?> root;

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
    var resource = root().select(path);
    if (resource == null) {
      return null;
    }
    try (var stream = resource.readFrom()) {
      return definition.parser().parse(stream, definition.type(), false);
    } catch (IOException e) {
      return null;
    }
  }

  public void registerArtifactChangedHandler(ArtifactChangedHandler handler) {
    artifactChangedHandlers.add(handler);
  }

  protected void onChanged(String path, Artifact artifact) {
    artifactChangedHandlers.forEach(handler -> handler.changed(path, artifact));
  }

  public void registerArtifactDeletedHandler(ArtifactDeletedHandler handler) {
    artifactDeletedHandlers.add(handler);
  }

  protected void onDeleted(String path) {
    artifactDeletedHandlers.forEach(handler -> handler.deleted(path));
  }

  public Resource<?> root() {
    if (root == null) {
      root = newRoot();
    }
    return root;
  }

  protected abstract Resource<?> newRoot();

  public void close() throws IOException {}

  public abstract Resource<?> find(T external);
}
