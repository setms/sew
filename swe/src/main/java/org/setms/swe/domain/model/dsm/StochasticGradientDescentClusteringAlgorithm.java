package org.setms.swe.domain.model.dsm;

import java.util.Collection;
import java.util.HashSet;

public class StochasticGradientDescentClusteringAlgorithm<E> extends BaseClusteringAlgorithm<E> {

  private static final int MAX_SUBSET_SIZE = 5;

  /*
   * How heavy to penalize bigger clusters. It should be bigger than 1, or else a supercluster
   * results containing all elements.
   */
  private static final double DISTANCE_POWER = 1.5;

  private Collection<Dependency<E>> dependencies;

  /*
   * We're using stochastic gradient descent, which is a greedy hillclimbing algorithm.
   * The stochastic variant scales better wrt DSM size, because it selects from a subset of all elements.
   */
  protected void optimizeClusters() {
    dependencies = getDependencies();
    var currentCost = calculateCost();
    var maxNoImprovements = Math.ceilDiv(2 * getElements().size(), MAX_SUBSET_SIZE);
    var noImprovement = 0;
    while (noImprovement < maxNoImprovements) {
      var bestMove = Move.<E>none(currentCost);
      var candidates = randomElements();
      for (var candidate : candidates) {
        var currentCluster = clusterOf(candidate);
        for (var cluster : getClusters().all()) {
          if (cluster.equals(currentCluster)) {
            continue;
          }
          getClusters().move(candidate, cluster);
          bestMove = bestMove.or(candidate, cluster, calculateCost());
          getClusters().move(candidate, currentCluster);
        }
      }
      if (bestMove.cost() < currentCost) {
        getClusters().move(bestMove.element(), bestMove.target());
        currentCost = bestMove.cost();
      } else {
        noImprovement++;
      }
    }
  }

  private Collection<E> randomElements() {
    if (getElements().size() <= MAX_SUBSET_SIZE) {
      return getElements();
    }
    var result = new HashSet<E>();
    while (result.size() < MAX_SUBSET_SIZE) {
      result.add(randomElement());
    }
    return result;
  }

  private double calculateCost() {
    // PERFORMANCE: Since most DSMs are sparse, it makes sense to iterate over dependencies rather
    // than elements
    return dependencies.stream().mapToDouble(this::costOf).sum();
  }

  /*
   * See Balancing Coupling In Software Design by Vlad Khononov:
   * Maintenance effort = strength * distance * volatility.
   *
   * - Strength is simply the weight of the dependency.
   * - Distance depends on whether the two elements are in the same or in different clusters. We take the size of the
   *   cluster to a power > 1 to penalize larger clusters.
   * - Volatility is unknown at this point, so assume it's the same for all clusters for now and leave it out of scope.
   */
  private double costOf(Dependency<E> dependency) {
    var fromCuster = clusterOf(dependency.from());
    var toCluster = clusterOf(dependency.to());
    var distance = fromCuster.equals(toCluster) ? fromCuster.size() : getElements().size();
    return dependency.weight() * Math.pow(distance, DISTANCE_POWER);
  }

  private record Move<E>(E element, Cluster<E> target, double cost) {

    public static <E> Move<E> none(double cost) {
      return new Move<>(null, null, cost);
    }

    public Move<E> or(E candidate, Cluster<E> cluster, double cost) {
      return cost < this.cost ? new Move<>(candidate, cluster, cost) : this;
    }
  }
}
