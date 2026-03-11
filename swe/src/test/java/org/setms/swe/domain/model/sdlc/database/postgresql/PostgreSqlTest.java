package org.setms.swe.domain.model.sdlc.database.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;

class PostgreSqlTest {

  @Test
  void shouldGenerateSqlCreateTableFromEntity() {
    var entity = newProductEntity();

    var actual = new PostgreSql().schemaFor(entity);

    assertThat(actual).as("Schema for Product entity").isNotNull();
    assertThat(actual.getCode())
        .as("SQL CREATE TABLE for Product with id and name fields")
        .isEqualTo(
            """
            CREATE TABLE product (
              id UUID,
              name VARCHAR(255)
            );
            """);
  }

  private Entity newProductEntity() {
    return new Entity(new FullyQualifiedName("shop", "Product"))
        .setFields(List.of(newField("id", FieldType.ID), newField("name", FieldType.TEXT)));
  }

  private Field newField(String name, FieldType type) {
    return new Field(new FullyQualifiedName("shop.Product", name)).setType(type);
  }
}
