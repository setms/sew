package org.setms.sew.core.domain.model.dsm;

import static java.util.Arrays.asList;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import lombok.Getter;

public class Cluster<E> extends TreeSet<E> {

  @Getter private final Set<Cluster<E>> dependencies = new LinkedHashSet<>();
  private final UUID id = UUID.randomUUID();

  @SafeVarargs
  public Cluster(E... elements) {
    this.addAll(asList(elements));
  }

  public void addDependency(Cluster<E> dependency) {
    dependencies.add(dependency);
  }

  // Overriding equals() and hasCode() to give the cluster its own identity, separate from its
  // elements
  @Override
  public boolean equals(Object other) {
    if (other instanceof Cluster<?> that) {
      return this.id.equals(that.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
