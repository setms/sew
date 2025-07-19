package org.setms.sew.core.domain.model.dsm;

import static lombok.AccessLevel.PROTECTED;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;

public abstract class BaseClusteringAlgorithm<E> implements ClusteringAlgorithm<E> {

  private final Random random = new SecureRandom();
  private DesignStructureMatrix<E> dsm;

  @Getter(PROTECTED)
  private List<E> elements;

  @Getter(PROTECTED)
  private Clusters<E> clusters;

  private Clusters<E> independentClusters;

  @Override
  public Set<Cluster<E>> apply(DesignStructureMatrix<E> dsm) {
    this.dsm = removeIndependentElementsFrom(dsm);
    this.elements = new ArrayList<>(this.dsm.getElements());
    this.clusters = new Clusters<>(elements);
    var independents = new LinkedHashSet<>(dsm.getElements());
    elements.forEach(independents::remove);
    this.independentClusters = new Clusters<>(independents);

    return findClusters();
  }

  protected DesignStructureMatrix<E> removeIndependentElementsFrom(
      DesignStructureMatrix<E> source) {
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

  private Set<Cluster<E>> findClusters() {
    optimizeClusters();

    var result = independentClusters.all();
    result.addAll(clusters.all());
    return result;
  }

  protected abstract void optimizeClusters();

  protected double random() {
    return random.nextDouble();
  }

  protected Double weight(E from, E to) {
    return dsm.getWeight(from, to).orElse(0.0);
  }

  protected Collection<Dependency<E>> getDependencies() {
    return dsm.getDependencies();
  }

  protected E randomElement() {
    return randomElement(getElements());
  }

  protected E randomElement(Collection<E> candidates) {
    return new ArrayList<>(candidates).get(random.nextInt(candidates.size()));
  }

  protected Cluster<E> clusterOf(E element) {
    return getClusters().get(element);
  }
}
