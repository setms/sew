package org.setms.swe.domain.model.sdlc.database;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;

/** A SQL schema for a database table. */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DatabaseSchema extends CodeArtifact {

  public DatabaseSchema(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
