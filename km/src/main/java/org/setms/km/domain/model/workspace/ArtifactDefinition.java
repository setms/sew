package org.setms.km.domain.model.workspace;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Parser;

public record ArtifactDefinition(Class<? extends Artifact> type, Glob glob, Parser parser) {}
