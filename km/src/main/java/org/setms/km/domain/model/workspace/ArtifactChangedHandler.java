package org.setms.km.domain.model.workspace;

import org.setms.km.domain.model.artifact.Artifact;

@FunctionalInterface
public interface ArtifactChangedHandler {

  void changed(String path, Artifact artifact);
}
