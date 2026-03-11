package org.setms.swe.domain.model.sdlc.database.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlDatabaseTest {

  @Test
  void shouldProvideGlobForSqlFiles() {
    var actual = new SqlDatabase().databaseSchemas();

    assertThat(actual)
        .as("Database schema globs for SQL files")
        .hasSize(1)
        .allSatisfy(glob -> assertThat(glob.extension()).as("SQL file extension").isEqualTo("sql"));
  }
}
