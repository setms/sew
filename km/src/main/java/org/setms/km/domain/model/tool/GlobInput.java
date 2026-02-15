package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.initLower;

import java.util.Objects;
import java.util.Optional;
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
    this(toName(type), Glob.of(path, extension), format, type);
  }

  private static String toName(Class<?> type) {
    return initLower(new English().plural(type.getSimpleName()));
  }

  public GlobInput(Glob glob, Format format, Class<T> type) {
    this(toName(type), glob, format, type);
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

  @Override
  public boolean equals(Object other) {
    if (other
        instanceof
        GlobInput<?>(String thatName, Glob thatGlob, Format thatFormat, Class<?> thatType)) {
      return Objects.equals(this.name, thatName)
          && Objects.equals(this.glob, thatGlob)
          && Objects.equals(typeOf(this.format), typeOf(thatFormat))
          && Objects.equals(this.type, thatType);
    }
    return false;
  }

  private String typeOf(Format format) {
    return Optional.ofNullable(format).map(Object::getClass).map(Class::getName).orElse(null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, glob, typeOf(format), type);
  }
}
