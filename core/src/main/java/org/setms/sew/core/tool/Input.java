package org.setms.sew.core.tool;

import org.setms.sew.core.format.Format;
import org.setms.sew.core.schema.NamedObject;

public record Input<T extends NamedObject>(String name, Glob glob, Format format, Class<T> type) {}
