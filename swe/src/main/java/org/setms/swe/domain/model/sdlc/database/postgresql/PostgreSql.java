package org.setms.swe.domain.model.sdlc.database.postgresql;

import java.util.stream.Collectors;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.technology.Database;

/** PostgreSQL database implementation. */
public class PostgreSql implements Database {

  @Override
  public DatabaseSchema schemaFor(Entity entity) {
    var result = new DatabaseSchema(new FullyQualifiedName(entity.getPackage(), entity.getName()));
    result.setCode(codeFor(entity));
    return result;
  }

  private String codeFor(Entity entity) {
    return """
    CREATE TABLE %s (
    %s
    );
    """
        .formatted(
            entity.getName().toLowerCase(),
            entity.getFields().stream().map(this::columnFor).collect(Collectors.joining(",\n")));
  }

  private String columnFor(Field field) {
    return "  %s %s".formatted(field.getName(), sqlTypeFor(field.getType()));
  }

  private String sqlTypeFor(FieldType type) {
    return switch (type) {
      case ID -> "UUID";
      case TEXT, SELECTION -> "VARCHAR(255)";
      case NUMBER -> "NUMERIC";
      case BOOLEAN -> "BOOLEAN";
      case DATE -> "DATE";
      case TIME -> "TIME";
      case DATETIME -> "TIMESTAMPTZ";
    };
  }
}
