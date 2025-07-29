package org.setms.km.domain.model.workspace;

@FunctionalInterface
public interface ArtifactDeletedHandler {

  void deleted(String path);
}
