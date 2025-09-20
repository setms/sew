package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.initLower;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.workspace.Glob;

public record GlobInput<T extends Artifact>(String name, Glob glob, Format format, Class<T> type)
    implements Input<T> {

  public GlobInput(String path, Format format, Class<T> type) {
    this(path, format, type, initLower(type.getSimpleName()));
  }

  public GlobInput(String path, Format format, Class<T> type, String extension) {
    this(
        initLower(new English().plural(type.getSimpleName())),
        Glob.of(path, extension),
        format,
        type);
  }

  @Override
  public boolean targets(Artifact artifact) {
    return type.equals(artifact.getClass());
  }

  @Override
  public boolean matches(String path) {
    return glob.matches(path);
  }

  @Override
  public String path() {
    return glob.path();
  }

  @Override
  public String extension() {
    return glob.extension();
  }
}
