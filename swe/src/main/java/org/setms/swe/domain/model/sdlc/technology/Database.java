package org.setms.swe.domain.model.sdlc.technology;

import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;

/** A database technology. */
public interface Database {

  /**
   * @return a schema that describes persistence for the given entity
   */
  DatabaseSchema schemaFor(Entity entity);
}
