package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Optional;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;

/** A database technology. */
public interface Database {

  /**
   * @return a schema that describes persistence for the given entity
   */
  DatabaseSchema schemaFor(Entity entity);

  /**
   * @return the fields in the schema (could be empty for schema-less databases)
   */
  Collection<Field> extractFieldsFrom(DatabaseSchema schema);

  /**
   * @return the local JDBC data source URL for the given database name, if applicable
   */
  default Optional<String> localDataSourceUrl(String databaseName) {
    return Optional.empty();
  }
}
