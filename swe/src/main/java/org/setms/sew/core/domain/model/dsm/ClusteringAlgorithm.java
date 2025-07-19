package org.setms.sew.core.domain.model.dsm;

import java.util.Set;
import java.util.function.Function;

public interface ClusteringAlgorithm<E>
    extends Function<DesignStructureMatrix<E>, Set<Cluster<E>>> {}
