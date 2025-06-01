package org.setms.sew.core.domain.model.dsm;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ClusteringAlgorithmTest {

  private final DesignStructureMatrix<String> dsm =
      new DesignStructureMatrix<>("A", "B", "C", "E", "F", "G", "H", "I", "O", "P");

  @Test
  @SuppressWarnings("unchecked")
  void shouldFindClusters() {
    givenDependencies();

    var clusters =
        new StochasticGradientDescentClusteringAlgorithm<String>()
            .apply(dsm).stream().map(Set.class::cast).collect(toSet());

    assertThat(clusters).hasSize(3);
    Set.of(Set.of("A", "B"), Set.of("C", "G", "O", "P"), Set.of("E", "F", "H", "I"))
        .forEach(
            elements ->
                assertThat(
                        clusters.stream()
                            .filter(c -> c.size() == elements.size() && c.containsAll(elements))
                            .findFirst())
                    .as("Cluster with values " + elements)
                    .isPresent());
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
