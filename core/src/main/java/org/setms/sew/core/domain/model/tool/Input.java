package org.setms.sew.core.domain.model.tool;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.inbound.format.sew.SewFormat;

public record Input<T extends NamedObject>(String name, Glob glob, Format format, Class<T> type) {

  public Input(String path, Class<T> type) {
    this(path, new SewFormat(), type);
  }

  public Input(String path, Format format, Class<T> type) {
    this(
        initLower(English.plural(type.getSimpleName())),
        new Glob(path, "**/*.%s".formatted(initLower(type.getSimpleName()))),
        format,
        type);
  }
}
