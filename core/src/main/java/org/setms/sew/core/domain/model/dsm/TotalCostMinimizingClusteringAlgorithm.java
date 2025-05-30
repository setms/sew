package org.setms.sew.core.domain.model.dsm;

import static java.util.function.Predicate.not;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public class TotalCostMinimizingClusteringAlgorithm<E> {

  private static final double BID_EXPONENT = 1;
  private static final double DEPENDENCY_EXPONENT = 5;
  private static final double SIZE_EXPONENT = 3;
  private static final int TIMES = 4;
  private static final double ACCEPT_PROBABILITY = 0.3;
  private static final int MAX_STALLED = 5;

  private final Random random = new SecureRandom();
  private final DesignStructureMatrix<E> dsm;
  private final List<E> elements;
  private final Clusters<E> clusters;
  private final Clusters<E> independentClusters;
  private double innerCost;
  private double outerCost;

  public TotalCostMinimizingClusteringAlgorithm(DesignStructureMatrix<E> dsm) {
    this.dsm = removeIndependentElementsFrom(dsm);
    this.elements = new ArrayList<>(this.dsm.getElements());
    this.clusters = new Clusters<>(elements);
    var independents = new LinkedHashSet<>(dsm.getElements());
    elements.forEach(independents::remove);
    this.independentClusters = new Clusters<>(independents);
  }

  private DesignStructureMatrix<E> removeIndependentElementsFrom(DesignStructureMatrix<E> source) {
    return source.without(
        source.getElements().stream().filter(hasNoDependenciesIn(source)).toList());
  }

  private Predicate<? super E> hasNoDependenciesIn(DesignStructureMatrix<E> dsm) {
    return e -> {
      for (var other : dsm.getElements()) {
        if (dsm.getWeight(e, other).isPresent() || dsm.getWeight(other, e).isPresent()) {
          return false;
        }
      }
      return true;
    };
  }

  public Set<Cluster<E>> findClusters() {
    var time = System.currentTimeMillis();
    initializeCosts();
    var numIterations = 0;
    var totalTimes = elements.size() * TIMES;
    var stalledCount = 0;
    while (stalledCount < MAX_STALLED) {
      System.out.printf(
          "Current costs: %,.0f + %,.0f = %,.0f%n", innerCost, outerCost, innerCost + outerCost);
      var improvedCount = 0;
      var candidates = new ArrayList<>(elements);
      for (var i = 0; i < totalTimes; i++) {
        if (candidates.isEmpty()) {
          System.out.println("No more candidates for improvement, terminating");
          stalledCount = MAX_STALLED;
          improvedCount = 1;
          break;
        }
        numIterations++;
        var saveInnerCost = innerCost;
        var saveOuterCost = outerCost;
        var element = randomElement(candidates);
        var fromCluster = clusters.get(element);
        System.out.printf("%nTrying to move %s from %s%n", element, fromCluster);
        var toCluster = bid(element);
        if (fromCluster.equals(toCluster)) {
          System.out.println("Current cluster is best");
          candidates.remove(element);
          System.out.printf("Remaining candidates: %s%n", candidates);
          continue;
        }
        System.out.printf("Best target cluster: %s%n", toCluster);
        clusters.move(element, toCluster);
        updateCosts(element, fromCluster, toCluster);
        System.out.printf(
            "Costs after move: %,.0f + %,.0f = %,.0f%n",
            innerCost, outerCost, innerCost + outerCost);

        var relativeCostDecrease =
            (innerCost + outerCost - saveInnerCost - saveOuterCost)
                / (saveInnerCost + saveOuterCost);
        if (relativeCostDecrease < 0) {
          System.out.printf(
              "Decreased cost by %.2f%%: accepting move%n", -100 * relativeCostDecrease);
          candidates = new ArrayList<>(elements);
          improvedCount++;
        } else if (shouldAcceptAnyway(relativeCostDecrease)) {
          System.out.printf(
              "Increased cost by %.2f%%, but keeping move anyway%n", 100 * relativeCostDecrease);
          candidates = new ArrayList<>(elements);
        } else {
          System.out.println("Not an improvement, reverting");
          clusters.move(element, fromCluster);
          innerCost = saveInnerCost;
          outerCost = saveOuterCost;
        }
      }
      if (improvedCount == 0) {
        stalledCount++;
        System.out.printf("%nNo improvement #%d%n%n", stalledCount);
      }
    }
    time = System.currentTimeMillis() - time;
    System.out.printf(
        "Clustering complete in %d iterations; took %s%n",
        numIterations, time < 1000 ? time + "ms" : Duration.ofMillis(time));
    var result = independentClusters.all();
    result.addAll(clusters.all());
    return result;
  }

  private void initializeCosts() {
    var outerSizePenalty = sizePenalty(elements.size());
    for (var e1 : elements) {
      for (var e2 : elements) {
        if (e1 == e2) {
          continue;
        }
        var weight = weightBetween(e1, e2);
        var c1 = clusters.get(e1);
        var c2 = clusters.get(e2);
        if (c1 == c2) {
          innerCost += weight * sizePenalty(c1.size());
        } else {
          outerCost += weight * outerSizePenalty;
        }
      }
    }
  }

  private double weightBetween(E e1, E e2) {
    return dsm.getWeight(e1, e2).orElse(0.0) + dsm.getWeight(e2, e1).orElse(0.0);
  }

  private double sizePenalty(int size) {
    return Math.pow(size, SIZE_EXPONENT);
  }

  private E randomElement(List<E> candidates) {
    return candidates.get(random.nextInt(candidates.size()));
  }

  private Cluster<E> bid(E element) {
    Cluster<E> result = null;
    var bestBid = -0.1;
    for (var cluster : clusters.all()) {
      var bid =
          cluster.stream()
              .filter(not(element::equals))
              .mapToDouble(e -> dsm.getWeight(element, e).orElse(0.0))
              .sum();
      bid = Math.pow(bid, DEPENDENCY_EXPONENT) / Math.pow(cluster.size(), BID_EXPONENT);
      if (bid > bestBid) {
        bestBid = bid;
        result = cluster;
      }
    }
    return result;
  }

  private void updateCosts(E element, Cluster<E> from, Cluster<E> to) {
    var fromSizePenalty = sizePenalty(from.size());
    var dsmSizePenalty = sizePenalty(dsm.getElements().size());
    for (var e : from) {
      var oldInner = weightBetween(element, e);
      innerCost -= oldInner * fromSizePenalty;
      outerCost += oldInner * dsmSizePenalty;
    }
    var toSizePenalty = sizePenalty(to.size());
    for (var e : to) {
      var oldOuter = weightBetween(element, e);
      innerCost += oldOuter * toSizePenalty;
      outerCost -= oldOuter * dsmSizePenalty;
    }
  }

  private boolean shouldAcceptAnyway(double deterioration) {
    return random.nextDouble() * (1 - deterioration) > 1 - ACCEPT_PROBABILITY;
  }
}
