package org.setms.sew.core.domain.model.dsm;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Clusters<E> {

  private final Map<E, Cluster<E>> clustersByElement = new HashMap<>();

  public Clusters(Collection<E> elements) {
    elements.forEach(e -> clustersByElement.put(e, new Cluster<>(e)));
  }

  public Cluster<E> get(E element) {
    return clustersByElement.get(element);
  }

  public void move(E element, Cluster<E> target) {
    clustersByElement.get(element).remove(element);
    clustersByElement.put(element, target);
    target.add(element);
  }

  public Set<Cluster<E>> all() {
    return new LinkedHashSet<>(clustersByElement.values());
  }
}
