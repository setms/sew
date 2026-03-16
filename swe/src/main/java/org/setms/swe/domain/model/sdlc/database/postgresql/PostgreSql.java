package org.setms.swe.domain.model.sdlc.database.postgresql;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.format.Strings.toPascalCase;
import static org.setms.km.domain.model.format.Strings.toSnakeCase;
import static org.setms.swe.domain.model.sdlc.design.FieldConstraint.NONEMPTY;
import static org.setms.swe.domain.model.sdlc.design.FieldType.BOOLEAN;
import static org.setms.swe.domain.model.sdlc.design.FieldType.DATE;
import static org.setms.swe.domain.model.sdlc.design.FieldType.DATETIME;
import static org.setms.swe.domain.model.sdlc.design.FieldType.ID;
import static org.setms.swe.domain.model.sdlc.design.FieldType.NUMBER;
import static org.setms.swe.domain.model.sdlc.design.FieldType.TEXT;
import static org.setms.swe.domain.model.sdlc.design.FieldType.TIME;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Enums;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.technology.Database;

/** PostgreSQL database implementation. */
public class PostgreSql implements Database {

  private static final Pattern COLUMN =
      Pattern.compile(
          "(?<name>[a-z_]+)\\s+(?<type>[A-Z()\\d]+)\\s*(?<required>NOT NULL)?\\s*(PRIMARY KEY)?");

  @Override
  public DatabaseSchema schemaFor(Entity entity) {
    var result = new DatabaseSchema(new FullyQualifiedName(entity.getPackage(), entity.getName()));
    result.setCode(createTableScriptFor(entity));
    return result;
  }

  private String createTableScriptFor(Entity entity) {
    return """
    CREATE TABLE %s (
    %s
    );
    """
        .formatted(toSnakeCase(entity.getName()), columnsFor(entity));
  }

  private String columnsFor(Entity entity) {
    var fields = entity.getFields();
    var hasIdField = fields.stream().anyMatch(f -> toSnakeCase(f.getName()).equals("id"));
    var idColumn = hasIdField ? Stream.<String>empty() : Stream.of("  id UUID PRIMARY KEY");
    return Stream.concat(idColumn, fields.stream().map(this::columnFor))
        .collect(Collectors.joining(",\n"));
  }

  private String columnFor(Field field) {
    return "  %s %s%s%s"
        .formatted(
            toSnakeCase(field.getName()),
            sqlTypeFor(field.getType()),
            allowsNull(field) ? "" : " NOT NULL",
            field.getType() == ID ? " PRIMARY KEY" : "");
  }

  private boolean allowsNull(Field field) {
    return field.getConstraints() == null || !field.getConstraints().contains(NONEMPTY);
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

  @Override
  public Optional<String> localDataSourceUrl(String databaseName) {
    return Optional.of("jdbc:postgresql://localhost:5432/" + databaseName);
  }

  @Override
  public Optional<String> driverDependency() {
    return Optional.of("org.postgresql:postgresql");
  }

  @Override
  public Collection<Field> extractFieldsFrom(DatabaseSchema schema) {
    var script = schema.getCode();
    var index = script.indexOf("CREATE TABLE ");
    if (index < 0) {
      return emptyList();
    }
    script = script.substring(script.indexOf("(", index + 1) + 1, script.lastIndexOf(')')).trim();
    return Arrays.stream(script.split(","))
        .map(String::trim)
        .map(column -> toField(column, schema.getPackage(), schema.getName()))
        .filter(Objects::nonNull)
        .toList();
  }

  private Field toField(String column, String packageName, String tableName) {
    var matcher = COLUMN.matcher(column);
    if (!matcher.matches()) {
      return null;
    }
    var name = toPascalCase(matcher.group("name"));
    var result =
        new Field(new FullyQualifiedName("%s.%s.%s".formatted(packageName, tableName, name)));
    result.setType(toFieldType(matcher.group("type")));
    if (matcher.group("required") != null) {
      result.setConstraints(Enums.of(NONEMPTY));
    }
    return result;
  }

  private FieldType toFieldType(String sqlType) {
    return switch (sqlType) {
      case "UUID" -> ID;
      case "VARCHAR(255)" -> TEXT;
      case "NUMERIC" -> NUMBER;
      case "BOOLEAN" -> BOOLEAN;
      case "DATE" -> DATE;
      case "TIME" -> TIME;
      case "TIMESTAMPTZ" -> DATETIME;
      default -> null;
    };
  }
}
