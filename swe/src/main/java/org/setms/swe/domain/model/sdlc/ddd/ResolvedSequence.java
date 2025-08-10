package org.setms.swe.domain.model.sdlc.ddd;

import java.util.List;
import org.setms.km.domain.model.artifact.Artifact;

public record ResolvedSequence(List<Artifact> items) {}
