package org.setms.swe.domain.model.sdlc.acceptancetest;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

public abstract class Scenario extends Artifact {

  public Scenario(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
