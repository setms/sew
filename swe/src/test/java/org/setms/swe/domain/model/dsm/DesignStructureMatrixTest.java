package org.setms.swe.domain.model.dsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DesignStructureMatrixTest {

  private final DesignStructureMatrix<String> dsm = new DesignStructureMatrix<>("A", "B", "C");

  @Test
  void shouldRejectDependencyOnUnknownElement() {
    assertThatThrownBy(() -> dsm.addDependency("A", "Z"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> dsm.addDependency("Z", "A"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAddUnweightedDependency() {
    dsm.addDependency("A", "B");

    assertThat(dsm.getWeight("A", "B")).as("Existing dependency").isPresent().hasValue(1.0);
    assertThat(dsm.getWeight("B", "A")).as("Non-existing dependency").isEmpty();
  }

  @Test
  void shouldAddWeightedDependency() {
    dsm.addDependency("A", "B", 0.1);

    assertThat(dsm.getWeight("A", "B")).isPresent().hasValue(0.1);
  }

  @Test
  void shouldRepresentEmptyDsm() {
    assertThatDsmLooksLike(
        """
        |   | A | B | C |
        |---|---|---|---|
        | A | X |   |   |
        | B |   | X |   |
        | C |   |   | X |
        """);
  }

  private void assertThatDsmLooksLike(String expected) {
    assertThat(dsm).hasToString(expected);
  }

  @Test
  void shouldRepresentUnweightedDsm() {
    dsm.addDependency("A", "C");

    assertThatDsmLooksLike(
        """
        |   | A | B | C |
        |---|---|---|---|
        | A | X |   | X |
        | B |   | X |   |
        | C |   |   | X |
        """);
  }

  @Test
  void shouldRepresentWeightedDsm() {
    dsm.addDependency("A", "C", 0.5);

    assertThatDsmLooksLike(
        """
        |   | A | B | C |
        |---|---|---|---|
        | A | X |   | 0.5 |
        | B |   | X |   |
        | C |   |   | X |
        """);
  }
}
