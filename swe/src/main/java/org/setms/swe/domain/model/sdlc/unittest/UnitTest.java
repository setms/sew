package org.setms.swe.domain.model.sdlc.unittest;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UnitTest extends CodeArtifact {

  public UnitTest(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
