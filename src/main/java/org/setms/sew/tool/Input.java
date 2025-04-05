package org.setms.sew.tool;

import org.setms.sew.format.Format;
import org.setms.sew.schema.NamedObject;

public record Input<T extends NamedObject>(String name, Glob glob, Format format, Class<T> type) {}
