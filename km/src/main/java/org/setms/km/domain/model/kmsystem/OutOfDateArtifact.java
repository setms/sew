package org.setms.km.domain.model.kmsystem;

import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.BaseTool;

public record OutOfDateArtifact<T extends Artifact>(
    String path, Class<T> artifactType, Optional<BaseTool<T>> maybeTool) {}
