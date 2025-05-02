package org.setms.sew.core.domain.model.tool;

import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

public record Input<T extends NamedObject>(String name, Glob glob, Format format, Class<T> type) {}
