package org.setms.sew.core.domain.model.tool;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.nlp.English;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.inbound.format.sal.SalFormat;

public record Input<T extends NamedObject>(String name, Glob glob, Format format, Class<T> type) {

  public Input(String path, Class<T> type) {
    this(path, new SalFormat(), type);
  }

  public Input(String path, Format format, Class<T> type) {
    this(path, format, type, initLower(type.getSimpleName()));
  }

  public Input(String path, Format format, Class<T> type, String extension) {
    this(
        initLower(new English().plural(type.getSimpleName())),
        new Glob(path, "**/*.%s".formatted(extension)),
        format,
        type);
  }
}
