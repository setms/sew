package org.setms.swe.domain.model.sdlc.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseTopicProviderTest {

  private final DatabaseTopicProvider provider = new DatabaseTopicProvider();

  @Test
  void shouldDeclareDatabaseTopic() {
    var actual = provider.topics();

    assertThat(actual)
        .as("Topics declared by DatabaseTopicProvider")
        .contains(DatabaseTopicProvider.TOPIC);
  }

  @Test
  void shouldAcceptPostgreSqlAsValidDatabaseChoice() {
    var actual = provider.isValidChoice(DatabaseTopicProvider.TOPIC, "PostgreSql");

    assertThat(actual).as("PostgreSql is a valid choice for the Database topic").isTrue();
  }
}
