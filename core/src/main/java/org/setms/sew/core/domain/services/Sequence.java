package org.setms.sew.core.domain.services;

import org.setms.sew.core.domain.model.sdlc.Pointer;

public record Sequence(Pointer first, Pointer last) {}
