package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.initLower;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.nlp.English;

public record Input<T extends Artifact>(String name, Glob glob, Format format, Class<T> type) {

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
