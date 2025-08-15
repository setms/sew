package org.setms.km.domain.model.kmsystem;

import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Tool;

public record OutOfDateArtifact<T extends Artifact>(
    String path, Class<T> artifactType, Optional<Tool<T>> maybeTool) {}
