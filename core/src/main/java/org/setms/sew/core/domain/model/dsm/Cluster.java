package org.setms.sew.core.domain.model.dsm;

import static java.util.Arrays.asList;

import java.util.TreeSet;

public class Cluster<E> extends TreeSet<E> {

  @SafeVarargs
  public Cluster(E... elements) {
    this.addAll(asList(elements));
  }
}
