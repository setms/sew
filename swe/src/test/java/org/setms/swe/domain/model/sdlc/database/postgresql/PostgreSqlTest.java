package org.setms.swe.domain.model.sdlc.database.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.design.FieldConstraint.NONEMPTY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Enums;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.technology.Database;

class PostgreSqlTest {

  private static final String PRODUCT_TABLE =
      """
  CREATE TABLE product (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC
  );
  """;

  private final Database database = new PostgreSql();

  @Test
  void shouldGenerateSqlCreateTableFromEntity() {
    var entity = newProductEntity();

    var actual = database.schemaFor(entity);

    assertThat(actual).as("Schema for Product entity").isNotNull();
    assertThat(actual.getCode())
        .as("SQL CREATE TABLE for Product with id and name fields")
        .isEqualTo(PRODUCT_TABLE);
  }

  @Test
  void shouldAddIdColumnWhenEntityHasNoIdField() {
    var entity = newProductEntityWithoutId();

    var actual = database.schemaFor(entity);

    assertThat(actual.getCode())
        .as("SQL CREATE TABLE for Product without id field should still include id UUID column")
        .isEqualTo(
            """
            CREATE TABLE product (
              id UUID PRIMARY KEY,
              name VARCHAR(255) NOT NULL
            );
            """);
  }

  private Entity newProductEntityWithoutId() {
    return new Entity(new FullyQualifiedName("shop", "Product"))
        .setFields(List.of(newField("Name", FieldType.TEXT, false)));
  }

  private Entity newProductEntity() {
    return new Entity(new FullyQualifiedName("shop", "Product")).setFields(productFields());
  }

  private List<Field> productFields() {
    return List.of(
        newField("ID", FieldType.ID, false),
        newField("Name", FieldType.TEXT, false),
        newField("Price", FieldType.NUMBER, true));
  }

  private Field newField(String name, FieldType type, boolean nullable) {
    var result = new Field(new FullyQualifiedName("shop.Product", name)).setType(type);
    if (!nullable) {
      result.setConstraints(Enums.of(NONEMPTY));
    }
    return result;
  }

  @Test
  void shouldExtractFields() {
    var schema = new DatabaseSchema(new FullyQualifiedName("shop", "Product"));
    schema.setCode(PRODUCT_TABLE);

    var actual = database.extractFieldsFrom(schema);

    assertThat(actual).as("Extracted fields").isEqualTo(productFields());
  }
}
