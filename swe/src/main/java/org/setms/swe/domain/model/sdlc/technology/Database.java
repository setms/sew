package org.setms.swe.domain.model.sdlc.technology;

import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;

/** A database technology. */
public interface Database {

  /**
   * @return a SQL schema for the given entity's table
   */
  DatabaseSchema schemaFor(Entity entity);
}
