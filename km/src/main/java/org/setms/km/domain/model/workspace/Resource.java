package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Resource<T extends Resource<T>> {

  String SEPARATOR = "/";

  String name();

  default String path() {
    return "%s%s%s".formatted(parent().map(Resource::path).orElse(""), SEPARATOR, name());
  }

  Optional<T> parent();

  @SuppressWarnings("unchecked")
  default T root() {
    return parent().map(Resource::root).orElse((T) this);
  }

  List<T> children();

  default Optional<T> select(String path) {
    var index = path.indexOf(SEPARATOR);
    if (index < 0) {
      return childNamed(path);
    }
    if (index == 0) {
      index = path.indexOf(SEPARATOR, 1);
      if (index < 0) {
        return Optional.empty();
      }
      var rootName = path.substring(1, index);
      var remainder = path.substring(index + 1);
      return Optional.of(root())
          .filter(r -> r.name().equals(rootName))
          .flatMap(r -> select(remainder));
    }
    var name = path.substring(0, index);
    var remainder = path.substring(index + 1);
    return childNamed(name).flatMap(c -> c.select(remainder));
  }

  default Optional<T> childNamed(String name) {
    return children().stream().filter(c -> c.name().equals(name)).findFirst();
  }

  Collection<T> matching(Glob glob);

  InputStream readFrom() throws IOException;

  OutputStream writeTo() throws IOException;

  void delete() throws IOException;
}
