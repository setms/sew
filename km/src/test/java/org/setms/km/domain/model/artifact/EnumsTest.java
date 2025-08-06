package org.setms.km.domain.model.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnumsTest {

  @Test
  void shouldCreateTypedCollection() {
    var actual = Enums.of(Item.class);
    assertThat(actual.getType()).isEqualTo(Item.class);
  }

  @Test
  void shouldAddEnums() {
    var actual = Enums.of(Item.ONE, Item.THREE);

    assertThat(actual).hasSize(2).doesNotContain(Item.TWO);
  }

  public enum Item {
    ONE,
    TWO,
    THREE
  }
}
