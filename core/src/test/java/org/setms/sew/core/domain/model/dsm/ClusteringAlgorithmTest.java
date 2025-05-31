package org.setms.sew.core.domain.model.dsm;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ClusteringAlgorithmTest {

  private final DesignStructureMatrix<String> dsm =
      new DesignStructureMatrix<>("A", "B", "C", "E", "F", "G", "H", "I", "O", "P");

  @Test
  void shouldFindClusters() {
    givenDependencies();

    var clusters =
        new StochasticGradientDescentClusteringAlgorithm<String>()
            .apply(dsm).stream().map(Set.class::cast).collect(toSet());

    assertThat(clusters)
        .containsExactlyInAnyOrder(
            Set.of("A", "B"), Set.of("C", "G", "O", "P"), Set.of("E", "F", "H", "I"));
  }

  private void givenDependencies() {
    addDependency("B", "A");
    addDependency("E", "B");
    addDependency("F", "E");
    addDependency("H", "E");
    addDependency("H", "F");
    addDependency("I", "F");
    addDependency("I", "H");
    addDependency("P", "C");
    addDependency("P", "G");
    addDependency("P", "H");
    addDependency("P", "O");
  }

  private void addDependency(String from, String to) {
    dsm.addDependency(from, to);
    dsm.addDependency(to, from);
  }
}
