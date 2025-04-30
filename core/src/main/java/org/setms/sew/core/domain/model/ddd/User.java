package org.setms.sew.core.domain.model.ddd;

import org.setms.sew.core.domain.model.schema.FullyQualifiedName;

public class User extends Stakeholder {

  public User(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
