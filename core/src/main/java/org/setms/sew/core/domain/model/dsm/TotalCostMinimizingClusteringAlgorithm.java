package org.setms.sew.core.domain.model.dsm;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TotalCostMinimizingClusteringAlgorithm<E> {

  private static final double BID_EXPONENT = 1;
  private static final double DEPENDENCY_EXPONENT = 5;
  private static final double SIZE_EXPONENT = 3;
  private static final int TIMES = 4;
  private static final int ACCEPT_TIMES = 5;

  private final Random random = new SecureRandom();
  private final DesignStructureMatrix<E> dsm;
  private final List<E> elements;
  private final Clusters<E> clusters;
  private double innerCost;
  private double outerCost;

  public TotalCostMinimizingClusteringAlgorithm(DesignStructureMatrix<E> dsm) {
    this.dsm = dsm;
    this.elements = new ArrayList<>(dsm.getElements());
    this.clusters = new Clusters<>(elements);
  }

  public Set<? extends Set<E>> findClusters() {
    initializeCosts();
    var totalCost = Double.MAX_VALUE;
    var totalTimes = elements.size() * TIMES;
    while (innerCost + outerCost < totalCost) {
      totalCost = innerCost + outerCost;
      for (var i = 0; i < totalTimes; i++) {
        var element = randomElement();
        var fromCluster = clusters.get(element);
        var toCluster = bid(element);
        clusters.move(element, toCluster);
        updateCosts(element, fromCluster, toCluster);
        if (innerCost + outerCost >= totalCost && !shouldAcceptAnyway()) {
          clusters.move(element, fromCluster);
          updateCosts(element, toCluster, fromCluster);
        }
      }
    }
    return clusters.all();
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

  private E randomElement() {
    return elements.get(random.nextInt(elements.size()));
  }

  private Cluster<E> bid(E element) {
    Cluster<E> result = null;
    var bestBid = -0.1;
    for (var cluster : clusters.all()) {
      var bid = 0.0;
      for (var e2 : cluster) {
        bid += dsm.getWeight(element, e2).orElse(0.0);
      }
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

  private boolean shouldAcceptAnyway() {
    return random.nextDouble() < 1.0 / ACCEPT_TIMES;
  }
}
