package org.setms.swe.domain.model.dsm;

public record Dependency<E>(E from, E to, double weight) {}
