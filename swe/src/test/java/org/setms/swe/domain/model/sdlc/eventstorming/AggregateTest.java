package org.setms.swe.domain.model.sdlc.eventstorming;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

class AggregateTest {

  @Test
  void shouldReturnRootEntityNameAsDomainObjectName() {
    var aggregate =
        new Aggregate(new FullyQualifiedName("design", "TodoItems"))
            .setRoot(new Link("entity", "TodoItem"));

    var actual = aggregate.domainObjectName();

    assertThat(actual)
        .as("Domain object name should be the root entity name when a root entity is set")
        .isEqualTo("TodoItem");
  }

  @Test
  void shouldReturnAggregateNameAsDomainObjectNameWhenNoRootEntity() {
    var aggregate = new Aggregate(new FullyQualifiedName("design", "TodoItems"));

    var actual = aggregate.domainObjectName();

    assertThat(actual)
        .as("Domain object name should be the aggregate name when no root entity is set")
        .isEqualTo("TodoItem");
  }
}
