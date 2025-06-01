package org.setms.sew.core.domain.model.dsm;

public record Dependency<E>(E from, E to, double weight) {}
