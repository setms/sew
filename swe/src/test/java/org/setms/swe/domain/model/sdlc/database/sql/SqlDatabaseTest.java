package org.setms.swe.domain.model.sdlc.database.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlDatabaseTest {

  @Test
  void shouldExtractTableNameFromCreateTableSql() {
    var sql =
        """
        CREATE TABLE product (
          id UUID,
          name VARCHAR(255)
        );
        """;

    var actual = new SqlDatabase().extractName(sql);

    assertThat(actual).as("Name extracted from CREATE TABLE SQL").isNotNull();
    assertThat(actual.getName()).as("Table name from SQL").isEqualTo("product");
  }

  @Test
  void shouldProvideGlobForSqlFiles() {
    var actual = new SqlDatabase().databaseSchemas();

    assertThat(actual)
        .as("Database schema globs for SQL files")
        .hasSize(1)
        .allSatisfy(glob -> assertThat(glob.extension()).as("SQL file extension").isEqualTo("sql"));
  }
}
